package com.greggory.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
fun ProjectListScreen(
    projects: List<Project>, 
    onViewPdf: (String, String) -> Unit,
    onViewDetails: (Project) -> Unit = {}
) {
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
                    onViewProposal = { selectedProjectForProposal = project },
                    onClick = { onViewDetails(project) }
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
fun ProjectCard(
    project: Project, 
    onViewRoadmap: () -> Unit, 
    onViewProposal: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
        title = { 
            Column {
                Text("${project.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Strategic Execution Roadmap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentStep = when {
                    project.progress < 20 -> 0
                    project.progress < 40 -> 1
                    project.progress < 70 -> 2
                    project.progress < 90 -> 3
                    else -> 4
                }

                RoadmapStep("Discovery & Audit", "Analysis of existing infrastructure and pain points.", currentStep >= 0, currentStep == 0)
                RoadmapStep("Strategy Development", "Crafting the unique solution architecture.", currentStep >= 1, currentStep == 1)
                RoadmapStep("Implementation Phase", "Active engineering and systems deployment.", currentStep >= 2, currentStep == 2)
                RoadmapStep("Review & Testing", "Quality assurance and performance optimization.", currentStep >= 3, currentStep == 3)
                RoadmapStep("Final Handover", "Training, documentation, and go-live.", currentStep >= 4, currentStep == 4)
                
                if (project.progress > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Progress: ${project.progress}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("GOT IT") }
        }
    )
}

@Composable
fun RoadmapStep(title: String, description: String, isDone: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) MaterialTheme.colorScheme.primary 
                    else if (isCurrent) MaterialTheme.colorScheme.secondary 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            } else if (isCurrent) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            Text(
                text = description, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (isCurrent) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("ACTIVE PHASE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                }
            }
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
