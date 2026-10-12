package com.example.unisphere.db.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val surname: String,
    val username: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val email: String,
    val username: String
)

@Serializable
data class UserProfileDto(
    val id: Int,
    val email: String,
    val name: String,
    val surname: String,
    val username: String,
    val profilePictureUri: String?,
    val currentTheme: String
)

@Serializable
data class UpdateProfileImageRequest(val uri: String)

@Serializable
data class UpdateThemeRequest(val theme: String)

@Serializable
data class UpdateUsernameRequest(val username: String)

@Serializable
data class UpdateEmailRequest(val email: String)