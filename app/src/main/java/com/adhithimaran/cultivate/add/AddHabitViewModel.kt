package com.adhithimaran.cultivate.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhithimaran.cultivate.data.model.Habit
import com.adhithimaran.cultivate.data.model.HabitType
import com.adhithimaran.cultivate.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds and manages all UI state for the Add Habit screen.
 * Keeps the screen itself stateless — it only reads from [uiState] and
 * calls the `on*` handler functions.
 *
 * The [HabitRepository] is injected rather than constructed here so the
 * class is easy to test in isolation.
 */
class AddHabitViewModel(
    private val repository: HabitRepository = HabitRepository()
) : ViewModel() {

    // -------------------------------------------------------------------------
    // UI State
    // -------------------------------------------------------------------------

    private val _uiState = MutableStateFlow(AddHabitUiState())

    /**
     * The single source of truth for what the Add Habit screen should render.
     * Collect this in the Composable with [collectAsStateWithLifecycle].
     */
    val uiState: StateFlow<AddHabitUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Event handlers — called directly from the Composable
    // -------------------------------------------------------------------------

    /** Called whenever the user edits the habit name field. */
    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    /**
     * Called whenever the duration slider moves.
     * [raw] is the raw Float from the Slider; we round to the nearest integer minute.
     */
    fun onDurationChange(raw: Float) {
        _uiState.update { it.copy(duration = raw.toInt(), durationError = null) }
    }

    /** Called when the user picks a different [HabitType] from the segmented selector. */
    fun onTypeChange(type: HabitType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    /**
     * Validates the current form fields, then — if valid — suspends while writing
     * the new habit to Firestore and signals the screen to navigate back on success.
     *
     * Any error surfaces via [AddHabitUiState.nameError] / [durationError] so the
     * Composable can show inline feedback without a separate dialog.
     */
    fun onSave() {
        val state = _uiState.value

        // --- Validate ---
        val nameError     = if (state.name.isBlank()) "Habit name can't be empty" else null
        val durationError = if (state.duration <= 0)  "Duration must be at least 1 minute" else null

        if (nameError != null || durationError != null) {
            _uiState.update { it.copy(nameError = nameError, durationError = durationError) }
            return
        }

        // --- Save ---
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.addHabit(
                    Habit(
                        name            = state.name.trim(),
                        durationMinutes = state.duration,
                        type            = state.selectedType
                    )
                )
                // Signal success — the screen observes this to call onNavigateBack()
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving   = false,
                        saveError  = "Failed to save habit. Please try again."
                    )
                }
            }
        }
    }

    /** Called by the screen after it has consumed the navigation signal. */
    fun onNavigationHandled() {
        _uiState.update { it.copy(savedSuccessfully = false) }
    }

    /** Called after the screen has shown the transient save-error message. */
    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveError = null) }
    }
}

// -----------------------------------------------------------------------------
// UI State model
// -----------------------------------------------------------------------------

/**
 * Immutable snapshot of everything the Add Habit screen needs to render itself.
 *
 * @property name            Current text in the habit-name field.
 * @property nameError       Non-null when [name] failed validation; shown under the field.
 * @property duration        Current slider value in whole minutes.
 * @property durationError   Non-null when [duration] failed validation.
 * @property selectedType    Which [HabitType] tab is selected.
 * @property isSaving        True while the Firestore write is in-flight; drives the loading indicator.
 * @property savedSuccessfully True for one frame after a successful save; triggers back-navigation.
 * @property saveError       Non-null when the Firestore write threw; shown as a Snackbar message.
 */
data class AddHabitUiState(
    val name             : String    = "",
    val nameError        : String?   = null,
    val duration         : Int       = 15,       // sensible default
    val durationError    : String?   = null,
    val selectedType     : HabitType = HabitType.DAILY,
    val isSaving         : Boolean   = false,
    val savedSuccessfully: Boolean   = false,
    val saveError        : String?   = null
)