package com.example.pos_system.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pos_system.POSSystem

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val appModule = (context.applicationContext as POSSystem).appModule
    private val salesRepo = appModule.salesRepository

    override suspend fun doWork(): Result {
        return try {
            val offlineSales = salesRepo.getUnsyncedSales()

            for (sale in offlineSales) {
                // Try to push each offline sale to Firebase
                salesRepo.pushToFirebase(sale)
                // If successful, mark it so we don't sync it again
                salesRepo.markAsSynced(sale.id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry() // Tells Android to try again when connection is better
        }
    }
}