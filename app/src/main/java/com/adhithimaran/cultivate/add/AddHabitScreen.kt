package com.adhithimaran.cultivate.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adhithimaran.cultivate.data.model.HabitType

/**
 * Add Habit screen — a clean, professional single-purpose form.
 *
 * Every color and text style resolves through [MaterialTheme.colorScheme] and
 * [MaterialTheme.typography] — there are zero hardcoded values in this file.
 * That means any future edit to Color.kt or Type.kt automatically cascades here.
 *
 * @param onNavigateBack Called once after a successful save, or when the user taps Back.
 * @param viewModel      Injected by [viewModel()] factory; override in tests to pass a fake.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddHabitViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Side Effects

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            viewModel.onNavigationHandled()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSaveErrorShown()
        }
    }

    // Root Container

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Top Bar
            TopBar(onNavigateBack = onNavigateBack)

            // Page Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 28.dp)
            ) {
                Text(
                    text  = "Add Habit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Add any habit you'd like to track!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Form Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Habit Name
                FormCard {
                    FieldLabel(text = "Habit Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = state.name,
                        onValueChange = viewModel::onNameChange,
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text(
                                text  = "e.g. Morning run, Read 10 pages",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine      = true,
                        isError         = state.nameError != null,
                        colors          = cultivateTextFieldColors(),
                        textStyle       = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction      = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FieldError(message = state.nameError)
                }

                // Duration
                FormCard {
                    FieldLabel(text = "Duration")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "How many minutes will this habit take?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        // Empty string when duration is 0 so the placeholder shows cleanly
                        value         = if (state.duration == 0) "" else state.duration.toString(),
                        onValueChange = { raw ->
                            // Strip any non-digit characters the system keyboard might sneak in,
                            // cap at 3 digits (999 min max), then forward to the ViewModel as Float
                            // so the existing onDurationChange signature stays unchanged
                            val cleaned = raw.filter { it.isDigit() }.take(3)
                            viewModel.onDurationChange(cleaned.toFloatOrNull() ?: 0f)
                        },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text(
                                text  = "e.g. 30",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        // "min" appears as a permanent trailing hint inside the field border
                        suffix = {
                            Text(
                                text  = "min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine      = true,
                        isError         = state.durationError != null,
                        colors          = cultivateTextFieldColors(),
                        textStyle       = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FieldError(message = state.durationError)
                }

                // Freq Selector
                FormCard {
                    FieldLabel(text = "Frequency")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "How often do you want to do this habit?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        HabitType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = state.selectedType == type,
                                onClick  = { viewModel.onTypeChange(type) },
                                shape    = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = HabitType.entries.size
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor   = MaterialTheme.colorScheme.primary,
                                    activeContentColor     = MaterialTheme.colorScheme.onPrimary,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                                    activeBorderColor      = MaterialTheme.colorScheme.primary,
                                    inactiveBorderColor    = MaterialTheme.colorScheme.outline
                                ),
                                label = {
                                    Text(
                                        text  = type.name
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save Button
                Button(
                    onClick  = viewModel::onSave,
                    enabled  = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        contentColor           = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        disabledContentColor   = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text  = "Save Habit",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Error snackbar (floats above content)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                shape          = RoundedCornerShape(12.dp)
            )
        }
    }
}

// Private sub-components: styling helpers
// TODO: DesignSystem.kt if you need to copy them to other screens.

/**
 * Back-arrow top bar row. Blends into the background (no elevation or heavy toolbar)
 * for a clean, modern look — the page title below does the heavy lifting.
 */
@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint               = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Card container used to visually group each form section.
 * Elevation is intentionally kept at 1.dp for a flat, modern feel.
 * Color resolves to [MaterialTheme.colorScheme.surface] — CultivateSurface (white)
 * in light mode, CultivateSurfaceDark in dark mode.
 */
@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            content()
        }
    }
}

/**
 * Small section label rendered above each field.
 * [titleSmall] (Medium 14sp, 0.1sp tracking) is the right scale — clearly a label
 * without competing with the field content.
 */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/**
 * Animated validation error shown beneath a field.
 * Slides up + fades in so the card doesn't abruptly resize on error appearance.
 * Passing null collapses it entirely with no residual spacing.
 */
@Composable
private fun FieldError(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter   = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit    = fadeOut()
    ) {
        Text(
            text     = message ?: "",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )
    }
}

/**
 * Centralised [OutlinedTextFieldDefaults.colors] configuration so every text field
 * on this screen shares identical theming with a single edit point.
 *
 * Every color cascades from [MaterialTheme.colorScheme], which means light/dark
 * switching and any future Color.kt palette change both apply automatically.
 */
@Composable
private fun cultivateTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
    errorBorderColor        = MaterialTheme.colorScheme.error,
    focusedLabelColor       = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor             = MaterialTheme.colorScheme.primary,
    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
    errorTextColor          = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor   = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor     = MaterialTheme.colorScheme.surface
)