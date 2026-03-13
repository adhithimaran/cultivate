package com.adhithimaran.cultivate.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhithimaran.cultivate.data.model.Completion
import com.adhithimaran.cultivate.data.model.HabitType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Habit Detail / History screen.
 *
 * Shows:
 *  - Habit name, type chip, and duration chip in the header
 *  - Three stat cards: current streak, longest streak, completion rate
 *  - Monthly calendar grid (DAILY habits) or recent history list (WEEKLY / MONTHLY)
 *  - Full recent completions list (last 30) with timestamps
 *
 * All colors and text styles resolve through [MaterialTheme] — no hardcoded values.
 *
 * @param onNavigateBack Called when the user taps Back, or when the habit is deleted
 *                       while this screen is open.
 * @param viewModel      Injected by the standard [viewModel()] factory.
 *                       [SavedStateHandle] is populated automatically by Compose
 *                       Navigation from the "habitId" route argument.
 */
@Composable
fun HabitDetailScreen(
    onNavigateBack : () -> Unit,
    viewModel      : HabitDetailViewModel = viewModel(factory = HabitDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    // If the habit was deleted in Firestore while we were on this screen, pop back
    LaunchedEffect(state.habitDeleted) {
        if (state.habitDeleted) onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        when {

            // ── Loading ───────────────────────────────────────────────────────
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary
                )
            }

            // ── Error ─────────────────────────────────────────────────────────
            state.error != null -> {
                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "Something went wrong",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = state.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            state.habit != null -> {
                val habit = state.habit!!

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {

                    // ── Top bar ───────────────────────────────────────────────
                    item {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint               = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // ── Header: name + meta chips ─────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                text  = habit.name,
                                // headlineMedium = SemiBold 28sp
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetaChip(
                                    text  = habit.type.name
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    color = MaterialTheme.colorScheme.primary
                                )
                                MetaChip(
                                    text  = "${habit.durationMinutes} min",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // ── Stats row ─────────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                value    = "${state.currentStreak}",
                                label    = "Current\nStreak",
                                unit     = periodUnit(habit.type)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                value    = "${state.longestStreak}",
                                label    = "Longest\nStreak",
                                unit     = periodUnit(habit.type)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                value    = "${(state.completionRate * 100).toInt()}%",
                                label    = "Completion\nRate",
                                unit     = ""
                            )
                        }
                    }

                    // ── Completion rate bar ───────────────────────────────────
                    item {
                        CompletionRateBar(
                            rate  = state.completionRate,
                            label = state.completionLabel
                        )
                    }

                    // ── Calendar (DAILY only) ─────────────────────────────────
                    if (habit.type == HabitType.DAILY && state.calendarDays.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionTitle(text = "This Month")
                            MonthCalendar(days = state.calendarDays)
                        }
                    }

                    // ── Recent completions list ───────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionTitle(text = "Recent History")
                    }

                    if (state.recentCompletions.isEmpty()) {
                        item {
                            Text(
                                text     = "No completions recorded yet.",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        item {
                            Card(
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape     = RoundedCornerShape(16.dp),
                                colors    = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                state.recentCompletions.forEachIndexed { index, completion ->
                                    CompletionRow(completion = completion)
                                    if (index < state.recentCompletions.lastIndex) {
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            color     = MaterialTheme.colorScheme.surfaceVariant,
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(48.dp)) }
                }
            }
        }
    }
}

// ── Calendar ──────────────────────────────────────────────────────────────────

/**
 * A full monthly calendar grid. Days of the week are labelled Mon–Sun.
 * Each cell is a small circle:
 *  - Completed day: filled with [primary] color
 *  - Today (not yet completed): outlined with [primary]
 *  - Future: dimmed, non-interactive
 *  - Past with no completion: neutral surface dot
 */
@Composable
private fun MonthCalendar(days: List<CalendarDay>) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Day-of-week header row
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text      = label,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    // labelSmall = Medium 11sp
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar day cells — chunked into rows of 7
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                week.forEach { day ->
                    CalendarCell(
                        day      = day,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * A single calendar cell. Uses [aspectRatio(1f)] inside a [weight(1f)] parent
 * so every cell is always a perfect square regardless of screen width.
 */
@Composable
private fun CalendarCell(day: CalendarDay, modifier: Modifier = Modifier) {
    Box(
        modifier          = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment  = Alignment.Center
    ) {
        when {
            // Empty padding cell — render nothing
            !day.isInMonth -> Unit

            // Completed day — filled primary circle
            day.isCompleted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = day.date!!.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Today, not yet completed — outlined circle
            day.isToday -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = day.date!!.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Future day — dimmed number, no circle
            day.isFuture -> {
                Text(
                    text  = day.date!!.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }

            // Past day, no completion — plain number
            else -> {
                Text(
                    text  = day.date!!.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ── Stat cards ────────────────────────────────────────────────────────────────

/**
 * A single stat card displaying a large [value], a two-line [label], and a
 * small [unit] suffix (e.g. "days" or "weeks"). The large value uses
 * [headlineSmall] (SemiBold 24sp) so the number reads at a glance.
 */
@Composable
private fun StatCard(
    value    : String,
    label    : String,
    unit     : String,
    modifier : Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = value,
                // headlineSmall = SemiBold 24sp
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (unit.isNotEmpty()) {
                Text(
                    text  = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text      = label,
                // bodySmall = Normal 12sp
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Completion rate bar ───────────────────────────────────────────────────────

/**
 * A card containing a labelled [LinearProgressIndicator] showing the completion
 * rate for the current period window.
 */
@Composable
private fun CompletionRateBar(rate: Float, label: String) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = label,
                    // bodyMedium = Normal 14sp
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress      = { rate },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color         = MaterialTheme.colorScheme.primary,
                trackColor    = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap     = StrokeCap.Round
            )
        }
    }
}

// ── Recent completions list ───────────────────────────────────────────────────

/**
 * A single row in the recent history list: a check icon, the formatted date,
 * and the time of the completion.
 */
@Composable
private fun CompletionRow(completion: Completion) {
    val date = completion.completedAt
        .toDate()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    val time = completion.completedAt
        .toDate()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalTime()

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text     = date.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = time.format(DateTimeFormatter.ofPattern("h:mm a")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

/** Section title label sitting above calendar and history sections. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text     = text,
        // titleMedium = Medium 16sp
        style    = MaterialTheme.typography.titleMedium,
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

/**
 * A small pill chip used in the header to show the habit type and duration.
 * [color] is used for both the background tint and the text, so it visually
 * links back to the same accent used in the Home screen section.
 */
@Composable
private fun MetaChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text     = text,
            // labelMedium = Medium 12sp
            style    = MaterialTheme.typography.labelMedium,
            color    = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Returns a readable period unit string for stat card subtitles. */
private fun periodUnit(type: HabitType): String = when (type) {
    HabitType.DAILY   -> "days"
    HabitType.WEEKLY  -> "weeks"
    HabitType.MONTHLY -> "months"
}