package com.example.unisphere.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = true,
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUri: String? = null,
    val currentTheme: String = "Default",
    val isLoggedIn: Boolean = false,
    val dialogError: String? = null,

    val showEditUsernameDialog: Boolean = false,
    val showEditEmailDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showAppInfoDialog: Boolean = false,
    val tempUsernameText: String = "",
    val tempEmailText: String = "",

    val isLoggedOut: Boolean = false
)

sealed interface ProfileAction {
    data class OnUsernameDialogToggle(val show: Boolean) : ProfileAction
    data class OnEmailDialogToggle(val show: Boolean) : ProfileAction
    data class OnThemeDialogToggle(val show: Boolean) : ProfileAction
    data class OnAppInfoDialogToggle(val show: Boolean) : ProfileAction
    data class OnTempUsernameChanged(val value: String) : ProfileAction
    data class OnTempEmailChanged(val value: String) : ProfileAction
    data class OnThemeSelected(val theme: String) : ProfileAction
    data class OnProfileImageUpdated(val uri: String) : ProfileAction
    data object OnSaveUsernameClicked : ProfileAction
    data object OnSaveEmailClicked : ProfileAction
    data object OnLogoutClicked : ProfileAction
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val profile = userRepository.getCurrentUserProfile()

            android.util.Log.d(
                "THEME_DEBUG",
                "loadUserProfile | " +
                        "VM=${System.identityHashCode(this@ProfileViewModel)} | " +
                        "profileNull=${profile == null} | " +
                        "theme=${profile?.currentTheme}"
            )
            if (profile != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        name = profile.name,
                        surname = profile.surname,
                        username = profile.username,
                        email = profile.email,
                        profilePictureUri = profile.profilePictureUri,
                        currentTheme = profile.currentTheme
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, isLoggedIn = false) }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnUsernameDialogToggle -> {
                _state.update {
                    it.copy(
                        showEditUsernameDialog = action.show,
                        tempUsernameText = if (action.show) it.username else "",
                        dialogError = null
                    )
                }
            }
            is ProfileAction.OnEmailDialogToggle -> {
                _state.update {
                    it.copy(
                        showEditEmailDialog = action.show,
                        tempEmailText = if (action.show) it.email else "",
                        dialogError = null
                    )
                }
            }
            is ProfileAction.OnThemeDialogToggle -> _state.update { it.copy(showThemeDialog = action.show) }
            is ProfileAction.OnAppInfoDialogToggle -> _state.update { it.copy(showAppInfoDialog = action.show) }
            is ProfileAction.OnTempUsernameChanged -> _state.update { it.copy(tempUsernameText = action.value) }
            is ProfileAction.OnTempEmailChanged -> _state.update { it.copy(tempEmailText = action.value) }
            is ProfileAction.OnThemeSelected -> setTheme(action.theme)
            is ProfileAction.OnProfileImageUpdated -> updateProfileImage(action.uri)
            ProfileAction.OnSaveUsernameClicked -> saveUsername()
            ProfileAction.OnSaveEmailClicked -> saveEmail()
            is ProfileAction.OnLogoutClicked -> logout()
        }
    }

    private fun updateProfileImage(uriString: String) {
        // Aggiorna subito la UI
        _state.update { it.copy(profilePictureUri = uriString) }
        viewModelScope.launch {
            // Chiama Ktor per aggiornare sul server
            userRepository.updateProfileImage(uriString)
        }
    }

    private fun saveUsername() {
        val currentState = _state.value
        val newUsername = currentState.tempUsernameText
        if (newUsername.trim().isBlank()) {
            _state.update { it.copy(dialogError = "Lo username non può essere vuoto.") }
            return
        }

        if (newUsername == currentState.username) {
            _state.update { it.copy(showEditUsernameDialog = false) }
            return
        }

        viewModelScope.launch {
            // Chiama Ktor per l'update (Conflict gestito dal bool restituito)
            val success = userRepository.updateUsername(newUsername)
            if (success) {
                _state.update {
                    it.copy(
                        username = newUsername,
                        dialogError = null,
                        showEditUsernameDialog = false
                    )
                }
            } else {
                _state.update { it.copy(dialogError = "Questo username è già in uso.") }
            }
        }
    }

    private fun saveEmail() {
        val currentState = _state.value
        val newEmail = currentState.tempEmailText
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            _state.update { it.copy(dialogError = "Inserisci un'email valida.") }
            return
        }

        if (newEmail == currentState.email) {
            _state.update { it.copy(showEditEmailDialog = false) }
            return
        }

        viewModelScope.launch {
            val success = userRepository.updateEmail(newEmail)
            if (success) {
                _state.update {
                    it.copy(
                        email = newEmail,
                        dialogError = null,
                        showEditEmailDialog = false
                    )
                }
            } else {
                _state.update { it.copy(dialogError = "Questa email è già associata a un account.") }
            }
        }
    }

    private fun setTheme(theme: String) {
        android.util.Log.d(
            "THEME_DEBUG",
            "setTheme | " +
                    "VM=${System.identityHashCode(this)} | " +
                    "themeSelezionato=$theme | " +
                    "isLoggedIn=${_state.value.isLoggedIn} | " +
                    "themePrecedente=${_state.value.currentTheme}"
        )
        // Aggiorna subito lo stato osservato da MainActivity
        _state.update {
            it.copy(
                currentTheme = theme,
                showThemeDialog = false
            )
        }

        // Persistenza sul backend indipendente dal cambio visivo
        viewModelScope.launch {
            val success = userRepository.updateLocalTheme(theme)

            if (!success) {
                _state.update {
                    it.copy(
                        dialogError = "Tema aggiornato sul dispositivo, ma non salvato sul server."
                    )
                }
            }
        }
    }

    private fun logout() {
        userRepository.logout()
        _state.update { it.copy(isLoggedOut = true) }
    }
}
