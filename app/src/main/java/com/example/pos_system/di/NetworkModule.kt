package com.example.pos_system.di

import android.content.Context
import com.example.pos_system.data.remote.CloudinaryService
import com.example.pos_system.data.remote.FirebaseHelper
import com.example.pos_system.data.remote.FirebaseService

class NetworkModule(private val context: Context) {

    private val firebaseHelper: FirebaseHelper by lazy {
        FirebaseHelper()
    }

    val firebaseService: FirebaseService by lazy {
        FirebaseService(firebaseHelper)
    }

    val cloudinaryService: CloudinaryService by lazy {
        CloudinaryService().apply {
            init(context) // Ensure Cloudinary is initialized with your keys
        }
    }
}
