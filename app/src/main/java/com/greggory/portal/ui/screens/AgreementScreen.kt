package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.ClientAgreementEntity
import com.greggory.portal.data.local.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgreementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val savedAgreements by database.agreementDao().getAllAgreements().collectAsState(initial = emptyList())

    var clientName by remember { mutableStateOf(prefs.getUserName() ?: "") }
    var companyName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefs.getUserEmail() ?: "") }
    var phoneNumber by remember { mutableStateOf(prefs.getUserPhone() ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val serviceOptions = listOf(
        "Executive Business Advisory & Corporate Strategy",
        "Financial Engineering & M-Pesa Integration",
        "Software & Enterprise Cloud Applications",
        "Digital Transformation & Growth Strategy",
        "Data Intelligence, Analytics & AI Telemetry",
        "Operational Efficiency & Workflow Optimization",
        "Legal, Governance & Regulatory Compliance",
        "Cybersecurity & Data Vault Systems",
        "Website Development",
        "Web Applications",
        "Mobile App Development",
        "Desktop Applications",
        "Networking & Infrastructure",
        "Systems Architecture",
        "Financial Systems"
    )
    var serviceCategory by remember { mutableStateOf(serviceOptions[0]) }
    var declarationAccepted by remember { mutableStateOf(false) }
    var signatureName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Client Registration Agreement", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Master Service Agreement & Engagement SOW", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TERMS & CONDITIONS SUMMARY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "By submitting this official registration agreement to The Greggory Systems & Strategy Firm (GSSF), the client agrees to enterprise engagement protocols, milestone SLAs, secure data processing standards, and transparent billing terms.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            label = { Text("Representative Full Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Company / Organization Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Official Email Address *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = serviceCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Primary Service Engagement Scope *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                serviceOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            serviceCategory = option
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = declarationAccepted,
                onCheckedChange = { declarationAccepted = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I hereby certify, authorize, and agree to bind my organization to the GSSF Master Service Agreement.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = signatureName,
            onValueChange = { signatureName = it },
            label = { Text("Digital Signature (Type Full Name) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (clientName.isBlank() || companyName.isBlank() || email.isBlank() || signatureName.isBlank()) {
                    Toast.makeText(context, "Please fill in all required fields and sign.", Toast.LENGTH_SHORT).show()
                } else if (!declarationAccepted) {
                    Toast.makeText(context, "You must accept the terms and conditions declaration.", Toast.LENGTH_SHORT).show()
                } else {
                    isSubmitting = true
                    scope.launch {
                        try {
                            val agreement = ClientAgreementEntity(
                                clientName = clientName,
                                companyName = companyName,
                                email = email,
                                phoneNumber = phoneNumber,
                                serviceCategory = serviceCategory,
                                declarationAccepted = declarationAccepted,
                                signatureName = signatureName,
                                timestamp = System.currentTimeMillis()
                            )
                            database.agreementDao().insertAgreement(agreement)
                            isSubmitting = false
                            Toast.makeText(context, "Agreement successfully saved to Secure Room DB!", Toast.LENGTH_LONG).show()
                            onBack()
                        } catch (e: Exception) {
                            isSubmitting = false
                            Toast.makeText(context, "Failed to save agreement: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isSubmitting
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Agreement to Secure Database", fontWeight = FontWeight.Bold)
        }

        if (savedAgreements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Previously Filed Agreements (${savedAgreements.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            savedAgreements.forEach { saved ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(saved.companyName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Representative: ${saved.clientName} (${saved.email})", style = MaterialTheme.typography.bodySmall)
                        Text("Scope: ${saved.serviceCategory}", style = MaterialTheme.typography.bodySmall)
                        Text("Signed by: ${saved.signatureName} on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(saved.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
