package com.greggory.portal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.greggory.portal.data.api.*
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.ui.components.ChangePasswordDialog
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun ProfileScreen(dashboardData: DashboardResponse? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    
    // Fallback logic: Use Dashboard data first, then cached prefs
    val userFromDash = dashboardData?.dashboard?.user
    val initialName = userFromDash?.displayName ?: if (userFromDash != null) "${userFromDash.firstName} ${userFromDash.lastName ?: ""}".trim() else (prefs.getUserName() ?: "")
    val initialEmail = userFromDash?.email ?: prefs.getUserEmail() ?: ""
    val initialPhone = userFromDash?.phone ?: prefs.getUserPhone() ?: ""
    val initialBriefing = userFromDash?.missionBriefing ?: prefs.getUserBriefing() ?: "Strategic partnership in progress."
    val initialPhotoData = userFromDash?.profilePhotoData ?: prefs.getUserPhotoData()

    var firstName by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var phoneNumber by remember { mutableStateOf(initialPhone) }
    var missionBriefing by remember { mutableStateOf(initialBriefing) }
    var profilePhotoData by remember { mutableStateOf(initialPhotoData) }
    var isUpdating by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Synchronize local state if dashboard data arrives late
    LaunchedEffect(userFromDash) {
        if (userFromDash != null) {
            firstName = userFromDash.displayName ?: "${userFromDash.firstName} ${userFromDash.lastName ?: ""}".trim()
            email = userFromDash.email
            phoneNumber = userFromDash.phone ?: ""
            missionBriefing = userFromDash.missionBriefing ?: missionBriefing
            profilePhotoData = userFromDash.profilePhotoData ?: profilePhotoData
            
            // Persist the latest info
            prefs.saveUserInfo(
                userId = userFromDash.id, 
                email = userFromDash.email, 
                name = firstName, 
                phone = phoneNumber, 
                role = userFromDash.primaryRole,
                briefing = missionBriefing,
                photoData = profilePhotoData
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Trigger upload
            uploadPhoto(context, it, scope) { success, message, imageUrl ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (success && !imageUrl.isNullOrEmpty()) {
                    profilePhotoData = imageUrl
                    prefs.saveUserInfo(
                        userId = prefs.getUserId(),
                        email = email,
                        name = firstName,
                        phone = phoneNumber,
                        role = prefs.getUserRole(),
                        briefing = missionBriefing,
                        photoData = imageUrl
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Profile Photo Section
        val initials = firstName.split(" ").filter { it.isNotEmpty() }.mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        var imageError by remember { mutableStateOf(false) }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!imageError) {
                    val photoModel = when {
                        selectedImageUri != null -> selectedImageUri
                        !profilePhotoData.isNullOrEmpty() -> {
                            if (profilePhotoData!!.startsWith("http") || profilePhotoData!!.startsWith("data:")) profilePhotoData!! else "${RetrofitClient.BASE_URL}$profilePhotoData"
                        }
                        else -> "${RetrofitClient.BASE_URL}api/users/profile-photo/me"
                    }
                    val imageRequest = remember(photoModel, prefs.getToken()) {
                        val builder = coil.request.ImageRequest.Builder(context)
                            .data(photoModel)
                            .crossfade(true)
                        if (photoModel is String && prefs.getToken() != null) {
                            builder.addHeader("Authorization", "Bearer ${prefs.getToken()}")
                        }
                        builder.build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { 
                            imageError = true 
                        },
                        onSuccess = { imageError = false }
                    )
                }
                
                if (imageError || initials.isEmpty()) {
                    Text(
                        text = initials.ifEmpty { "G" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FilledIconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info Cards
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            enabled = !isUpdating
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { /* Email usually locked */ },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            enabled = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            enabled = !isUpdating
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isUpdating = true
                scope.launch {
                    try {
                        val response = RetrofitClient.instance.updateProfile(
                            ProfileUpdateRequest(firstName, phoneNumber)
                        )
                        if (response.isSuccessful && response.body()?.success == true) {
                            prefs.saveUserInfo(
                                userId = prefs.getUserId(),
                                email = email,
                                name = firstName,
                                phone = phoneNumber,
                                role = prefs.getUserRole(),
                                briefing = missionBriefing,
                                photoData = profilePhotoData
                            )
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Update failed: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUpdating = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUpdating
        ) {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("SAVE CHANGES")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(16.dp))
        
        var showPasswordDialog by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CHANGE PASSWORD")
        }

        if (showPasswordDialog) {
            ChangePasswordDialog(onDismiss = { showPasswordDialog = false })
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Strategic Relationship", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MISSION BRIEFING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = missionBriefing,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                RelationshipStatItem(
                    "Account Status", 
                    userFromDash?.primaryRole?.uppercase() ?: "CLIENT", 
                    Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                RelationshipStatItem(
                    "Total Projects", 
                    "${dashboardData?.dashboard?.businessSummary?.activeProjects ?: 0} Active", 
                    Icons.Default.History
                )
                Spacer(modifier = Modifier.height(12.dp))
                RelationshipStatItem(
                    "Financial Standing", 
                    if ((dashboardData?.dashboard?.businessSummary?.openInvoices ?: 0) > 0) "Action Required" else "Good Standing", 
                    Icons.Default.AccountBalanceWallet
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun RelationshipStatItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun uploadPhoto(
    context: android.content.Context,
    uri: Uri,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (Boolean, String, String?) -> Unit
) {
    scope.launch {
        try {
            val file = getFileFromUri(context, uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            
            val response = RetrofitClient.instance.uploadProfilePhoto(body)
            if (response.isSuccessful && response.body()?.success == true) {
                onResult(true, "Photo uploaded successfully", response.body()?.imageUrl)
            } else {
                onResult(false, "Upload failed: ${response.body()?.message}", null)
            }
        } catch (e: Exception) {
            onResult(false, "Error: ${e.localizedMessage}", null)
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val file = File(context.cacheDir, "profile_upload.jpg")
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }
    return file
}
