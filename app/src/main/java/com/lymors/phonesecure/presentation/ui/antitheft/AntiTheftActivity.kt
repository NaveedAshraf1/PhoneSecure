package com.lymors.phonesecure.presentation.ui.antitheft

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.guolindev.permissionx.PermissionX
import com.lymors.phonesecure.R
import com.lymors.phonesecure.databinding.ActivityAntiTheftBinding
import com.lymors.phonesecure.service.AntiTheftService
import com.lymors.phonesecure.util.RemoteLockManager
import com.lymors.phonesecure.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AntiTheftActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAntiTheftBinding

    // UI components
    private lateinit var simChangeSwitch: SwitchMaterial
    private lateinit var motionDetectionSwitch: SwitchMaterial
    private lateinit var sensitivitySlider: Slider
    private lateinit var sensitivityText: TextView
    private lateinit var wrongPasswordSwitch: SwitchMaterial
    private lateinit var attemptsInput: TextInputEditText
    private lateinit var attemptsInputLayout: TextInputLayout
    private lateinit var remoteLockSwitch: SwitchMaterial
    private lateinit var secretCodeInput: TextInputEditText
    private lateinit var secretCodeInputLayout: TextInputLayout
    private lateinit var saveButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    private val viewModel: AntiTheftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAntiTheftBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up action bar
        setSupportActionBar(binding.toolbar)
        setupToolbar(showBackButton = true)
        supportActionBar?.title = getString(R.string.anti_theft_protection)
        
        initializeViews()
        setupListeners()
        observeUiState()
    }
    
    private fun initializeViews() {
        // Initialize views from binding
        simChangeSwitch = binding.simChangeSwitch
        motionDetectionSwitch = binding.motionDetectionSwitch
        sensitivitySlider = binding.sensitivitySlider
        sensitivityText = binding.sensitivityText
        wrongPasswordSwitch = binding.wrongPasswordSwitch
        attemptsInput = binding.attemptsInput
        attemptsInputLayout = binding.attemptsInputLayout
        remoteLockSwitch = binding.remoteLockSwitch
        secretCodeInput = binding.secretCodeInput
        secretCodeInputLayout = binding.secretCodeInputLayout
        saveButton = binding.saveButton
        progressIndicator = binding.progressIndicator
    }

    private fun setupListeners() {
        simChangeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSimChangeDetection(isChecked)
        }

        motionDetectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateMotionDetection(isChecked)
            sensitivitySlider.isEnabled = isChecked
        }

        sensitivitySlider.addOnChangeListener { _, value, _ ->
            viewModel.updateMotionSensitivity(value.toInt())
            updateSensitivityText(value.toInt())
        }

        wrongPasswordSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateWrongPasswordDetection(isChecked)
            attemptsInputLayout.isEnabled = isChecked
        }

        binding.attemptsInput.doAfterTextChanged { text ->
            text?.toString()?.toIntOrNull()?.let { attempts ->
                viewModel.updateMaxPasswordAttempts(attempts)
            }
        }

        remoteLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateRemoteLock(isChecked)
            secretCodeInputLayout.isEnabled = isChecked
        }

        secretCodeInput.doAfterTextChanged { text ->
            viewModel.updateSecretCode(text.toString())
        }

        saveButton.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState is AntiTheftUiState.Success) {
                if (validateSettings(currentState.settings)) {
                    checkRequiredPermissions(currentState.settings) { permissionsGranted ->
                        if (permissionsGranted) {
                            viewModel.saveSettings(currentState.settings)
                            
                            // Start or update anti-theft service if any feature is enabled
                            if (currentState.settings.simChangeDetectionEnabled || 
                                currentState.settings.motionDetectionEnabled || 
                                currentState.settings.remoteLockEnabled) {
                                AntiTheftService.startService(this)
                                AntiTheftService.updateSettings(this)
                            } else {
                                AntiTheftService.stopService(this)
                            }
                            
                            // Check device admin permission if remote lock is enabled
                            if (currentState.settings.remoteLockEnabled) {
                                val remoteLockManager = RemoteLockManager.getInstance(this)
                                if (!remoteLockManager.isDeviceAdminActive()) {
                                    remoteLockManager.requestDeviceAdminPermission()
                                }
                            }
                            
                            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is AntiTheftUiState.Loading -> {
                            progressIndicator.visibility = View.VISIBLE
                            saveButton.isEnabled = false
                        }
                        is AntiTheftUiState.Success -> {
                            progressIndicator.visibility = View.GONE
                            saveButton.isEnabled = true
                            updateUI(state.settings)
                        }
                        is AntiTheftUiState.Error -> {
                            progressIndicator.visibility = View.GONE
                            saveButton.isEnabled = true
                            Toast.makeText(this@AntiTheftActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(settings: AntiTheftSettings) {
        simChangeSwitch.isChecked = settings.simChangeDetectionEnabled

        motionDetectionSwitch.isChecked = settings.motionDetectionEnabled
        sensitivitySlider.value = settings.motionSensitivity.toFloat()
        sensitivitySlider.isEnabled = settings.motionDetectionEnabled
        updateSensitivityText(settings.motionSensitivity)

        wrongPasswordSwitch.isChecked = settings.wrongPasswordDetectionEnabled
        attemptsInput.setText(settings.maxPasswordAttempts.toString())
        attemptsInputLayout.isEnabled = settings.wrongPasswordDetectionEnabled

        remoteLockSwitch.isChecked = settings.remoteLockEnabled
        secretCodeInput.setText(settings.secretCode)
        secretCodeInputLayout.isEnabled = settings.remoteLockEnabled
    }

    private fun updateSensitivityText(sensitivity: Int) {
        val text = when {
            sensitivity <= 3 -> getString(R.string.sensitivity_low)
            sensitivity <= 7 -> getString(R.string.sensitivity_medium)
            else -> getString(R.string.sensitivity_high)
        }
        sensitivityText.text = text
    }

    private fun validateSettings(settings: AntiTheftSettings): Boolean {
        return when {
            settings.wrongPasswordDetectionEnabled && settings.maxPasswordAttempts <= 0 -> {
                attemptsInputLayout.error = getString(R.string.invalid_attempts)
                false
            }
            settings.remoteLockEnabled && settings.secretCode.length < 4 -> {
                secretCodeInputLayout.error = getString(R.string.invalid_secret_code)
                false
            }
            else -> true
        }
    }

    private fun checkRequiredPermissions(
        settings: AntiTheftSettings,
        onResult: (Boolean) -> Unit
    ) {
        val permissions = mutableListOf<String>()
        
        if (settings.simChangeDetectionEnabled) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        if (settings.motionDetectionEnabled) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        
        if (permissions.isEmpty()) {
            onResult(true)
            return
        }
        
        PermissionX.init(this)
            .permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_required),
                    getString(R.string.antitheft_permission_rationale),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_required),
                    getString(R.string.antitheft_permission_settings_rationale),
                    getString(R.string.settings),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, _, _ ->
                onResult(allGranted)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
