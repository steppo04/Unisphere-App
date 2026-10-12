package com.example.unisphere.ui.screen.accessScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordState(
    val newPasswordCode: String = "",
    val confirmPasswordCode: String = "",
    val isButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ResetPasswordState())
    val state: StateFlow<ResetPasswordState> = _state.asStateFlow()

    private val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(newPasswordCode = value) }
        validateFormState()
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { it.copy(confirmPasswordCode = value) }
        validateFormState()
    }

    private fun validateFormState() {
        val currentState = _state.value
        val pass = currentState.newPasswordCode
        val confirmPass = currentState.confirmPasswordCode

        val passwordsDoNotMatch = confirmPass.isNotBlank() &&
                confirmPass.length >= pass.length &&
                pass != confirmPass

        if (passwordsDoNotMatch) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "Le password inserite non corrispondono.",
                    isButtonEnabled = false
                )
            }
            return
        }

        val isRegexValid = pass.matches(passwordRegex)

        if (pass.isNotBlank() && confirmPass.isNotBlank() && !isRegexValid) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero.",
                    isButtonEnabled = false
                )
            }
            return
        }

        val shouldEnableButton = isRegexValid && pass == confirmPass

        _state.update {
            it.copy(
                isError = false,
                errorMessage = null,
                isButtonEnabled = shouldEnableButton
            )
        }
    }

    fun finalizePasswordReset() {
        val currentState = _state.value
        if (currentState.newPasswordCode != currentState.confirmPasswordCode || !currentState.newPasswordCode.matches(passwordRegex)) {
            _state.update { it.copy(isError = true, errorMessage = "Dati non validi.", isButtonEnabled = false) }
            return
        }

        if (currentState.isLoading) return

        _state.update { it.copy(isLoading = true, isError = false) }

        viewModelScope.launch {
            delay(1000)

            _state.update {
                it.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "Funzione temporaneamente disabilitata (Il server Ktor non ha ancora un servizio email).",
                    isButtonEnabled = true
                )
            }
        }
    }
}