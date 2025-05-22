package com.lymors.phonesecure.presentation.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lymors.phonesecure.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordLoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordLoginUiState>(PasswordLoginUiState.Initial)
    val uiState: StateFlow<PasswordLoginUiState> = _uiState.asStateFlow()

    fun verifyPassword(password: String) {
        if (password.isBlank()) {
            _uiState.value = PasswordLoginUiState.Error("Password is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = PasswordLoginUiState.Loading
            try {
                val isValid = userRepository.verifyPassword(password)
                if (isValid) {
                    // Reset failed attempts on successful login
                    userRepository.resetFailedPasswordAttempts()
                    _uiState.value = PasswordLoginUiState.Success
                } else {
                    // Increment failed attempts
                    val currentAttempts = userRepository.incrementFailedPasswordAttempts()
                    val maxAttempts = userRepository.getMaxFailedPasswordAttempts()
                    
                    if (currentAttempts >= maxAttempts) {
                        _uiState.value = PasswordLoginUiState.Error("Too many failed attempts. Try again later.")
                    } else {
                        _uiState.value = PasswordLoginUiState.Error("Invalid password (${currentAttempts}/${maxAttempts} attempts)")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PasswordLoginUiState.Error(e.message ?: "Error verifying password")
            }
        }
    }

    fun resetState() {
        _uiState.value = PasswordLoginUiState.Initial
    }

    fun isBiometricAvailable() {
        viewModelScope.launch {
            val settings = userRepository.getPasswordSettings()
            _uiState.value = PasswordLoginUiState.BiometricAvailability(settings.useBiometric)
        }
    }

    fun biometricAuthSuccess() {
        viewModelScope.launch {
            userRepository.resetFailedPasswordAttempts()
            _uiState.value = PasswordLoginUiState.Success
        }
    }

    fun biometricAuthFailed(errorMessage: String) {
        _uiState.value = PasswordLoginUiState.Error(errorMessage)
    }
}

sealed class PasswordLoginUiState {
    object Initial : PasswordLoginUiState()
    object Loading : PasswordLoginUiState()
    object Success : PasswordLoginUiState()
    object TooManyAttempts : PasswordLoginUiState()
    data class Error(val message: String) : PasswordLoginUiState()
    data class BiometricAvailability(val available: Boolean) : PasswordLoginUiState()
}
