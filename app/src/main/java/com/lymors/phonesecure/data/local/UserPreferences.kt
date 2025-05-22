package com.lymors.phonesecure.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.SecuritySettings
import com.lymors.phonesecure.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val userFlow = MutableStateFlow<User?>(null)

    init {
        // Initialize userFlow with stored data
        val storedUser = getStoredUser()
        userFlow.value = storedUser
    }

    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .putString(KEY_USER_NAME, user.name)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_PHONE, user.phoneNumber)
                .apply()
            
            // Update the flow
            userFlow.value = user
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserFlow(): Flow<User?> = userFlow.asStateFlow()

    suspend fun getUser(): User? = withContext(Dispatchers.IO) {
        getStoredUser()
    }

    suspend fun saveSecuritySettings(settings: SecuritySettings): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(settings)
            prefs.edit().putString(KEY_SECURITY_SETTINGS, json).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSecuritySettings(): SecuritySettings = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_SECURITY_SETTINGS, null)
        if (json != null) {
            try {
                gson.fromJson(json, SecuritySettings::class.java)
            } catch (e: Exception) {
                SecuritySettings() // Return default settings on error
            }
        } else {
            SecuritySettings() // Return default settings if none stored
        }
    }

    suspend fun saveEmergencyContacts(contacts: List<EmergencyContact>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(contacts)
            prefs.edit().putString(KEY_EMERGENCY_CONTACTS, json).apply()
            
            // Update the flow with new contacts
            val currentUser = userFlow.value
            if (currentUser != null) {
                userFlow.value = currentUser.copy(emergencyContacts = contacts)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getEmergencyContacts(): List<EmergencyContact> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_EMERGENCY_CONTACTS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<EmergencyContact>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun getStoredUser(): User? {
        val name = prefs.getString(KEY_USER_NAME, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val phone = prefs.getString(KEY_USER_PHONE, null)

        return if (name != null) {
            User(
                name = name,
                email = email ?: "",
                phoneNumber = phone ?: "",
                emergencyContacts = getEmergencyContactsSync()
            )
        } else {
            null
        }
    }

    private fun getEmergencyContactsSync(): List<EmergencyContact> {
        val json = prefs.getString(KEY_EMERGENCY_CONTACTS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<EmergencyContact>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_SECURITY_SETTINGS = "security_settings"
        private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
    }
}
