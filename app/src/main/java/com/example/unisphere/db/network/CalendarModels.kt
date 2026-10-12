package com.example.unisphere.db.network

import kotlinx.serialization.Serializable

@Serializable
data class CalendarDto(
    val id: Int,
    val name: String,
    val color: String
)

@Serializable
data class CreateCalendarRequest(
    val name: String,
    val color: String
)

@Serializable
data class EventDto(
    val id: Int,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val calendarId: Int
)

@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val calendarId: Int
)