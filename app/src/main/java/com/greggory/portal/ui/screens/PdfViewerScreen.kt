package com.greggory.portal.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                    val prefs = PreferencesManager(context)
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
    val context = LocalContext.current
    val renderer = remember(file) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(fileDescriptor)
    }
    
    val pageCount = renderer.pageCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((0 until pageCount).toList()) { index ->
            PdfPageItem(renderer, index)
        }
    }
    
    DisposableEffect(renderer) {
        onDispose {
            renderer.close()
        }
    }
}

@Composable
fun PdfPageItem(renderer: PdfRenderer, index: Int) {
    val bitmap = remember(renderer, index) {
        val page = renderer.openPage(index)
        // Adjust width to screen and maintain aspect ratio
        // For simplicity, we use a fixed high-quality width and let Image scale it
        val width = 1200
        val height = (width * page.height / page.width)
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        b
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}
