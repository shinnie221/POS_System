package com.example.pos_system.data.remote

import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.data.model.Sales
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

//Link id to firebase id is added here
class FirebaseService(val helper: FirebaseHelper = FirebaseHelper()) {
    private val db = FirebaseFirestore.getInstance()

    fun addCategory(id: String, name: String,createdAt: Long) {
        val data = hashMapOf(
            "categoryId" to id,
            "categoryName" to name,
            "createdAt" to createdAt
            )

        // Use .document(id).set() to force the specific ID
        db.collection("category")
            .document(id)
            .set(data)
    }

    fun addItem(id: String, name: String, price: Double, catId: String, type: String,createdAt: Long) {
        val item = mapOf(
            "itemId" to id,
            "itemName" to name,
            "itemPrice" to price,
            "categoryId" to catId,
            "itemType" to type,
            "createdAt" to createdAt
        )
        // Use the specific ID instead of .add()
        db.collection("item").document(id).set(item)
    }

    suspend fun recordSale(sale: Sales) {
        // We let Firebase generate the Alphanumeric ID first
        val saleData = mapOf(
            "saleId" to sale.saleId,
            "totalAmount" to sale.totalAmount,
            "discountApplied" to sale.discountApplied,
            "paymentType" to sale.paymentType,
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

    suspend fun deleteCategory(id: String) {
        db.collection("category").document(id).delete().await()
    }

    suspend fun deleteItem(id: String) {
        db.collection("item").document(id).delete().await()
    }

    suspend fun deleteSale(id: String) {
        // If your sales IDs are Strings in Firebase
        db.collection("sales").document(id).delete().await()
    }
}
