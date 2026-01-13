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
    }

    // Save Login State
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Save User Role (Admin, Cashier, etc.)
    fun setUserRole(role: String) {
        sharedPreferences.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, "Staff")
    }

    // Save Branch ID (Useful for POS systems with multiple outlets)
    fun setBranchId(branchId: String) {
        sharedPreferences.edit().putString(KEY_BRANCH_ID, branchId).apply()
    }

    fun getBranchId(): String? {
        return sharedPreferences.getString(KEY_BRANCH_ID, "Main")
    }

    // Track Last Firebase Sync Time
    fun setLastSyncTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC, 0L)
    }

    // Clear all data (Logout)
    fun clearPrefs() {
        sharedPreferences.edit().clear().apply()
    }
}
