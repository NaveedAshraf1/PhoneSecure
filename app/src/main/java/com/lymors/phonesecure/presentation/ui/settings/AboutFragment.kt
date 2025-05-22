package com.lymors.phonesecure.presentation.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.lymors.phonesecure.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupVersionInfo(view)
        setupClickListeners(view)
    }

    private fun setupVersionInfo(view: View) {
        val versionInfo = view.findViewById<TextView>(R.id.versionInfo)
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            val version = "Version ${packageInfo.versionName} (${packageInfo.versionCode})"
            versionInfo.text = version
        } catch (e: PackageManager.NameNotFoundException) {
            versionInfo.visibility = View.GONE
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<TextView>(R.id.privacyPolicy).setOnClickListener {
            openUrl("https://phonesecure.app/privacy")
        }

        view.findViewById<TextView>(R.id.licenses).setOnClickListener {
            // TODO: Implement licenses dialog or screen
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    companion object {
        fun newInstance() = AboutFragment()
    }
}
