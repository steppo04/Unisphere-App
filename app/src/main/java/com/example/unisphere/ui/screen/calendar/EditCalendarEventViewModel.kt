package com.example.unisphere.ui.screen.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.CalendarDto
import com.example.unisphere.db.network.CreateCalendarRequest
import com.example.unisphere.db.network.CreateEventRequest
import com.example.unisphere.repository.EventRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

// 1. STATO COMPLETAMENTE INDIPENDENTE
data class EditCalendarEventState(
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedStartTime: LocalTime = LocalTime.of(9, 0),
    val selectedEndTime: LocalTime = LocalTime.of(10, 0),
    val isButtonEnabled: Boolean = false,
    val isTimeError: Boolean = false,
    val timeErrorMessage: String? = null,
    val selectedCalendarId: Int = 0,
    val calendarTypes: List<CalendarDto> = emptyList(),
    val isTypeExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val showStartTimePicker: Boolean = false,
    val showEndTimePicker: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLocationExpanded: Boolean = false,
    val locationSuggestions: List<String> = emptyList(),
    val isSearchingSuggestions: Boolean = false
)

// 2. ACTION COMPLETAMENTE INDIPENDENTI
sealed interface EditCalendarEventAction {
    data class OnTitleChanged(val value: String) : EditCalendarEventAction
    data class OnLocationChanged(val value: String) : EditCalendarEventAction
    data class OnDescriptionChanged(val value: String) : EditCalendarEventAction
    data class OnCalendarChanged(val value: Int) : EditCalendarEventAction
    data class OnDateChanged(val value: LocalDate) : EditCalendarEventAction
    data class OnStartTimeChanged(val value: LocalTime) : EditCalendarEventAction
    data class OnEndTimeChanged(val value: LocalTime) : EditCalendarEventAction
    data class ToggleTypeExpanded(val expanded: Boolean) : EditCalendarEventAction
    data class ToggleDatePicker(val show: Boolean) : EditCalendarEventAction
    data class ToggleStartTimePicker(val show: Boolean) : EditCalendarEventAction
    data class ToggleEndTimePicker(val show: Boolean) : EditCalendarEventAction
    data class ToggleLocationExpanded(val expanded: Boolean) : EditCalendarEventAction
    data class OnCreateCalendarType(val name: String, val colorHex: String) : EditCalendarEventAction
    data class OnDeleteCalendarType(val calendar: CalendarDto) : EditCalendarEventAction
    data object OnGetCurrentLocation : EditCalendarEventAction
    data object OnSaveClicked : EditCalendarEventAction
}

@HiltViewModel
class EditCalendarEventViewModel @Inject constructor(
    application: Application,
    private val repository: EventRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // 3. USO DEL NUOVO STATO INDIPENDENTE (STATEFLOW)
    private val _state = MutableStateFlow(EditCalendarEventState())
    val state: StateFlow<EditCalendarEventState> = _state.asStateFlow()

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"])
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var searchJob: Job? = null

    init {
        loadUserCalendars()
        loadEventDetails()
    }

    private fun loadUserCalendars() {
        viewModelScope.launch {
            val calendars = repository.getCalendars()
            _state.update { it.copy(calendarTypes = calendars) }
        }
    }

    private fun loadEventDetails() {
        viewModelScope.launch {
            val event = repository.getEventById(eventId)
            if (event != null) {
                _state.update {
                    it.copy(
                        title = event.title,
                        location = event.location,
                        description = event.description,
                        selectedDate = LocalDate.parse(event.date),
                        selectedStartTime = LocalTime.parse(event.startTime),
                        selectedEndTime = LocalTime.parse(event.endTime),
                        selectedCalendarId = event.calendarId
                    )
                }
                validateFormState() // Aggiorna lo stato del bottone dopo il caricamento
            }
        }
    }

    fun onAction(action: EditCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is EditCalendarEventAction.OnTitleChanged -> {
                _state.update { it.copy(title = action.value) }
                validateFormState()
            }
            is EditCalendarEventAction.OnLocationChanged -> {
                _state.update { it.copy(location = action.value, isLocationExpanded = true) }
                fetchLocationSuggestions(action.value)
                validateFormState()
            }
            is EditCalendarEventAction.OnDescriptionChanged -> _state.update { it.copy(description = action.value) }
            is EditCalendarEventAction.OnCalendarChanged -> {
                _state.update { it.copy(selectedCalendarId = action.value, isTypeExpanded = false) }
                validateFormState()
            }
            is EditCalendarEventAction.OnDateChanged -> {
                _state.update { it.copy(selectedDate = action.value, showDatePicker = false) }
                validateFormState()
            }
            is EditCalendarEventAction.OnStartTimeChanged -> {
                _state.update { it.copy(selectedStartTime = action.value, showStartTimePicker = false) }
                validateFormState()
            }
            is EditCalendarEventAction.ToggleStartTimePicker -> _state.update { it.copy(showStartTimePicker = action.show) }
            is EditCalendarEventAction.OnEndTimeChanged -> {
                _state.update { it.copy(selectedEndTime = action.value, showEndTimePicker = false) }
                validateFormState()
            }
            is EditCalendarEventAction.ToggleEndTimePicker -> _state.update { it.copy(showEndTimePicker = action.show) }
            is EditCalendarEventAction.ToggleTypeExpanded -> _state.update { it.copy(isTypeExpanded = action.expanded) }
            is EditCalendarEventAction.ToggleLocationExpanded -> _state.update { it.copy(isLocationExpanded = action.expanded) }
            is EditCalendarEventAction.ToggleDatePicker -> _state.update { it.copy(showDatePicker = action.show) }
            is EditCalendarEventAction.OnCreateCalendarType -> createCalendarType(action.name, action.colorHex)
            is EditCalendarEventAction.OnDeleteCalendarType -> deleteCalendarType(action.calendar)
            EditCalendarEventAction.OnGetCurrentLocation -> getCurrentLocation()
            EditCalendarEventAction.OnSaveClicked -> updateEvent(onBack)
        }
    }

    private fun validateFormState() {
        _state.update {
            val isTimeInvalid = it.selectedEndTime.isBefore(it.selectedStartTime)
            it.copy(
                isTimeError = isTimeInvalid,
                timeErrorMessage = if (isTimeInvalid) "L'orario di fine non può antecedere quello d'inizio." else null,
                isButtonEnabled = it.title.isNotBlank() && it.selectedCalendarId != 0 && !isTimeInvalid
            )
        }
    }

    private fun createCalendarType(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val request = CreateCalendarRequest(name = name, color = colorHex)
            if (repository.saveCalendar(request)) {
                loadUserCalendars()
            }
        }
    }

    private fun deleteCalendarType(calendar: CalendarDto) {
        viewModelScope.launch {
            if (repository.deleteCalendar(calendar.id)) {
                loadUserCalendars()
            }
        }
    }

    private fun updateEvent(onSuccess: () -> Unit) {
        val currentState = _state.value
        if (!currentState.isButtonEnabled) return
        viewModelScope.launch {
            val request = CreateEventRequest(
                title = currentState.title,
                location = currentState.location,
                description = currentState.description,
                date = currentState.selectedDate.toString(),
                startTime = currentState.selectedStartTime.toString(),
                endTime = currentState.selectedEndTime.toString(),
                calendarId = currentState.selectedCalendarId
            )
            val success = repository.updateEvent(eventId, request)
            if (success) onSuccess()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLocation = true) }
            try {
                val result = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) {
                        _state.update { it.copy(location = addressName) }
                        validateFormState()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { _state.update { it.copy(isLoadingLocation = false) } }
        }
    }

    private fun fetchLocationSuggestions(query: String) {
        if (query.length < 3) {
            _state.update { it.copy(locationSuggestions = emptyList()) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isSearchingSuggestions = true) }
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }
                val suggestions = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
                _state.update { it.copy(locationSuggestions = suggestions) }
            } catch (e: Exception) { e.printStackTrace() }
            finally { _state.update { it.copy(isSearchingSuggestions = false) } }
        }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return try {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
            }
        } catch (e: Exception) { null }
    }
}