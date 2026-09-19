package com.greggory.portal.utils

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.greggory.portal.data.api.PaymentReportRequest
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class MpesaNotificationService : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Intercept both standard SMS apps and direct network carrier/Safaricom notification payloads
        if (packageName.contains("sms") || packageName.contains("messaging") || packageName.contains("safaricom") || packageName.contains("mpesa")) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val fullBody = "$title $text"

            // Autonomous Validation Check: Detect valid M-Pesa transaction patterns (e.g., "Ksh1,500 paid to THE GREGGORY SYSTEMS")
            if (fullBody.contains("Confirmed") && (fullBody.contains("paid to") || fullBody.contains("sent to")) && 
                (fullBody.contains("Greggory") || fullBody.contains("07115525854"))) {
                
                Log.d("MpesaNotification", "M-Pesa company transfer text intercepted successfully: $fullBody")
                extractAndReportTransaction(fullBody)
            }
        }
    }

    private fun extractAndReportTransaction(message: String) {
        scope.launch {
            try {
                // Heuristic regex to locate any 10-character alphanumeric transaction code (e.g., QJG87HDKS9)
                val pattern = Pattern.compile("\\b[A-Z0-9]{10}\\b")
                val matcher = pattern.matcher(message)
                val txnCode = if (matcher.find()) matcher.group() else "AUTOMATED_INTERCEPT"

                // Dispatch the intercepted message string down to the central accounting endpoint for auto-reconciliation
                val response = RetrofitClient.instance.reportManualPayment(
                    PaymentReportRequest(
                        invoiceId = txnCode, // Pass token code context so back-end knows it's a structural receipt broadcast
                        mpesaMessage = message.trim()
                    )
                )
                if (response.isSuccessful) {
                    Log.i("MpesaNotification", "Intercepted company payment reported and reconciled matching txn code: $txnCode")
                }
            } catch (e: Exception) {
                Log.e("MpesaNotification", "Failed to dispatch intercepted payment message to server pipeline", e)
            }
        }
    }
}
