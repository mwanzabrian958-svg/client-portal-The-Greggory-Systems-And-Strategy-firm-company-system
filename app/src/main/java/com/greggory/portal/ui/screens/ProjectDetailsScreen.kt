package com.greggory.portal.ui.screens
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.greggory.portal.data.api.Project
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.TeamMember
import com.greggory.portal.ui.components.*

data class DevelopmentMedia(
    val id: Int,
    val type: String, // "image" or "video"
    val url: String,
    val caption: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    project: Project?,
    team: List<TeamMember>,
    onBack: () -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Project not found")
        }
        return
    }

    var selectedExpert by remember { mutableStateOf<TeamMember?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Status Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Current Status", style = MaterialTheme.typography.labelSmall)
                        ProjectStatusBadge(project.status)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Progress", style = MaterialTheme.typography.labelSmall)
                        Text("${project.progress}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Execution Roadmap", Icons.Default.Timeline)
            
            val currentStep = when {
                project.progress < 20 -> 0
                project.progress < 40 -> 1
                project.progress < 70 -> 2
                project.progress < 90 -> 3
                else -> 4
            }

            RoadmapStep("Discovery & Audit", "Analysis of existing infrastructure.", currentStep >= 0, currentStep == 0)
            RoadmapStep("Strategy Development", "Crafting solution architecture.", currentStep >= 1, currentStep == 1)
            RoadmapStep("Implementation Phase", "Active engineering deployment.", currentStep >= 2, currentStep == 2)
            RoadmapStep("Review & Testing", "Quality assurance optimization.", currentStep >= 3, currentStep == 3)
            RoadmapStep("Final Handover", "Training and documentation.", currentStep >= 4, currentStep == 4)

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Development Gallery", Icons.Default.Collections)
            Text(
                text = "Visual updates and milestones captured during development.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Mock Media Data (Syncing with strategy backend)
            val mockMedia = listOf(
                DevelopmentMedia(1, "image", "https://images.unsplash.com/photo-1551434678-e076c223a692?w=500&q=80", "Initial systems architecture draft", "2024-09-10"),
                DevelopmentMedia(2, "image", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500&q=80", "Core API implementation progress", "2024-09-12"),
                DevelopmentMedia(3, "video", "https://images.unsplash.com/photo-1587620962725-abab7fe55159?w=500&q=80", "UI Prototype Walkthrough", "2024-09-15"),
                DevelopmentMedia(4, "image", "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500&q=80", "Dashboard layout finalization", "2024-09-18")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mockMedia) { media ->
                    MediaCard(media)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Project Strategy Team", Icons.Default.Groups)
            Text(
                text = "The elite professionals dedicated to your project's success.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val projectTeam = team.filter { it.projectName == project.name }
            if (projectTeam.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Your strategy lead will assign dedicated experts shortly.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                projectTeam.forEach { member ->
                    TeamMemberCard(member, onClick = { selectedExpert = member })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (selectedExpert != null) {
            ContactExpertDialog(
                expert = selectedExpert!!,
                onDismiss = { selectedExpert = null }
            )
        }
    }
}

@Composable
fun MediaCard(media: DevelopmentMedia) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(250.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                AsyncImage(
                    model = media.url,
                    contentDescription = media.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (media.type == "video") {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(media.caption, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(modifier = Modifier.weight(1f))
                Text(media.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
