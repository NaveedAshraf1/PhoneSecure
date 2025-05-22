package com.lymors.phonesecure.presentation.ui.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.PasswordSettings
import com.lymors.phonesecure.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasswordSettingsFragment : Fragment() {

    private val viewModel: PasswordSettingsViewModel by viewModels()

    // UI components
    private lateinit var enablePasswordSwitch: SwitchMaterial
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var enableBiometricSwitch: SwitchMaterial
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var timeoutSwitch: SwitchMaterial
    private lateinit var timeoutInputLayout: TextInputLayout
    private lateinit var timeoutInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize biometric helper
        biometricHelper = BiometricHelper.getInstance(requireContext())

        // Initialize views
        enablePasswordSwitch = view.findViewById(R.id.enablePasswordSwitch)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        passwordInput = view.findViewById(R.id.passwordInput)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput)
        enableBiometricSwitch = view.findViewById(R.id.biometricSwitch)
        timeoutSwitch = view.findViewById(R.id.timeoutSwitch)
        timeoutInputLayout = view.findViewById(R.id.timeoutInputLayout)
        timeoutInput = view.findViewById(R.id.timeoutInput)
        saveButton = view.findViewById(R.id.saveButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)

        // Setup listeners
        setupListeners()

        // Observe ViewModel
        observeViewModel()

        // Load current settings
        viewModel.loadSettings()
    }

    private fun setupListeners() {
        enablePasswordSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePasswordEnabled(isChecked)
            updateInputsEnabled(isChecked)
        }

        passwordInput.doAfterTextChanged { text ->
            viewModel.updatePassword(text.toString())
        }

        enableBiometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (biometricHelper.isBiometricAvailable()) {
                    viewModel.updateBiometricEnabled(true)
                } else {
                    Toast.makeText(requireContext(), R.string.biometric_not_available, Toast.LENGTH_SHORT).show()
                    enableBiometricSwitch.isChecked = false
                }
            } else {
                viewModel.updateBiometricEnabled(false)
            }
        }

        timeoutSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateLockAfterTimeout(isChecked)
            timeoutInputLayout.isEnabled = isChecked
        }

        timeoutInput.doAfterTextChanged { text ->
            text?.toString()?.toIntOrNull()?.let { minutes ->
                viewModel.updateTimeoutMinutes(minutes)
            }
        }

        saveButton.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState is PasswordSettingsUiState.Success) {
                if (validateSettings(currentState.settings)) {
                    viewModel.saveSettings(currentState.settings)
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PasswordSettingsUiState.Loading -> {
                            progressIndicator.visibility = View.VISIBLE
                            saveButton.isEnabled = false
                        }
                        is PasswordSettingsUiState.Success -> {
                            progressIndicator.visibility = View.GONE
                            saveButton.isEnabled = true
                            updateUI(state.settings)
                            state.error?.let { error ->
                                com.google.android.material.snackbar.Snackbar.make(requireView(), error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                                // Reset biometric switch if there was an error
                                if (error.contains("Password protection must be enabled")) {
                                    enableBiometricSwitch.isChecked = false
                                }
                            }
                        }
                        is PasswordSettingsUiState.Error -> {
                            progressIndicator.visibility = View.GONE
                            saveButton.isEnabled = true
                            com.google.android.material.snackbar.Snackbar.make(requireView(), state.message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(settings: PasswordSettings) {
        enablePasswordSwitch.isChecked = settings.passwordEnabled
        updateInputsEnabled(settings.passwordEnabled)
        
        passwordInput.setText(settings.password)
        confirmPasswordInput.setText(settings.password)
        
        biometricSwitch.isChecked = settings.useBiometric
        
        timeoutSwitch.isChecked = settings.lockAfterTimeout
        timeoutInputLayout.isEnabled = settings.lockAfterTimeout
        timeoutInput.setText(settings.timeoutMinutes.toString())
    }

    private fun updateInputsEnabled(enabled: Boolean) {
        passwordInputLayout.isEnabled = enabled
        confirmPasswordInputLayout.isEnabled = enabled
        biometricSwitch.isEnabled = enabled
        timeoutSwitch.isEnabled = enabled
        timeoutInputLayout.isEnabled = enabled && timeoutSwitch.isChecked
    }

    private fun validateSettings(settings: PasswordSettings): Boolean {
        // If password protection is not enabled, no validation needed
        if (!settings.passwordEnabled) {
            return true
        }

        // Check if password is empty
        if (settings.password.isBlank()) {
            Toast.makeText(requireContext(), R.string.password_required, Toast.LENGTH_SHORT).show()
            passwordInput.requestFocus()
            return false
        }

        // Check if password is too short
        if (settings.password.length < 4) {
            Toast.makeText(requireContext(), R.string.password_too_short, Toast.LENGTH_SHORT).show()
            passwordInput.requestFocus()
            return false
        }

        // Check if passwords match
        if (settings.password != confirmPasswordInput.text.toString()) {
            Toast.makeText(requireContext(), R.string.password_mismatch, Toast.LENGTH_SHORT).show()
            confirmPasswordInput.requestFocus()
            return false
        }

        return true
    }
}
