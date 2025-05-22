package com.lymors.phonesecure.presentation.ui.antitheft

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lymors.phonesecure.domain.model.AntiTheftSettings
import com.lymors.phonesecure.domain.repository.UserRepository
import com.lymors.phonesecure.service.AntiTheftService
import com.lymors.phonesecure.util.MotionDetector
import com.lymors.phonesecure.util.RemoteLockManager
import com.lymors.phonesecure.util.SimChangeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AntiTheftViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AntiTheftUiState>(AntiTheftUiState.Loading)
    val uiState: StateFlow<AntiTheftUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = AntiTheftUiState.Loading
            try {
                val settings = userRepository.getAntiTheftSettings()
                _uiState.value = AntiTheftUiState.Success(settings)
            } catch (e: Exception) {
                _uiState.value = AntiTheftUiState.Error("Failed to load anti-theft settings: ${e.message}")
            }
        }
    }

    fun saveSettings(settings: AntiTheftSettings) {
        viewModelScope.launch {
            _uiState.value = AntiTheftUiState.Loading
            try {
                val success = userRepository.saveAntiTheftSettings(settings)
                if (success) {
                    _uiState.value = AntiTheftUiState.Success(settings)
                    
                    // Initialize utility classes based on settings
                    val context = getApplication<Application>().applicationContext
                    
                    // Configure SimChangeDetector
                    if (settings.simChangeDetectionEnabled) {
                        val simChangeDetector = SimChangeDetector.getInstance(context)
                        simChangeDetector.initialize()
                    }
                    
                    // Configure MotionDetector
                    val motionDetector = MotionDetector.getInstance(context)
                    if (settings.motionDetectionEnabled) {
                        motionDetector.startMonitoring(settings.motionSensitivity)
                    } else {
                        motionDetector.stopMonitoring()
                    }
                    
                    // Configure RemoteLockManager
                    if (settings.remoteLockEnabled) {
                        val remoteLockManager = RemoteLockManager.getInstance(context)
                        remoteLockManager.setSecretCode(settings.secretCode)
                    }
                    
                } else {
                    _uiState.value = AntiTheftUiState.Error("Failed to save anti-theft settings")
                }
            } catch (e: Exception) {
                _uiState.value = AntiTheftUiState.Error("Failed to save anti-theft settings: ${e.message}")
            }
        }
    }

    fun updateSimChangeDetection(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(simChangeDetectionEnabled = enabled)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateMotionDetection(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(motionDetectionEnabled = enabled)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateMotionSensitivity(sensitivity: Int) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(motionSensitivity = sensitivity)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateWrongPasswordDetection(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(wrongPasswordDetectionEnabled = enabled)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateMaxPasswordAttempts(attempts: Int) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(maxPasswordAttempts = attempts)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateRemoteLock(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(remoteLockEnabled = enabled)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }

    fun updateSecretCode(code: String) {
        val currentState = _uiState.value
        if (currentState is AntiTheftUiState.Success) {
            val updatedSettings = currentState.settings.copy(secretCode = code)
            _uiState.value = AntiTheftUiState.Success(updatedSettings)
        }
    }
}

sealed class AntiTheftUiState {
    object Loading : AntiTheftUiState()
    data class Success(val settings: AntiTheftSettings) : AntiTheftUiState()
    data class Error(val message: String) : AntiTheftUiState()
}
