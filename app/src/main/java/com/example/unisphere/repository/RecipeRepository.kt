package com.example.unisphere.repository

import com.example.unisphere.db.network.FavoriteRecipeDto
import com.example.unisphere.db.network.IsFavoriteResponse
import com.example.unisphere.db.network.SaveFavoriteRecipeRequest
import com.example.unisphere.ui.screen.cook.RecipeDetail
import com.example.unisphere.ui.screen.cook.RecipeResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val client: HttpClient
) {
    private val spoonacularApiKey = "e09429ff9b1c4c3ca2e6e4318890b313"
    private val spoonacularBaseUrl = "https://api.spoonacular.com/recipes"

    // ==========================================
    // CHIAMATE ESTERNE (SPOONACULAR API)
    // ==========================================

    suspend fun searchRecipes(query: String): Result<RecipeResponse> = runCatching {
        client.get("$spoonacularBaseUrl/complexSearch") {
            parameter("apiKey", spoonacularApiKey)
            parameter("query", query)
            parameter("number", 20)
            parameter("addRecipeInformation", true)
        }.body()
    }

    suspend fun getRecipeDetails(id: Int): Result<RecipeDetail> = runCatching {
        client.get("$spoonacularBaseUrl/$id/information") {
            parameter("apiKey", spoonacularApiKey)
        }.body()
    }

    // ==========================================
    // PREFERITI (SUL TUO BACKEND CLOUD)
    // ==========================================

    /**
     * Recupera la lista dei preferiti dell'utente loggato (letto dal JWT).
     */
    suspend fun getFavorites(): Result<List<FavoriteRecipeDto>> = runCatching {
        client.get("/recipes/favorites").body()
    }

    /**
     * Controlla se una specifica ricetta è tra i preferiti.
     * Sostituisce la vecchia query "EXISTS" di Room.
     */
    suspend fun isFavorite(recipeId: Int): Result<Boolean> = runCatching {
        val response = client.get("/recipes/favorites/$recipeId/check")

        if (!response.status.isSuccess()) {
            error("Controllo preferito fallito: HTTP ${response.status.value}")
        }

        response.body<IsFavoriteResponse>().isFavorite
    }

    /**
     * Salva una ricetta tra i preferiti.
     */
    suspend fun saveFavorite(request: SaveFavoriteRecipeRequest): Result<Unit> = runCatching {
        client.post("/recipes/favorites") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /**
     * Rimuove una ricetta dai preferiti in base al suo ID Spoonacular.
     */
    suspend fun removeFavorite(recipeId: Int): Result<Unit> = runCatching {
        client.delete("/recipes/favorites/$recipeId")
    }
}