package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// I modelli DTO rimangono invariati per Spoonacular
@Serializable
data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int = 0,
    val servings: Int = 0
)

@Serializable
data class RecipeResponse(
    val results: List<Recipe>
)

data class CookState(
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface CookAction {
    data class OnSearchQueryChanged(val query: String) : CookAction
    data object OnRetryClicked : CookAction
}

@HiltViewModel
class CookViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository
) : AndroidViewModel(application) {

    // Passaggio a StateFlow
    private val _state = MutableStateFlow(CookState())
    val state: StateFlow<CookState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchRecipes()
    }

    fun onAction(action: CookAction) {
        val currentState = _state.value
        when (action) {
            is CookAction.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
                debouncedSearch(action.query)
            }
            CookAction.OnRetryClicked -> {
                fetchRecipes(currentState.searchQuery)
            }
        }
    }

    private fun debouncedSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchRecipes(query)
        }
    }

    private fun fetchRecipes(query: String = "") {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.searchRecipes(query).onSuccess { response ->
                _state.update { it.copy(recipes = response.results, isLoading = false) }
            }.onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, error = "Errore durante il caricamento delle ricette")
                }
                e.printStackTrace()
            }
        }
    }
}