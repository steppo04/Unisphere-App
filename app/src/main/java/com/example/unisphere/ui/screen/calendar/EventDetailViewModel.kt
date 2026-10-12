package com.example.unisphere.ui.screen.calendar

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.EventDto
import com.example.unisphere.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject

data class EventDetailState(
    val event: EventDto? = null,
    val calendarName: String = "Caricamento...",
    val calendarColorHex: String = "#8E8E93",
    val isLoading: Boolean = true,
    val geoPoint: GeoPoint? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    application: Application,
    private val repository: EventRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"])

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            val event = repository.getEventById(eventId)
            if (event != null) {

                val point = if (event.location.isNotBlank()) {
                    getCoordinatesFromAddress(event.location)
                } else null

                // Recuperiamo i calendari dal server per trovare il nome e il colore corretti
                val calendars = repository.getCalendars()
                val matchedCalendar = calendars.find { it.id == event.calendarId }

                _state.update {
                    it.copy(
                        event = event,
                        calendarName = matchedCalendar?.name ?: "Nessun Calendario",
                        calendarColorHex = matchedCalendar?.color ?: "#8E8E93",
                        geoPoint = point,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun getCoordinatesFromAddress(address: String): GeoPoint? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)

                results?.firstOrNull()?.let {
                    GeoPoint(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun deleteEvent(onSuccess: () -> Unit) {
        _state.value.event?.let { currentEvent ->
            viewModelScope.launch {
                val success = repository.deleteEvent(currentEvent.id)
                if (success) {
                    onSuccess()
                }
            }
        }
    }
}