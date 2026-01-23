package com.example.pos_system.data.local.prefs

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_LAST_SYNC = "last_sync_time"
        // New keys for offline auth
        private const val KEY_SAVED_EMAIL = "saved_user_email"
        private const val KEY_SAVED_PASSWORD = "saved_user_password"
    }

    // --- Existing Methods ---
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun setUserRole(role: String) {
        sharedPreferences.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String? = sharedPreferences.getString(KEY_USER_ROLE, "Staff")

    fun setBranchId(branchId: String) {
        sharedPreferences.edit().putString(KEY_BRANCH_ID, branchId).apply()
    }

    fun getBranchId(): String? = sharedPreferences.getString(KEY_BRANCH_ID, "Main")

    // --- NEW METHODS FOR OFFLINE LOGIN ---

    /**
     * Saves credentials locally after a successful Firebase login.
     */
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit()
            .putString(KEY_SAVED_EMAIL, email)
            .putString(KEY_SAVED_PASSWORD, password)
            .apply()
    }

    fun getSavedEmail(): String? = sharedPreferences.getString(KEY_SAVED_EMAIL, null)

    fun getSavedPassword(): String? = sharedPreferences.getString(KEY_SAVED_PASSWORD, null)

    fun setLastSyncTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun getLastSyncTime(): Long = sharedPreferences.getLong(KEY_LAST_SYNC, 0L)

    fun clearPrefs() {
        sharedPreferences.edit().clear().apply()
    }
}