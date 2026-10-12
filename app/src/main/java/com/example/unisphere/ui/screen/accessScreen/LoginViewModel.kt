package com.example.unisphere.ui.screen.accessScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Aggiunta variabile fondamentale per la navigazione
    val isLoginSuccessful: Boolean = false
)

sealed interface LoginAction {
    data class OnEmailChanged(val value: String) : LoginAction
    data class OnPasswordChanged(val value: String) : LoginAction
    data object OnLoginClicked : LoginAction
    data object OnForgotPasswordClicked : LoginAction
    data object OnDismissMessages : LoginAction
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Sostituito mutableStateOf con StateFlow
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    // Rimosso "onSuccess" dalla firma, gestiamo la navigazione con lo stato
    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChanged -> {
                _state.update { it.copy(email = action.value, isError = false, successMessage = null) }
            }
            is LoginAction.OnPasswordChanged -> {
                _state.update { it.copy(password = action.value, isError = false) }
            }
            LoginAction.OnLoginClicked -> performLogin()
            LoginAction.OnForgotPasswordClicked -> performPasswordReset()
            LoginAction.OnDismissMessages -> {
                _state.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun isEmailInvalid(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isBlank() || !trimmed.contains("@") || !trimmed.contains(".")
    }

    private fun performLogin() {
        val currentState = _state.value

        if (isEmailInvalid(currentState.email)) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "Inserisci un indirizzo email valido prima di accedere."
                )
            }
            return
        }

        if (currentState.password.isBlank()) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "La password non può essere vuota."
                )
            }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false, successMessage = null) }

            // Chiamata al nuovo Repository Ktor
            val success = userRepository.login(currentState.email.trim(), currentState.password)

            if (success) {
                // Impostiamo isLoginSuccessful a true per innescare la navigazione sulla UI
                _state.update { it.copy(isLoading = false, isLoginSuccessful = true) }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = "Credenziali errate o errore di rete"
                    )
                }
            }
        }
    }

    private fun performPasswordReset() {
        val currentState = _state.value
        val emailTrimmed = currentState.email.trim()

        if (isEmailInvalid(emailTrimmed)) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "Inserisci una mail valida nel campo di testo per ricevere il link di ripristino."
                )
            }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false, successMessage = null) }

            delay(1000)

            _state.update {
                it.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "Funzione temporaneamente disabilitata (Il server Ktor non ha ancora un servizio email)."
                )
            }
        }
    }
}