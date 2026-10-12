package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.SaveFavoriteRecipeRequest
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// I modelli della risposta di Spoonacular rimangono identici
@Serializable
data class RecipeDetail(
    val id: Int,
    val title: String,
    val image: String,
    val summary: String = "",
    val instructions: String? = null,
    val readyInMinutes: Int = 0,
    val servings: Int = 0,
    val extendedIngredients: List<Ingredient> = emptyList()
)

@Serializable
data class Ingredient(
    val original: String
)

data class RecipeDetailState(
    val recipe: RecipeDetail? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null
)

sealed interface RecipeDetailAction {
    data object OnToggleFavorite : RecipeDetailAction
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // Passaggio a StateFlow
    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    private val recipeId: Int = checkNotNull(savedStateHandle["recipeId"])

    init {
        loadRecipeData()
    }

    private fun loadRecipeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Effettuiamo le due chiamate in parallelo per caricare la pagina più velocemente
            val detailsDef = async { repository.getRecipeDetails(recipeId) }
            val favoriteDef = async { repository.isFavorite(recipeId) }

            val detailsResult = detailsDef.await()
            val isFavResult = favoriteDef.await().getOrDefault(false)

            detailsResult.onSuccess { recipeDetails ->
                _state.update {
                    it.copy(
                        recipe = recipeDetails,
                        isFavorite = isFavResult,
                        isLoading = false
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Impossibile caricare i dettagli della ricetta"
                    )
                }
            }
        }
    }

    fun onAction(action: RecipeDetailAction) {
        when (action) {
            RecipeDetailAction.OnToggleFavorite -> toggleFavorite()
        }
    }

    private fun toggleFavorite() {
        val currentState = _state.value
        val currentRecipe = currentState.recipe ?: return

        viewModelScope.launch {
            if (currentState.isFavorite) {
                // Rimuoviamo dai preferiti chiamando il backend
                repository.removeFavorite(recipeId).onSuccess {
                    _state.update { it.copy(isFavorite = false) }
                }
            } else {
                // Aggiungiamo ai preferiti chiamando il backend
                val request = SaveFavoriteRecipeRequest(
                    recipeId = currentRecipe.id,
                    title = currentRecipe.title,
                    image = currentRecipe.image,
                    readyInMinutes = currentRecipe.readyInMinutes,
                    servings = currentRecipe.servings
                )
                repository.saveFavorite(request).onSuccess {
                    _state.update { it.copy(isFavorite = true) }
                }
            }
        }
    }
}