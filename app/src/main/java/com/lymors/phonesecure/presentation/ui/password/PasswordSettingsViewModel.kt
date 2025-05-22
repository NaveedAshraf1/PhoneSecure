package com.lymors.phonesecure.presentation.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lymors.phonesecure.domain.model.PasswordSettings
import com.lymors.phonesecure.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordSettingsUiState>(PasswordSettingsUiState.Loading)
    val uiState: StateFlow<PasswordSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = PasswordSettingsUiState.Loading
            try {
                val settings = userRepository.getPasswordSettings()
                _uiState.value = PasswordSettingsUiState.Success(settings)
            } catch (e: Exception) {
                _uiState.value = PasswordSettingsUiState.Error("Failed to load password settings: ${e.message}")
            }
        }
    }

    fun saveSettings(settings: PasswordSettings) {
        viewModelScope.launch {
            _uiState.value = PasswordSettingsUiState.Loading
            try {
                val success = userRepository.savePasswordSettings(settings)
                if (success) {
                    _uiState.value = PasswordSettingsUiState.Success(settings)
                } else {
                    _uiState.value = PasswordSettingsUiState.Error("Failed to save password settings")
                }
            } catch (e: Exception) {
                _uiState.value = PasswordSettingsUiState.Error("Failed to save password settings: ${e.message}")
            }
        }
    }

    fun updatePasswordEnabled(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is PasswordSettingsUiState.Success) {
            val updatedSettings = currentState.settings.copy(passwordEnabled = enabled)
            _uiState.value = PasswordSettingsUiState.Success(updatedSettings)
        }
    }

    fun updatePassword(password: String) {
        val currentState = _uiState.value
        if (currentState is PasswordSettingsUiState.Success) {
            val updatedSettings = currentState.settings.copy(password = password)
            _uiState.value = PasswordSettingsUiState.Success(updatedSettings)
        }
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        if (enabled) {
            // Only enable biometric if password is also enabled
            if (!_uiState.value.settings.passwordEnabled) {
                _uiState.value = _uiState.value.copy(
                    error = "Password protection must be enabled to use biometric authentication"
                )
                return
            }
        }
        
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(useBiometric = enabled),
            error = null
        )
    }

    fun updateLockAfterTimeout(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is PasswordSettingsUiState.Success) {
            val updatedSettings = currentState.settings.copy(lockAfterTimeout = enabled)
            _uiState.value = PasswordSettingsUiState.Success(updatedSettings)
        }
    }

    fun updateTimeoutMinutes(minutes: Int) {
        val currentState = _uiState.value
        if (currentState is PasswordSettingsUiState.Success) {
            val updatedSettings = currentState.settings.copy(timeoutMinutes = minutes)
            _uiState.value = PasswordSettingsUiState.Success(updatedSettings)
        }
    }
}

sealed class PasswordSettingsUiState {
    object Loading : PasswordSettingsUiState()
    data class Success(
        val settings: PasswordSettings,
        val error: String? = null
    ) : PasswordSettingsUiState()
    data class Error(val message: String) : PasswordSettingsUiState()
}
