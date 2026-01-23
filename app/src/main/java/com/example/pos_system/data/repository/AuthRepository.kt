package com.example.pos_system.data.repository

import com.example.pos_system.data.local.prefs.PreferenceManager

class AuthRepository(private val preferenceManager: PreferenceManager) {

    /**
     * Called after successful Firebase Authentication.
     * Saves the session and stores credentials for offline access.
     */
    fun login(email: String, role: String, password: String? = null) {
        preferenceManager.setLoggedIn(true)
        preferenceManager.setUserRole(role)
        preferenceManager.setBranchId(email)

        // Save the password locally so we can check it when WiFi is off
        if (password != null) {
            preferenceManager.saveCredentials(email, password)
        }
    }

    /**
     * Checks if the entered credentials match the last successful login.
     * Use this when the device is offline.
     */
    fun checkOfflineLogin(email: String, password: String): Boolean {
        val savedEmail = preferenceManager.getSavedEmail()
        val savedPass = preferenceManager.getSavedPassword()

        return email == savedEmail && password == savedPass
    }

    fun logout() {
        preferenceManager.clearPrefs()
    }

    fun isUserLoggedIn(): Boolean = preferenceManager.isLoggedIn()
}