package com.example.pos_system.data.local.database.dao

import androidx.room.*
import com.example.pos_system.data.local.database.entity.SalesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getSalesPeriod(startTime: Long, endTime: Long): Flow<List<SalesEntity>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SalesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Use REPLACE to handle updates
    suspend fun insertSale(sale: SalesEntity)

    @Delete
    suspend fun deleteSale(sale: SalesEntity)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSaleById(saleId: String)

    // Used by SyncWorker to find sales that failed to upload
    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SalesEntity>

    // Used to mark a sale as successfully uploaded
    @Query("UPDATE sales SET isSynced = 1 WHERE id = :saleId")
    suspend fun markAsSynced(saleId: String)
}