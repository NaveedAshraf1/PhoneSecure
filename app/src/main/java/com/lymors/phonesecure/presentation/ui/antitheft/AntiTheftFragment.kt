package com.lymors.phonesecure.presentation.ui.antitheft

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.guolindev.permissionx.PermissionX
import com.lymors.phonesecure.service.AntiTheftService
import com.lymors.phonesecure.util.RemoteLockManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.AntiTheftSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AntiTheftFragment : Fragment() {

    private val viewModel: AntiTheftViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_anti_theft, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        simChangeSwitch = view.findViewById(R.id.simChangeSwitch)
        motionDetectionSwitch = view.findViewById(R.id.motionDetectionSwitch)
        sensitivitySlider = view.findViewById(R.id.sensitivitySlider)
        sensitivityText = view.findViewById(R.id.sensitivityText)
        wrongPasswordSwitch = view.findViewById(R.id.wrongPasswordSwitch)
        attemptsInput = view.findViewById(R.id.attemptsInput)
        attemptsInputLayout = view.findViewById(R.id.attemptsInputLayout)
        remoteLockSwitch = view.findViewById(R.id.remoteLockSwitch)
        secretCodeInput = view.findViewById(R.id.secretCodeInput)
        secretCodeInputLayout = view.findViewById(R.id.secretCodeInputLayout)
        saveButton = view.findViewById(R.id.saveButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)

        // Set up listeners
        setupListeners()

        // Observe UI state
        observeUiState()
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

        attemptsInput.doAfterTextChanged { text ->
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
                    // Check for required permissions based on enabled features
                    checkRequiredPermissions(currentState.settings) { permissionsGranted ->
                        if (permissionsGranted) {
                            viewModel.saveSettings(currentState.settings)
                            
                            // Start or update anti-theft service if any feature is enabled
                            if (currentState.settings.simChangeDetectionEnabled || 
                                currentState.settings.motionDetectionEnabled || 
                                currentState.settings.remoteLockEnabled) {
                                AntiTheftService.startService(requireContext())
                                AntiTheftService.updateSettings(requireContext())
                            } else {
                                AntiTheftService.stopService(requireContext())
                            }
                            
                            // Check device admin permission if remote lock is enabled
                            if (currentState.settings.remoteLockEnabled) {
                                val remoteLockManager = RemoteLockManager.getInstance(requireContext())
                                if (!remoteLockManager.isDeviceAdminActive()) {
                                    remoteLockManager.requestDeviceAdminPermission()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
        if (settings.remoteLockEnabled && settings.secretCode.isBlank()) {
            Toast.makeText(requireContext(), R.string.secret_code_required, Toast.LENGTH_SHORT).show()
            secretCodeInput.requestFocus()
            return false
        }

        if (settings.wrongPasswordDetectionEnabled && settings.maxPasswordAttempts <= 0) {
            Toast.makeText(requireContext(), R.string.max_attempts_required, Toast.LENGTH_SHORT).show()
            attemptsInput.requestFocus()
            return false
        }

        return true
    }
    
    private fun checkRequiredPermissions(settings: AntiTheftSettings, onResult: (Boolean) -> Unit) {
        val requiredPermissions = mutableListOf<String>()
        
        // Add permissions based on enabled features
        if (settings.simChangeDetectionEnabled) {
            requiredPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        if (settings.remoteLockEnabled) {
            requiredPermissions.add(Manifest.permission.RECEIVE_SMS)
            requiredPermissions.add(Manifest.permission.READ_SMS)
        }
        
        // If no permissions needed, return success
        if (requiredPermissions.isEmpty()) {
            onResult(true)
            return
        }
        
        // Request permissions
        PermissionX.init(this)
            .permissions(requiredPermissions)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "These permissions are required for the anti-theft protection features",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, _, _ ->
                onResult(allGranted)
                if (!allGranted) {
                    Toast.makeText(
                        requireContext(),
                        "Some permissions were denied. Some features may not work properly.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
