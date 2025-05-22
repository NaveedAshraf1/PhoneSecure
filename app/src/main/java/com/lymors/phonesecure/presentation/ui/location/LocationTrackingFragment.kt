package com.lymors.phonesecure.presentation.ui.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.presentation.services.LocationTrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingFragment : Fragment() {

    @Inject
    lateinit var locationRepository: LocationRepository

    private lateinit var statusText: TextView
    private lateinit var trackingSwitch: SwitchMaterial
    private lateinit var historyEmptyText: TextView
    private lateinit var locationHistoryList: RecyclerView
    private lateinit var clearHistoryButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var mapProgressIndicator: CircularProgressIndicator
    private lateinit var refreshButton: FloatingActionButton

    private var googleMap: GoogleMap? = null
    private var locationAdapter: LocationHistoryAdapter? = null
    private var isTrackingActive = false
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            enableLocationTracking(true)
        } else {
            trackingSwitch.isChecked = false
            showPermissionDeniedMessage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_tracking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        statusText = view.findViewById(R.id.statusText)
        trackingSwitch = view.findViewById(R.id.trackingSwitch)
        historyEmptyText = view.findViewById(R.id.historyEmptyText)
        locationHistoryList = view.findViewById(R.id.locationHistoryList)
        clearHistoryButton = view.findViewById(R.id.clearHistoryButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        mapProgressIndicator = view.findViewById(R.id.mapProgressIndicator)
        refreshButton = view.findViewById(R.id.refreshButton)

        // Set up RecyclerView
        locationAdapter = LocationHistoryAdapter(
            onViewOnMapClicked = { location ->
                showLocationOnMap(location)
            }
        )
        locationHistoryList.layoutManager = LinearLayoutManager(requireContext())
        locationHistoryList.adapter = locationAdapter

        // Set up Google Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            checkLocationPermissions()
        }

        // Set up click listeners
        trackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationPermissions()
            } else {
                enableLocationTracking(false)
            }
        }

        clearHistoryButton.setOnClickListener {
            clearLocationHistory()
        }

        refreshButton.setOnClickListener {
            refreshLocationData()
        }

        // Check initial tracking status
        checkTrackingStatus()
        
        // Load location history
        loadLocationHistory()
        
        // Observe location updates
        observeLocationUpdates()
    }

    private fun checkLocationPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        when {
            missingPermissions.isEmpty() -> {
                // All permissions granted
                enableLocationTracking(trackingSwitch.isChecked)
            }
            missingPermissions.any { shouldShowRequestPermissionRationale(it) } -> {
                // Show permission rationale
                showPermissionRationale(missingPermissions)
            }
            else -> {
                // Request permissions
                requestPermissionLauncher.launch(missingPermissions)
            }
        }
    }

    private fun showPermissionRationale(permissions: Array<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_permissions_required)
            .setMessage(R.string.location_permissions_rationale)
            .setPositiveButton(R.string.grant) { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                trackingSwitch.isChecked = false
            }
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            requireContext(),
            R.string.location_permissions_required,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun enableLocationTracking(enable: Boolean) {
        progressIndicator.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                if (enable) {
                    // Start location tracking service
                    val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
                    }
                    requireContext().startService(intent)
                    
                    // Update UI
                    isTrackingActive = true
                    statusText.text = getString(R.string.tracking_active)
                    Toast.makeText(requireContext(), R.string.location_tracking_started, Toast.LENGTH_SHORT).show()
                } else {
                    // Stop location tracking service
                    val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_STOP_LOCATION_TRACKING
                    }
                    requireContext().startService(intent)
                    
                    // Update UI
                    isTrackingActive = false
                    statusText.text = getString(R.string.tracking_inactive)
                    Toast.makeText(requireContext(), R.string.location_tracking_stopped, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                trackingSwitch.isChecked = isTrackingActive
            } finally {
                progressIndicator.visibility = View.GONE
            }
        }
    }

    private fun checkTrackingStatus() {
        progressIndicator.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                isTrackingActive = locationRepository.isLocationTrackingActive()
                trackingSwitch.isChecked = isTrackingActive
                statusText.text = getString(
                    if (isTrackingActive) R.string.tracking_active else R.string.tracking_inactive
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                progressIndicator.visibility = View.GONE
            }
        }
    }

    private fun loadLocationHistory() {
        progressIndicator.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Get last 7 days of history
                val endDate = Date()
                val startDate = Date(endDate.time - (7 * 24 * 60 * 60 * 1000))
                val locationHistory = locationRepository.getLocationHistoryBetweenDates(startDate, endDate)
                
                if (locationHistory.isEmpty()) {
                    historyEmptyText.visibility = View.VISIBLE
                    locationHistoryList.visibility = View.GONE
                } else {
                    historyEmptyText.visibility = View.GONE
                    locationHistoryList.visibility = View.VISIBLE
                    
                    // Convert to LocationHistoryItem
                    val historyItems = locationHistory.map { (location, date) ->
                        LocationHistoryItem(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = date,
                            address = getAddressFromLocation(location)
                        )
                    }
                    
                    // Update adapter
                    locationAdapter?.submitList(historyItems)
                    
                    // Show most recent location on map
                    historyItems.firstOrNull()?.let { showLocationOnMap(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            } finally {
                progressIndicator.visibility = View.GONE
            }
        }
    }

    private fun clearLocationHistory() {
        progressIndicator.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                locationRepository.clearLocationHistory()
                locationAdapter?.submitList(emptyList())
                historyEmptyText.visibility = View.VISIBLE
                locationHistoryList.visibility = View.GONE
                Toast.makeText(requireContext(), R.string.location_history_cleared, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            } finally {
                progressIndicator.visibility = View.GONE
            }
        }
    }

    private fun refreshLocationData() {
        checkTrackingStatus()
        loadLocationHistory()
    }

    private fun observeLocationUpdates() {
        locationRepository.getLocationUpdates()
            .onEach { location ->
                // Save location to history
                locationRepository.saveLocationHistory(location, Date())
                
                // Refresh location history
                loadLocationHistory()
            }
            .catch { e ->
                e.printStackTrace()
            }
            .launchIn(lifecycleScope)
    }

    private fun showLocationOnMap(locationItem: LocationHistoryItem) {
        googleMap?.let { map ->
            mapProgressIndicator.visibility = View.VISIBLE
            
            // Clear previous markers
            map.clear()
            
            // Add marker for the location
            val position = LatLng(locationItem.latitude, locationItem.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(locationItem.address ?: getString(R.string.unknown_location))
                    .snippet(dateFormat.format(locationItem.timestamp))
            )
            
            // Move camera to the location
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(position, 15f)
            )
            
            mapProgressIndicator.visibility = View.GONE
        }
    }

    private fun getAddressFromLocation(location: Location): String? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses.isNullOrEmpty()) {
                getString(R.string.unknown_location)
            } else {
                val address = addresses[0]
                val addressParts = mutableListOf<String>()
                
                // Add address line
                if (!address.thoroughfare.isNullOrEmpty()) {
                    addressParts.add(address.thoroughfare)
                }
                
                // Add city
                if (!address.locality.isNullOrEmpty()) {
                    addressParts.add(address.locality)
                }
                
                // Add country
                if (!address.countryName.isNullOrEmpty()) {
                    addressParts.add(address.countryName)
                }
                
                addressParts.joinToString(", ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getString(R.string.unknown_location)
        }
    }
}
