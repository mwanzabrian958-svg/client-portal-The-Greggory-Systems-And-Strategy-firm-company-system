package com.greggory.portal.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.greggory.portal.data.api.PushTokenRequest
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
        
        // Persist token locally
        val prefs = com.greggory.portal.data.local.PreferencesManager.getInstance(applicationContext)
        prefs.saveFcmToken(token)
        
        // Upload to server
        scope.launch {
            try {
                RetrofitClient.instance.updatePushToken(PushTokenRequest(token))
            } catch (e: Exception) {
                Log.e("FCM", "Failed to upload token", e)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received from: ${message.from}")
        
        // Show notification even when app is in foreground
        message.notification?.let {
            NotificationHelper.showNotification(applicationContext, it.title, it.body)
        }
    }
}
