package com.lymors.phonesecure.presentation.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.EmergencyContact

class EmergencyContactAdapter(
    private val onDeleteClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder>() {

    private var contacts = mutableListOf<EmergencyContact>()

    fun updateContacts(newContacts: List<EmergencyContact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    fun addContact(contact: EmergencyContact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun removeContact(contact: EmergencyContact) {
        val position = contacts.indexOf(contact)
        if (position != -1) {
            contacts.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.contactName)
        private val phoneText: TextView = itemView.findViewById(R.id.contactPhone)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(contact: EmergencyContact) {
            nameText.text = contact.name
            phoneText.text = contact.phoneNumber
            deleteButton.setOnClickListener { onDeleteClick(contact) }
        }
    }
}
