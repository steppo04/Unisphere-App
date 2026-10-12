
package com.example.unisphere.repository

import com.example.unisphere.db.network.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val client: HttpClient
) {

    // --- GESTIONE EVENTI ---

    suspend fun saveEvent(request: CreateEventRequest): Boolean {
        return try {
            val response = client.post("/calendar/events") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                true
            } else {
                println(
                    "SAVE EVENT FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                false
            }
        } catch (e: Exception) {
            println("SAVE EVENT EXCEPTION: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getEventsByDate(date: LocalDate): List<EventDto> {
        return try {
            val response = client.get("/calendar/events/date/$date") {
                accept(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                response.body<List<EventDto>>()
            } else {
                println(
                    "GET EVENTS FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                emptyList()
            }
        } catch (e: Exception) {
            println("GET EVENTS EXCEPTION: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getEventById(eventId: Int): EventDto? {
        return try {
            val response = client.get("/calendar/events/$eventId") {
                accept(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                response.body<EventDto>()
            } else {
                println(
                    "GET EVENT FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                null
            }
        } catch (e: Exception) {
            println("GET EVENT EXCEPTION: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteEvent(eventId: Int): Boolean {
        return try {
            val response = client.delete("/calendar/events/$eventId")

            if (response.status.isSuccess()) {
                true
            } else {
                println(
                    "DELETE EVENT FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                false
            }
        } catch (e: Exception) {
            println("DELETE EVENT EXCEPTION: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // --- GESTIONE CALENDARI ---

    suspend fun getCalendars(): List<CalendarDto> {
        return try {
            val response = client.get("/calendar/calendars") {
                accept(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                response.body<List<CalendarDto>>()
            } else {
                println(
                    "GET CALENDARS FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                emptyList()
            }
        } catch (e: Exception) {
            println("GET CALENDARS EXCEPTION: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveCalendar(request: CreateCalendarRequest): Boolean {
        return try {
            val response = client.post("/calendar/calendars") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                true
            } else {
                println(
                    "SAVE CALENDAR FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                false
            }
        } catch (e: Exception) {
            println("SAVE CALENDAR EXCEPTION: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteCalendar(calendarId: Int): Boolean {
        return try {
            val response = client.delete("/calendar/calendars/$calendarId")

            if (response.status.isSuccess()) {
                true
            } else {
                println(
                    "DELETE CALENDAR FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                false
            }
        } catch (e: Exception) {
            println("DELETE CALENDAR EXCEPTION: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun updateEvent(
        eventId: Int,
        request: CreateEventRequest
    ): Boolean {
        return try {
            val response = client.put("/calendar/events/$eventId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                true
            } else {
                println(
                    "UPDATE EVENT FAILED: HTTP ${response.status.value} - " +
                            response.bodyAsText()
                )
                false
            }
        } catch (e: Exception) {
            println("UPDATE EVENT EXCEPTION: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}