package com.example.unisphere.repository

import com.example.unisphere.db.network.*
import com.example.unisphere.db.TokenManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {

    // --- AUTENTICAZIONE ---

    suspend fun login(email: String, password: String): Boolean {
        return try {
            val response = client.post("/auth/login") {
                setBody(LoginRequest(email, password))
            }
            if (response.status == HttpStatusCode.OK) {
                val authData = response.body<AuthResponse>()
                tokenManager.saveToken(authData.token) // Salva il token crittografato
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun register(request: RegisterRequest): Boolean {
        return try {
            val response = client.post("/auth/register") {
                setBody(request)
            }
            if (response.status == HttpStatusCode.Created) {
                val authData = response.body<AuthResponse>()
                tokenManager.saveToken(authData.token)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    // --- PROFILO UTENTE ---

    // Recupera il profilo dal server. Ritorna null se c'è un errore o il token non è valido.
    suspend fun getCurrentUserProfile(): UserProfileDto? {
        return try {
            val response = client.get("/users/me")
            if (response.status == HttpStatusCode.OK) {
                response.body<UserProfileDto>()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Aggiorna l'immagine profilo
    suspend fun updateProfileImage(uri: String): Boolean {
        return try {
            val response = client.put("/users/me/image") {
                setBody(UpdateProfileImageRequest(uri))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    // Aggiorna il tema
    suspend fun updateLocalTheme(theme: String): Boolean {
        return try {
            val response = client.put("/users/me/theme") {
                setBody(UpdateThemeRequest(theme))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    // Aggiorna l'username (il server ritorna Conflict se è già in uso)
    suspend fun updateUsername(newUsername: String): Boolean {
        return try {
            val response = client.put("/users/me/username") {
                setBody(UpdateUsernameRequest(newUsername))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    // Aggiorna l'email (il server ritorna Conflict se è già in uso)
    suspend fun updateEmail(newEmail: String): Boolean {
        return try {
            val response = client.put("/users/me/email") {
                setBody(UpdateEmailRequest(newEmail))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }
}