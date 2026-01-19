package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.entity.ItemEntity
import com.example.pos_system.data.remote.CloudinaryService
import com.example.pos_system.data.remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ItemRepository(
    private val itemDao: ItemDao,
    private val firebaseService: FirebaseService
) {
    // Get all items from local Room database
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    // Get items filtered by category ID from local Room database
    fun getItemsByCategory(catId: String): Flow<List<ItemEntity>> = itemDao.getItemsByCategory(catId)

    /**
     * Adds a new item to both the local Room database and remote Firebase.
     * Generates a unique String ID to ensure compatibility with Firebase auto-generated IDs.
     */
    suspend fun addItem(name: String, price: Double, categoryId: String, itemType: String, imagePath: String? = null) {

        val db = FirebaseFirestore.getInstance()
        val firebaseId = db.collection("item").document().id
        val currentTime = System.currentTimeMillis()

        // 3. Save to local Room database
        val itemEntity = ItemEntity(
            id = firebaseId,
            name = name,
            price = price,
            categoryId = categoryId,
            itemType = itemType,
            createdAt = currentTime
        )
        itemDao.insertItem(itemEntity)

        // 4. Sync to Firebase
        firebaseService.addItem(
            id = firebaseId,
            name = name,
            price = price,
            catId = categoryId,
            type = itemType,
            createdAt = currentTime
        )
    }

    // In ItemRepository.kt
    suspend fun syncItems() {
        try {
            val snapshot = firebaseService.helper.fetchCollectionWithIds("item")
            itemDao.deleteAllItems() // Optional: keeps local in sync with remote deletions

            snapshot.forEach { (docId, data) ->
                val name = data["itemName"] as? String ?: ""
                val price = (data["itemPrice"] as? Number)?.toDouble() ?: 0.0
                val catId = data["categoryId"] as? String ?: ""
                val type = data["itemType"] as? String ?: "water"
                // FIX: Get the original timestamp from Firebase
                val createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()

                if (name.isNotEmpty()) {
                    itemDao.insertItem(
                        ItemEntity(
                            id = docId,
                            name = name,
                            price = price,
                            categoryId = catId,
                            itemType = type,
                            createdAt = createdAt // Use the Firebase time
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Error: ${e.message}")
        }
    }

    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
        try {
            firebaseService.deleteItem(item.id)
        } catch (e: Exception) {
            android.util.Log.e("DELETE_ERROR", "Item Firebase delete failed: ${e.message}")
        }
    }
}