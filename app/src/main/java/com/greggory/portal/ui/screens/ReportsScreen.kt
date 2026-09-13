package com.greggory.portal.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun ReportsScreen(reports: List<Report>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadingReportId by remember { mutableStateOf<Int?>(null) }

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports available yet", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Project Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(reports) { report ->
                ReportCard(
                    report = report,
                    isDownloading = downloadingReportId == report.id,
                    onDownload = {
                        downloadingReportId = report.id
                        scope.launch {
                            val success = downloadReportFile(context, report)
                            downloadingReportId = null
                            if (success) {
                                Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to download report", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

private suspend fun downloadReportFile(context: Context, report: Report): Boolean = withContext(Dispatchers.IO) {
    try {
        val fileName = "${report.title.replace(" ", "_")}.pdf"
        val request = DownloadManager.Request(Uri.parse("${RetrofitClient.BASE_URL}api/users/my-reports/${report.id}/download"))
            .setTitle(report.title)
            .setDescription("Downloading report from The Greggory Firm")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .addRequestHeader("Authorization", "Bearer ${com.greggory.portal.data.local.PreferencesManager(context).getToken()}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ReportCard(report: Report, isDownloading: Boolean, onDownload: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = report.project_name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = report.report_date,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
            }
        }
    }
}
