package com.example.pos_system.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pos_system.data.local.database.dao.CategoryDao
import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.dao.SalesDao
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.local.database.entity.ItemEntity
import com.example.pos_system.data.local.database.entity.SalesEntity


@Database(
    entities = [CategoryEntity::class, ItemEntity::class, SalesEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun salesDao(): SalesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_database"
                )
                    .fallbackToDestructiveMigration() // Useful during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
