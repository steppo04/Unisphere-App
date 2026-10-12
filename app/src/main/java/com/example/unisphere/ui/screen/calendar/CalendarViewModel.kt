package com.example.unisphere.ui.screen.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.CalendarDto
import com.example.unisphere.db.network.EventDto
import com.example.unisphere.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val gridDays: List<LocalDate?> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val calendars: List<CalendarDto> = emptyList()
)

sealed interface CalendarAction {
    data class OnDateSelected(val date: LocalDate) : CalendarAction
    data class OnMonthChanged(val month: YearMonth) : CalendarAction
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var eventsJob: Job? = null

    init {
        loadUserCalendars()
        generateCalendarGrid(_state.value.currentMonth)
        loadEventsForDate(_state.value.selectedDate)
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.OnDateSelected -> {
                if (_state.value.selectedDate != action.date) {
                    _state.update { it.copy(selectedDate = action.date) }
                    loadEventsForDate(action.date)
                }
            }
            is CalendarAction.OnMonthChanged -> {
                _state.update { it.copy(currentMonth = action.month) }
                generateCalendarGrid(action.month)
            }
        }
    }

    private fun generateCalendarGrid(month: YearMonth) {
        val firstDayOfMonth = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1

        val totalGridItems = mutableListOf<LocalDate?>()

        for (i in 0 until firstDayOfWeekIndex) {
            totalGridItems.add(null)
        }

        for (day in 1..daysInMonth) {
            totalGridItems.add(month.atDay(day))
        }

        _state.update { it.copy(gridDays = totalGridItems) }
    }

    private fun loadUserCalendars() {
        viewModelScope.launch {
            val listaCalendari = eventRepository.getCalendars()
            _state.update { it.copy(calendars = listaCalendari) }
        }
    }

    private fun loadEventsForDate(date: LocalDate) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            val listaEventi = eventRepository.getEventsByDate(date)
            _state.update { it.copy(events = listaEventi) }
        }
    }
}