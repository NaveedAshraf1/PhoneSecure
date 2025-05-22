package com.lymors.phonesecure.presentation.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.usecase.IntruderDetectionUseCase
import com.lymors.phonesecure.domain.usecase.SimChangeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for configuring advanced security settings
 */
class SecuritySettingsFragment : PreferenceFragmentCompat() {
    
    private lateinit var intruderDetectionUseCase: IntruderDetectionUseCase
    private lateinit var simChangeUseCase: SimChangeUseCase
    
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_security, rootKey)
        
        // Initialize use cases
        initializeUseCases()
        
        // Set up preference listeners
        setupPreferenceListeners()
        
        // Update switch preferences based on current settings
        updateSwitchPreferences()
    }
    
    private fun initializeUseCases() {
        val app = requireActivity().application as PhoneSecureApp
        intruderDetectionUseCase = app.getIntruderDetectionUseCase()
        simChangeUseCase = app.getSimChangeUseCase()
    }
    
    private fun setupPreferenceListeners() {
        // Setup photo capture on failed unlock preference
        val captureOnFailedUnlockPref = findPreference<SwitchPreferenceCompat>("capture_on_failed_unlock")
        captureOnFailedUnlockPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    if (enabled) {
                        intruderDetectionUseCase.enableCaptureOnFailedUnlock()
                    } else {
                        intruderDetectionUseCase.disableCaptureOnFailedUnlock()
                    }
                }
            }
            true
        }
        
        // Setup photo capture on SIM change preference
        val captureOnSimChangePref = findPreference<SwitchPreferenceCompat>("capture_on_sim_change")
        captureOnSimChangePref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    if (enabled) {
                        simChangeUseCase.enableCaptureOnSimChange()
                    } else {
                        simChangeUseCase.disableCaptureOnSimChange()
                    }
                }
            }
            true
        }
        
        // Setup SMS alert on SIM change preference
        val smsOnSimChangePref = findPreference<SwitchPreferenceCompat>("sms_on_sim_change")
        smsOnSimChangePref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    if (enabled) {
                        simChangeUseCase.enableSmsOnSimChange()
                    } else {
                        simChangeUseCase.disableSmsOnSimChange()
                    }
                }
            }
            true
        }
        
        // Setup email alert on SIM change preference
        val emailOnSimChangePref = findPreference<SwitchPreferenceCompat>("email_on_sim_change")
        emailOnSimChangePref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    if (enabled) {
                        simChangeUseCase.enableEmailOnSimChange()
                    } else {
                        simChangeUseCase.disableEmailOnSimChange()
                    }
                }
            }
            true
        }
    }
    
    private fun updateSwitchPreferences() {
        mainScope.launch {
            // Update capture on failed unlock preference
            val captureOnFailedUnlockEnabled = withContext(Dispatchers.IO) {
                intruderDetectionUseCase.isCaptureOnFailedUnlockEnabled()
            }
            findPreference<SwitchPreferenceCompat>("capture_on_failed_unlock")?.isChecked = captureOnFailedUnlockEnabled
            
            // Update capture on SIM change preference
            val captureOnSimChangeEnabled = withContext(Dispatchers.IO) {
                simChangeUseCase.isCaptureOnSimChangeEnabled()
            }
            findPreference<SwitchPreferenceCompat>("capture_on_sim_change")?.isChecked = captureOnSimChangeEnabled
            
            // Update SMS on SIM change preference
            val smsOnSimChangeEnabled = withContext(Dispatchers.IO) {
                simChangeUseCase.shouldSendSmsOnSimChange()
            }
            findPreference<SwitchPreferenceCompat>("sms_on_sim_change")?.isChecked = smsOnSimChangeEnabled
            
            // Update email on SIM change preference
            val emailOnSimChangeEnabled = withContext(Dispatchers.IO) {
                simChangeUseCase.shouldSendEmailOnSimChange()
            }
            findPreference<SwitchPreferenceCompat>("email_on_sim_change")?.isChecked = emailOnSimChangeEnabled
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateSwitchPreferences()
    }
}
