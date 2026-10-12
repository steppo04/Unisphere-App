package com.example.unisphere.ui.screen.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.CalendarDto
import com.example.unisphere.db.network.CreateEventRequest
import com.example.unisphere.db.network.CreateCalendarRequest
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

data class AddCalendarEventState(
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

sealed interface AddCalendarEventAction {
    data class OnTitleChanged(val value: String) : AddCalendarEventAction
    data class OnLocationChanged(val value: String) : AddCalendarEventAction
    data class OnDescriptionChanged(val value: String) : AddCalendarEventAction
    data class OnCalendarChanged(val value: Int) : AddCalendarEventAction
    data class OnDateChanged(val value: LocalDate) : AddCalendarEventAction
    data class OnStartTimeChanged(val value: LocalTime) : AddCalendarEventAction
    data class OnEndTimeChanged(val value: LocalTime) : AddCalendarEventAction
    data class ToggleTypeExpanded(val expanded: Boolean) : AddCalendarEventAction
    data class ToggleDatePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleStartTimePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleEndTimePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleLocationExpanded(val expanded: Boolean) : AddCalendarEventAction
    data class OnCreateCalendarType(val name: String, val colorHex: String) : AddCalendarEventAction
    data class OnDeleteCalendarType(val calendar: CalendarDto) : AddCalendarEventAction
    data object OnGetCurrentLocation : AddCalendarEventAction
    data object OnSaveClicked : AddCalendarEventAction
}

@HiltViewModel
class AddCalendarEventViewModel @Inject constructor(
    application: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AddCalendarEventState())
    val state: StateFlow<AddCalendarEventState> = _state.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var searchJob: Job? = null

    init {
        loadUserCalendars()
    }

    private fun loadUserCalendars() {
        viewModelScope.launch {
            val calendars = eventRepository.getCalendars()
            _state.update {
                it.copy(
                    calendarTypes = calendars,
                    selectedCalendarId = if (it.selectedCalendarId == 0 || calendars.none { cal -> cal.id == it.selectedCalendarId }) {
                        calendars.firstOrNull()?.id ?: 0
                    } else {
                        it.selectedCalendarId
                    }
                )
            }
            validateFormState()
        }
    }

    fun onAction(action: AddCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is AddCalendarEventAction.OnTitleChanged -> {
                _state.update { it.copy(title = action.value) }
                validateFormState()
            }
            is AddCalendarEventAction.OnLocationChanged -> {
                _state.update { it.copy(location = action.value, isLocationExpanded = true) }
                fetchLocationSuggestions(action.value)
                validateFormState()
            }
            is AddCalendarEventAction.OnDescriptionChanged -> _state.update { it.copy(description = action.value) }
            is AddCalendarEventAction.OnCalendarChanged -> {
                _state.update { it.copy(selectedCalendarId = action.value, isTypeExpanded = false) }
                validateFormState()
            }
            is AddCalendarEventAction.OnDateChanged -> {
                _state.update { it.copy(selectedDate = action.value, showDatePicker = false) }
                validateFormState()
            }
            is AddCalendarEventAction.OnStartTimeChanged -> {
                _state.update { it.copy(selectedStartTime = action.value, showStartTimePicker = false) }
                validateFormState()
            }
            is AddCalendarEventAction.ToggleStartTimePicker -> _state.update { it.copy(showStartTimePicker = action.show) }
            is AddCalendarEventAction.OnEndTimeChanged -> {
                _state.update { it.copy(selectedEndTime = action.value, showEndTimePicker = false) }
                validateFormState()
            }
            is AddCalendarEventAction.ToggleEndTimePicker -> _state.update { it.copy(showEndTimePicker = action.show) }
            is AddCalendarEventAction.ToggleTypeExpanded -> _state.update { it.copy(isTypeExpanded = action.expanded) }
            is AddCalendarEventAction.ToggleLocationExpanded -> _state.update { it.copy(isLocationExpanded = action.expanded) }
            is AddCalendarEventAction.ToggleDatePicker -> _state.update { it.copy(showDatePicker = action.show) }
            is AddCalendarEventAction.OnCreateCalendarType -> createCalendarType(action.name, action.colorHex)
            is AddCalendarEventAction.OnDeleteCalendarType -> deleteCalendarType(action.calendar)
            AddCalendarEventAction.OnGetCurrentLocation -> getCurrentLocation()
            AddCalendarEventAction.OnSaveClicked -> saveEventToKtor(onBack)
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
            if (eventRepository.saveCalendar(request)) {
                loadUserCalendars()
            }
        }
    }

    private fun deleteCalendarType(calendar: CalendarDto) {
        viewModelScope.launch {
            if (eventRepository.deleteCalendar(calendar.id)) {
                loadUserCalendars()
            }
        }
    }

    private fun saveEventToKtor(onSuccess: () -> Unit) {
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
            val success = eventRepository.saveEvent(request)
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