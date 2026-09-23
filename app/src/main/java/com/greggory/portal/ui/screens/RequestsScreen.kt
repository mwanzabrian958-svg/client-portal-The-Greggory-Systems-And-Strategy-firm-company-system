package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.*
import com.greggory.portal.data.local.AppDatabase
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
                        if (body.quotes.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }) {
                            quotes = body.quotes
                        } else {
                            Toast.makeText(context, "Security Alert: Routing Integrity Mismatch", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // handle error
            } finally { isLoading = false }
        }
    }

    fun handleDecision(quoteId: Int, decision: String) {
        scope.launch {
            try {
                val response = RetrofitClient.instance.quoteDecision(quoteId, DecisionRequest(decision))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Quote marked as $decision", Toast.LENGTH_SHORT).show()
                    fetchQuotes()
                } else {
                    Toast.makeText(context, "Failed to update quote decision", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { fetchQuotes() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (quotes.isEmpty()) {
                    item { Text("No quotes found.", style = MaterialTheme.typography.bodyMedium) }
                }
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
            Spacer(modifier = Modifier.height(2.dp))
            Text("Quote #${quote.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(quote.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Amount: KSH ${quote.amount.toInt()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (quote.status.lowercase() == "pending") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDecision("approved") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("APPROVE QUOTE")
                    }
                    OutlinedButton(
                        onClick = { onDecision("rejected") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DECLINE")
                    }
                }
            } else {
                Badge(containerColor = if (quote.status.lowercase() == "approved") Color(0xFF2A9D8F) else MaterialTheme.colorScheme.secondaryContainer) {
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
                            Toast.makeText(context, "Security Alert: Routing Integrity Mismatch", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // handle error
            } finally { isLoading = false }
        }
    }

    fun handleDecision(id: Int, decision: String) {
        scope.launch {
            try {
                val response = RetrofitClient.instance.signatureDecision(id, DecisionRequest(decision))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Document marked as $decision", Toast.LENGTH_SHORT).show()
                    fetchRequests()
                } else {
                    Toast.makeText(context, "Decision update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { fetchRequests() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (requests.isEmpty()) {
                    item { Text("No pending digital signature requests.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(requests) { req ->
                    SignatureCard(req, onDecision = { handleDecision(req.id, it) })
                }
            }
        }
    }
}

@Composable
fun SignatureCard(req: SignatureRequest, onDecision: (String) -> Unit) {
    var showConfirmSign by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(req.project_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(req.document_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Badge(containerColor = if (req.status.lowercase() == "signed") Color(0xFF2A9D8F) else MaterialTheme.colorScheme.secondaryContainer) {
                    Text(req.status.uppercase(), modifier = Modifier.padding(4.dp))
                }
            }

            if (req.status.lowercase() == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showConfirmSign = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                    ) {
                        Text("SIGN DOCUMENT")
                    }
                    OutlinedButton(
                        onClick = { onDecision("declined") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DECLINE")
                    }
                }
            }
        }
    }

    if (showConfirmSign) {
        AlertDialog(
            onDismissRequest = { showConfirmSign = false },
            title = { Text("Confirm Digital Signature") },
            text = { Text("Sign \"${req.document_name}\"? This action records your legally binding digital approval in the pipeline.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSign = false
                        onDecision("signed")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                ) { Text("CONFIRM & SIGN") }
            },
            dismissButton = { TextButton(onClick = { showConfirmSign = false }) { Text("CANCEL") } }
        )
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
                            Toast.makeText(context, "Security Alert: Routing Integrity Mismatch", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // handle error
            } finally { isLoading = false }
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
                    Text("Request History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Request")
                    }
                }
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (requests.isEmpty()) {
                        item { Text("No change requests found.", style = MaterialTheme.typography.bodyMedium) }
                    }
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
                    Toast.makeText(context, "Scope change request submitted to pipeline", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitChangeRequestDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val localProjects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())

    var selectedProjectId by remember { mutableStateOf(localProjects.firstOrNull()?.id ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Scope Change Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Submit a formal scope adjustment request for your project.", style = MaterialTheme.typography.bodySmall)

                if (localProjects.isNotEmpty()) {
                    val selectedProject = localProjects.find { it.id == selectedProjectId } ?: localProjects.first()
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Project: ${selectedProject.name}")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            localProjects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    onClick = {
                                        selectedProjectId = proj.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Request Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Scope Description") },
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
                            val response = RetrofitClient.instance.submitChangeRequest(
                                ChangeRequestAction(selectedProjectId, title, description)
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                onSuccess()
                            } else {
                                Toast.makeText(context, response.body()?.message ?: "Submission failed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("SUBMIT REQUEST")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun ChangeRequestCard(req: ChangeRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(req.project_name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(req.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(req.description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(req.status.uppercase(), modifier = Modifier.padding(4.dp))
            }
        }
    }
}
