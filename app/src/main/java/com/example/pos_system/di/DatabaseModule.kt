package com.example.pos_system.di

import android.content.Context
import com.example.pos_system.data.local.database.AppDatabase
import com.example.pos_system.data.local.database.dao.CategoryDao
import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.dao.SalesDao
import com.example.pos_system.data.local.prefs.PreferenceManager

class DatabaseModule(private val context: Context) {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(context)
    }

    fun provideCategoryDao(): CategoryDao = database.categoryDao()
    fun provideItemDao(): ItemDao = database.itemDao()
    fun provideSalesDao(): SalesDao = database.salesDao()
}

