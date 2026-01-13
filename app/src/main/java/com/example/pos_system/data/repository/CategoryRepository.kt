package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.CategoryDao
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val firebaseService: FirebaseService
) {
    // Get real-time updates for the UI from local Room database
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    /**
     * Adds a category both locally and to Firebase.
     * We generate a UUID for the local ID which will be overwritten by
     * the Firebase Sync later, or we use the Firebase document ID.
     */
    suspend fun addCategory(name: String) {
        // 1. We generate a temporary ID to save locally first
        val tempId = UUID.randomUUID().toString()
        val newCategory = CategoryEntity(id = tempId, name = name)
        categoryDao.insertCategory(newCategory)

        // 2. Sync to Firebase - Firebase will generate its own auto-id
        // We pass the name to addCategory.
        firebaseService.addCategory(id = tempId, name = name)
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategory(categoryId)
    }

    /**
     * Syncs categories from Firebase 'category' collection to Room.
     * It uses the Firebase Document ID as the Room Primary Key.
     */
    suspend fun syncCategories() {
        try {
            // Fetch the Map of <DocumentID, DataMap>
            val snapshot = firebaseService.helper.fetchCollectionWithIds("category")

            android.util.Log.d("SYNC_DEBUG", "Syncing ${snapshot.size} categories from Firebase")

            snapshot.forEach { (docId, data) ->
                val name = data["categoryName"] as? String ?: ""

                if (name.isNotEmpty()) {
                    // We use the ACTUAL Firebase Document ID as the ID in Room.
                    // This ensures that items with categoryId = "docId" will match perfectly.
                    val entity = CategoryEntity(
                        id = docId,
                        name = name
                    )
                    categoryDao.insertCategory(entity)
                    android.util.Log.d("SYNC_DEBUG", "Synced Category: $name with ID: $docId")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Error syncing categories: ${e.message}")
        }
    }
}
