package com.lymors.phonesecure.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.data.local.UserPreferences
import com.lymors.phonesecure.domain.model.AntiTheftSettings
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.PasswordSettings
import com.lymors.phonesecure.domain.model.SecuritySettings
import com.lymors.phonesecure.domain.model.User
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementation of UserRepository that uses SharedPreferences for storage
 */
class UserRepositoryImpl @Inject constructor() : UserRepository {
    
    private val context: Context = PhoneSecureApp.getAppContext()
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val antiTheftSettingsFlow = MutableStateFlow(AntiTheftSettings())
    private val passwordSettingsFlow = MutableStateFlow(PasswordSettings())
    private val _userFlow = MutableStateFlow<User?>(null)
    
    init {
        // Initialize user flow with stored user
        val userJson = prefs.getString("user", null)
        if (userJson != null) {
            val user = gson.fromJson(userJson, User::class.java)
            _userFlow.value = user
        }
    }
    
    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userJson = prefs.getString("user", null)
        if (userJson != null) {
            return@withContext gson.fromJson(userJson, User::class.java)
        }
        return@withContext null
    }
    
    override suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        val userJson = gson.toJson(user)
        val success = prefs.edit().putString("user", userJson).commit()
        if (success) {
            _userFlow.value = user
        }
        return@withContext success
    }
    
    override fun getUserFlow(): Flow<User?> = _userFlow.asStateFlow()
    
    override suspend fun getSecuritySettings(): SecuritySettings = withContext(Dispatchers.IO) {
        val user = getCurrentUser()
        return@withContext user?.securitySettings ?: SecuritySettings()
    }
    
    override suspend fun updateSecuritySettings(settings: SecuritySettings): Boolean = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: createDefaultUser()
        val updatedUser = user.copy(securitySettings = settings)
        return@withContext saveUser(updatedUser)
    }
    
    override suspend fun getEmergencyContacts(): List<EmergencyContact> = withContext(Dispatchers.IO) {
        val user = getCurrentUser()
        return@withContext user?.emergencyContacts ?: emptyList()
    }
    
    override suspend fun addEmergencyContact(contact: EmergencyContact): Boolean = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: createDefaultUser()
        val updatedContacts = user.emergencyContacts.toMutableList().apply {
            add(contact)
        }
        val updatedUser = user.copy(emergencyContacts = updatedContacts)
        return@withContext saveUser(updatedUser)
    }
    
    override suspend fun updateEmergencyContact(contact: EmergencyContact): Boolean = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext false
        val updatedContacts = user.emergencyContacts.toMutableList().apply {
            val index = indexOfFirst { it.id == contact.id }
            if (index != -1) {
                set(index, contact)
            }
        }
        val updatedUser = user.copy(emergencyContacts = updatedContacts)
        return@withContext saveUser(updatedUser)
    }
    
    override suspend fun deleteEmergencyContact(contactId: String): Boolean {
        val user = getCurrentUser()
        val updatedContacts = user.emergencyContacts.filter { it.id != contactId }
        return saveUser(user.copy(emergencyContacts = updatedContacts))
    }
    
    override suspend fun getAntiTheftSettings(): AntiTheftSettings {
        return withContext(Dispatchers.IO) {
            val json = prefs.getString(KEY_ANTI_THEFT_SETTINGS, null)
            if (json != null) {
                try {
                    val settings = gson.fromJson(json, AntiTheftSettings::class.java)
                    antiTheftSettingsFlow.value = settings
                    return@withContext settings
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val defaultSettings = AntiTheftSettings()
            antiTheftSettingsFlow.value = defaultSettings
            return@withContext defaultSettings
        }
    }
    
    override suspend fun saveAntiTheftSettings(settings: AntiTheftSettings): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(settings)
                val success = prefs.edit().putString(KEY_ANTI_THEFT_SETTINGS, json).commit()
                if (success) {
                    antiTheftSettingsFlow.value = settings
                }
                return@withContext success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }
    
    override fun getAntiTheftSettingsFlow(): Flow<AntiTheftSettings> {
        // Initialize if not already done
        if (antiTheftSettingsFlow.value == AntiTheftSettings()) {
            CoroutineScope(Dispatchers.IO).launch {
                getAntiTheftSettings()
            }
        }
        return antiTheftSettingsFlow.asStateFlow()
    }
    
    override suspend fun getPasswordSettings(): PasswordSettings {
        return withContext(Dispatchers.IO) {
            val json = prefs.getString(KEY_PASSWORD_SETTINGS, null)
            if (json != null) {
                try {
                    val settings = gson.fromJson(json, PasswordSettings::class.java)
                    passwordSettingsFlow.value = settings
                    return@withContext settings
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val defaultSettings = PasswordSettings()
            passwordSettingsFlow.value = defaultSettings
            return@withContext defaultSettings
        }
    }
    
    override suspend fun savePasswordSettings(settings: PasswordSettings): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(settings)
                val success = prefs.edit().putString(KEY_PASSWORD_SETTINGS, json).commit()
                if (success) {
                    passwordSettingsFlow.value = settings
                }
                return@withContext success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }
    
    override fun getPasswordSettingsFlow(): Flow<PasswordSettings> {
        // Initialize if not already done
        if (passwordSettingsFlow.value == PasswordSettings()) {
            CoroutineScope(Dispatchers.IO).launch {
                getPasswordSettings()
            }
        }
        return passwordSettingsFlow.asStateFlow()
    }
    
    override suspend fun verifyPassword(password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val settings = getPasswordSettings()
            // Check if too many failed attempts within cooldown period
            if (settings.failedAttempts >= settings.maxFailedAttempts) {
                val cooldownPeriod = 5 * 60 * 1000L // 5 minutes
                val timeSinceLastFailure = System.currentTimeMillis() - settings.lastFailedTimestamp
                if (timeSinceLastFailure < cooldownPeriod) {
                    return@withContext false
                } else {
                    // Reset attempts after cooldown
                    resetFailedPasswordAttempts()
                }
            }
            return@withContext settings.passwordEnabled && settings.password == password
        }
    }
    
    override suspend fun incrementFailedPasswordAttempts(): Int {
        return withContext(Dispatchers.IO) {
            val settings = getPasswordSettings()
            val updatedSettings = settings.copy(
                failedAttempts = settings.failedAttempts + 1,
                lastFailedTimestamp = System.currentTimeMillis()
            )
            savePasswordSettings(updatedSettings)
            return@withContext updatedSettings.failedAttempts
        }
    }
    
    override suspend fun resetFailedPasswordAttempts() {
        withContext(Dispatchers.IO) {
            val settings = getPasswordSettings()
            val updatedSettings = settings.copy(
                failedAttempts = 0,
                lastFailedTimestamp = 0L
            )
            savePasswordSettings(updatedSettings)
        }
    }

    override suspend fun getMaxFailedPasswordAttempts(): Int {
        return withContext(Dispatchers.IO) {
            val settings = getPasswordSettings()
            return@withContext settings.maxFailedAttempts
        }
    }
    
    private fun createDefaultUser(): User {
        return User(
            id = UUID.randomUUID().toString(),
            name = "",
            email = "",
            phone = "",
            emergencyContacts = emptyList(),
            securitySettings = SecuritySettings()
        )
    }
    
    companion object {
        private const val PREFS_NAME = "phone_secure_prefs"
        private const val KEY_USER = "user"
        private const val KEY_ANTI_THEFT_SETTINGS = "anti_theft_settings"
        private const val KEY_PASSWORD_SETTINGS = "password_settings"
    }
}
