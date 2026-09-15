package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.Invoice
import com.greggory.portal.data.api.MpesaStkPushRequest
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.utils.FileDownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BillingScreen(invoices: List<Invoice>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    var showReportDialog by remember { mutableStateOf(false) }
    var mpesaFeedbackMessage by remember { mutableStateOf("") }
    var isReporting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Financial Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(invoices) { invoice ->
                InvoiceCard(
                    invoice = invoice,
                    onPayClick = {
                        selectedInvoice = invoice
                        showPhoneDialog = true
                    },
                    onDownloadClick = {
                        val fileName = "Invoice_${invoice.id}.pdf"
                        val success = FileDownloadHelper.downloadFile(
                            context = context,
                            url = FileDownloadHelper.getInvoiceUrl(invoice.id),
                            fileName = fileName,
                            description = "Downloading Invoice #${invoice.id}"
                        )
                        if (success) {
                            Toast.makeText(context, "Invoice download started", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to start download", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReportClick = {
                        selectedInvoice = invoice
                        mpesaFeedbackMessage = ""
                        showReportDialog = true
                    }
                )
            }
        }
    }

    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showPhoneDialog = false },
            title = { Text("Payment Options") },
            text = {
                Column {
                    Text("Option 1: Quick Pay (STK Push)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Pay KSH ${selectedInvoice?.amount} for Invoice #${selectedInvoice?.id}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() } && it.length <= 12) {
                                phoneNumber = it 
                            }
                        },
                        label = { Text("M-Pesa Phone Number") },
                        placeholder = { Text("2547XXXXXXXX") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Option 2: Manual Pay (Outside App)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Dial *334# or use M-Pesa menu:")
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Column {
                            Text("• Action: Send Money", fontWeight = FontWeight.Medium)
                            Text("• Number: 07115525854", fontWeight = FontWeight.Medium)
                            Text("• Amount: KSH ${selectedInvoice?.amount}", fontWeight = FontWeight.Medium)
                            Text("• Name: The Greggory Systems", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Sending STK Push...", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                // Normalize phone number to 254 format for Safaricom Production
                                val normalizedPhone = when {
                                    phoneNumber.startsWith("0") -> "254" + phoneNumber.substring(1)
                                    phoneNumber.startsWith("7") || phoneNumber.startsWith("1") -> "254" + phoneNumber
                                    phoneNumber.startsWith("254") -> phoneNumber
                                    else -> phoneNumber
                                }

                                val response = RetrofitClient.instance.initiateSTKPush(
                                    MpesaStkPushRequest(
                                        phoneNumber = normalizedPhone,
                                        amount = selectedInvoice?.amount ?: 0.0,
                                        accountReference = selectedInvoice?.id.toString(),
                                        description = "Payment for Invoice #${selectedInvoice?.id}"
                                    )
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val checkoutRequestId = response.body()?.checkoutRequestId
                                    snackbarHostState.showSnackbar("STK Push sent to $phoneNumber")
                                    
                                    // Start polling for status
                                    if (checkoutRequestId != null) {
                                        pollPaymentStatus(checkoutRequestId, snackbarHostState)
                                    }
                                    
                                    showPhoneDialog = false
                                } else {
                                    snackbarHostState.showSnackbar(response.body()?.message ?: "Payment initiation failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = phoneNumber.length >= 10 && !isProcessing
                ) {
                    Text("PROCEED")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }, enabled = !isProcessing) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isReporting) showReportDialog = false },
            title = { Text("Report Payment Feedback") },
            text = {
                Column {
                    Text(
                        text = "Submit the M-Pesa receipt message or transaction code for Invoice #${selectedInvoice?.id} to the accounts office for manually reconciling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mpesaFeedbackMessage,
                        onValueChange = { mpesaFeedbackMessage = it },
                        label = { Text("M-Pesa Transaction Code / Message") },
                        placeholder = { Text("Example: PKG87HDKS9 Confirmed. Ksh...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        enabled = !isReporting,
                        maxLines = 5
                    )
                    if (isReporting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReporting = true
                        scope.launch {
                            try {
                                val response = RetrofitClient.instance.reportManualPayment(
                                    com.greggory.portal.data.api.PaymentReportRequest(
                                        invoiceId = selectedInvoice?.id.toString(),
                                        mpesaMessage = mpesaFeedbackMessage.trim()
                                    )
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    snackbarHostState.showSnackbar("✅ Payment report submitted successfully to webmaster.")
                                    showReportDialog = false
                                } else {
                                    snackbarHostState.showSnackbar("❌ Submission failed: ${response.body()?.message ?: "Unknown error"}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                            } finally {
                                isReporting = false
                            }
                        }
                    },
                    enabled = mpesaFeedbackMessage.trim().isNotEmpty() && !isReporting
                ) {
                    Text("SUBMIT REPORT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }, enabled = !isReporting) {
                    Text("CANCEL")
                }
            }
        )
    }
}

private suspend fun pollPaymentStatus(checkoutRequestId: String, snackbarHostState: SnackbarHostState) {
    var attempts = 0
    while (attempts < 12) { // Poll for ~1 minute (5s intervals)
        delay(5000)
        try {
            val statusResponse = RetrofitClient.instance.getMpesaStatus(checkoutRequestId)
            if (statusResponse.isSuccessful) {
                val status = statusResponse.body()?.status
                if (status == "completed") {
                    snackbarHostState.showSnackbar("✅ Payment Successful! Receipt: ${statusResponse.body()?.mpesa_receipt}")
                    return
                } else if (status == "failed") {
                    snackbarHostState.showSnackbar("❌ Payment Failed: ${statusResponse.body()?.result_desc}")
                    return
                }
            }
        } catch (e: Exception) {
            // Ignore polling errors
        }
        attempts++
    }
    snackbarHostState.showSnackbar("⌛ Payment is still being processed. Please check back later.")
}

@Composable
fun InvoiceCard(invoice: Invoice, onPayClick: () -> Unit, onDownloadClick: () -> Unit, onReportClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Invoice #${invoice.id}", style = MaterialTheme.typography.labelSmall)
                    Text(text = "KSH ${invoice.amount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                InvoiceStatusBadge(invoice.status)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
                if (invoice.status.lowercase() != "paid") {
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAY")
                    }
                }
            }
            if (invoice.status.lowercase() != "paid") {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onReportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already Paid? Submit M-Pesa Code / Message", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "paid" -> Color(0xFF2A9D8F)
        "pending" -> Color(0xFFE9C46A)
        "overdue" -> Color(0xFFE76F51)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
