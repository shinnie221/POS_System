package com.example.pos_system.di

import com.example.pos_system.data.repository.AuthRepository
import com.example.pos_system.data.repository.CategoryRepository
import com.example.pos_system.data.repository.ItemRepository
import com.example.pos_system.data.repository.SalesRepository

class RepositoryModule(
    private val databaseModule: DatabaseModule,
    private val networkModule: NetworkModule
) {

    val authRepository: AuthRepository by lazy {
        AuthRepository(databaseModule.preferenceManager)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(
            categoryDao = databaseModule.provideCategoryDao(),
            firebaseService = networkModule.firebaseService
        )
    }

    val itemRepository: ItemRepository by lazy {
        ItemRepository(
            itemDao = databaseModule.provideItemDao(),
            firebaseService = networkModule.firebaseService,
            cloudinaryService = networkModule.cloudinaryService
        )
    }

    val salesRepository: SalesRepository by lazy {
        SalesRepository(
            salesDao = databaseModule.provideSalesDao(),
            firebaseService = networkModule.firebaseService
        )
    }
}
