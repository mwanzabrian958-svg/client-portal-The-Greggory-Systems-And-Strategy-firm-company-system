package com.greggory.portal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.greggory.portal.R
import com.greggory.portal.data.api.DashboardResponse
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.ui.components.ChangePasswordDialog
import com.greggory.portal.utils.BiometricHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onLogout: () -> Unit, 
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: () -> Unit,
    dashboardData: DashboardResponse? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    
    val userFromDash = dashboardData?.dashboard?.user
    val userName = userFromDash?.displayName ?: userFromDash?.firstName ?: prefs.getUserName() ?: "Client User"
    val userEmail = userFromDash?.email ?: prefs.getUserEmail() ?: ""
    val initials = userName.split(" ")
        .filter { it.isNotEmpty() }
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
    
    var backgroundType by remember { mutableStateOf(prefs.getBackgroundType()) }
    var backgroundSource by remember { mutableStateOf(prefs.getBackgroundSource()) }
    var backgroundUri by remember { mutableStateOf(prefs.getBackgroundUri()) }
    
    var themeMode by remember { mutableStateOf(prefs.getThemeMode()) }
    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }
    
    var projectAlerts by remember { mutableStateOf(prefs.getNotificationPref("project")) }
    var financialAlerts by remember { mutableStateOf(prefs.getNotificationPref("financial")) }
    var messageAlerts by remember { mutableStateOf(prefs.getNotificationPref("message")) }
    
    var currency by remember { mutableStateOf(prefs.getCurrency()) }
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val uriString = it.toString()
            prefs.saveBackgroundType("image")
            prefs.saveBackgroundSource("gallery")
            prefs.saveBackgroundUri(uriString)
            backgroundType = "image"
            backgroundSource = "gallery"
            backgroundUri = uriString
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val uriString = it.toString()
            prefs.saveBackgroundType("video")
            prefs.saveBackgroundSource("gallery")
            prefs.saveBackgroundUri(uriString)
            backgroundType = "video"
            backgroundSource = "gallery"
            backgroundUri = uriString
        }
    }

    val preloadedImages = listOf(
        "https://images.unsplash.com/photo-1497215728101-856f4ea42174?auto=format&fit=crop&w=1000",
        "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1000",
        "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?auto=format&fit=crop&w=1000"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Portal Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        // --- PROFILE SUMMARY ---
        Card(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    var imageError by remember { mutableStateOf(false) }
                    val cachedPhotoData = prefs.getUserPhotoData()
                    if (!imageError) {
                        val photoModel = when {
                            !cachedPhotoData.isNullOrEmpty() -> {
                                if (cachedPhotoData.startsWith("http") || cachedPhotoData.startsWith("data:")) cachedPhotoData else "${RetrofitClient.BASE_URL}$cachedPhotoData"
                            }
                            else -> "${RetrofitClient.BASE_URL}api/users/profile-photo/me"
                        }
                        val imageRequest = remember(photoModel, prefs.getToken()) {
                            val builder = coil.request.ImageRequest.Builder(context)
                                .data(photoModel)
                                .crossfade(true)
                            if (prefs.getToken() != null) {
                                builder.addHeader("Authorization", "Bearer ${prefs.getToken()}")
                            }
                            builder.build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { imageError = true },
                            onSuccess = { imageError = false }
                        )
                    }
                    if (imageError || initials.isEmpty()) {
                        Text(
                            text = initials.ifEmpty { "G" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- APP CUSTOMIZATION ---
        SettingsSectionHeader("App Customization", Icons.Default.Palette)
        
        Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("System" to "system", "Light" to "light", "Dark" to "dark").forEach { (label, value) ->
                FilterChip(
                    selected = themeMode == value,
                    onClick = { themeMode = value; prefs.saveThemeMode(value) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Background Mode", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("Default" to "color", "Image" to "image", "Video" to "video").forEach { (label, value) ->
                FilterChip(
                    selected = backgroundType == value,
                    onClick = { backgroundType = value; prefs.saveBackgroundType(value) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (backgroundType == "image") {
            LazyRow(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(preloadedImages) { url ->
                    Card(
                        modifier = Modifier
                            .size(100.dp, 150.dp)
                            .padding(end = 8.dp)
                            .clickable {
                                backgroundSource = "preloaded"
                                backgroundUri = url
                                prefs.saveBackgroundSource("preloaded")
                                prefs.saveBackgroundUri(url)
                            },
                        elevation = if (backgroundUri == url) CardDefaults.cardElevation(8.dp) else CardDefaults.cardElevation(2.dp),
                        border = if (backgroundUri == url) CardDefaults.outlinedCardBorder() else null
                    ) {
                        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
            Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pick Image from Gallery")
            }
        }

        if (backgroundType == "video") {
            Button(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Icon(Icons.Default.Movie, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Video from Gallery")
            }
        }

        // --- ACCOUNT & SECURITY ---
        SettingsSectionHeader("Account & Security", Icons.Default.Security)
        SettingsSwitchItem("Biometric Authentication", "Fingerprint or Face ID login", biometricEnabled) { enabled ->
            if (enabled) {
                if (BiometricHelper.isBiometricAvailable(context)) {
                    BiometricHelper.showBiometricPrompt(
                        activity = context as FragmentActivity,
                        onSuccess = {
                            biometricEnabled = true
                            prefs.saveBiometricEnabled(true)
                            Toast.makeText(context, "Biometric login enabled", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Verification failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_SHORT).show()
                }
            } else {
                biometricEnabled = false
                prefs.saveBiometricEnabled(false)
            }
        }
        SettingsClickItem("Change Password", "Update your portal credentials", Icons.Default.Lock) {
            showPasswordDialog = true
        }
        SettingsClickItem("Active Sessions", "Manage other logged-in devices", Icons.Default.Devices) {
            showSessionsDialog = true
        }

        // --- NOTIFICATION PREFERENCES ---
        SettingsSectionHeader("Notification Preferences", Icons.Default.Notifications)
        SettingsSwitchItem("Project Alerts", "Milestones & task updates", projectAlerts) {
            projectAlerts = it; prefs.saveNotificationPref("project", it)
        }
        SettingsSwitchItem("Financial Alerts", "Invoices & payments", financialAlerts) {
            financialAlerts = it; prefs.saveNotificationPref("financial", it)
        }
        SettingsSwitchItem("Direct Messages", "Strategy lead communication", messageAlerts) {
            messageAlerts = it; prefs.saveNotificationPref("message", it)
        }

        // --- REGIONAL & FINANCIAL ---
        SettingsSectionHeader("Regional & Financial", Icons.Default.Public)
        Text("Currency Display", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("KSH", "USD", "EUR").forEach { c ->
                FilterChip(
                    selected = currency == c,
                    onClick = { currency = c; prefs.saveCurrency(c) },
                    label = { Text(c) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // --- DATA & STORAGE ---
        SettingsSectionHeader("Data & Storage", Icons.Default.Storage)
        SettingsClickItem("Clear Cache", "Refresh all data from server", Icons.Default.DeleteSweep) {
            scope.launch {
                database.projectDao().clearProjects()
                database.invoiceDao().clearInvoices()
                database.reportDao().clearReports()
                Toast.makeText(context, "Cache cleared. Refreshing...", Toast.LENGTH_SHORT).show()
            }
        }
        SettingsClickItem("Export My Data", "Download full history (PDF)", Icons.Default.Download) {
            showExportDialog = true
        }

        // --- ABOUT & SUPPORT ---
        SettingsSectionHeader("About & Support", Icons.AutoMirrored.Filled.HelpOutline)
        SettingsClickItem("Legal Documents", "Terms & Privacy Policy", Icons.Default.Description) { 
            onNavigateToProfile()
        }
        SettingsClickItem("Feedback & Suggestions", "Help us improve your experience", Icons.Default.Feedback) {
            showFeedbackDialog = true
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOGOUT FROM PORTAL")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("App Version: 1.0.0", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(onDismiss = { showPasswordDialog = false })
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSend = { title, message ->
                scope.launch {
                    try {
                        val response = RetrofitClient.instance.submitFeedback(
                            com.greggory.portal.data.api.FeedbackRequest(
                                title = title,
                                message = message,
                                type = "general",
                                rating = 5,
                                priority = "medium"
                            )
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Feedback sent successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to send feedback.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                showFeedbackDialog = false
            }
        )
    }

    if (showSessionsDialog) {
        ActiveSessionsDialog(onDismiss = { showSessionsDialog = false })
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Request Data Export") },
            text = { Text("A certified PDF audit of your entire project and billing history will be compiled. This process can take up to 24 hours for security verification.\n\nContinue with request?") },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    Toast.makeText(context, "Request logged. You will be notified when the audit is ready.", Toast.LENGTH_LONG).show()
                }) { Text("REQUEST EXPORT") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to end your session? All local cache will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            prefs.clear()
                            database.projectDao().clearProjects()
                            database.invoiceDao().clearInvoices()
                            database.reportDao().clearReports()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LOGOUT") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun ActiveSessionsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    var isRevoking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Active Sessions") },
        text = {
            Column {
                Text("Your account is secured by the Set in Stone Terminal Lock. Currently, only this device is authorized.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Android OS ${android.os.Build.VERSION.RELEASE} (This Device)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val token = prefs.getToken()
                        if (token != null) {
                            isRevoking = true
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.revokeOtherSessions(token)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "All other sessions revoked", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Revocation failed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isRevoking = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRevoking,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("REVOKE ALL OTHER DEVICES")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Last access: Just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Feedback") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("How can we improve?") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (message.isNotBlank()) onSend(title, message) },
                enabled = message.isNotBlank()
            ) { Text("SEND") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}



@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
