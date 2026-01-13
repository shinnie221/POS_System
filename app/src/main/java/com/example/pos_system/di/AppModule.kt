package com.example.pos_system.di

import android.content.Context

/**
 * AppModule acts as the central coordinator for Dependency Injection.
 * It initializes specialized modules and provides easy access to repositories.
 */
class AppModule(private val context: Context) {

    // 1. Initialize specialized modules
    private val databaseModule = DatabaseModule(context)
    private val networkModule = NetworkModule(context)

    // 2. Initialize Repository module using the Database and Network modules
    private val repositoryModule = RepositoryModule(databaseModule, networkModule)

    // 3. Expose Repositories for use in ViewModels
    val authRepository get() = repositoryModule.authRepository
    val categoryRepository get() = repositoryModule.categoryRepository
    val itemRepository get() = repositoryModule.itemRepository
    val salesRepository get() = repositoryModule.salesRepository

    // 4. Expose Preference Manager (if needed directly in UI)
    val preferenceManager get() = databaseModule.preferenceManager
}
