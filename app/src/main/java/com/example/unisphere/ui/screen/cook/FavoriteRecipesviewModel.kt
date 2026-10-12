package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.FavoriteRecipeDto
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteRecipesState(
    val favorites: List<FavoriteRecipeDto> = emptyList(), // Usiamo il DTO
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoriteRecipesViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository
) : AndroidViewModel(application) {

    // Passaggio a StateFlow
    private val _state = MutableStateFlow(FavoriteRecipesState())
    val state: StateFlow<FavoriteRecipesState> = _state.asStateFlow()

    init {
        fetchFavorites()
    }

    fun fetchFavorites() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // L'ID utente è gestito in automatico dal token JWT backend
            repository.getFavorites().onSuccess { list ->
                _state.update { it.copy(favorites = list, isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}