package com.greggory.portal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.utils.FileDownloadHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun ReportsScreen(reports: List<Report>, onViewPdf: (String, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadingReportId by remember { mutableStateOf<Int?>(null) }
    var isUploadingAsset by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingAsset = true
            scope.launch {
                try {
                    val file = getFileFromUri(context, it)
                    val requestFile = file.asRequestBody(context.contentResolver.getType(it)?.toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("asset", file.name, requestFile)
                    
                    val response = RetrofitClient.instance.uploadAsset(body)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "Asset Securely Received: Strategy pipeline updated.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Upload failed: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Security Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingAsset = false
                }
            }
        }
    }

    // Mock Pinned Legal Documents (Until synced to DB)
    val legalDocs = listOf(
        Report(
            id = -1,
            title = "Client Registration Agreement (PDF)",
            summary = "Standard firm agreement for all service engagements.",
            file_type = "application/pdf",
            file_size = 245000,
            report_date = "2024-09-01",
            project_name = "Legal & Compliance",
            client_id = 0
        ),
        Report(
            id = -2,
            title = "Client Registration Agreement (Word)",
            summary = "Editable version for contract review.",
            file_type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            file_size = 180000,
            report_date = "2024-09-01",
            project_name = "Legal & Compliance",
            client_id = 0
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Legal & Contracts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(legalDocs) { doc ->
            ReportCard(
                report = doc,
                isDownloading = downloadingReportId == doc.id,
                onDownload = {
                    if (doc.file_type.contains("pdf")) {
                        onViewPdf(FileDownloadHelper.getReportUrl(doc.id), doc.title)
                    } else {
                        Toast.makeText(context, "Pinned Legal Doc: Syncing with cloud...", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Active Proposals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Approved and pending proposals for new and existing projects.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Mock Proposals (To show variety of jobs)
        val mockProposals = listOf(
            Report(-10, "Proposal: Firm Networking Overhaul", "Detailed setup for Cisco failover networking.", "pdf", 1200000, "2024-09-12", "Networking", 0),
            Report(-11, "Proposal: Mobile Client Portal V2", "Expansion of current Android app features.", "pdf", 950000, "2024-09-14", "Mobile Development", 0)
        )

        items(mockProposals) { proposal ->
            ReportCard(
                report = proposal,
                isDownloading = false,
                onDownload = { 
                    if (proposal.file_type.contains("pdf")) {
                        onViewPdf(FileDownloadHelper.getReportUrl(proposal.id), proposal.title)
                    } else {
                        Toast.makeText(context, "Fetching proposal...", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Project Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (reports.isEmpty()) {
            item {
                Text("No project reports available yet", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(reports) { report ->
                ReportCard(
                    report = report,
                    isDownloading = downloadingReportId == report.id,
                    onDownload = {
                        if (report.file_type.contains("pdf")) {
                            onViewPdf(FileDownloadHelper.getReportUrl(report.id), report.title)
                        } else {
                            downloadingReportId = report.id
                            scope.launch {
                                val extension = if (report.file_type.contains("word")) "docx" else "pdf"
                                val fileName = "${report.title.replace(" ", "_").replace("(", "").replace(")", "")}.$extension"
                                val success = FileDownloadHelper.downloadFile(
                                    context = context,
                                    url = FileDownloadHelper.getReportUrl(report.id),
                                    fileName = fileName
                                )
                                downloadingReportId = null
                                if (success) {
                                    Toast.makeText(context, "Report saved to Downloads. You can open and print it from your file manager.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to download report", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Client Asset Drop",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Securely upload brand assets, briefs, or project requirements directly to your consulting team.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isUploadingAsset) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Securing & Uploading...", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { pickerLauncher.launch("*/*") }) {
                            Text("SELECT FILES TO UPLOAD")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    } ?: "upload_file"
    
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }
    return file
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.report_date,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${report.file_size / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
