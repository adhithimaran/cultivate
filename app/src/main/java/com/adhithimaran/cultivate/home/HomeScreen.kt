package com.adhithimaran.cultivate.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Home screen — the user's daily habit dashboard.
 *
 * Habits are grouped into three sections (Today / This Week / This Month).
 * Each card shows name, duration, streak, and a tap-to-complete check button.
 * Swipe a card left to delete the habit.
 *
 * All colors and text styles resolve through [MaterialTheme] so future changes
 * to Color.kt or Type.kt cascade here automatically.
 *
 * @param onSignOut   Called when the user taps the sign-out icon.
 * @param onAddHabit  Called when the user taps the FAB to open the Add Habit screen.
 * @param viewModel   Injected by [viewModel()] factory; override in tests.
 */
@Composable
fun HomeScreen(
    onSignOut  : () -> Unit,
    onAddHabit : () -> Unit,
    viewModel  : HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        when {
            // ── Loading splash ────────────────────────────────────────────────
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary
                )
            }

            // ── Error state ───────────────────────────────────────────────────
            state.error != null -> {
                Column(
                    modifier              = Modifier.align(Alignment.Center),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Text("😕", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text  = "Something went wrong.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text  = state.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Main content ──────────────────────────────────────────────────
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Header ────────────────────────────────────────────────
                    item {
                        HomeHeader(onSignOut = onSignOut)
                    }

                    // ── Today (Daily habits) ──────────────────────────────────
                    item {
                        SectionHeader(
                            title        = "Today",
                            emoji        = "☀️",
                            totalCount   = state.dailyHabits.size,
                            doneCount    = state.dailyHabits.count { it.isCompleted }
                        )
                    }
                    if (state.dailyHabits.isEmpty()) {
                        item { EmptySection(hint = "No daily habits yet — tap + to add one") }
                    } else {
                        items(state.dailyHabits, key = { it.habit.id }) { item ->
                            SwipeableHabitCard(
                                item      = item,
                                onCheck   = { viewModel.onCheckHabit(item.habit, item.isCompleted) },
                                onDelete  = { viewModel.onDeleteHabit(item.habit.id) },
                                modifier  = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // ── This Week (Weekly habits) ─────────────────────────────
                    item {
                        SectionHeader(
                            title      = "This Week",
                            emoji      = "📅",
                            totalCount = state.weeklyHabits.size,
                            doneCount  = state.weeklyHabits.count { it.isCompleted }
                        )
                    }
                    if (state.weeklyHabits.isEmpty()) {
                        item { EmptySection(hint = "No weekly habits yet — tap + to add one") }
                    } else {
                        items(state.weeklyHabits, key = { it.habit.id }) { item ->
                            SwipeableHabitCard(
                                item     = item,
                                onCheck  = { viewModel.onCheckHabit(item.habit, item.isCompleted) },
                                onDelete = { viewModel.onDeleteHabit(item.habit.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // ── This Month (Monthly habits) ───────────────────────────
                    item {
                        SectionHeader(
                            title      = "This Month",
                            emoji      = "🗓",
                            totalCount = state.monthlyHabits.size,
                            doneCount  = state.monthlyHabits.count { it.isCompleted }
                        )
                    }
                    if (state.monthlyHabits.isEmpty()) {
                        item { EmptySection(hint = "No monthly habits yet — tap + to add one") }
                    } else {
                        items(state.monthlyHabits, key = { it.habit.id }) { item ->
                            SwipeableHabitCard(
                                item     = item,
                                onCheck  = { viewModel.onCheckHabit(item.habit, item.isCompleted) },
                                onDelete = { viewModel.onDeleteHabit(item.habit.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Bottom padding so FAB doesn't cover the last card
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }

        // ── FAB (always visible above the list) ───────────────────────────────
        FloatingActionButton(
            onClick           = onAddHabit,
            modifier          = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            shape             = RoundedCornerShape(16.dp),
            containerColor    = MaterialTheme.colorScheme.primary,
            contentColor      = MaterialTheme.colorScheme.onPrimary,
            elevation         = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add habit")
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

/**
 * Top header: time-based greeting, today's date, and a sign-out icon.
 */
@Composable
private fun HomeHeader(onSignOut: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = greeting(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "🌱", fontSize = 26.sp)
            }
            Text(
                text  = todayLabel(),
                // bodyMedium = Normal 14sp in Type.kt
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Sign-out tucked in the corner — visible but not prominent
        IconButton(onClick = onSignOut) {
            Icon(
                imageVector        = Icons.Outlined.ExitToApp,
                contentDescription = "Sign out",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Section divider showing the period label and a "done/total" completion badge.
 */
@Composable
private fun SectionHeader(
    title      : String,
    emoji      : String,
    totalCount : Int,
    doneCount  : Int
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .padding(bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text  = title,
                // titleLarge = SemiBold 22sp in Type.kt
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (totalCount > 0) {
            // "2 / 3" badge — all green when everything is done
            val allDone    = doneCount == totalCount
            val badgeColor = if (allDone) MaterialTheme.colorScheme.primary
            else        MaterialTheme.colorScheme.surfaceVariant
            val textColor  = if (allDone) MaterialTheme.colorScheme.onPrimary
            else        MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                color  = badgeColor,
                shape  = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text     = "$doneCount / $totalCount",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Friendly placeholder shown inside an empty section.
 */
@Composable
private fun EmptySection(hint: String) {
    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        contentAlignment  = Alignment.CenterStart
    ) {
        Text(
            text  = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Wraps a [HabitCard] in a [SwipeToDismissBox] that reveals a red delete background
 * when the user swipes left. Confirming the swipe calls [onDelete].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHabitCard(
    item     : HabitUiItem,
    onCheck  : () -> Unit,
    onDelete : () -> Unit,
    modifier : Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true   // confirm the dismiss — item disappears when Firestore Flow updates
            } else false
        },
        // Require the user to swipe at least 40% of the card width before confirming
        positionalThreshold = { totalWidth -> totalWidth * 0.4f }
    )

    SwipeToDismissBox(
        state                    = dismissState,
        modifier                 = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent        = { DeleteBackground(isRevealed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) }
    ) {
        HabitCard(item = item, onCheck = onCheck)
    }
}

/**
 * The red background revealed beneath a habit card while the user is swiping left.
 * Animates from transparent to errorContainer as the swipe progresses.
 */
@Composable
private fun DeleteBackground(isRevealed: Boolean) {
    val bgColor by animateColorAsState(
        targetValue   = if (isRevealed) MaterialTheme.colorScheme.errorContainer
        else            Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label         = "deleteBg"
    )
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(end = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isRevealed) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = "Delete habit",
                tint               = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * A single habit row: name, chips (duration + streak), and an animated check button.
 *
 * - Background animates from [surface] → [surfaceVariant] when completed.
 * - Name gets a strikethrough when completed so it's immediately scannable.
 * - Check circle fills with [primary] color on completion.
 */
@Composable
private fun HabitCard(
    item    : HabitUiItem,
    onCheck : () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue   = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant
        else                  MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 400),
        label         = "cardBg"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Left: habit info ──────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = item.habit.name,
                    // titleMedium = Medium 16sp in Type.kt
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough
                        else                  TextDecoration.None
                    ),
                    color     = if (item.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    InfoChip(text = "⏱ ${item.habit.durationMinutes} min")
                    if (item.streak > 0) {
                        InfoChip(text = "🔥 ${item.streak}")
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Right: check button ───────────────────────────────────────────
            CheckCircle(
                isCompleted = item.isCompleted,
                isLoading   = item.isLoading,
                onClick     = onCheck
            )
        }
    }
}

/**
 * Animated circular check button.
 *
 * - Idle: transparent fill, [outline]-colored border.
 * - Completed: [primary]-colored fill + checkmark icon, animates over 300ms.
 * - Loading: shows a [CircularProgressIndicator] while the Firestore write is in-flight.
 */
@Composable
private fun CheckCircle(
    isCompleted : Boolean,
    isLoading   : Boolean,
    onClick     : () -> Unit
) {
    val fillColor by animateColorAsState(
        targetValue   = if (isCompleted) MaterialTheme.colorScheme.primary
        else             Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label         = "checkFill"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isCompleted) MaterialTheme.colorScheme.primary
        else             MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 300),
        label         = "checkBorder"
    )

    Box(
        modifier         = Modifier
            .size(36.dp)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .background(color = fillColor, shape = CircleShape)
            .clip(CircleShape)
            .clickable(enabled = !isLoading && !isCompleted, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // AnimatedContent swaps between loading spinner and checkmark icon
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "checkContent"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            } else if (isCompleted) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Small pill chip used to display duration and streak below the habit name.
 */
@Composable
private fun InfoChip(text: String) {
    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        shape  = RoundedCornerShape(8.dp)
    ) {
        Text(
            text     = text,
            // labelMedium = Medium 12sp in Type.kt
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ── Pure helper functions ─────────────────────────────────────────────────────

/** Returns a time-appropriate greeting string, e.g. "Good morning". */
private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else      -> "Good night"
    }
}

/** Returns today's date formatted as "Thursday, March 12". */
private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))