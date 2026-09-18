package com.greggory.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.greggory.portal.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(url: String, title: String, onBack: () -> Unit) {
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(url) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val prefs = com.greggory.portal.data.local.PreferencesManager.getInstance(context)
                    val token = prefs.getToken()
                    val userId = prefs.getUserId()
                    
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("X-Greggory-Client-ID", userId.toString())
                        .addHeader("X-Routing-Policy", "set-in-stone-v1")
                        .build()
                    
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val file = File(context.cacheDir, "temp_view.pdf")
                        val outputStream = FileOutputStream(file)
                        response.body?.byteStream()?.copyTo(outputStream)
                        outputStream.close()
                        pdfFile = file
                    } else {
                        error = "Failed to load PDF: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                error = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.DarkGray)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (pdfFile != null) {
                PdfViewContainer(file = pdfFile!!)
            }
        }
    }
}

@Composable
fun PdfViewContainer(file: File) {
    AndroidView(
        factory = { context ->
            PDFView(context, null).apply {
                fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .enableAnnotationRendering(false)
                    .password(null)
                    .scrollHandle(null)
                    .enableAntialiasing(true)
                    .spacing(10)
                    .load()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
