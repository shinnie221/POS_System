package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.CategoryDao
import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val firebaseService: FirebaseService
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun addCategory(name: String) {
        val db = FirebaseFirestore.getInstance()
        val firebaseId = db.collection("category").document().id
        val currentTime = System.currentTimeMillis()

        val newCategory = CategoryEntity(id = firebaseId, name = name, createdAt = currentTime)
        categoryDao.insertCategory(newCategory)

        firebaseService.addCategory(id = firebaseId, name = name, createdAt = currentTime)
    }

    suspend fun deleteCategory(categoryId: String) {
        try {
            val associatedItems = itemDao.getItemsByCategory(categoryId).first()

            associatedItems.forEach { item ->
                itemDao.deleteItem(item)
                firebaseService.deleteItem(item.id)
            }

            categoryDao.deleteCategory(categoryId)
            firebaseService.deleteCategory(categoryId)

        } catch (e: Exception) {
            android.util.Log.e("DELETE_ERROR", "Cascade delete failed: ${e.message}")
        }
    }

    suspend fun syncCategories() {
        try {
            val snapshot = firebaseService.helper.fetchCollectionWithIds("category")
            snapshot.forEach { (docId, data) ->
                val name = data["categoryName"] as? String ?: ""
                val createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()

                if (name.isNotEmpty()) {
                    val entity = CategoryEntity(
                        id = docId,
                        name = name,
                        createdAt = createdAt
                    )
                    categoryDao.insertCategory(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Error: ${e.message}")
        }
    }

    /**
     * FIX: Specified <Map<String, Any>> to resolve type inference.
     * Added handling for deletedIds to ensure categories removed from Firebase disappear from the app.
     */
    fun startRealTimeSync() {
        firebaseService.listenToCollection<Map<String, Any>>("category") { dataList, idList, deletedIds ->
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {

                // 1. Handle Deletions (Critical for Real-time Sync)
                deletedIds.forEach { id ->
                    categoryDao.deleteCategory(id)
                }

                // 2. Handle Adds/Updates
                dataList.forEachIndexed { index, data ->
                    val entity = CategoryEntity(
                        id = idList[index],
                        name = data["categoryName"] as? String ?: "",
                        createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
                    )
                    categoryDao.insertCategory(entity)
                }
            }
        }
    }
}