package com.example.pos_system.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SalesEntity(
    @PrimaryKey
    val saleId: String,
    val totalAmount: Double,
    val timestamp: Long,
    val itemsJson: String // Stores the list of items sold as a JSON string
)
