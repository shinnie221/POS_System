package com.example.pos_system

import android.app.Application
import com.example.pos_system.di.AppModule

/**
 * The main Application class for the POS System.
 * This class is used to initialize Dependency Injection and
 * maintain a global state for the app's lifetime.
 */
class POSSystem : Application() {

    // Global instance of the AppModule to be accessed by ViewModels
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()

        // Initialize the Dependency Injection container
        appModule = AppModule(this)
    }
}
