package com.example.pos_system.data.model

data class CartItem(
    val item: Item,
    var quantity: Int = 1
) {
    // Helper to calculate total for this specific line item
    val totalPrice: Double
        get() = item.itemPrice * quantity
}
