package com.example.pos_system.data.repository

import com.example.pos_system.data.local.database.dao.CategoryDao
import com.example.pos_system.data.local.database.dao.ItemDao
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore // Add this import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val firebaseService: FirebaseService
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    /**
     * Uses a Firebase generated ID for both Room and Firestore.
     */
    suspend fun addCategory(name: String) {
        // 1. Get a new ID from Firebase without creating the document yet
        val db = FirebaseFirestore.getInstance()
        val firebaseId = db.collection("category").document().id

        // 2. Save locally using that specific Firebase ID (e.g., 0oVAzXR...)
        val newCategory = CategoryEntity(id = firebaseId, name = name)
        categoryDao.insertCategory(newCategory)

        // 3. Sync to Firebase using the SAME ID
        firebaseService.addCategory(id = firebaseId, name = name)
    }

    suspend fun deleteCategory(categoryId: String) {
        try {
            val associatedItems = itemDao.getItemsByCategory(categoryId).first()

            associatedItems.forEach { item ->
                itemDao.deleteItem(item)
                firebaseService.deleteItem(item.id)
            }

            // 3. Delete the category itself locally
            categoryDao.deleteCategory(categoryId)

            // 4. Delete the category from Firebase
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
                if (name.isNotEmpty()) {
                    // Uses docId (Firebase ID) directly as the primary key
                    val entity = CategoryEntity(id = docId, name = name)
                    categoryDao.insertCategory(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Error: ${e.message}")
        }
    }
}