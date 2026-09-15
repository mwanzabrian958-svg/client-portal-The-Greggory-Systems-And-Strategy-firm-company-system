package com.greggory.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Notification
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(notifications: List<Notification>) {
    val scope = rememberCoroutineScope()

    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No new notifications", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Alerts",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.instance.markAllNotificationsRead()
                            } catch (e: Exception) {}
                        }
                    }) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All Read")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(notifications) { notification ->
                NotificationCard(
                    notification = notification,
                    onMarkRead = {
                        scope.launch {
                            try {
                                RetrofitClient.instance.markNotificationRead(notification.id)
                            } catch (e: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(notification: Notification, onMarkRead: () -> Unit) {
    Card(
        onClick = { if (notification.status == "unread") onMarkRead() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.status == "unread") 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PriorityBadge(notification.priority)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.created_at,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority.lowercase()) {
        "high" -> Color.Red
        "normal" -> MaterialTheme.colorScheme.primary
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = priority.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
