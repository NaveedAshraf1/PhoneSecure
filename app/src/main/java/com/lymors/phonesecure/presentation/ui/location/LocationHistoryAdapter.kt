package com.lymors.phonesecure.presentation.ui.location

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lymors.phonesecure.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying location history items in a RecyclerView
 */
class LocationHistoryAdapter(
    private val onViewOnMapClicked: (LocationHistoryItem) -> Unit
) : ListAdapter<LocationHistoryItem, LocationHistoryAdapter.ViewHolder>(LocationDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationAddressText: TextView = itemView.findViewById(R.id.locationAddressText)
        private val locationTimeText: TextView = itemView.findViewById(R.id.locationTimeText)
        private val locationCoordsText: TextView = itemView.findViewById(R.id.locationCoordsText)
        private val viewOnMapButton: MaterialButton = itemView.findViewById(R.id.viewOnMapButton)

        fun bind(item: LocationHistoryItem) {
            // Set address
            locationAddressText.text = item.address ?: itemView.context.getString(R.string.unknown_location)
            
            // Set time
            locationTimeText.text = dateFormat.format(item.timestamp)
            
            // Set coordinates
            locationCoordsText.text = itemView.context.getString(
                R.string.location_coordinates,
                item.latitude,
                item.longitude
            )
            
            // Set button click listener
            viewOnMapButton.setOnClickListener {
                onViewOnMapClicked(item)
            }
        }
    }

    class LocationDiffCallback : DiffUtil.ItemCallback<LocationHistoryItem>() {
        override fun areItemsTheSame(oldItem: LocationHistoryItem, newItem: LocationHistoryItem): Boolean {
            return oldItem.timestamp == newItem.timestamp &&
                    oldItem.latitude == newItem.latitude &&
                    oldItem.longitude == newItem.longitude
        }

        override fun areContentsTheSame(oldItem: LocationHistoryItem, newItem: LocationHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Data class representing a location history item for display in the UI
 */
data class LocationHistoryItem(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Date,
    val address: String? = null
)
