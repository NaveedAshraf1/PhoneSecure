package com.lymors.phonesecure.util

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lymors.phonesecure.R

/**
 * Helper class for biometric authentication
 */
class BiometricHelper(private val context: Context) {

    private val tag = "BiometricHelper"

    /**
     * Check if biometric authentication is available on the device
     * @return true if biometric authentication is available, false otherwise
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e(tag, "No biometric hardware")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e(tag, "Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e(tag, "No biometric credentials enrolled")
                false
            }
            else -> {
                Log.e(tag, "Biometric error")
                false
            }
        }
    }

    /**
     * Authenticate using biometric
     * @param activity the activity to use for biometric authentication
     * @param onSuccess callback for successful authentication
     * @param onError callback for authentication error
     * @param onFailed callback for authentication failure
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("Biometric authentication not available")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setSubtitle(context.getString(R.string.biometric_prompt_description))
            .setNegativeButtonText(context.getString(R.string.password))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        @Volatile
        private var INSTANCE: BiometricHelper? = null

        fun getInstance(context: Context): BiometricHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
