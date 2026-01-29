package com.example.pos_system.data.model

data class Sales(
    val saleId: String = "",
    val totalAmount: Double = 0.0,
    val discountApplied: Double = 0.0,
    val dateTime: Long = System.currentTimeMillis(), // Maps to your Firebase dateTime
    val finalPrice: Double = 0.0,
    val items: List<CartItem> = emptyList(), // Firebase Array of Maps
    val paymentType: String = ""
)