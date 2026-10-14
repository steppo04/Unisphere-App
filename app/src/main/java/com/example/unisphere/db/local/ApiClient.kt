package com.example.unisphere.db.local

import com.example.unisphere.db.TokenManager
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "https://api.unisphere-db.it" // L'IP del tuo server

    // Passiamo il TokenManager come parametro per crearlo dinamicamente
    fun create(tokenManager: TokenManager): HttpClient {
        return HttpClient(Android) {

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            defaultRequest {
                url(BASE_URL)
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        // Legge il token VERO salvato nel dispositivo
                        val currentToken = tokenManager.getToken()
                        if (currentToken != null) {
                            BearerTokens(currentToken, "")
                        } else {
                            null // Nessun token, la chiamata fallirà se la rotta è protetta
                        }
                    }
                }
            }
        }
    }
}
