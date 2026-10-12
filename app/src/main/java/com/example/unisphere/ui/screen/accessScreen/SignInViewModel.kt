package com.example.unisphere.ui.screen.accessScreen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.RegisterRequest
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isButtonEnabled: Boolean = false,
    val profilePictureUri: String? = null,
    val theme: String = "Default",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationSuccessful: Boolean = false
)

sealed interface SignInAction {
    data class OnNameChanged(val value: String) : SignInAction
    data class OnSurnameChanged(val value: String) : SignInAction
    data class OnUsernameChanged(val value: String) : SignInAction
    data class OnEmailChanged(val value: String) : SignInAction
    data class OnPasswordChanged(val value: String) : SignInAction
    data class OnConfirmPasswordChanged(val value: String) : SignInAction
    data class OnImageSelected(val uri: Uri?) : SignInAction
    data object OnCreateAccountClicked : SignInAction
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnNameChanged -> {
                _state.update { it.copy(name = action.value) }
                validateFormState()
            }
            is SignInAction.OnSurnameChanged -> {
                _state.update { it.copy(surname = action.value) }
                validateFormState()
            }
            is SignInAction.OnUsernameChanged -> {
                _state.update { it.copy(username = action.value) }
                validateFormState()
            }
            is SignInAction.OnEmailChanged -> {
                _state.update { it.copy(email = action.value) }
                validateFormState()
            }
            is SignInAction.OnPasswordChanged -> {
                _state.update { it.copy(password = action.value) }
                validateFormState()
            }
            is SignInAction.OnConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = action.value) }
                validateFormState()
            }
            is SignInAction.OnImageSelected -> {
                _state.update { it.copy(profilePictureUri = action.uri?.toString()) }
            }
            is SignInAction.OnCreateAccountClicked -> validateAndCreate()
        }
    }

    private fun validateFormState() {
        val currentState = _state.value
        val name = currentState.name
        val surname = currentState.surname
        val username = currentState.username
        val email = currentState.email
        val pass = currentState.password
        val confirmPass = currentState.confirmPassword

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

        val isPasswordRegexValid = pass.matches(passwordRegex)
        if (pass.isNotBlank() && confirmPass.isNotBlank() && !isPasswordRegexValid) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero.",
                    isButtonEnabled = false
                )
            }
            return
        }

        val isEmailFormatValid = email.isBlank() || (email.contains("@") && email.contains("."))
        if (!isEmailFormatValid) {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "Inserisci un indirizzo email valido.",
                    isButtonEnabled = false
                )
            }
            return
        }

        val allFieldsFilled = name.isNotBlank() && surname.isNotBlank() &&
                username.isNotBlank() && email.isNotBlank() &&
                pass.isNotBlank() && confirmPass.isNotBlank()

        val shouldEnable = allFieldsFilled && isPasswordRegexValid && pass == confirmPass

        _state.update {
            it.copy(
                isError = false,
                errorMessage = null,
                isButtonEnabled = shouldEnable
            )
        }
    }

    private fun validateAndCreate() {
        val currentState = _state.value
        if (currentState.password != currentState.confirmPassword || !currentState.password.matches(passwordRegex)) {
            _state.update { it.copy(isError = true, errorMessage = "Dati non validi.", isButtonEnabled = false) }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }

            val request = RegisterRequest(
                email = currentState.email.trim(),
                password = currentState.password,
                name = currentState.name.trim(),
                surname = currentState.surname.trim(),
                username = currentState.username.trim()
            )

            val success = userRepository.register(request)

            if (success) {
                _state.update { it.copy(isLoading = false, isRegistrationSuccessful = true) }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = "Errore durante la registrazione. Email o Username potrebbero essere già in uso.",
                        isButtonEnabled = true
                    )
                }
            }
        }
    }
}