package com.lymors.phonesecure.presentation.ui.settings

import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lymors.phonesecure.R

class PhoneNumberAdapter(
    private val onPhoneNumberSelected: (PhoneNumberItem) -> Unit
) : RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder>() {

    private var phoneNumbers = listOf<PhoneNumberItem>()

    fun setPhoneNumbers(numbers: List<PhoneNumberItem>) {
        phoneNumbers = numbers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_phone_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phoneNumber = phoneNumbers[position]
        holder.bind(phoneNumber)
    }

    override fun getItemCount(): Int = phoneNumbers.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val phoneNumberText: TextView = itemView.findViewById(R.id.phoneNumberText)
        private val phoneTypeText: TextView = itemView.findViewById(R.id.phoneTypeText)
        private val phoneTypeIcon: ImageView = itemView.findViewById(R.id.phoneTypeIcon)
        private val selectButton: MaterialButton = itemView.findViewById(R.id.selectButton)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhoneNumberSelected(phoneNumbers[position])
                }
            }
            
            selectButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhoneNumberSelected(phoneNumbers[position])
                }
            }
        }

        fun bind(phoneNumber: PhoneNumberItem) {
            phoneNumberText.text = formatPhoneNumber(phoneNumber.number)
            phoneTypeText.text = getPhoneTypeLabel(phoneNumber.type)
            setPhoneTypeIcon(phoneNumber.type)
        }
        
        private fun formatPhoneNumber(phoneNumber: String): String {
            // Simple formatting for demonstration purposes
            // For production, consider using libphonenumber library for proper formatting
            if (phoneNumber.length == 10) {
                return "(${phoneNumber.substring(0, 3)}) ${phoneNumber.substring(3, 6)}-${phoneNumber.substring(6)}"
            }
            return phoneNumber
        }
        
        private fun setPhoneTypeIcon(type: Int) {
            when (type) {
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> {
                    phoneTypeIcon.setImageResource(R.drawable.ic_phone)
                }
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> {
                    phoneTypeIcon.setImageResource(R.drawable.ic_phone)
                }
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> {
                    phoneTypeIcon.setImageResource(R.drawable.ic_phone)
                }
                else -> {
                    phoneTypeIcon.setImageResource(R.drawable.ic_phone)
                }
            }
        }

        private fun getPhoneTypeLabel(type: Int): String {
            return when (type) {
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> 
                    itemView.context.getString(R.string.phone_type_home)
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> 
                    itemView.context.getString(R.string.phone_type_mobile)
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> 
                    itemView.context.getString(R.string.phone_type_work)
                ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> 
                    itemView.context.getString(R.string.phone_type_main)
                else -> itemView.context.getString(R.string.phone_type_other)
            }
        }
    }
}

data class PhoneNumberItem(
    val number: String,
    val type: Int,
    val photoUri: String? = null
)
