package com.example.pos_system.data.remote

import com.example.pos_system.data.model.Sales

class FirebaseService(val helper: FirebaseHelper = FirebaseHelper()) {

    suspend fun addCategory(id: String, name: String) {
        helper.saveData("category", mapOf(
            "categoryId" to id,
            "categoryName" to name
        ))
    }

    suspend fun addItem(id: String, name: String, price: Double, catId: String, type: String) {
        val item = mapOf(
            "itemId" to id,
            "itemName" to name,
            "itemPrice" to price,
            "categoryId" to catId,
            "itemType" to type
        )
        // Use the specific ID instead of .add()
        helper.setDataWithId("item", id, item)
    }

    suspend fun recordSale(sale: Sales) {
        // We let Firebase generate the Alphanumeric ID first
        val saleData = mapOf(
            "saleId" to sale.saleId,
            "totalAmount" to sale.totalAmount,
            "discountApplied" to sale.discountApplied,
            "dateTime" to com.google.firebase.Timestamp(java.util.Date(sale.dateTime)), // Store as Firebase Timestamp
            "finalPrice" to sale.finalPrice,
            "items" to sale.items.map { cartItem ->
                mapOf(
                    "item" to mapOf(
                        "categoryId" to cartItem.item.categoryId,
                        "itemName" to cartItem.item.itemName,
                        "itemPrice" to cartItem.item.itemPrice
                    ),
                    "quantity" to cartItem.quantity
                )
            }
        )
        // Use the ID generated in the ViewModel to save
        helper.setDataWithId("sales", sale.saleId, saleData)
    }
}
