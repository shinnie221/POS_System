package com.example.pos_system.data.repository

import androidx.activity.result.launch
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

class SalesRepository(
    private val salesDao: SalesDao,
    private val firebaseService: FirebaseService
) {
    val salesHistory = salesDao.getAllSales()

    suspend fun processCheckout(salesModel: Sales) {
        val db = FirebaseFirestore.getInstance()
        val firebaseId = db.collection("sales").document().id


        // 1. Prepare Room Entity
        val salesEntity = SalesEntity(
            id = firebaseId,
            totalAmount = salesModel.finalPrice,
            timestamp = salesModel.dateTime,
            itemsJson = Gson().toJson(salesModel.items),
            paymentType = salesModel.paymentType
        )

        // 2. Save to Local Room
        salesDao.insertSale(salesEntity)

        // 3. Sync to Firebase
        val updatedSalesModel =
            salesModel.copy(saleId = firebaseId) // Assuming Sales data class has a saleId property
        firebaseService.recordSale(updatedSalesModel)
    } // processCheckout ends here

    suspend fun deleteSale(sale: SalesEntity) {
        //Delete from local room
        salesDao.deleteSale(sale)

        //Delete from firebase
        try {
            firebaseService.deleteSale(sale.id)
        } catch (e: Exception) {
            android.util.Log.e("DELETE_ERROR", "Sale Firebase delete failed: ${e.message}")
        }
    }

    fun getSalesForPeriod(startDate: Long, endDate: Long): Flow<List<SalesEntity>> {
        // You might need to add this @Query to your SalesDao
        return salesDao.getSalesPeriod(startDate, endDate)
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

                val itemsData = data["items"]
                val itemsJson = when (itemsData) {
                    is String -> itemsData // If it's already a string
                    is List<*> -> Gson().toJson(itemsData) // If it's a Firebase Array (Manual Entry)
                    else -> data["itemsJson"] as? String ?: "[]" // Fallback to your old key
                }

                val entity = SalesEntity(
                    id = docId,
                    totalAmount = finalPrice,
                    timestamp = timestamp,
                    itemsJson = itemsJson,
                    paymentType = data["paymentType"] as? String ?: "Cash"
                )

                // 2. Save to Room
                salesDao.insertSale(entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Sales Sync Error: ${e.message}")
        }
    }

    /**
     * Listens to Firebase "sales" collection and updates Room in real-time.
     */
    // In SalesRepository.kt

    fun startRealTimeSync() {
        firebaseService.listenToCollection<Map<String, Any>>("sales") { dataList, idList, deletedIds ->
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {

                // 1. Handle Deletions: If ID is in deletedIds, remove it from Room
                deletedIds.forEach { id ->
                    salesDao.deleteSaleById(id)
                }

                // 2. Handle Adds/Updates
                dataList.forEachIndexed { index, data ->
                    val finalPrice = (data["finalPrice"] as? Number)?.toDouble() ?: 0.0
                    val timestamp = when (val dt = data["dateTime"]) {
                        is com.google.firebase.Timestamp -> dt.toDate().time
                        is Long -> dt
                        else -> System.currentTimeMillis()
                    }

                    val itemsData = data["items"]
                    val itemsJson = when (itemsData) {
                        is String -> itemsData
                        is List<*> -> Gson().toJson(itemsData)
                        else -> data["itemsJson"] as? String ?: "[]"
                    }

                    val entity = SalesEntity(
                        id = idList[index],
                        totalAmount = finalPrice,
                        timestamp = timestamp,
                        itemsJson = itemsJson,
                        paymentType = data["paymentType"] as? String ?: "Cash"
                    )
                    salesDao.insertSale(entity)
                }
            }
        }
    }
}