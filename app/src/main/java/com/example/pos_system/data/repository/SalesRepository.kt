package com.example.pos_system.data.repository

import android.util.Log
import com.example.pos_system.data.local.database.dao.SalesDao
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.data.model.Sales
import com.example.pos_system.data.remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SalesRepository(
    private val salesDao: SalesDao,
    private val firebaseService: FirebaseService
) {
    val salesHistory: Flow<List<SalesEntity>> = salesDao.getAllSales()
    fun startRealTimeSync() {
        firebaseService.listenToCollection<Map<String, Any>>("sales") { dataList, idList, _ ->
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {
                dataList.forEachIndexed { index, data ->

                    // FIX 1: Safe Date Extraction
                    val rawDate = data["dateTime"] ?: data["timestamp"] ?: data["createdAt"]
                    val actualTimestamp = when (rawDate) {
                        is Number -> rawDate.toLong()
                        is com.google.firebase.Timestamp -> rawDate.toDate().time
                        is String -> rawDate.toLongOrNull() ?: System.currentTimeMillis()
                        else -> System.currentTimeMillis()
                    }

                    // FIX 2: Safe Items Extraction (Handle both List and JSON String)
                    val rawItems = data["items"] ?: data["itemsJson"]
                    val itemsJsonString = when (rawItems) {
                        is String -> rawItems
                        is List<*> -> Gson().toJson(rawItems) // Convert Firebase Array back to JSON
                        else -> "[]"
                    }

                    val entity = SalesEntity(
                        id = idList[index],
                        totalAmount = (data["finalPrice"] as? Number)?.toDouble()
                            ?: (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        timestamp = actualTimestamp,
                        itemsJson = itemsJsonString,
                        paymentType = data["paymentType"] as? String ?: "Cash",
                        isSynced = true
                    )
                    salesDao.insertSale(entity)
                }
            }
        }
    }

    suspend fun pushToFirebase(entity: SalesEntity) {
        val db = FirebaseFirestore.getInstance()
        // We use a Map to ensure names match exactly what startRealTimeSync expects
        val saleData = hashMapOf(
            "finalPrice" to entity.totalAmount,
            "dateTime" to entity.timestamp,
            "itemsJson" to entity.itemsJson, // Storing as String for Room compatibility
            "paymentType" to entity.paymentType,
            "isSynced" to true
        )
        db.collection("sales").document(entity.id).set(saleData).await()
    }

    suspend fun processCheckout(salesModel: Sales) {
        // 1. Create the local Entity
        val salesEntity = SalesEntity(
            id = salesModel.saleId,
            totalAmount = salesModel.finalPrice,
            timestamp = salesModel.dateTime,
            itemsJson = Gson().toJson(salesModel.items),
            paymentType = salesModel.paymentType,
            isSynced = false // Mark as NOT synced yet
        )

        // 2. Save to Room (Local Database) - THIS WORKS OFFLINE
        salesDao.insertSale(salesEntity)

        // 3. Attempt to upload to Firebase in the background (Non-blocking)
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // We use pushToFirebase directly here to try an immediate sync
                pushToFirebase(salesEntity)

                // 4. If the code reaches here, it means we are online!
                // Mark as synced so the SyncWorker doesn't do it again later.
                markAsSynced(salesEntity.id)
                Log.d("POS_SYNC", "Checkout synced to Firebase immediately.")
            } catch (e: Exception) {
                // If this fails (No WiFi), we DON'T show an error to the user.
                // The sale is already safe in the local database.
                // The SyncWorker (scheduled in POSSystem.kt) will pick it up later.
                Log.d("POS_SYNC", "Offline: Sale saved locally. Will sync when WiFi returns.")
            }
        }
    }

    suspend fun deleteSale(sale: SalesEntity) {
        salesDao.deleteSale(sale)
        try { firebaseService.deleteSale(sale.id) } catch (e: Exception) {}
    }

    suspend fun getUnsyncedSales() = salesDao.getUnsyncedSales()
    suspend fun markAsSynced(saleId: String) = salesDao.markAsSynced(saleId)
}