package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Task

@Composable
fun TasksScreen(tasks: List<Task>) {
    val context = LocalContext.current
    var taskList by remember(tasks) { mutableStateOf(tasks) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    val groupedTasks = taskList.groupBy { it.project }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Operational Milestones",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tap any milestone below to update its progress or status.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (taskList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No active milestones found.")
                }
            }
        }

        groupedTasks.forEach { (projectName, projectTasks) ->
            item {
                Text(
                    text = projectName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(projectTasks) { task ->
                TaskCard(task) {
                    selectedTask = task
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    selectedTask?.let { task ->
        TaskTrackingDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onSave = { updatedProgress, updatedStatus ->
                taskList = taskList.map {
                    if (it.id == task.id) {
                        it.copy(progress = updatedProgress, status = updatedStatus)
                    } else {
                        it
                    }
                }
                selectedTask = null
                Toast.makeText(context, "Milestone updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TaskCard(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(task.project, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.weight(1f))
                TaskPriorityBadge(task.priority)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Assignee: ${task.assignee}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (task.status == "blocked") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${task.progress}%", style = MaterialTheme.typography.labelSmall)
                Text(task.status.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            
            if (!task.dueDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Due: ${task.dueDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TaskPriorityBadge(priority: String) {
    val color = when(priority.lowercase()) {
        "critical" -> Color(0xFFD00000)
        "high" -> Color(0xFFE85D04)
        "low" -> Color(0xFF2A9D8F)
        else -> Color(0xFF415A77)
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(priority.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun TaskTrackingDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var progressSlider by remember { mutableStateOf(task.progress.toFloat()) }
    var selectedStatus by remember { mutableStateOf(task.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Milestone: ${task.title}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Project: ${task.project}", style = MaterialTheme.typography.bodySmall)
                Text("Assignee: ${task.assignee}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Progress: ${progressSlider.toInt()}%", fontWeight = FontWeight.Bold)
                Slider(
                    value = progressSlider,
                    onValueChange = { progressSlider = it },
                    valueRange = 0f..100f,
                    steps = 19
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Status", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("in_progress", "completed", "blocked").forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.uppercase()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(progressSlider.toInt(), selectedStatus) }) {
                Text("Save Milestone Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
