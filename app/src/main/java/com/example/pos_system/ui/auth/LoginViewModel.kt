package com.example.pos_system.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pos_system.POSSystem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val authRepository = (application as POSSystem).appModule.authRepository

    private val _isLoggedIn = MutableStateFlow(authRepository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Please enter email and password"
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""

                    // Fetch the username from the "users" collection using the UID
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val name = document.getString("username") ?: "User"
                                // We store the Name in the branchId field of prefs for display
                                authRepository.login(name, "Admin")
                                _isLoggedIn.value = true
                            } else {
                                _error.value = "User profile not found"
                            }
                        }
                        .addOnFailureListener { e ->
                            _error.value = "Database Error: ${e.message}"
                        }
                } else {
                    _error.value = task.exception?.message ?: "Invalid credentials"
                }
            }
    }

    fun clearError() {
        _error.value = null
    }
}