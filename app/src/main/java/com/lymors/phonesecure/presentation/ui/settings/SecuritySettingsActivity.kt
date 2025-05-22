package com.lymors.phonesecure.presentation.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.lymors.phonesecure.R
import com.lymors.phonesecure.databinding.ActivitySecuritySettingsBinding
import com.lymors.phonesecure.util.setupToolbar

/**
 * Activity for managing security-related settings
 */
class SecuritySettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySecuritySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecuritySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        setupToolbar(showBackButton = true)
        title = getString(R.string.security_settings)
        
        // Set up preferences
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SecuritySettingsPreferenceFragment())
            .commit()
    }
    
    /**
     * Security settings preference fragment
     */
    class SecuritySettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_security, rootKey)
            
            // Set up any preference change listeners here
        }
    }
}
