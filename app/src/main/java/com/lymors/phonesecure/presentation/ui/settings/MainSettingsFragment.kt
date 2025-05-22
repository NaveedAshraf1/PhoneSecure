package com.lymors.phonesecure.presentation.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.usecase.FakeShutdownUseCase
import com.lymors.phonesecure.domain.usecase.IntruderDetectionUseCase
import com.lymors.phonesecure.domain.usecase.LocationTrackingUseCase
import com.lymors.phonesecure.domain.usecase.SimChangeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main settings fragment that displays all available setting categories
 */
class MainSettingsFragment : PreferenceFragmentCompat() {
    
    private lateinit var fakeShutdownUseCase: FakeShutdownUseCase
    private lateinit var intruderDetectionUseCase: IntruderDetectionUseCase
    private lateinit var locationTrackingUseCase: LocationTrackingUseCase
    private lateinit var simChangeUseCase: SimChangeUseCase
    
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)
        
        // Initialize use cases
        initializeUseCases()
        
        // Set up preference click listeners
        setupPreferenceListeners()
        
        // Update switch preferences based on current settings
        updateSwitchPreferences()
    }
    
    private fun initializeUseCases() {
        val app = requireActivity().application as PhoneSecureApp
        fakeShutdownUseCase = app.getFakeShutdownUseCase()
        intruderDetectionUseCase = app.getIntruderDetectionUseCase()
        locationTrackingUseCase = app.getLocationTrackingUseCase()
        simChangeUseCase = app.getSimChangeUseCase()
    }
    
    private fun setupPreferenceListeners() {
        // Setup fake shutdown preference
        val fakeShutdownPref = findPreference<SwitchPreferenceCompat>("fake_shutdown_enabled")
        fakeShutdownPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                if (enabled) {
                    withContext(Dispatchers.IO) {
                        fakeShutdownUseCase.enableFakeShutdown()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        fakeShutdownUseCase.disableFakeShutdown()
                    }
                }
            }
            true
        }
        
        // Setup intruder detection preference
        val intruderDetectionPref = findPreference<SwitchPreferenceCompat>("intruder_detection_enabled")
        intruderDetectionPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                if (enabled) {
                    withContext(Dispatchers.IO) {
                        intruderDetectionUseCase.enableIntruderDetection()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        intruderDetectionUseCase.disableIntruderDetection()
                    }
                }
            }
            true
        }
        
        // Setup location tracking preference
        val locationTrackingPref = findPreference<SwitchPreferenceCompat>("location_tracking_enabled")
        locationTrackingPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                if (enabled) {
                    withContext(Dispatchers.IO) {
                        locationTrackingUseCase.enableLocationTracking()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        locationTrackingUseCase.disableLocationTracking()
                    }
                }
            }
            true
        }
        
        // Setup SIM change detection preference
        val simChangePref = findPreference<SwitchPreferenceCompat>("sim_change_alert_enabled")
        simChangePref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            mainScope.launch {
                if (enabled) {
                    withContext(Dispatchers.IO) {
                        simChangeUseCase.enableSimChangeDetection()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        simChangeUseCase.disableSimChangeDetection()
                    }
                }
            }
            true
        }
        
        // Setup navigation to other preference screens
        setupNavigationPreferences()
    }
    
    private fun setupNavigationPreferences() {
        // User profile preference
        findPreference<Preference>("user_profile")?.setOnPreferenceClickListener {
            navigateToFragment(UserProfileFragment())
            true
        }
        
        // Emergency contacts preference
        findPreference<Preference>("emergency_contacts")?.setOnPreferenceClickListener {
            navigateToFragment(EmergencyContactsFragment())
            true
        }
        
        // Security settings preference
        findPreference<Preference>("security_settings")?.setOnPreferenceClickListener {
            navigateToFragment(SecuritySettingsFragment())
            true
        }
        
        // About preference
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            navigateToFragment(AboutFragment())
            true
        }
    }
    
    private fun navigateToFragment(fragment: PreferenceFragmentCompat) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun updateSwitchPreferences() {
        mainScope.launch {
            // Update fake shutdown preference
            val fakeShutdownEnabled = withContext(Dispatchers.IO) {
                fakeShutdownUseCase.isFakeShutdownEnabled()
            }
            findPreference<SwitchPreferenceCompat>("fake_shutdown_enabled")?.isChecked = fakeShutdownEnabled
            
            // Update intruder detection preference
            val intruderDetectionEnabled = withContext(Dispatchers.IO) {
                intruderDetectionUseCase.isIntruderDetectionEnabled()
            }
            findPreference<SwitchPreferenceCompat>("intruder_detection_enabled")?.isChecked = intruderDetectionEnabled
            
            // Update location tracking preference
            val locationTrackingEnabled = withContext(Dispatchers.IO) {
                locationTrackingUseCase.isLocationTrackingEnabled()
            }
            findPreference<SwitchPreferenceCompat>("location_tracking_enabled")?.isChecked = locationTrackingEnabled
            
            // Update SIM change detection preference
            val simChangeEnabled = withContext(Dispatchers.IO) {
                simChangeUseCase.isSimChangeDetectionEnabled()
            }
            findPreference<SwitchPreferenceCompat>("sim_change_alert_enabled")?.isChecked = simChangeEnabled
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateSwitchPreferences()
    }
}
