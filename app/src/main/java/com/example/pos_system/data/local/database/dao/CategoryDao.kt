package com.example.pos_system.data.local.database.dao

import androidx.room.*
import com.example.pos_system.data.local.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    //Add category follow created time
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
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
