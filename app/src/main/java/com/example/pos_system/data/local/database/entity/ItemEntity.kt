package com.example.pos_system.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Double,
    val categoryId: String, // Link to CategoryEntity
    val itemType: String,
    val createdAt: Long = System.currentTimeMillis()
)
