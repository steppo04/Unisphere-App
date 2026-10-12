
package com.example.unisphere.db.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRecipeDto(
    @SerialName("id")
    val recipeId: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val servings: Int
)

@Serializable
data class SaveFavoriteRecipeRequest(
    @SerialName("id")
    val recipeId: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val servings: Int
)

@Serializable
data class IsFavoriteResponse(
    val isFavorite: Boolean
)