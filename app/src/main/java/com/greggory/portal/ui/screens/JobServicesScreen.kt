package com.greggory.portal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.local.PreferencesManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class ServiceItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

@Composable
fun JobServicesScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    val services = listOf(
        ServiceItem(
            "Website Development",
            "Responsive, high-performance websites built with modern frameworks to establish your firm's online presence.",
            Icons.Default.Language,
            "Digital Presence"
        ),
        ServiceItem(
            "Web Applications",
            "Complex cloud-based applications (SAAS, Portals, ERPs) designed for scalable business logic.",
            Icons.Default.Web,
            "Software"
        ),
        ServiceItem(
            "Mobile App Development",
            "Native Android (Compose) and iOS development tailored for seamless client engagement and field operations.",
            Icons.Default.AppShortcut,
            "Software"
        ),
        ServiceItem(
            "Desktop Applications",
            "Windows, Mac, and Linux desktop software for localized processing and deep system integration.",
            Icons.Default.DesktopWindows,
            "Software"
        ),
        ServiceItem(
            "Networking & Infrastructure",
            "Setup of secure firm-wide networks, failover systems, and server infrastructure (On-premise & Cloud).",
            Icons.Default.Router,
            "Infrastructure"
        ),
        ServiceItem(
            "Systems Architecture",
            "Designing robust IT and operational systems for firm-wide efficiency.",
            Icons.Default.Dns,
            "Systems"
        ),
        ServiceItem(
            "Financial Systems",
            "Automated billing and M-Pesa Daraja integration for seamless cashflow management.",
            Icons.Default.AccountBalance,
            "Financial"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Firm Capabilities",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "We provide a wide range of technical and strategic solutions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(services) { service ->
            ServiceCard(service)
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Custom Project Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Don't see what you're looking for? Contact your strategy lead to discuss a custom job scope.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { 
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:strategy@greggory.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Consultation Request: ${prefs.getUserName()}")
                                    putExtra(Intent.EXTRA_TEXT, "Hello Greggory Strategy Team,\n\nI would like to request a consultation regarding a new project for my firm.")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Consultation")
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: ServiceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = service.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = service.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
