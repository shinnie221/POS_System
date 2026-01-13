package com.example.pos_system.data.local.database.dao

import androidx.room.*
import com.example.pos_system.data.local.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // Get all categories to display in your POS list
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    // Add a new category (like "Drinks" or "Snacks")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Update a category name
    @Update
    suspend fun updateCategory(category: CategoryEntity)

    // Delete a category
    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)
}
