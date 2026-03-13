package com.adhithimaran.cultivate.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhithimaran.cultivate.data.model.Completion
import com.adhithimaran.cultivate.data.model.Habit
import com.adhithimaran.cultivate.data.model.HabitType
import com.adhithimaran.cultivate.data.repository.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Drives the Habit Detail screen for a single habit.
 *
 * The [habitId] is received from the navigation back-stack via [SavedStateHandle],
 * which Compose Navigation populates automatically from the route argument.
 *
 * Two Firestore listeners run concurrently:
 *  - [HabitRepository.getHabitFlow] — watches the single habit document
 *  - [HabitRepository.getCompletionsForHabit] — watches only this habit's completions
 *
 * Both are combined into a single [HabitDetailUiState] that the screen renders.
 * All stat calculations (streaks, completion rate, calendar days) are pure functions
 * with no side effects, making them easy to unit test.
 */
class HabitDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository = HabitRepository()
) : ViewModel() {

    // Pulled from the nav argument: "habit_detail/{habitId}"
    private val habitId: String = checkNotNull(savedStateHandle["habitId"])

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.getHabitFlow(habitId),
        repository.getCompletionsForHabit(habitId)
    ) { habit, completions ->

        // Habit was deleted while this screen was open — signal the screen to pop
        if (habit == null) return@combine HabitDetailUiState(habitDeleted = true)

        val completedDates = completions
            .map { it.toLocalDate() }
            .toSortedSet()

        HabitDetailUiState(
            isLoading        = false,
            habit            = habit,
            currentStreak    = currentStreak(completions, habit.type),
            longestStreak    = longestStreak(completions, habit.type),
            completionRate   = completionRate(completions, habit.type),
            completionLabel  = completionLabel(completions, habit.type),
            calendarDays     = buildCalendarDays(completedDates),
            recentCompletions = completions
                .sortedByDescending { it.completedAt.seconds }
                .take(30)
        )
    }
        .catch { e -> emit(HabitDetailUiState(isLoading = false, error = e.message)) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitDetailUiState()
        )

    // ── Streak calculations ───────────────────────────────────────────────────

    /**
     * Current streak: consecutive periods ending at the most recent period.
     * If the current period has no completion yet, we start counting from the
     * previous period so an in-progress streak doesn't show as broken.
     */
    private fun currentStreak(completions: List<Completion>, type: HabitType): Int {
        if (completions.isEmpty()) return 0
        return when (type) {
            HabitType.DAILY -> {
                val days = completions.map { it.toLocalDate() }.toSet()
                val today = LocalDate.now()
                var cursor = if (today in days) today else today.minusDays(1)
                var streak = 0
                while (cursor in days) { streak++; cursor = cursor.minusDays(1) }
                streak
            }
            HabitType.WEEKLY -> {
                val weeks = completions.map { it.toLocalDate().isoWeekKey() }.toSet()
                var cursor = LocalDate.now()
                // If this week has no entry yet, start from last week
                if (cursor.isoWeekKey() !in weeks) cursor = cursor.minusWeeks(1)
                var streak = 0
                while (cursor.isoWeekKey() in weeks) { streak++; cursor = cursor.minusWeeks(1) }
                streak
            }
            HabitType.MONTHLY -> {
                val months = completions.map { it.toLocalDate().monthKey() }.toSet()
                var cursor = LocalDate.now()
                if (cursor.monthKey() !in months) cursor = cursor.minusMonths(1)
                var streak = 0
                while (cursor.monthKey() in months) { streak++; cursor = cursor.minusMonths(1) }
                streak
            }
        }
    }

    /**
     * Longest streak ever: walks the full completion history and finds the
     * longest unbroken run of consecutive periods.
     */
    private fun longestStreak(completions: List<Completion>, type: HabitType): Int {
        if (completions.isEmpty()) return 0
        return when (type) {
            HabitType.DAILY -> {
                val days = completions.map { it.toLocalDate() }.toSortedSet()
                var longest = 0; var current = 0; var prev: LocalDate? = null
                for (day in days) {
                    current = if (prev != null && day == prev.plusDays(1)) current + 1 else 1
                    if (current > longest) longest = current
                    prev = day
                }
                longest
            }
            HabitType.WEEKLY -> {
                val weeks = completions.map { it.toLocalDate().isoWeekKey() }.toSortedSet()
                var longest = 0; var current = 0; var prev: String? = null
                for (week in weeks) {
                    current = if (prev != null && isNextWeek(prev, week)) current + 1 else 1
                    if (current > longest) longest = current
                    prev = week
                }
                longest
            }
            HabitType.MONTHLY -> {
                val months = completions.map { it.toLocalDate().monthKey() }.toSortedSet()
                var longest = 0; var current = 0; var prev: String? = null
                for (month in months) {
                    current = if (prev != null && isNextMonth(prev, month)) current + 1 else 1
                    if (current > longest) longest = current
                    prev = month
                }
                longest
            }
        }
    }

    // ── Completion rate ───────────────────────────────────────────────────────

    /**
     * Returns a 0.0–1.0 Float representing how often the habit was completed
     * in the current period window (month for daily, year for weekly/monthly).
     */
    private fun completionRate(completions: List<Completion>, type: HabitType): Float {
        val today = LocalDate.now()
        return when (type) {
            HabitType.DAILY -> {
                // Unique days with a completion this calendar month
                val thisMonth    = YearMonth.now()
                val daysElapsed  = today.dayOfMonth          // days so far in the month
                val doneThisMonth = completions
                    .map { it.toLocalDate() }
                    .filter { YearMonth.from(it) == thisMonth }
                    .toSet()
                    .size
                if (daysElapsed == 0) 0f else doneThisMonth.toFloat() / daysElapsed
            }
            HabitType.WEEKLY -> {
                // Unique weeks (ISO) with a completion this calendar year
                val weeksElapsed = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                val doneThisYear = completions
                    .map { it.toLocalDate() }
                    .filter { it.year == today.year }
                    .map { it.isoWeekKey() }
                    .toSet()
                    .size
                if (weeksElapsed == 0) 0f else doneThisYear.toFloat() / weeksElapsed
            }
            HabitType.MONTHLY -> {
                // Unique months with a completion this calendar year
                val monthsElapsed = today.monthValue
                val doneThisYear  = completions
                    .map { it.toLocalDate() }
                    .filter { it.year == today.year }
                    .map { it.monthKey() }
                    .toSet()
                    .size
                if (monthsElapsed == 0) 0f else doneThisYear.toFloat() / monthsElapsed
            }
        }
    }

    /**
     * Human-readable completion rate string, e.g. "18 of 28 days this month".
     */
    private fun completionLabel(completions: List<Completion>, type: HabitType): String {
        val today = LocalDate.now()
        return when (type) {
            HabitType.DAILY -> {
                val thisMonth    = YearMonth.now()
                val daysElapsed  = today.dayOfMonth
                val done = completions
                    .map { it.toLocalDate() }
                    .filter { YearMonth.from(it) == thisMonth }
                    .toSet().size
                "$done of $daysElapsed days this month"
            }
            HabitType.WEEKLY -> {
                val weeksElapsed = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                val done = completions
                    .map { it.toLocalDate() }
                    .filter { it.year == today.year }
                    .map { it.isoWeekKey() }
                    .toSet().size
                "$done of $weeksElapsed weeks this year"
            }
            HabitType.MONTHLY -> {
                val monthsElapsed = today.monthValue
                val done = completions
                    .map { it.toLocalDate() }
                    .filter { it.year == today.year }
                    .map { it.monthKey() }
                    .toSet().size
                "$done of $monthsElapsed months this year"
            }
        }
    }

    // ── Calendar grid builder ─────────────────────────────────────────────────

    /**
     * Builds a [CalendarDay] list representing the current calendar month.
     *
     * The list always starts on Monday of the week containing the 1st, padded
     * with [CalendarDay.empty] cells so the grid is always a multiple of 7.
     * Each day knows whether it has a completion, whether it's today, and
     * whether it's in the current month or a padding cell.
     *
     * Only used for [HabitType.DAILY] habits — weekly/monthly habits use the
     * recent completions list instead.
     */
    private fun buildCalendarDays(completedDates: Set<LocalDate>): List<CalendarDay> {
        val today     = LocalDate.now()
        val yearMonth = YearMonth.now()
        val firstDay  = yearMonth.atDay(1)
        val lastDay   = yearMonth.atEndOfMonth()

        // Pad the start so the grid begins on Monday
        val startPadding = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val days = mutableListOf<CalendarDay>()

        // Leading empty cells
        repeat(startPadding) { days.add(CalendarDay.empty()) }

        // Actual month days
        var cursor = firstDay
        while (!cursor.isAfter(lastDay)) {
            days.add(
                CalendarDay(
                    date        = cursor,
                    isInMonth   = true,
                    isToday     = cursor == today,
                    isFuture    = cursor.isAfter(today),
                    isCompleted = cursor in completedDates
                )
            )
            cursor = cursor.plusDays(1)
        }

        // Trailing empty cells to complete the last row
        val remainder = days.size % 7
        if (remainder != 0) repeat(7 - remainder) { days.add(CalendarDay.empty()) }

        return days
    }

    // ── Small pure helpers ────────────────────────────────────────────────────

    /** Converts a Firestore [Completion] timestamp to a [LocalDate] in the device's zone. */
    private fun Completion.toLocalDate(): LocalDate =
        completedAt.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    /** Unique key for the ISO week containing this date: "2025-W12". */
    private fun LocalDate.isoWeekKey(): String {
        val week = get(WeekFields.ISO.weekOfWeekBasedYear())
        val year = get(WeekFields.ISO.weekBasedYear())
        return "$year-W${week.toString().padStart(2, '0')}"
    }

    /** Unique key for the calendar month containing this date: "2025-03". */
    private fun LocalDate.monthKey(): String =
        "${year}-${monthValue.toString().padStart(2, '0')}"

    /**
     * Returns true if [next] is exactly the ISO week after [prev].
     * Both are formatted as "yyyy-Www".
     */
    private fun isNextWeek(prev: String, next: String): Boolean {
        val (py, pw) = prev.split("-W").map { it.toInt() }
        val (ny, nw) = next.split("-W").map { it.toInt() }
        return (ny == py && nw == pw + 1) || (ny == py + 1 && pw >= 52 && nw == 1)
    }

    /**
     * Returns true if [next] is exactly the calendar month after [prev].
     * Both are formatted as "yyyy-MM".
     */
    private fun isNextMonth(prev: String, next: String): Boolean {
        val (py, pm) = prev.split("-").map { it.toInt() }
        val (ny, nm) = next.split("-").map { it.toInt() }
        return (ny == py && nm == pm + 1) || (ny == py + 1 && pm == 12 && nm == 1)
    }

    companion object {
        /**
         * Explicit factory so the default [viewModel()] call can supply
         * [SavedStateHandle] without a no-arg constructor crash.
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val savedStateHandle = extras.createSavedStateHandle()
                return HabitDetailViewModel(savedStateHandle) as T
            }
        }
    }
}

// ── UI state models ───────────────────────────────────────────────────────────

/**
 * Complete snapshot of everything the Habit Detail screen needs to render.
 *
 * @property isLoading         True during the first Firestore fetch.
 * @property habit             The habit being shown (null until first emit).
 * @property currentStreak     Consecutive periods ending now.
 * @property longestStreak     Best ever consecutive-period run.
 * @property completionRate    0.0–1.0 Float for the progress arc/bar.
 * @property completionLabel   Human-readable rate, e.g. "18 of 28 days this month".
 * @property calendarDays      Grid cells for the monthly calendar (DAILY habits only).
 * @property recentCompletions Last 30 completions sorted newest-first, for the history list.
 * @property habitDeleted      True if the document was deleted while the screen was open.
 * @property error             Non-null if the Firestore Flow threw an exception.
 */
data class HabitDetailUiState(
    val isLoading         : Boolean          = true,
    val habit             : Habit?           = null,
    val currentStreak     : Int              = 0,
    val longestStreak     : Int              = 0,
    val completionRate    : Float            = 0f,
    val completionLabel   : String           = "",
    val calendarDays      : List<CalendarDay> = emptyList(),
    val recentCompletions : List<Completion> = emptyList(),
    val habitDeleted      : Boolean          = false,
    val error             : String?          = null
)

/**
 * Represents a single cell in the monthly calendar grid.
 *
 * @property date        The date this cell represents (null for padding cells).
 * @property isInMonth   False for leading/trailing padding cells.
 * @property isToday     True if this cell is today's date.
 * @property isFuture    True if this cell is after today — shown as dimmed and non-interactive.
 * @property isCompleted True if there is at least one completion on this date.
 */
data class CalendarDay(
    val date        : LocalDate?,
    val isInMonth   : Boolean,
    val isToday     : Boolean,
    val isFuture    : Boolean,
    val isCompleted : Boolean
) {
    companion object {
        /** A blank padding cell with no associated date. */
        fun empty() = CalendarDay(
            date        = null,
            isInMonth   = false,
            isToday     = false,
            isFuture    = false,
            isCompleted = false
        )
    }
}