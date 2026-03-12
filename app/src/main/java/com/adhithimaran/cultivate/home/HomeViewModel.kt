package com.adhithimaran.cultivate.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhithimaran.cultivate.data.model.Completion
import com.adhithimaran.cultivate.data.model.Habit
import com.adhithimaran.cultivate.data.model.HabitType
import com.adhithimaran.cultivate.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Drives the Home screen by combining the habits Flow and completions Flow from
 * Firestore into a single [HomeUiState] the screen can render directly.
 *
 * No screen logic lives here — only data transformation and Firestore writes.
 */
class HomeViewModel(
    private val repository: HabitRepository = HabitRepository()
) : ViewModel() {

    /**
     * Tracks which habit IDs currently have an in-flight check write.
     * Used to show a per-card loading indicator while Firestore confirms the write.
     */
    private val _pendingChecks = MutableStateFlow<Set<String>>(emptySet())

    /**
     * The single source of truth for the Home screen.
     *
     * [combine] merges three reactive sources into one snapshot:
     * - habits list (real-time from Firestore)
     * - all completions (real-time from Firestore)
     * - in-flight check IDs (local state only)
     *
     * Whenever any of the three changes, the block re-runs and the screen recomposes.
     * [SharingStarted.WhileSubscribed] keeps the upstream Firestore listeners alive
     * for 5 seconds after the last subscriber disappears, avoiding a restart on
     * config change (e.g. screen rotation).
     */
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getHabitsFlow(),
        repository.getAllCompletionsFlow(),
        _pendingChecks
    ) { habits, completions, pendingIds ->

        // Group completions by habitId once so each buildItems call is O(1) per lookup
        val byHabit = completions.groupBy { it.habitId }

        fun buildItems(type: HabitType): List<HabitUiItem> = habits
            .filter { it.type == type }
            .map { habit ->
                val cs = byHabit[habit.id] ?: emptyList()
                HabitUiItem(
                    habit       = habit,
                    isCompleted = isCompletedThisPeriod(cs, type),
                    streak      = calculateStreak(cs, type),
                    isLoading   = habit.id in pendingIds
                )
            }
            // Show incomplete habits first so there's always something actionable at the top
            .sortedBy { it.isCompleted }

        HomeUiState(
            isLoading     = false,
            dailyHabits   = buildItems(HabitType.DAILY),
            weeklyHabits  = buildItems(HabitType.WEEKLY),
            monthlyHabits = buildItems(HabitType.MONTHLY)
        )
    }
        .catch { e -> emit(HomeUiState(isLoading = false, error = e.message)) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Marks a habit as complete for the current period.
     * No-ops if already completed or if a write is already in-flight,
     * preventing accidental double-taps from writing duplicate completions.
     */
    fun onCheckHabit(habit: Habit, alreadyCompleted: Boolean) {
        if (alreadyCompleted || habit.id in (_pendingChecks.value)) return
        viewModelScope.launch {
            _pendingChecks.update { it + habit.id }
            try {
                repository.addCompletion(habit.id, habit.type)
            } catch (_: Exception) {
                // Firestore write failed — the optimistic UI will revert naturally
                // because the Flow won't have updated. A production app would surface
                // a Snackbar here via a separate error channel.
            } finally {
                _pendingChecks.update { it - habit.id }
            }
        }
    }

    /**
     * Permanently deletes a habit from Firestore.
     * The LazyColumn item disappears automatically once the Firestore Flow emits
     * the updated list — no manual list manipulation needed.
     */
    fun onDeleteHabit(habitId: String) {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habitId)
            } catch (_: Exception) {
                // Same as above — a production app would surface an error here.
            }
        }
    }

    // ── Pure helpers (no side effects) ────────────────────────────────────────

    /**
     * Returns true if the habit has at least one completion in its current period.
     *
     * - DAILY  → completed today
     * - WEEKLY → completed any day in the current Mon–Sun week
     * - MONTHLY → completed any day in the current calendar month
     */
    private fun isCompletedThisPeriod(completions: List<Completion>, type: HabitType): Boolean {
        if (completions.isEmpty()) return false
        val today = LocalDate.now()
        return when (type) {
            HabitType.DAILY   -> completions.any { it.toLocalDate() == today }
            HabitType.WEEKLY  -> {
                val weekStart = today.with(DayOfWeek.MONDAY)
                completions.any { it.toLocalDate() >= weekStart }
            }
            HabitType.MONTHLY -> completions.any {
                val d = it.toLocalDate()
                d.year == today.year && d.month == today.month
            }
        }
    }

    /**
     * Calculates the current streak in whole periods (days / weeks / months).
     *
     * Algorithm: starting from today's period, walk backwards in time, counting
     * consecutive periods that have at least one completion. If today's period has
     * no completion yet, we still look one period back so an ongoing streak doesn't
     * show as broken partway through the day/week/month.
     *
     * Examples (DAILY):
     *   completions on Mon, Tue, Wed → streak = 3 on Wed, streak = 3 on Thu until Thu is done
     *   completions on Mon, Wed (gap on Tue) → streak = 1 on Wed
     */
    private fun calculateStreak(completions: List<Completion>, type: HabitType): Int {
        if (completions.isEmpty()) return 0

        return when (type) {
            HabitType.DAILY -> {
                // Unique dates with any completion
                val days = completions.map { it.toLocalDate() }.toSet()
                var streak = 0
                // Start from today; if today not yet complete, start from yesterday
                var cursor = LocalDate.now().let { if (it !in days) it.minusDays(1) else it }
                while (cursor in days) {
                    streak++
                    cursor = cursor.minusDays(1)
                }
                streak
            }
            HabitType.WEEKLY -> {
                val days = completions.map { it.toLocalDate() }.toSet()
                var streak = 0
                // Start of this week (Monday)
                var weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
                // If this week has no completion yet, start checking from last week
                if (days.none { it >= weekStart }) weekStart = weekStart.minusWeeks(1)
                while (days.any { it >= weekStart && it < weekStart.plusWeeks(1) }) {
                    streak++
                    weekStart = weekStart.minusWeeks(1)
                }
                streak
            }
            HabitType.MONTHLY -> {
                val days = completions.map { it.toLocalDate() }.toSet()
                var streak = 0
                // First day of this month
                var monthStart = LocalDate.now().withDayOfMonth(1)
                // If this month has no completion yet, start from last month
                if (days.none { it.year == monthStart.year && it.month == monthStart.month }) {
                    monthStart = monthStart.minusMonths(1)
                }
                while (days.any { it.year == monthStart.year && it.month == monthStart.month }) {
                    streak++
                    monthStart = monthStart.minusMonths(1)
                }
                streak
            }
        }
    }

    /** Converts a Firestore [Completion] timestamp to a [LocalDate] in the device's timezone. */
    private fun Completion.toLocalDate(): LocalDate =
        completedAt.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

// ── UI state models ───────────────────────────────────────────────────────────

/**
 * Complete snapshot of what the Home screen needs to render.
 * Habits are pre-split by type so the screen doesn't do any filtering itself.
 *
 * @property isLoading      True on first load before any Firestore data arrives.
 * @property dailyHabits    Habits with [HabitType.DAILY], sorted incomplete-first.
 * @property weeklyHabits   Habits with [HabitType.WEEKLY], sorted incomplete-first.
 * @property monthlyHabits  Habits with [HabitType.MONTHLY], sorted incomplete-first.
 * @property error          Non-null if the Firestore Flow threw; shown as an error state.
 */
data class HomeUiState(
    val isLoading     : Boolean           = true,
    val dailyHabits   : List<HabitUiItem> = emptyList(),
    val weeklyHabits  : List<HabitUiItem> = emptyList(),
    val monthlyHabits : List<HabitUiItem> = emptyList(),
    val error         : String?           = null
)

/**
 * A single row in the habit list — the [Habit] enriched with computed display data.
 *
 * @property habit        The raw habit data from Firestore.
 * @property isCompleted  True if the habit has been checked off in the current period.
 * @property streak       Number of consecutive periods this habit has been completed.
 * @property isLoading    True while the check-off write is in-flight; drives the spinner.
 */
data class HabitUiItem(
    val habit       : Habit,
    val isCompleted : Boolean,
    val streak      : Int,
    val isLoading   : Boolean = false
)