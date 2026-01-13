package com.example.pos_system.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()

    // Save any object (Item, Category, or Sale)
    suspend fun saveData(collection: String, data: Any): Boolean {
        return try {
            db.collection(collection).add(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Fetch all documents from a collection as a List of Maps
    suspend fun fetchCollection(collection: String): List<Map<String, Any>> {
        return try {
            android.util.Log.d("SYNC_DEBUG", "Fetching collection: $collection")
            val snapshot = db.collection(collection).get().await()
            android.util.Log.d("SYNC_DEBUG", "Success! Found ${snapshot.size()} docs in $collection")
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Firebase Fetch Error: ${e.message}")
            emptyList()
        }
    }
    suspend fun fetchCollectionWithIds(collection: String): Map<String, Map<String, Any>> {
        return try {
            val snapshot = db.collection(collection).get().await()
            snapshot.documents.associate { it.id to (it.data ?: emptyMap()) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun setDataWithId(collection: String, documentId: String, data: Any): Boolean {
        return try {
            db.collection(collection).document(documentId).set(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
