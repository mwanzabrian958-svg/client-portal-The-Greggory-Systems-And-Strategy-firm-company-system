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
        private const val CLASS_NAME = "com.greggory.portal.ui.test.TestImageProvider"
        private const val TEST_IMAGE = 1

        val CONTENT_URI: Uri = Uri.parse("content://$CLASS_NAME/test_image")
    }

    private lateinit var assetManager: AssetManager
    private lateinit var cacheDir: File
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(CLASS_NAME, "test_image", TEST_IMAGE)
    }

    override fun onCreate(): Boolean {
        assetManager = context?.getAssets() ?: return false
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

    override fun getType(uri: Uri): String? = getType(uri)

    override fun getUriPermission(uri: Uri, modeFlags: Int): String? = null

    override fun getUriPermissions(uri: Uri, flags: Int, userId: Int): MutableList<String> = mutableListOf()

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
        val accessMode = when {
            mode.contains("rw") && mode.contains("wt") -> OsConstants.O_RDWR or OsConstants.O_CREAT or OsConstants.O_TRUNC
            mode.contains("w") -> OsConstants.O_RDWR or OsConstants.O_CREAT or OsConstants.O_TRUNC
            mode.contains("r") -> OsConstants.O_RDONLY
            else -> OsConstants.O_RDONLY
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
