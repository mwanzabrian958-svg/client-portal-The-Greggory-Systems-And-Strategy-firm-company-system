package com.greggory.portal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.tooling.preview.Preview
import com.greggory.portal.ui.theme.GreggoryPortalTheme

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.greggory.portal.R
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.LoginRequest
import com.greggory.portal.data.api.PushTokenRequest
import com.greggory.portal.data.api.LoginResponse
import com.greggory.portal.data.local.PreferencesManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "Greggory Logo",
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "THE GREGGORY",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Systems & Strategy Firm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email or Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { onNavigateToForgotPassword() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
            }
        }

        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                val cleanEmail = email.trim().lowercase()
                val cleanPassword = password.trim()
                if (cleanEmail.isNotEmpty() && cleanPassword.isNotEmpty()) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val response = RetrofitClient.instance.login(
                                LoginRequest(cleanEmail, cleanPassword)
                            )
                            isLoading = false
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                val token = body.token
                                if (token != null) {
                                    val prefs = PreferencesManager.getInstance(context)
                                    prefs.saveToken(token)
                                    // Backend returns user details at top level for login success
                                    prefs.saveUserInfo(
                                        body.id,
                                        body.email,
                                        body.firstName,
                                        body.phone ?: ""
                                    )
                                    
                                    // Trigger immediate re-init of Retrofit with the new token
                                    RetrofitClient.initialize(context)
                                    
                                    // Register FCM Token for Push Notifications
                                    try {
                                        @Suppress("DEPRECATION")
                                        val fcmToken = FirebaseMessaging.getInstance().token.await()
                                        prefs.saveFcmToken(fcmToken)
                                        RetrofitClient.instance.updatePushToken(
                                            PushTokenRequest(fcmToken)
                                        )
                                    } catch (e: Exception) {
                                        // Non-critical: failure to register token shouldn't block login
                                        android.util.Log.e("FCM", "Failed to register token on login", e)
                                    }

                                    onLoginSuccess()
                                } else {
                                    errorMessage = body.message ?: body.error ?: "Invalid response from server"
                                }
                            } else {
                                // Extract error message from body if possible
                                val errorBody = response.errorBody()?.string()
                                val errorMsg = try {
                                    val json = Gson().fromJson(errorBody, LoginResponse::class.java)
                                    json.error ?: json.message
                                } catch (e: Exception) {
                                    null
                                }
                                errorMessage = errorMsg ?: "Login failed: ${response.code()}"
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Connection error: ${e.localizedMessage}"
                        }
                    }
                } else {
                    errorMessage = "Please enter email and password"
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("LOG IN", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateToSignup() }) {
            Text("Don't have an account? Sign up", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    GreggoryPortalTheme {
        LoginScreen(onLoginSuccess = {}, onNavigateToSignup = {}, onNavigateToForgotPassword = {})
    }
}
