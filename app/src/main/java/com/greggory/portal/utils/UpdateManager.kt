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
    val features: List<String>,
    // SECURITY (M5): expected SHA-256 of the APK, lowercase hex. The download
    // is REJECTED when this is missing or does not match — an unsigned/foreign
    // APK can no longer be smuggled through the update channel.
    val sha256: String?
)

object UpdateManager {
    private const val UPDATE_URL = "https://raw.githubusercontent.com/mwanzabrian958-svg/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/main/docs/version.json"
    // SECURITY (M5): allowlist for APK hosts. An update URL pointing anywhere
    // else (evil mirror, IP literal, non-HTTPS) is rejected before download.
    private val ALLOWED_APK_HOSTS = setOf(
        "raw.githubusercontent.com",
        "github.com",
        "objects.githubusercontent.com"
    )
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
            // SECURITY (M5): resolve the expected checksum from the manifest we
            // ALREADY fetched — never trust a parameter alone.
            val manifest = checkForUpdates() ?: run {
                android.util.Log.e("UpdateManager", "Refusing update: manifest unavailable")
                return@withContext false
            }
            if (manifest.url != updateUrl) {
                android.util.Log.e("UpdateManager", "Refusing update: URL does not match manifest")
                return@withContext false
            }
            verifyUpdateUrl(updateUrl) ?: return@withContext false
            val expectedSha = manifest.sha256?.lowercase()
            if (expectedSha.isNullOrBlank() || !expectedSha.matches(Regex("[0-9a-f]{64}"))) {
                android.util.Log.e("UpdateManager", "Refusing update: manifest has no valid sha256")
                return@withContext false
            }

            val request = Request.Builder().url(updateUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val apkFile = File(context.cacheDir, "update.apk")
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            val totalBytes = body.contentLength()
            val digest = java.security.MessageDigest.getInstance("SHA-256")

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes)
                        }
                    }
                }
            }

            // SECURITY (M5): checksum BEFORE install. A mismatch means the file
            // was tampered with (or the manifest lies) — delete and abort.
            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualSha != expectedSha) {
                android.util.Log.e("UpdateManager", "Refusing update: SHA-256 mismatch, deleting file")
                apkFile.delete()
                return@withContext false
            }

            installApk(context, apkFile)
            true
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Download failed", e)
            false
        }
    }

    /** Returns the URL when it passes the allowlist, else logs and returns null. */
    internal fun verifyUpdateUrl(updateUrl: String): String? {
        return try {
            val parsed = android.net.Uri.parse(updateUrl)
            val host = (parsed.host ?: "").lowercase()
            if (parsed.scheme != "https") {
                android.util.Log.e("UpdateManager", "Refusing update: non-HTTPS URL")
                return null
            }
            if (host !in ALLOWED_APK_HOSTS) {
                android.util.Log.e("UpdateManager", "Refusing update: host not allowlisted: $host")
                return null
            }
            updateUrl
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Refusing update: malformed URL", e)
            null
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
