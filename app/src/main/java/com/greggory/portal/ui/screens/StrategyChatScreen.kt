package com.greggory.portal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.SendChatRequest
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.StrategyChatMessageEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val chatHistory by database.strategyChatDao().getChatHistory().collectAsState(initial = emptyList())
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // Refresh history from API
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getChatHistory()
            if (response.isSuccessful) {
                response.body()?.messages?.forEach { item ->
                    database.strategyChatDao().insertMessage(
                        StrategyChatMessageEntity(
                            id = item.id,
                            senderId = item.sender_id,
                            senderName = item.sender_name,
                            message = item.message,
                            timestamp = item.timestamp,
                            isFromMe = item.is_from_me
                        )
                    )
                }
            }
        } catch (ignored: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Strategy Chat", style = MaterialTheme.typography.titleMedium)
                        Text("Direct line to Firm Strategists", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val text = messageText
                                messageText = ""
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.instance.sendChatMessage(SendChatRequest(text))
                                        if (response.isSuccessful) {
                                            // Optimistic UI update could be handled here or wait for sync
                                            // For now, let's insert a local placeholder
                                            database.strategyChatDao().insertMessage(
                                                StrategyChatMessageEntity(
                                                    id = "local_${System.currentTimeMillis()}",
                                                    senderId = 0, // Current user
                                                    senderName = "Me",
                                                    message = text,
                                                    timestamp = System.currentTimeMillis(),
                                                    isFromMe = true
                                                )
                                            )
                                        }
                                    } catch (ignored: Exception) {}
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: StrategyChatMessageEntity) {
    val alignment = if (msg.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (msg.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (msg.isFromMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (msg.isFromMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (msg.isFromMe) Alignment.End else Alignment.Start
        ) {
            if (!msg.isFromMe) {
                Text(msg.senderName, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Surface(
                color = color,
                shape = shape,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = msg.message,
                    color = textColor,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
