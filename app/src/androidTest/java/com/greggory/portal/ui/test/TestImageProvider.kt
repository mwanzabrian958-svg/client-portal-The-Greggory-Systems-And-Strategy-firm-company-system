package com.greggory.portal.ui.test

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetManager
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class TestImageProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.greggory.portal.testimageprovider"
        private const val TEST_IMAGE = 1

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/test_image")
    }

    private lateinit var assetManager: AssetManager
    private lateinit var cacheDir: File
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "test_image", TEST_IMAGE)
    }

    override fun onCreate(): Boolean {
        assetManager = context?.assets ?: return false
        cacheDir = File(context?.cacheDir, "test_images").apply {
            if (!exists()) mkdirs()
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            TEST_IMAGE -> "image/jpeg"
            else -> null
        }
    }


    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (uriMatcher.match(uri) != TEST_IMAGE) return null
        val file = getOrCreateTestImageFile()
        val accessMode = when (mode) {
            "r" -> ParcelFileDescriptor.MODE_READ_ONLY
            "w", "wt" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
            "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
            else -> ParcelFileDescriptor.MODE_READ_ONLY
        }
        return ParcelFileDescriptor.open(file, accessMode)
    }

    private fun getOrCreateTestImageFile(): File {
        val targetFile = File(cacheDir, "test_grid.jpeg")
        if (!targetFile.exists()) {
            copyAsset("test_grid.jpeg", targetFile)
        }
        return targetFile
    }

    private fun copyAsset(assetName: String, targetFile: File) {
        assetManager.open(assetName).use { inputStream: InputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
