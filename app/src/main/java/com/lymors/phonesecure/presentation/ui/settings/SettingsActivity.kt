package com.lymors.phonesecure.presentation.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lymors.phonesecure.R
import com.lymors.phonesecure.databinding.ActivitySettingsBinding
import com.lymors.phonesecure.presentation.ui.about.AboutActivity
import com.lymors.phonesecure.presentation.ui.contacts.EmergencyContactsActivity
import com.lymors.phonesecure.presentation.ui.profile.UserProfileActivity
import com.lymors.phonesecure.util.setupToolbar

/**
 * Activity for managing app settings
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        setupToolbar(showBackButton = true)
        
        // Set up preferences
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, MainSettingsPreferenceFragment())
            .commit()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    /**
     * Main settings preference fragment
     */
    class MainSettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_main, rootKey)
            
            // Set up preference click listeners
            findPreference<Preference>("user_profile")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), UserProfileActivity::class.java))
                true
            }
            
            findPreference<Preference>("emergency_contacts")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), EmergencyContactsActivity::class.java))
                true
            }
            
            findPreference<Preference>("security_settings")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), SecuritySettingsActivity::class.java))
                true
            }
            
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                true
            }
        }
    }
}
