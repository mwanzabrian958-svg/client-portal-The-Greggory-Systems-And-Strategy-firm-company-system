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
        Log.d("FCM", "Message payload received: ${message.from}")
        
        // Show native notification banner on user device screen first
        message.notification?.let {
            NotificationHelper.showNotification(applicationContext, it.title, it.body)
        }

        // Process data payload to capture downloadable transaction receipt items into Room cache
        val dataMap = message.data
        if (dataMap.containsKey("type") && dataMap["type"] == "receipt") {
            val msgId = dataMap["id"] ?: System.currentTimeMillis().toString()
            val sender = dataMap["sender"] ?: "Accounting Office"
            val subject = dataMap["subject"] ?: "M-Pesa Payment Receipt Confirmed"
            val bodyText = dataMap["message"] ?: "Your company payment has been successfully recorded."
            val downloadUrl = dataMap["attachment_url"] // Target download link

            scope.launch {
                try {
                    val database = com.greggory.portal.data.local.AppDatabase.getDatabase(applicationContext)
                    database.messageCacheDao().insertMessage(
                        com.greggory.portal.data.local.MessageCacheEntity(
                            id = msgId,
                            sender = sender,
                            subject = subject,
                            message = bodyText,
                            time = "Just Now",
                            unread = true,
                            feedback = false,
                            attachmentUrl = downloadUrl
                        )
                    )
                    Log.i("FCM", "Downloadable payment receipt auto-cached into Room table messages successfully. Target: $msgId")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to cache automated incoming receipt metadata down to local database", e)
                }
            }
        }
    }
}
