package com.lymors.phonesecure.presentation.ui.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyAlertActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sendAlertButton: MaterialButton
    private lateinit var callEmergencyButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS
    )

    private val requestPermissionLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                sendEmergencyAlert()
            } else {
                showPermissionDeniedMessage()
            }
        }
    }

    private val viewModel: EmergencyAlertViewModel by lazy {
        EmergencyAlertViewModel() // Replace with proper ViewModelProvider if using Hilt/ViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_emergency_alert)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        sendAlertButton = findViewById(R.id.sendAlertButton)
        callEmergencyButton = findViewById(R.id.callEmergencyButton)
        progressIndicator = findViewById(R.id.progressIndicator)

        sendAlertButton.setOnClickListener {
            checkPermissionsAndSendAlert()
        }

        callEmergencyButton.setOnClickListener {
            callEmergencyServices()
        }

        observeUiState()
        viewModel.loadEmergencyContacts()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is EmergencyAlertUiState.Initial -> {
                            // Initial state, do nothing
                        }
                        is EmergencyAlertUiState.Loading -> {
                            progressIndicator.visibility = View.VISIBLE
                            sendAlertButton.isEnabled = false
                        }
                        is EmergencyAlertUiState.NoContacts -> {
                            progressIndicator.visibility = View.GONE
                            sendAlertButton.isEnabled = false
                            showNoContactsMessage()
                        }
                        is EmergencyAlertUiState.Ready -> {
                            progressIndicator.visibility = View.GONE
                            sendAlertButton.isEnabled = true
                        }
                        is EmergencyAlertUiState.Success -> {
                            progressIndicator.visibility = View.GONE
                            sendAlertButton.isEnabled = true
                            showAlertSentMessage()
                        }
                        is EmergencyAlertUiState.Error -> {
                            progressIndicator.visibility = View.GONE
                            sendAlertButton.isEnabled = true
                            showErrorMessage(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndSendAlert() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            sendEmergencyAlert()
        } else {
            if (notGranted.any { shouldShowRequestPermissionRationale(it) }) {
                showPermissionRationale(notGranted.toTypedArray())
            } else {
                requestPermissionLauncher.launch(notGranted.toTypedArray())
            }
        }
    }

    private fun showPermissionRationale(permissions: Array<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.emergency_permissions_rationale)
            .setPositiveButton(R.string.grant) { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            this,
            R.string.emergency_permissions_denied,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun sendEmergencyAlert() {
        lifecycleScope.launch {
            try {
                val location = try {
                    fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    null
                }
                viewModel.sendEmergencyAlert(location)
                val currentState = viewModel.uiState.value
                if (currentState is EmergencyAlertUiState.Ready || currentState is EmergencyAlertUiState.Success) {
                    val contacts = if (currentState is EmergencyAlertUiState.Ready) {
                        currentState.contacts
                    } else {
                        (currentState as EmergencyAlertUiState.Success).contacts
                    }
                    sendAlertMessages(contacts, location)
                }
            } catch (e: Exception) {
                showErrorMessage(e.message ?: "Unknown error")
            }
        }
    }

    private fun sendAlertMessages(contacts: List<EmergencyContact>, location: Location?) {
        val smsManager = SmsManager.getDefault()
        val message = buildAlertMessage(location)
        for (contact in contacts) {
            try {
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Failed to send to ${contact.name}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buildAlertMessage(location: Location?): String {
        val userName = userRepository.getCurrentUser().name
        val baseMessage = getString(R.string.emergency_alert_message, userName)
        return if (location != null) {
            val locationUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            "$baseMessage\n${getString(R.string.my_location)}: $locationUrl"
        } else {
            baseMessage
        }
    }

    private fun callEmergencyServices() {
        val emergencyNumber = "911" // This should be configurable or determined by country
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$emergencyNumber")
        }
        startActivity(intent)
    }

    private fun showNoContactsMessage() {
        Toast.makeText(
            this,
            R.string.no_emergency_contacts,
            Toast.LENGTH_LONG
        ).show()
        progressIndicator.visibility = View.GONE
        sendAlertButton.isEnabled = true
    }

    private fun showAlertSentMessage() {
        Toast.makeText(
            this,
            R.string.emergency_alert_sent,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(
            this,
            getString(R.string.error_sending_alert, message),
            Toast.LENGTH_LONG
        ).show()
    }
}
