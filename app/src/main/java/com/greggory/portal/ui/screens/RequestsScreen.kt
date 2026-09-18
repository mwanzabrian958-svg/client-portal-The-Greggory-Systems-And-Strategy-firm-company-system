package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.*
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.utils.DataRouter
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Quotes", "Signatures", "Change Requests")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> QuotesList()
            1 -> SignatureRequestsList()
            2 -> ChangeRequestsList()
        }
    }
}

@Composable
fun QuotesList() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun fetchQuotes() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.getQuotes()
                if (response.isSuccessful) {
                    val body = response.body()
                    val userId = prefs.getUserId()
                    if (body != null) {
                        // Integrity check
                        if (body.quotes.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }) {
                            quotes = body.quotes
                        } else {
                            Toast.makeText(context, "Security Alert: Routing Integrity Failure", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    fun handleDecision(quoteId: Int, decision: String) {
        scope.launch {
            try {
                val response = RetrofitClient.instance.quoteDecision(quoteId, DecisionRequest(decision))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Quote $decision", Toast.LENGTH_SHORT).show()
                    fetchQuotes()
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) { fetchQuotes() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (quotes.isEmpty()) item { Text("No quotes found.") }
                items(quotes) { quote ->
                    QuoteCard(quote, onDecision = { handleDecision(quote.id, it) })
                }
            }
        }
    }
}

@Composable
fun QuoteCard(quote: Quote, onDecision: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(quote.project_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("Quote #${quote.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(quote.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Amount: KSH ${quote.amount.toInt()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            
            if (quote.status.lowercase() == "pending") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecision("approved") }, modifier = Modifier.weight(1f)) { Text("Approve") }
                    OutlinedButton(onClick = { onDecision("rejected") }, modifier = Modifier.weight(1f)) { Text("Reject") }
                }
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(quote.status.uppercase(), modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
fun SignatureRequestsList() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var requests by remember { mutableStateOf<List<SignatureRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun fetchRequests() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.getSignatureRequests()
                if (response.isSuccessful) {
                    val body = response.body()
                    val userId = prefs.getUserId()
                    if (body != null) {
                        if (body.requests.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }) {
                            requests = body.requests
                        } else {
                            Toast.makeText(context, "Security Alert: Routing Integrity Failure", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    fun handleDecision(id: Int, decision: String) {
        scope.launch {
            try {
                val response = RetrofitClient.instance.signatureDecision(id, DecisionRequest(decision))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Document $decision", Toast.LENGTH_SHORT).show()
                    fetchRequests()
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) { fetchRequests() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (requests.isEmpty()) item { Text("No pending signature requests.") }
                items(requests) { req ->
                    SignatureCard(req, onDecision = { handleDecision(req.id, it) })
                }
            }
        }
    }
}

@Composable
fun SignatureCard(req: SignatureRequest, onDecision: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(req.project_name, style = MaterialTheme.typography.labelSmall)
                Text(req.document_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Badge { Text(req.status.uppercase()) }
            }
            if (req.status.lowercase() == "pending") {
                Row {
                    IconButton(onClick = { onDecision("signed") }) {
                        Icon(Icons.Default.History, contentDescription = "Sign", tint = Color(0xFF2A9D8F))
                    }
                    IconButton(onClick = { onDecision("declined") }) {
                        Icon(Icons.Default.History, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeRequestsList() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var requests by remember { mutableStateOf<List<ChangeRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun fetchRequests() {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.getChangeRequests()
                if (response.isSuccessful) {
                    val body = response.body()
                    val userId = prefs.getUserId()
                    if (body != null) {
                        if (body.requests.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }) {
                            requests = body.requests
                        } else {
                            Toast.makeText(context, "Security Alert: Routing Integrity Failure", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetchRequests() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            if (!isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("History", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Request")
                    }
                }
            }
            
            if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (requests.isEmpty()) item { Text("No change requests found.") }
                    items(requests) { req ->
                        ChangeRequestCard(req)
                    }
                }
            }
        }

        if (showAddDialog) {
            SubmitChangeRequestDialog(
                onDismiss = { showAddDialog = false },
                onSuccess = { 
                    showAddDialog = false
                    fetchRequests()
                    Toast.makeText(context, "Request submitted", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitChangeRequestDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Change Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Request an adjustment to project scope or deliverables.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Request Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        try {
                            // Note: project_id should come from current selection if available
                            val response = RetrofitClient.instance.submitChangeRequest(
                                ChangeRequestAction(0, title, description)
                            )
                            if (response.isSuccessful) onSuccess()
                        } catch (e: Exception) {} finally { isSubmitting = false }
                    }
                }
            ) { Text("SUBMIT") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun ChangeRequestCard(req: ChangeRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(req.project_name, style = MaterialTheme.typography.labelSmall)
            Text(req.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(req.description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Badge { Text(req.status.uppercase()) }
        }
    }
}
