package com.lymors.phonesecure.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.User
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class UserProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _uiState.value = UserProfileUiState.Success(
                    user ?: User(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        email = "",
                        phoneNumber = "",
                        emergencyContacts = emptyList()
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error("Failed to load user profile")
            }
        }
    }

    fun saveUserProfile(name: String, email: String, phone: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UserProfileUiState.Success) {
                    val updatedUser = currentState.user.copy(
                        name = name,
                        email = email,
                        phoneNumber = phone
                    )
                    val success = userRepository.saveUser(updatedUser)
                    if (success) {
                        _uiState.value = UserProfileUiState.Success(updatedUser)
                    } else {
                        _uiState.value = UserProfileUiState.Error("Failed to save user profile")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error("Failed to save user profile")
            }
        }
    }

    fun addEmergencyContact(name: String, phone: String) {
        viewModelScope.launch {
            try {
                val contact = EmergencyContact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phoneNumber = phone
                )
                val success = userRepository.addEmergencyContact(contact)
                if (success) {
                    loadUserProfile() // Reload to get updated contacts
                }
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error("Failed to add emergency contact")
            }
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.deleteEmergencyContact(contactId)
                if (success) {
                    loadUserProfile() // Reload to get updated contacts
                }
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error("Failed to delete emergency contact")
            }
        }
    }
}

sealed class UserProfileUiState {
    object Loading : UserProfileUiState()
    data class Success(val user: User) : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
}
