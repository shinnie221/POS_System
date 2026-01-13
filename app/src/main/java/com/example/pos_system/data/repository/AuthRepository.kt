package com.example.pos_system.data.repository

import com.example.pos_system.data.local.prefs.PreferenceManager

class AuthRepository(private val preferenceManager: PreferenceManager) {fun login(email: String, role: String) {
    preferenceManager.setLoggedIn(true)
    preferenceManager.setUserRole(role)
    // Storing email as a session identifier
    preferenceManager.setBranchId(email)
}

    fun logout() {
        preferenceManager.clearPrefs()
    }

    fun isUserLoggedIn(): Boolean = preferenceManager.isLoggedIn()
}
