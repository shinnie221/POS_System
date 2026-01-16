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


        // 3. Save to local Room database
        val itemEntity = ItemEntity(
            id = firebaseId,
            name = name,
            price = price,
            categoryId = categoryId,
            itemType = itemType
        )
        itemDao.insertItem(itemEntity)

        // 4. Sync to Firebase
        firebaseService.addItem(
            id = firebaseId,
            name = name,
            price = price,
            catId = categoryId,
            type = itemType
        )

        // Note: If you want itemType in Firebase (which you should for sync),
        // update FirebaseService.addItem to accept it.
    }

    /**
     * Pulls items from Firebase and saves them to the local Room database.
     * Ensures itemType is synced so the UI knows whether to show Milk/Oatmilk options.
     */
    suspend fun syncItems() {
        try {
            val snapshot = firebaseService.helper.fetchCollectionWithIds("item")

            // 1. CLEAR local items first so deleted Firebase items disappear from the app
            itemDao.deleteAllItems()

            snapshot.forEach { (docId, data) ->
                val name = data["itemName"] as? String ?: ""
                val price = when (val p = data["itemPrice"]) {
                    is Double -> p
                    is Long -> p.toDouble()
                    else -> 0.0
                }
                val catId = data["categoryId"] as? String ?: ""
                val type = data["itemType"] as? String ?: "water"

                if (name.isNotEmpty()) {
                    // 2. Use docId (Firebase ID) as the ID to prevent duplicates
                    itemDao.insertItem(
                        ItemEntity(
                            id = docId,
                            name = name,
                            price = price,
                            categoryId = catId,
                            itemType = type
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