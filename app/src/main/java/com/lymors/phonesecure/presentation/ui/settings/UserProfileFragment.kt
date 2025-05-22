package com.lymors.phonesecure.presentation.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.lymors.phonesecure.R
import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.User
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                retrieveContactInfo(contactUri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchContactPicker()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.permission_contacts_rationale,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var emergencyContactsList: RecyclerView
    private lateinit var addContactButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var contactsAdapter: EmergencyContactAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var viewModel: UserProfileViewModel
    private var dialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        
        // Initialize ViewModel
        viewModel = UserProfileViewModel(requireContext().userRepository)
    }

    private fun initializeViews(view: View) {
        nameInput = view.findViewById(R.id.nameInput)
        emailInput = view.findViewById(R.id.emailInput)
        phoneInput = view.findViewById(R.id.phoneInput)
        emergencyContactsList = view.findViewById(R.id.emergencyContactsList)
        addContactButton = view.findViewById(R.id.addContactButton)
        saveButton = view.findViewById(R.id.saveButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)
    }

    private fun setupRecyclerView() {
        contactsAdapter = EmergencyContactAdapter { contact ->
            contact.id?.let { viewModel.deleteEmergencyContact(it) }
        }
        emergencyContactsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }
    }

    private fun setupClickListeners() {
        addContactButton.setOnClickListener {
            showAddContactDialog()
        }

        saveButton.setOnClickListener {
            if (validateInputs()) {
                viewModel.saveUserProfile(
                    name = nameInput.text.toString(),
                    email = emailInput.text.toString(),
                    phone = phoneInput.text.toString()
                )
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UserProfileUiState.Loading -> {
                        progressIndicator.isVisible = true
                        saveButton.isEnabled = false
                    }
                    is UserProfileUiState.Success -> {
                        progressIndicator.isVisible = false
                        saveButton.isEnabled = true
                        updateUI(state.user)
                    }
                    is UserProfileUiState.Error -> {
                        progressIndicator.isVisible = false
                        saveButton.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateUI(user: User) {
        nameInput.setText(user.name)
        emailInput.setText(user.email)
        phoneInput.setText(user.phoneNumber)
        contactsAdapter.updateContacts(user.emergencyContacts)
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_contact, null)

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.contactNameInput)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.contactPhoneInput)
        val pickContactButton = dialogView.findViewById<MaterialButton>(R.id.pickContactButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_emergency_contact)
            .setView(dialogView)
            .setPositiveButton(R.string.add_contact, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString()
                val phone = phoneInput.text.toString()
                if (validateContactInput(name, phone)) {
                    viewModel.addEmergencyContact(name, phone)
                    dialog.dismiss()
                }
            }
        }

        pickContactButton.setOnClickListener {
            checkContactPermissionAndPick()
        }

        dialog.show()
    }

    private fun validateContactInput(name: String, phone: String): Boolean {
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter contact name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(context, R.string.error_invalid_phone, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkContactPermissionAndPick() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchContactPicker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showContactPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun showContactPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(R.string.permission_contacts_rationale)
            .setPositiveButton("Grant") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContact.launch(intent)
    }

    private fun retrieveContactInfo(contactUri: Uri) {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_URI
        )

        requireContext().contentResolver.query(
            contactUri, projection, null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val photoUriIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                val name = cursor.getString(nameIndex)
                val hasPhone = cursor.getString(hasPhoneIndex).toInt()
                val id = cursor.getString(idIndex)
                val photoUri = if (photoUriIndex != -1) cursor.getString(photoUriIndex) else null

                if (hasPhone > 0) {
                    retrievePhoneNumber(id, name, photoUri)
                }
            }
        }
    }

    private fun retrievePhoneNumber(contactId: String, contactName: String, photoUri: String? = null) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        val phoneNumbers = mutableListOf<PhoneNumberItem>()

        requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                val phoneNumber = cursor.getString(phoneIndex)
                val phoneType = cursor.getInt(typeIndex)

                phoneNumbers.add(PhoneNumberItem(phoneNumber, phoneType, photoUri))
            }
        }

        when (phoneNumbers.size) {
            0 -> Toast.makeText(
                requireContext(),
                "No phone numbers found for this contact",
                Toast.LENGTH_SHORT
            ).show()
            1 -> viewModel.addEmergencyContact(contactName, phoneNumbers[0].number)
            else -> showPhoneNumberSelectionDialog(contactName, phoneNumbers, photoUri)
        }
    }

    private fun showPhoneNumberSelectionDialog(
        contactName: String,
        phoneNumbers: List<PhoneNumberItem>,
        photoUri: String? = null
    ) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_select_phone_number, null)

        val contactNameText = dialogView.findViewById<TextView>(R.id.contactNameText)
        val contactPhotoImage = dialogView.findViewById<ImageView>(R.id.contactPhotoImage)
        val phoneNumbersList = dialogView.findViewById<RecyclerView>(R.id.phoneNumbersList)

        contactNameText.text = contactName
        
        // Set contact photo if available
        if (photoUri != null) {
            try {
                val contactPhotoUri = Uri.parse(photoUri)
                contactPhotoImage.setImageURI(contactPhotoUri)
                // If setImageURI fails (returns null drawable), fall back to default
                if (contactPhotoImage.drawable == null) {
                    contactPhotoImage.setImageResource(R.drawable.ic_person)
                }
            } catch (e: Exception) {
                contactPhotoImage.setImageResource(R.drawable.ic_person)
            }
        } else {
            contactPhotoImage.setImageResource(R.drawable.ic_person)
        }

        val adapter = PhoneNumberAdapter { selectedNumber ->
            viewModel.addEmergencyContact(contactName, selectedNumber.number)
            dialog?.dismiss()
        }

        phoneNumbersList.layoutManager = LinearLayoutManager(context)
        phoneNumbersList.adapter = adapter
        adapter.setPhoneNumbers(phoneNumbers)

        dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog?.show()
    }

    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val email = emailInput.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        val phone = phoneInput.text.toString()
        if (!Patterns.PHONE.matcher(phone).matches()) {
            phoneInput.error = getString(R.string.error_invalid_phone)
            isValid = false
        }

        return isValid
    }



    companion object {
        fun newInstance() = UserProfileFragment()
    }
}
