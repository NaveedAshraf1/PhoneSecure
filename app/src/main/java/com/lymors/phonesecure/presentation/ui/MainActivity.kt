package com.lymors.phonesecure.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.guolindev.permissionx.PermissionX
import com.lymors.phonesecure.PhoneSecureApp
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.usecase.FakeShutdownUseCase
import com.lymors.phonesecure.domain.usecase.IntruderDetectionUseCase
import com.lymors.phonesecure.domain.usecase.LocationTrackingUseCase
import com.lymors.phonesecure.domain.usecase.PanicButtonUseCase
import com.lymors.phonesecure.presentation.services.FakeShutdownAccessibilityService
import com.lymors.phonesecure.presentation.services.IntruderDetectionService
import com.lymors.phonesecure.presentation.services.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var fakeShutdownUseCase: FakeShutdownUseCase
    private lateinit var intruderDetectionUseCase: IntruderDetectionUseCase
    private lateinit var locationTrackingUseCase: LocationTrackingUseCase
    private lateinit var panicButtonUseCase: PanicButtonUseCase
    
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    private var lastPauseTime: Long = 0
    private var isPasswordChecked = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Initialize use cases
        initializeUseCases()
        
        // Set up UI components
        setupUI()
        
        // Check if password protection is enabled
        checkPasswordProtection()
    }
    
    private fun initializeUseCases() {
        val app = application as PhoneSecureApp
        fakeShutdownUseCase = app.getFakeShutdownUseCase()
        intruderDetectionUseCase = app.getIntruderDetectionUseCase()
        locationTrackingUseCase = app.getLocationTrackingUseCase()
        panicButtonUseCase = app.getPanicButtonUseCase()
    }
    
    private fun setupUI() {
        // Set up Anti-Theft Protection button
        val btnActivateProtection = findViewById<MaterialButton>(R.id.btnActivateProtection)
        btnActivateProtection.setOnClickListener {
            // Navigate to Anti-Theft Fragment
            findNavController(R.id.nav_host_fragment).navigate(R.id.action_homeFragment_to_antiTheftFragment)
        }
        
        // Set up Fake Shutdown feature
        val btnEnableFakeShutdown = findViewById<MaterialButton>(R.id.btnEnableFakeShutdown)
        btnEnableFakeShutdown.setOnClickListener {
            checkAccessibilityPermission()
        }
        
        val btnActivateFakeShutdown = findViewById<MaterialButton>(R.id.btnActivateFakeShutdown)
        btnActivateFakeShutdown.setOnClickListener {
            activateFakeShutdown()
        }
        
        // Set up Panic Button
        val btnPanic = findViewById<MaterialButton>(R.id.btnPanic)
        btnPanic.setOnClickListener {
            triggerPanicButton()
        }
        
        // Set up Settings button
        val btnSettings = findViewById<MaterialButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            // TODO: Navigate to Settings screen
            Toast.makeText(this, "Settings will be implemented in the next step", Toast.LENGTH_SHORT).show()
        }
        
        // Set up FAB
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            Snackbar.make(it, "PhoneSecure is protecting your device", Snackbar.LENGTH_LONG)
                .setAction("Settings") {
                    // TODO: Navigate to Settings screen
                    Toast.makeText(this, "Settings will be implemented in the next step", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        
        // Update UI based on current settings
        updateUI()
    }
    
    private fun updateUI() {
        mainScope.launch {
            // Update Fake Shutdown button state
            val isFakeShutdownEnabled = withContext(Dispatchers.IO) {
                fakeShutdownUseCase.isFakeShutdownEnabled()
            }
            
            val btnEnableFakeShutdown = findViewById<MaterialButton>(R.id.btnEnableFakeShutdown)
            btnEnableFakeShutdown.text = if (isFakeShutdownEnabled) {
                getString(R.string.action_disable)
            } else {
                getString(R.string.action_enable)
            }
            
            // Update location tracking status
            val isLocationTrackingEnabled = withContext(Dispatchers.IO) {
                locationTrackingUseCase.isLocationTrackingEnabled()
            }
            
            // Update security status text
            val tvSecurityStatus = findViewById<android.widget.TextView>(R.id.tvSecurityStatus)
            if (isFakeShutdownEnabled && isLocationTrackingEnabled) {
                tvSecurityStatus.text = "All security features are enabled"
                tvSecurityStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
            } else if (!isFakeShutdownEnabled && !isLocationTrackingEnabled) {
                tvSecurityStatus.text = "No security features are enabled"
                tvSecurityStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
            } else {
                tvSecurityStatus.text = "Some security features are enabled"
                tvSecurityStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_dark))
            }
        }
    }
    
    private fun requestRequiredPermissions() {
        PermissionX.init(this)
            .permissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_CONTACTS
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "These permissions are essential for the anti-theft features to work properly",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    enableAntiTheftProtection()
                } else {
                    Toast.makeText(this, "Permissions are required for anti-theft protection", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun enableAntiTheftProtection() {
        mainScope.launch {
            // Enable location tracking
            val locationEnabled = withContext(Dispatchers.IO) {
                locationTrackingUseCase.enableLocationTracking()
            }
            
            // Enable intruder detection
            val intruderDetectionEnabled = withContext(Dispatchers.IO) {
                intruderDetectionUseCase.enableIntruderDetection()
            }
            
            // Start location tracking service
            val serviceIntent = Intent(this@MainActivity, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
            }
            
            ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
            
            if (locationEnabled && intruderDetectionEnabled) {
                Toast.makeText(this@MainActivity, "Anti-theft protection enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to enable some protection features", Toast.LENGTH_SHORT).show()
            }
            
            // Update UI
            updateUI()
        }
    }
    
    private fun checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            // Show dialog explaining the need for accessibility permission
            AlertDialog.Builder(this)
                .setTitle("Accessibility Permission Required")
                .setMessage("The Fake Shutdown feature requires accessibility permission to work properly. Would you like to enable it now?")
                .setPositiveButton("Yes") { _, _ ->
                    // Open accessibility settings
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            // Toggle fake shutdown feature
            toggleFakeShutdown()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            return services.contains("${packageName}/${FakeShutdownAccessibilityService::class.java.name}")
        }
        
        return false
    }
    
    private fun toggleFakeShutdown() {
        mainScope.launch {
            val isEnabled = withContext(Dispatchers.IO) {
                fakeShutdownUseCase.isFakeShutdownEnabled()
            }
            
            val success = if (isEnabled) {
                withContext(Dispatchers.IO) {
                    fakeShutdownUseCase.disableFakeShutdown()
                }
            } else {
                withContext(Dispatchers.IO) {
                    fakeShutdownUseCase.enableFakeShutdown()
                }
            }
            
            if (success) {
                val message = if (!isEnabled) {
                    "Fake Shutdown feature enabled"
                } else {
                    "Fake Shutdown feature disabled"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to toggle Fake Shutdown feature", Toast.LENGTH_SHORT).show()
            }
            
            // Update UI
            updateUI()
        }
    }
    
    private fun activateFakeShutdown() {
        mainScope.launch {
            val isEnabled = withContext(Dispatchers.IO) {
                fakeShutdownUseCase.isFakeShutdownEnabled()
            }
            
            if (!isEnabled) {
                Toast.makeText(this@MainActivity, "Please enable Fake Shutdown feature first", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this@MainActivity, "Accessibility service is not enabled", Toast.LENGTH_SHORT).show()
                checkAccessibilityPermission()
                return@launch
            }
            
            // Show confirmation dialog
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Activate Fake Shutdown")
                .setMessage("This will make your device appear to be powered off, but it will still be tracking. Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    // Activate fake shutdown
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.action = FakeShutdownAccessibilityService.ACTION_ACTIVATE_FAKE_SHUTDOWN
                    sendBroadcast(intent)
                    
                    withContext(Dispatchers.IO) {
                        fakeShutdownUseCase.activateFakeShutdown()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    
    private fun triggerPanicButton() {
        mainScope.launch {
            // Show confirmation dialog
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Trigger Panic Button")
                .setMessage("This will alert your emergency contacts with your current location. Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    // Trigger panic button
                    withContext(Dispatchers.IO) {
                        panicButtonUseCase.triggerPanicButton()
                    }
                    
                    // Start location tracking if not already active
                    val serviceIntent = Intent(this@MainActivity, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
                    }
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                    
                    // Capture intruder photo
                    val intruderIntent = Intent(this@MainActivity, IntruderDetectionService::class.java).apply {
                        action = IntruderDetectionService.ACTION_CAPTURE_INTRUDER
                        putExtra(IntruderDetectionService.EXTRA_DESCRIPTION, "Panic button was triggered")
                    }
                    startService(intruderIntent)
                    
                    Toast.makeText(this@MainActivity, getString(R.string.alert_panic_triggered), Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // TODO: Navigate to Settings screen
                Toast.makeText(this, "Settings will be implemented in the next step", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_password_settings -> {
                // Navigate to Password Settings screen
                findNavController(R.id.nav_host_fragment).navigate(R.id.passwordSettingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
        
        // Check if we need to show the password screen after timeout
        checkPasswordAfterTimeout()
    }
    
    override fun onPause() {
        super.onPause()
        // Record the time when the app was paused
        lastPauseTime = System.currentTimeMillis()
    }
    
    private fun checkPasswordProtection() {
        mainScope.launch {
            try {
                val app = application as PhoneSecureApp
                val userRepository = app.getUserRepository()
                val passwordSettings = userRepository.getPasswordSettings()
                
                if (passwordSettings.passwordEnabled && !isPasswordChecked) {
                    // Navigate to password login screen
                    navigateToPasswordLogin()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking password protection", e)
            }
        }
    }
    
    private fun checkPasswordAfterTimeout() {
        if (lastPauseTime == 0L) return
        
        mainScope.launch {
            try {
                val app = application as PhoneSecureApp
                val userRepository = app.getUserRepository()
                val passwordSettings = userRepository.getPasswordSettings()
                
                if (passwordSettings.passwordEnabled && passwordSettings.lockAfterTimeout) {
                    val timeoutMillis = passwordSettings.timeoutMinutes * 60 * 1000L
                    val currentTime = System.currentTimeMillis()
                    val timePassed = currentTime - lastPauseTime
                    
                    if (timePassed > timeoutMillis) {
                        // Reset the checked flag
                        isPasswordChecked = false
                        // Navigate to password login screen
                        navigateToPasswordLogin()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking password timeout", e)
            }
        }
    }
    
    private fun navigateToPasswordLogin() {
        val navController = findNavController(R.id.nav_host_fragment)
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
        navController.navigate(R.id.passwordLoginFragment, null, navOptions)
    }
    
    fun setPasswordChecked(checked: Boolean) {
        isPasswordChecked = checked
    }
}
