package com.greggory.portal.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.local.PreferencesManager

object FileDownloadHelper {
    
    fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        description: String = "Downloading file from The Greggory Firm"
    ): Boolean {
        return try {
            val prefs = PreferencesManager.getInstance(context)
            val token = prefs.getToken()
            val userId = prefs.getUserId()
            
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription(description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .addRequestHeader("Authorization", "Bearer $token")
                .addRequestHeader("X-Greggory-Client-ID", userId.toString())
                .addRequestHeader("X-Routing-Policy", "set-in-stone-v1")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getReportUrl(reportId: Int): String = "${RetrofitClient.BASE_URL}api/users/my-reports/$reportId/download"
    
    fun getInvoiceUrl(invoiceId: Int): String = "${RetrofitClient.BASE_URL}api/users/my-invoices/$invoiceId/pdf"
}
