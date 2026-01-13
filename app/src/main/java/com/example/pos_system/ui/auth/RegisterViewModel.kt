package com.example.pos_system.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pos_system.POSSystem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val authRepository = (application as POSSystem).appModule.authRepository

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun register(email: String, name: String, pass: String, passConfirm: String, phone: String) {
        if (email.isBlank() || name.isBlank() || pass.isBlank() || phone.isBlank()) {
            _error.value = "All fields are required"
            return
        }
        if (pass != passConfirm) {
            _error.value = "Passwords do not match"
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""

                    // Structure matching your requirement
                    val userMap = hashMapOf(
                        "userId" to uid,
                        "username" to name,
                        "userEmail" to email,
                        "password" to pass,
                        "phone" to phone
                    )

                    // Automatically generate document using the Auth UID
                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            // Store the name locally so Profile screen can see it immediately
                            authRepository.login(name, "Admin")
                            _isRegistered.value = true
                        }
                        .addOnFailureListener { e ->
                            _error.value = "Failed to save user data: ${e.message}"
                        }
                } else {
                    _error.value = task.exception?.message ?: "Registration Failed"
                }
            }
    }

    fun clearError() { _error.value = null }
}