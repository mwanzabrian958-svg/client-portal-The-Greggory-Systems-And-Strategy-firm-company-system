package com.greggory.portal.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.greggory.portal.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val features: List<String>
)

object UpdateManager {
    private const val UPDATE_URL = "https://raw.githubusercontent.com/mwanzabrian958-svg/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/main/docs/version.json"
    private val client = OkHttpClient()

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(UPDATE_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body
                val json = body?.string()
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                    return@withContext updateInfo
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Check failed", e)
        }
        null
    }

    suspend fun downloadAndInstall(context: Context, updateUrl: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(updateUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val apkFile = File(context.cacheDir, "update.apk")
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            val totalBytes = body.contentLength()
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes)
                        }
                    }
                }
            }

            installApk(context, apkFile)
            true
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Download failed", e)
            false
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
