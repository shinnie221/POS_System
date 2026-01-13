package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.entity.ItemEntity
import com.example.pos_system.data.remote.CloudinaryService
import com.example.pos_system.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ItemRepository(
    private val itemDao: ItemDao,
    private val firebaseService: FirebaseService,
    private val cloudinaryService: CloudinaryService
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
        // 1. Handle image upload via Cloudinary if a path is provided
        var finalImageUrl = ""
        if (imagePath != null) {
            cloudinaryService.uploadItemImage(imagePath) { url ->
                finalImageUrl = url ?: ""
            }
        }

        // 2. Generate a unique ID for the new item
        val uniqueId = UUID.randomUUID().toString()

        // 3. Save to local Room database
        val itemEntity = ItemEntity(
            id = uniqueId,
            name = name,
            price = price,
            categoryId = categoryId,
            itemType = itemType
        )
        itemDao.insertItem(itemEntity)

        // 4. Sync to Firebase
        firebaseService.addItem(
            id = uniqueId,
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
        // Optionally add firebaseService.deleteItem here
    }
}