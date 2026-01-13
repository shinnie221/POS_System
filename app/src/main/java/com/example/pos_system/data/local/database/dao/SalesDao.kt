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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSale(sale: SalesEntity)

    @Delete
    suspend fun deleteSale(sale: SalesEntity)
}