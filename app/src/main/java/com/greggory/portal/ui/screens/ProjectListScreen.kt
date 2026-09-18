package com.greggory.portal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Project
import com.greggory.portal.utils.FileDownloadHelper

@Composable
fun ProjectListScreen(projects: List<Project>, onViewPdf: (String, String) -> Unit) {
    var selectedProjectForRoadmap by remember { mutableStateOf<Project?>(null) }
    var selectedProjectForProposal by remember { mutableStateOf<Project?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Active Projects",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    onViewRoadmap = { selectedProjectForRoadmap = project },
                    onViewProposal = { selectedProjectForProposal = project }
                )
            }
        }

        if (selectedProjectForRoadmap != null) {
            ProjectRoadmapDialog(
                project = selectedProjectForRoadmap!!,
                onDismiss = { selectedProjectForRoadmap = null }
            )
        }

        if (selectedProjectForProposal != null) {
            ProjectProposalDialog(
                project = selectedProjectForProposal!!,
                onDismiss = { selectedProjectForProposal = null },
                onViewPdf = onViewPdf
            )
        }
    }
}

@Composable
fun ProjectCard(project: Project, onViewRoadmap: () -> Unit, onViewProposal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ProjectStatusBadge(project.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${project.progress}% Complete",
                style = MaterialTheme.typography.labelSmall
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Manager: ${project.manager ?: "TBD"}", style = MaterialTheme.typography.labelSmall)
                if (!project.deadline.isNullOrEmpty()) {
                    Text(text = "Due: ${project.deadline}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewProposal,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PROPOSAL", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onViewRoadmap,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ROADMAP", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ProjectProposalDialog(project: Project, onDismiss: () -> Unit, onViewPdf: (String, String) -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registration Agreement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "View or download the Client Registration Agreement for '${project.name}'.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Electric Copy (PDF)", fontWeight = FontWeight.Bold)
                        Text("For instant viewing and review.", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    onViewPdf(FileDownloadHelper.getReportUrl(-1), "Agreement: ${project.name}")
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("VIEW")
                            }
                            OutlinedIconButton(onClick = {
                                FileDownloadHelper.downloadFile(
                                    context = context,
                                    url = FileDownloadHelper.getReportUrl(-1),
                                    fileName = "GSSF_Agreement_${project.name}.pdf"
                                )
                            }) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Download")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Editable Copy (Word)", fontWeight = FontWeight.Bold)
                        Text("Download to fill in manually for hard copy.", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                FileDownloadHelper.downloadFile(
                                    context = context,
                                    url = FileDownloadHelper.getReportUrl(-2),
                                    fileName = "GSSF_Agreement_${project.name}.docx"
                                )
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DOWNLOAD DOCX")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ProjectRoadmapDialog(project: Project, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${project.name} Roadmap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RoadmapStep("Discovery & Audit", "Completed", true)
                RoadmapStep("Strategy Development", "Completed", true)
                RoadmapStep("Implementation Phase", "In Progress", false)
                RoadmapStep("Review & Testing", "Pending", false)
                RoadmapStep("Final Handover", "Pending", false)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun RoadmapStep(title: String, status: String, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isDone, onClick = null, enabled = false)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold)
            Text(status, style = MaterialTheme.typography.labelSmall, color = if (isDone) Color.Gray else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProjectStatusBadge(status: String) {
    Surface(
        color = when(status.lowercase()) {
            "active" -> Color(0xFF2A9D8F)
            "completed" -> Color(0xFF415A77)
            else -> Color(0xFFE0C097)
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
