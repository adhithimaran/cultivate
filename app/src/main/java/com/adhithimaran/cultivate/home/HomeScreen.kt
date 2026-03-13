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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhithimaran.cultivate.data.model.HabitType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Home screen — the user's daily habit dashboard.
 *
 * @param onSignOut     Called when the user taps the sign-out icon.
 * @param onAddHabit    Called when the user taps the FAB.
 * @param onHabitClick  Called with the habit's Firestore ID when the user taps a card body.
 *                      Use this to navigate to HabitDetailScreen.
 * @param viewModel     Injected by [viewModel()] factory; override in tests.
 */
@Composable
fun HomeScreen(
    onSignOut    : () -> Unit,
    onAddHabit   : () -> Unit,
    onHabitClick : (habitId: String) -> Unit,
    viewModel    : HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary
                )
            }
            state.error != null -> {
                Column(
                    modifier            = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item { HomeHeader(onSignOut = onSignOut) }

                    item {
                        HabitSection(
                            title        = "Today",
                            icon         = Icons.Outlined.Star,
                            accentColor  = MaterialTheme.colorScheme.primary,
                            sectionBg    = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            totalCount   = state.dailyHabits.size,
                            doneCount    = state.dailyHabits.count { it.isCompleted },
                            items        = state.dailyHabits,
                            emptyHint    = "No daily habits yet — tap + to add one",
                            onCheck      = { item -> viewModel.onCheckHabit(item.habit, item.isCompleted) },
                            onDelete     = { item -> viewModel.onDeleteHabit(item.habit.id) },
                            onCardClick  = { item -> onHabitClick(item.habit.id) }
                        )
                    }

                    item {
                        HabitSection(
                            title        = "This Week",
                            icon         = Icons.Outlined.DateRange,
                            accentColor  = MaterialTheme.colorScheme.secondary,
                            sectionBg    = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                            totalCount   = state.weeklyHabits.size,
                            doneCount    = state.weeklyHabits.count { it.isCompleted },
                            items        = state.weeklyHabits,
                            emptyHint    = "No weekly habits yet — tap + to add one",
                            onCheck      = { item -> viewModel.onCheckHabit(item.habit, item.isCompleted) },
                            onDelete     = { item -> viewModel.onDeleteHabit(item.habit.id) },
                            onCardClick  = { item -> onHabitClick(item.habit.id) }
                        )
                    }

                    item {
                        HabitSection(
                            title        = "This Month",
                            icon         = Icons.Outlined.DateRange,
                            accentColor  = MaterialTheme.colorScheme.tertiary,
                            sectionBg    = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.03f),
                            totalCount   = state.monthlyHabits.size,
                            doneCount    = state.monthlyHabits.count { it.isCompleted },
                            items        = state.monthlyHabits,
                            emptyHint    = "No monthly habits yet — tap + to add one",
                            onCheck      = { item -> viewModel.onCheckHabit(item.habit, item.isCompleted) },
                            onDelete     = { item -> viewModel.onDeleteHabit(item.habit.id) },
                            onCardClick  = { item -> onHabitClick(item.habit.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick        = onAddHabit,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            shape          = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            elevation      = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add habit")
        }
    }
}

// ── Section ───────────────────────────────────────────────────────────────────

@Composable
private fun HabitSection(
    title       : String,
    icon        : ImageVector,
    accentColor : Color,
    sectionBg   : Color,
    totalCount  : Int,
    doneCount   : Int,
    items       : List<HabitUiItem>,
    emptyHint   : String,
    onCheck     : (HabitUiItem) -> Unit,
    onDelete    : (HabitUiItem) -> Unit,
    onCardClick : (HabitUiItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(sectionBg)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(20.dp)
                )
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (totalCount > 0) {
                val allDone = doneCount == totalCount
                Surface(
                    color = if (allDone) accentColor
                    else         MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text     = "$doneCount / $totalCount",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = if (allDone) MaterialTheme.colorScheme.onPrimary
                        else        MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Text(
                text     = emptyHint,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
            )
        } else {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    SwipeableHabitCard(
                        item        = item,
                        accentColor = accentColor,
                        onCheck     = { onCheck(item) },
                        onDelete    = { onDelete(item) },
                        onCardClick = { onCardClick(item) }
                    )
                }
            }
        }
    }
}

// ── Habit card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHabitCard(
    item        : HabitUiItem,
    accentColor : Color,
    onCheck     : () -> Unit,
    onDelete    : () -> Unit,
    onCardClick : () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange  = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { totalWidth -> totalWidth * 0.4f }
    )

    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent           = {
            DeleteBackground(
                isRevealed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            )
        }
    ) {
        HabitCard(
            item        = item,
            accentColor = accentColor,
            onCheck     = onCheck,
            onCardClick = onCardClick
        )
    }
}

@Composable
private fun DeleteBackground(isRevealed: Boolean) {
    val bg by animateColorAsState(
        targetValue   = if (isRevealed) MaterialTheme.colorScheme.errorContainer
        else            Color.Transparent,
        animationSpec = tween(200),
        label         = "deleteBg"
    )
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isRevealed) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = "Delete",
                tint               = MaterialTheme.colorScheme.error,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * A single habit card.
 *
 * The card body is clickable (navigates to detail). The check circle is a
 * separate independent click target so tapping the circle never triggers
 * navigation, and tapping anywhere else on the card never triggers a check.
 */
@Composable
private fun HabitCard(
    item        : HabitUiItem,
    accentColor : Color,
    onCheck     : () -> Unit,
    onCardClick : () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue   = if (item.isCompleted) accentColor.copy(alpha = 0.10f)
        else                  MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label         = "cardBg"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            // The whole card is clickable for navigation to detail
            .clickable(onClick = onCardClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = item.habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough
                        else                  TextDecoration.None
                    ),
                    color    = if (item.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(text = "${item.habit.durationMinutes} min")
                    if (item.streak > 0) {
                        InfoChip(text = "${item.streak} streak", tintColor = accentColor)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Check circle: independent click area — does NOT trigger onCardClick
            CheckCircle(
                isCompleted = item.isCompleted,
                isLoading   = item.isLoading,
                accentColor = accentColor,
                onClick     = onCheck
            )
        }
    }
}

@Composable
private fun CheckCircle(
    isCompleted : Boolean,
    isLoading   : Boolean,
    accentColor : Color,
    onClick     : () -> Unit
) {
    val fillColor by animateColorAsState(
        targetValue   = if (isCompleted) accentColor else Color.Transparent,
        animationSpec = tween(300),
        label         = "checkFill"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isCompleted) accentColor else MaterialTheme.colorScheme.outline,
        animationSpec = tween(300),
        label         = "checkBorder"
    )

    Box(
        modifier         = Modifier
            .size(34.dp)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .background(color = fillColor, shape = CircleShape)
            .clip(CircleShape)
            .clickable(enabled = !isLoading && !isCompleted, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState   = isLoading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label          = "checkContent"
        ) { loading ->
            when {
                loading     -> CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color       = accentColor
                )
                isCompleted -> Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, tintColor: Color? = null) {
    val bg = tintColor?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.surfaceVariant
    val fg = tintColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(onSignOut: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 28.dp, bottom = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text  = greeting(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = todayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onSignOut) {
            Icon(
                imageVector        = Icons.Outlined.ExitToApp,
                contentDescription = "Sign out",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..16 -> "Good evening"
    else      -> "Good night"
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))