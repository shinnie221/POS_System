package com.example.pos_system.data.local.database.dao

import androidx.room.*
import com.example.pos_system.data.local.database.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE categoryId = :catId ORDER BY createdAt ASC")
    fun getItemsByCategory(catId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY createdAt ASC")
    fun getAllItems(): Flow<List<ItemEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    @Delete
    suspend fun deleteItem(item: ItemEntity)
}