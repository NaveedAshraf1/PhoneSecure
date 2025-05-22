package com.lymors.phonesecure.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Extension functions for Activity to provide common functionality
 * without inheritance.
 */

/**
 * Extension to show a toast message
 */
fun Activity.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Extension to show a snackbar from an Activity
 */
fun Activity.snackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    action: String? = null,
    actionListener: (() -> Unit)? = null
) {
    val view = this.findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(view, message, duration)
    
    action?.let {
        snackbar.setAction(it) { actionListener?.invoke() }
    }
    
    snackbar.show()
}

/**
 * Extension to hide the keyboard
 */
fun Activity.hideKeyboard() {
    val view = this.currentFocus
    view?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

/**
 * Extension to set up the toolbar with back button
 */
fun AppCompatActivity.setupToolbar(showBackButton: Boolean = true) {
    supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(showBackButton)
        setDisplayShowHomeEnabled(showBackButton)
    }
}

/**
 * Extension to replace a fragment in a container
 */
fun AppCompatActivity.replaceFragmentInContainer(
    fragment: Fragment,
    @IdRes containerViewId: Int,
    addToBackStack: Boolean = true,
    tag: String? = null
) {
    supportFragmentManager.beginTransaction().apply {
        replace(containerViewId, fragment, tag)
        if (addToBackStack) {
            addToBackStack(tag)
        }
    }.commit()
}

/**
 * Extension to navigate to another activity with optional extras
 */
inline fun <reified T : Activity> Activity.navigateTo(
    finishCurrent: Boolean = false,
    clearBackStack: Boolean = false,
    noinline extras: (intent: Intent) -> Unit = {}
) {
    val intent = Intent(this, T::class.java).apply(extras)
    
    if (clearBackStack) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    startActivity(intent)
    
    if (finishCurrent) {
        finish()
    }
}

/**
 * Extension to handle back press in fragments
 */
fun AppCompatActivity.onBackPressedInFragments() {
    val fragment = supportFragmentManager.findFragmentById(android.R.id.content)
    if (fragment is OnBackPressListener) {
        if (!fragment.onBackPressed()) {
            super.onBackPressed()
        }
    } else {
        super.onBackPressed()
    }
}

/**
 * Interface for fragments to handle back press
 */
interface OnBackPressListener {
    fun onBackPressed(): Boolean
}
