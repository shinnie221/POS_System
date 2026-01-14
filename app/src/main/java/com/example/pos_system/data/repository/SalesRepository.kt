package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.SalesDao
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.data.model.Sales
import com.example.pos_system.data.remote.FirebaseService
import com.google.gson.Gson
import java.util.UUID

class SalesRepository(
    private val salesDao: SalesDao,
    private val firebaseService: FirebaseService
) {
    val salesHistory = salesDao.getAllSales()

    suspend fun processCheckout(salesModel: Sales) {
        // generate a unique ID
        val saleId = UUID.randomUUID().toString()

        // 1. Prepare Room Entity
        val salesEntity = SalesEntity(
            id = saleId,
            totalAmount = salesModel.finalPrice,
            timestamp = salesModel.dateTime,
            itemsJson = Gson().toJson(salesModel.items)
        )

        // 2. Save to Local Room
        salesDao.insertSale(salesEntity)

        // 3. Sync to Firebase
        val updatedSalesModel = salesModel.copy(saleId = saleId) // Assuming Sales data class has a saleId property
        firebaseService.recordSale(updatedSalesModel)
    } // processCheckout ends here

    // FIXED: Move this OUTSIDE of processCheckout
    suspend fun deleteSale(sale: SalesEntity) {
        salesDao.deleteSale(sale)
        // Optionally add Firebase deletion here later
    }

    // Inside SalesRepository.kt
    suspend fun syncSales() {
        try {
            // 1. Fetch from Firebase
            val snapshot = firebaseService.helper.fetchCollectionWithIds("sales")
            snapshot.forEach { (docId, data) ->
                // Convert Firebase data to SalesEntity
                val finalPrice = (data["finalPrice"] as? Number)?.toDouble() ?: 0.0

                // Handle Firebase Timestamp conversion
                val timestamp = when (val dt = data["dateTime"]) {
                    is com.google.firebase.Timestamp -> dt.toDate().time
                    is Long -> dt
                    else -> System.currentTimeMillis()
                }

                val itemsJson = data["itemsJson"] as? String ?: ""
                // Note: If you stored items as an Array in Firebase,
                // you'd need to convert them back to JSON here.

                val entity = SalesEntity(
                    id = docId,
                    totalAmount = finalPrice,
                    timestamp = timestamp,
                    itemsJson = itemsJson
                )

                // 2. Save to Room
                salesDao.insertSale(entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Sales Sync Error: ${e.message}")
        }
    }
}