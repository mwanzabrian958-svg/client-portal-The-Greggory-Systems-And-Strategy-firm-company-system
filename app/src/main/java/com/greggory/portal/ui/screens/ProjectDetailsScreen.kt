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
fun ContactExpertDialog(expert: TeamMember, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val phoneNumber = expert.phone ?: "254711525854" // Default if null
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    LaunchedEffect(phoneNumber) {
        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(phoneNumber))
        android.widget.Toast.makeText(context, "Phone number copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContactSupport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Strategy Expert")
            }
        },
        text = {
            Column {
                Text(
                    text = "How would you like to connect with ${expert.name} regarding your project strategy?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber"))
                            context.startActivity(intent)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF25D366).copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Open WhatsApp Chat", fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
                            Text("Fast response for strategy updates.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                            context.startActivity(intent)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Start Chat in SMS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Send a direct text message.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                            context.startActivity(intent)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Call Expert Directly", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Instant voice consultation.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoadmapStep(title: String, description: String, isDone: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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

@Composable
fun TeamMemberCard(member: TeamMember, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "${RetrofitClient.BASE_URL}api/users/profile-photo/${member.id}",
                    contentDescription = member.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null // Falls back to the Icon below if loading fails
                )
                // Fallback icon if image is not available
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(member.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(member.duties, style = MaterialTheme.typography.bodySmall)
            }
            if (!member.email.isNullOrEmpty()) {
                IconButton(onClick = { /* Handle Email */ }) {
                    Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
