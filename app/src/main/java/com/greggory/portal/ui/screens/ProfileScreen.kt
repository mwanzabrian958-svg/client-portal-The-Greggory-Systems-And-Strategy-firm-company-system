package com.greggory.portal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.greggory.portal.data.api.ProfileUpdateRequest
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.local.PreferencesManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    
    var firstName by remember { mutableStateOf(prefs.getUserName() ?: "") }
    var email by remember { mutableStateOf(prefs.getUserEmail() ?: "") }
    var phoneNumber by remember { mutableStateOf(prefs.getUserPhone() ?: "") }
    var isUpdating by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Trigger upload
            uploadPhoto(context, it, scope) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = if (selectedImageUri != null) {
                    rememberAsyncImagePainter(selectedImageUri)
                } else {
                    rememberAsyncImagePainter("https://w-the-greggory-systems-and-strategy-firm-vik4.onrender.com/api/users/profile-photo/me")
                },
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
            FilledIconButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.size(36.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info Cards
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
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
                            prefs.saveUserInfo(prefs.getUserId(), email, firstName, phoneNumber)
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

        Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = currentPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword && !isSubmitting,
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.instance.changePassword(com.greggory.portal.data.api.ChangePasswordRequest(currentPassword, newPassword))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed: ${response.body()?.message ?: "Check current password"}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            ) { Text("UPDATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun uploadPhoto(
    context: android.content.Context,
    uri: Uri,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (Boolean, String) -> Unit
) {
    scope.launch {
        try {
            val file = getFileFromUri(context, uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            
            val response = RetrofitClient.instance.uploadProfilePhoto(body)
            if (response.isSuccessful && response.body()?.success == true) {
                onResult(true, "Photo uploaded successfully")
            } else {
                onResult(false, "Upload failed: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            onResult(false, "Error: ${e.localizedMessage}")
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
