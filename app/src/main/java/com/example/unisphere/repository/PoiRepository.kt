package com.example.unisphere.repository

import com.example.unisphere.db.network.CreatePoiRequest
import com.example.unisphere.db.network.PointOfInterestDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(
    private val client: HttpClient
) {
    /**
     * Recupera i punti di interesse dell'utente loggato.
     * L'ID utente viene dedotto dal backend tramite il token JWT.
     */
    suspend fun getPois(): Result<List<PointOfInterestDto>> = runCatching {
        client.get("/pois").body()
    }

    /**
     * Salva un nuovo punto di interesse.
     */
    suspend fun savePoi(request: CreatePoiRequest): Result<PointOfInterestDto> = runCatching {
        client.post("/pois") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Elimina un punto di interesse usando il suo ID.
     */

    suspend fun deletePoi(poiId: Int): Result<Unit> =
        runCatching {
            android.util.Log.d(
                "POI_DELETE",
                "Inizio eliminazione: ID=$poiId"
            )

            val response = client.delete("/pois/$poiId")

            android.util.Log.d(
                "POI_DELETE",
                "Risposta server: ID=$poiId, HTTP=${response.status.value}"
            )

            if (response.status.value !in 200..299) {
                error("HTTP ${response.status.value}")
            }
        }.onFailure { error ->
            android.util.Log.e(
                "POI_DELETE",
                "Errore eliminando ID=$poiId",
                error
            )
        }

}