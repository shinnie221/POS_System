package com.example.pos_system.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pos_system.POSSystem

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val appModule = (application as POSSystem).appModule
    private val prefManager = appModule.preferenceManager

    // In AuthRepo, we set branchId = Name.
    val userName: String = prefManager.getBranchId() ?: "User"

    fun logout(onLogout: () -> Unit) {
        appModule.authRepository.logout()
        onLogout()
    }
}