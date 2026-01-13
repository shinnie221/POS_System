package com.example.pos_system.data.model

data class Item(
    val itemId: String = "",       // Firebase document ID
    val itemName: String = "",
    val itemPrice: Double = 0.0,
    val categoryId: String = ""
)
