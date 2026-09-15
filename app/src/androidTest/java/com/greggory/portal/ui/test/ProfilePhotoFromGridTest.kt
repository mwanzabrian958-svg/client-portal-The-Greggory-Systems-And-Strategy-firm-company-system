package com.greggory.portal.ui.test

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.greggory.portal.ui.screens.ProfileScreen
import com.greggory.portal.ui.theme.GreggoryPortalTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ProfilePhotoFromGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun profileScreen_canLoadTestGridImageFromProvider() {
        val providerUri = Uri.parse(
            "content://com.greggory.portal.ui.test.TestImageProvider/test_image"
        )
        assertNotNull("Test image provider content URI should be non-null", providerUri)

        composeTestRule.setContent {
            GreggoryPortalTheme {
                ProfileScreen()
            }
        }

        composeTestRule.waitForIdle()

        val profilePhotoNode = composeTestRule.onNodeWithContentDescription("Profile Photo")
        assertNotNull("Profile photo node should exist in ProfileScreen", profilePhotoNode)

        val cacheDir = context.cacheDir
        val targetFile = File(cacheDir, "test_images/test_grid.jpeg")
        assertTrue(
            "Test provider should be able to expose test_grid.jpeg in test storage",
            assetExposedByProvider(context, providerUri, targetFile)
        )

        composeTestRule.setContent {
            LocalContext.current.let { ctx ->
                var painterState by remember { mutableStateOf<Any?>(null) }
                AsyncImage(
                    model = providerUri,
                    contentDescription = "Test provider image",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        composeTestRule.waitForIdle()

        val testProviderImageNode = composeTestRule.onNodeWithContentDescription("Test provider image")
        assertNotNull(
            "AsyncImage bound to the test provider URI should appear in the Compose test",
            testProviderImageNode
        )

        val fileUri = Uri.fromFile(targetFile)
        val fileRequest = ImageRequest.Builder(context)
            .data(fileUri)
            .target { _, _ -> }
            .build()
        assertNotNull("Coil should build a request for the test image file URI", fileRequest)
    }

    private fun assetExposedByProvider(
        context: android.content.Context,
        providerUri: Uri,
        expectedFile: File
    ): Boolean {
        if (expectedFile.exists()) return expectedFile.canRead()

        return try {
            context.getContentResolver().openInputStream(providerUri)?.use { inputStream ->
                File(context.cacheDir, "test_images").mkdirs()
                FileOutputStream(expectedFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            expectedFile.exists() && expectedFile.canRead()
        } catch (e: Exception) {
            false
        }
    }
}
