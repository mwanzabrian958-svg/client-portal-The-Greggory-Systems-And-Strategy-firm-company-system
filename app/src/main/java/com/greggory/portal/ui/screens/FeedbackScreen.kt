package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.FeedbackItem
import com.greggory.portal.data.api.FeedbackRequest
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedbackList by remember { mutableStateOf<List<FeedbackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    fun fetchFeedback() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.getFeedback()
                if (response.isSuccessful && response.body()?.success == true) {
                    feedbackList = response.body()?.feedback ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchFeedback()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && feedbackList.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                            text = "Client Feedback",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(onClick = { showSubmitDialog = true }) {
                            Text("Give Feedback")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (feedbackList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No feedback history found.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                items(feedbackList) { item ->
                    FeedbackCard(item)
                }
            }
        }

        if (showSubmitDialog) {
            SubmitFeedbackDialog(
                onDismiss = { showSubmitDialog = false },
                onSuccess = { 
                    showSubmitDialog = false
                    fetchFeedback()
                    Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
fun FeedbackCard(item: FeedbackItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                RatingBar(rating = item.rating, modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(item.type.uppercase(), modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = if (item.priority == "high") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(item.priority.uppercase(), modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Submitted: ${item.created_at}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun RatingBar(rating: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitFeedbackDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("general") }
    var rating by remember { mutableStateOf(5) }
    var priority by remember { mutableStateOf("medium") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("general", "bug", "feature").forEach {
                        FilterChip(
                            selected = type == it,
                            onClick = { type = it },
                            label = { Text(it.uppercase()) }
                        )
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach {
                        FilterChip(
                            selected = priority == it,
                            onClick = { priority = it },
                            label = { Text(it.uppercase()) }
                        )
                    }
                }

                Text("Rating", style = MaterialTheme.typography.labelMedium)
                Row {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && message.isNotBlank() && !isSubmitting,
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.instance.submitFeedback(
                                FeedbackRequest(title, message, type, rating, priority)
                            )
                            if (response.isSuccessful) onSuccess()
                        } catch (e: Exception) {
                            // error handling
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
