package com.example.pos_system.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    // Called when a new notification arrives while app is in foreground
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("POS_NOTIFY", "Message received: ${remoteMessage.notification?.body}")
    }

    // Called when the device gets a unique ID from Firebase
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("POS_NOTIFY", "New device token: $token")
    }
}
