package com.example.unisphere.db.network

import kotlinx.serialization.Serializable

@Serializable
data class PointOfInterestDto(
    val id: Int,
    val userUid: String = "",
    val name: String,
    val address: String,
    val notes: String = "",
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class CreatePoiRequest(
    val name: String,
    val address: String,
    val notes: String,
    val latitude: Double,
    val longitude: Double
    // userUid non serve nella request, il backend Ktor lo legge dal token Bearer
)