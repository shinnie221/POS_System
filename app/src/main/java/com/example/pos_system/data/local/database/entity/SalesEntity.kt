package com.example.pos_system.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SalesEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val totalAmount: Double,
    val itemsJson: String
)