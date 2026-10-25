package com.greggory.portal.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.SendChatRequest
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.StrategyChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
            selectedMediaType = mimeType
        }
    }

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
                            isFromMe = item.is_from_me,
                            attachmentUrl = item.attachment_url,
                            attachmentType = item.attachment_type
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
                Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                    if (selectedMediaUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedMediaType?.startsWith("video") == true) {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(Color.Black, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
                                    }
                                } else {
                                    AsyncImage(
                                        model = selectedMediaUri,
                                        contentDescription = "Selected media",
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedMediaType?.startsWith("video") == true) "Selected Video" else "Selected Image",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Ready to attach", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { 
                                    selectedMediaUri = null
                                    selectedMediaType = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mediaPickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach media", tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message or attach media...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (messageText.isNotBlank() || selectedMediaUri != null) {
                                    val text = messageText
                                    val uri = selectedMediaUri
                                    val mime = selectedMediaType
                                    messageText = ""
                                    selectedMediaUri = null
                                    selectedMediaType = null

                                    scope.launch {
                                        try {
                                            var base64Data: String? = null
                                            if (uri != null) {
                                                base64Data = withContext(Dispatchers.IO) {
                                                    try {
                                                        context.contentResolver.openInputStream(uri)?.use { stream ->
                                                            val bytes = stream.readBytes()
                                                            Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                        }
                                                    } catch (_: Exception) { null }
                                                }
                                            }

                                            val response = RetrofitClient.instance.sendChatMessage(
                                                SendChatRequest(
                                                    message = text,
                                                    attachment_base64 = base64Data,
                                                    attachment_type = mime
                                                )
                                            )
                                            if (response.isSuccessful) {
                                                val localUrl = uri?.toString()
                                                database.strategyChatDao().insertMessage(
                                                    StrategyChatMessageEntity(
                                                        id = "local_${System.currentTimeMillis()}",
                                                        senderId = 0,
                                                        senderName = "Me",
                                                        message = text.ifBlank { if (mime?.startsWith("video") == true) "[Video Attachment]" else "[Image Attachment]" },
                                                        timestamp = System.currentTimeMillis(),
                                                        isFromMe = true,
                                                        attachmentUrl = localUrl,
                                                        attachmentType = mime
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
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!msg.attachmentUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (msg.attachmentType?.startsWith("video") == true) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = msg.attachmentUrl,
                                    contentDescription = "Chat attachment",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (msg.message.isNotBlank()) {
                        Text(
                            text = msg.message,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
