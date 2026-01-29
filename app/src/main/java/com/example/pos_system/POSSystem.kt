package com.example.pos_system

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pos_system.data.remote.SyncWorker
import com.example.pos_system.di.AppModule
import java.util.concurrent.TimeUnit

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

        // Start the background sync manager
        setupOfflineSync()
    }

    private fun setupOfflineSync() {
        // Define when the sync should happen (only when connected to internet)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule the sync to run every 15 minutes
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}