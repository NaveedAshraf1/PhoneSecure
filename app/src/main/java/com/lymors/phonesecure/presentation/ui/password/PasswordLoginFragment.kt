package com.lymors.phonesecure.presentation.ui.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lymors.phonesecure.R
import com.lymors.phonesecure.presentation.ui.MainActivity
import com.lymors.phonesecure.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@AndroidEntryPoint
class PasswordLoginFragment : Fragment() {

    private val viewModel: PasswordLoginViewModel by viewModels()

    // UI components
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var forgotPasswordText: TextView
    private lateinit var biometricButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    // Biometric helper
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        passwordInput = view.findViewById(R.id.passwordInput)
        loginButton = view.findViewById(R.id.loginButton)
        forgotPasswordText = view.findViewById(R.id.forgotPasswordText)
        biometricButton = view.findViewById(R.id.biometricButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)

        // Set up biometric authentication
        setupBiometricAuth()

        // Set up listeners
        setupListeners()

        // Observe UI state
        observeUiState()

        // Check if biometric authentication is available
        viewModel.isBiometricAvailable()
    }

    private fun setupBiometricAuth() {
        biometricHelper = BiometricHelper.getInstance(requireContext())
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val password = passwordInput.text.toString()
            viewModel.verifyPassword(password)
        }

        forgotPasswordText.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(requireContext(), "Forgot password functionality will be implemented in the future", Toast.LENGTH_SHORT).show()
        }

        biometricButton.setOnClickListener {
            biometricHelper.authenticate(
                requireActivity(),
                onSuccess = { viewModel.biometricAuthSuccess() },
                onError = { errorMessage -> viewModel.biometricAuthFailed(errorMessage) },
                onFailed = { viewModel.biometricAuthFailed(getString(R.string.biometric_failed)) }
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is PasswordLoginUiState.Initial -> {
                            progressIndicator.visibility = View.GONE
                            loginButton.isEnabled = true
                            passwordInput.text?.clear()
                        }
                        is PasswordLoginUiState.Loading -> {
                            progressIndicator.visibility = View.VISIBLE
                            loginButton.isEnabled = false
                        }
                        is PasswordLoginUiState.Success -> {
                            progressIndicator.visibility = View.GONE
                            loginButton.isEnabled = true
                            // Inform MainActivity that password check was successful
                            (requireActivity() as? MainActivity)?.setPasswordChecked(true)
                            // Navigate to the home screen
                            findNavController().navigate(R.id.action_passwordLoginFragment_to_homeFragment)
                        }
                        is PasswordLoginUiState.Error -> {
                            progressIndicator.visibility = View.GONE
                            loginButton.isEnabled = true
                            // Show error message
                            if (state.message.contains("Too many failed attempts")) {
                                // Disable the login button and password input
                                loginButton.isEnabled = false
                                passwordInput.isEnabled = false
                                // Show error in Snackbar with longer duration
                                Snackbar.make(requireView(), state.message, Snackbar.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is PasswordLoginUiState.TooManyAttempts -> {
                            progressIndicator.visibility = View.GONE
                            loginButton.isEnabled = false
                            Toast.makeText(requireContext(), R.string.too_many_attempts, Toast.LENGTH_LONG).show()
                        }
                        is PasswordLoginUiState.BiometricAvailability -> {
                            biometricButton.visibility = if (state.available && isBiometricAvailable()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun isBiometricAvailable(): Boolean {
        return biometricHelper.isBiometricAvailable()
    }
}
