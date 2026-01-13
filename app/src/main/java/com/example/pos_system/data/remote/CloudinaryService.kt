package com.example.pos_system.data.remote

import android.content.Context
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CloudinaryService {

    // Call this once in your Application class or MainActivity
    fun init(context: Context) {
        val config = hashMapOf(
            "cloud_name" to "your_cloud_name",
            "api_key" to "your_api_key",
            "api_secret" to "your_api_secret"
        )
        try {
            MediaManager.init(context, config)
        } catch (e: Exception) { /* Already initialized */ }
    }

    fun uploadItemImage(filePath: String, onComplete: (String?) -> Unit) {
        MediaManager.get().upload(filePath)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    onComplete(resultData?.get("secure_url") as String)
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onComplete(null)
                }
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }
}
