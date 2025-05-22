package com.lymors.phonesecure.presentation.ui.emergency

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class EmergencyAlertViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmergencyAlertUiState>(EmergencyAlertUiState.Initial)
    val uiState: StateFlow<EmergencyAlertUiState> = _uiState

    fun loadEmergencyContacts() {
        viewModelScope.launch {
            _uiState.value = EmergencyAlertUiState.Loading
            try {
                val user = userRepository.getCurrentUser()
                if (user.emergencyContacts.isEmpty()) {
                    _uiState.value = EmergencyAlertUiState.NoContacts
                } else {
                    _uiState.value = EmergencyAlertUiState.Ready(user.emergencyContacts)
                }
            } catch (e: Exception) {
                _uiState.value = EmergencyAlertUiState.Error("Failed to load emergency contacts")
            }
        }
    }

    fun sendEmergencyAlert(location: Location?) {
        val currentState = _uiState.value
        if (currentState is EmergencyAlertUiState.Ready) {
            _uiState.value = EmergencyAlertUiState.Sending
            
            viewModelScope.launch {
                try {
                    // In a real implementation, this would handle the SMS sending logic
                    // For now, we're just simulating success after a delay
                    _uiState.value = EmergencyAlertUiState.Success(currentState.contacts)
                } catch (e: Exception) {
                    _uiState.value = EmergencyAlertUiState.Error("Failed to send alert: ${e.message}")
                }
            }
        }
    }
}

sealed class EmergencyAlertUiState {
    object Initial : EmergencyAlertUiState()
    object Loading : EmergencyAlertUiState()
    object NoContacts : EmergencyAlertUiState()
    object Sending : EmergencyAlertUiState()
    data class Ready(val contacts: List<EmergencyContact>) : EmergencyAlertUiState()
    data class Success(val contacts: List<EmergencyContact>) : EmergencyAlertUiState()
    data class Error(val message: String) : EmergencyAlertUiState()
}
