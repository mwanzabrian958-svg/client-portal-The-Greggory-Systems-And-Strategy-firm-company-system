package com.greggory.portal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.WhatsAppAuthStatusResponse
import com.greggory.portal.data.api.WhatsAppCodeRequest
import com.greggory.portal.data.api.WhatsAppCodeResponse
import com.greggory.portal.data.api.WhatsAppVerifyRequest
import com.greggory.portal.data.api.WhatsAppVerifyResponse
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview
import com.greggory.portal.ui.theme.GreggoryPortalTheme

/**
 * WhatsApp OTP verification — wired to the backend auth-code pipeline at
 * /api/auth/whatsapp (routes/whatsappAuth.js):
 *
 *   1. request-code { identifier }  -> a 6-digit code is sent to the client's
 *      WhatsApp number on file. The response is intentionally GENERIC (anti
 *      enumeration), so always show the returned message verbatim.
 *   2. verify-code { identifier, code } -> burns the code and flips
 *      users.whatsapp_verified. It never issues a session — the password
 *      login still gates the portal.
 *
 * Mirrors the server's abuse controls: a 60s per-identifier resend cooldown
 * and 6-digit-only codes with a 10-minute TTL / 5-attempt cap server-side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(onBackToLogin: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) }
    var isSending by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verified by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var deliveryNote by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Resend cooldown ticker (server enforces the same 60s window).
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        }
    }

    // Success → hold the confirmation briefly, then return to Login.
    LaunchedEffect(verified) {
        if (verified) {
            delay(1500)
            onBackToLogin()
        }
    }

    // Provider health probe: warn up front if WhatsApp delivery is not
    // configured server-side, so clients don't wait for a code that can
    // never arrive. Failures here are non-blocking.
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getWhatsAppAuthStatus()
            val body: WhatsAppAuthStatusResponse? = response.body()
            if (response.isSuccessful && body?.success == true && body.configured == false) {
                deliveryNote = "WhatsApp delivery is not configured on the server yet — codes cannot be sent."
            }
        } catch (ignored: Exception) { /* offline: the request itself will surface errors */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Verification") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verify your WhatsApp",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the email or phone number registered to your portal account. We'll send a 6-digit verification code to the WhatsApp number on file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Email or Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSending && !isVerifying && !verified
            )

            if (codeSent) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        // Server only accepts exactly 6 digits — enforce it here.
                        if (input.all { it.isDigit() } && input.length <= 6) code = input
                    },
                    label = { Text("6-Digit Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isVerifying && !verified,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Didn't get it?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            if (resendCooldown == 0 && !isSending) {
                                val cleanIdentifier = identifier.trim()
                                if (cleanIdentifier.isNotEmpty()) {
                                    isSending = true
                                    message = null
                                    scope.launch {
                                        try {
                                            val response = RetrofitClient.instance.requestWhatsAppCode(
                                                WhatsAppCodeRequest(cleanIdentifier)
                                            )
                                            isSending = false
                                            if (response.isSuccessful && response.body()?.success == true) {
                                                message = response.body()?.message ?: "If an account exists, a code has been sent via WhatsApp."
                                                isError = false
                                                resendCooldown = 60
                                            } else {
                                                message = parseErrorMessage(response.errorBody()?.string())
                                                    ?: "Could not resend the code. Please try again."
                                                isError = true
                                            }
                                        } catch (e: Exception) {
                                            isSending = false
                                            message = "Network error: ${e.localizedMessage}"
                                            isError = true
                                        }
                                    }
                                } else {
                                    message = "Please enter your email or phone first"
                                    isError = true
                                }
                            }
                        },
                        enabled = resendCooldown == 0 && !isSending && !isVerifying
                    ) {
                        Text("Resend Code", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            deliveryNote?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2A9D8F),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val cleanIdentifier = identifier.trim()
                    if (!codeSent) {
                        // ── Step 1: request the code ──
                        if (cleanIdentifier.isNotEmpty()) {
                            isSending = true
                            message = null
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.requestWhatsAppCode(
                                        WhatsAppCodeRequest(cleanIdentifier)
                                    )
                                    isSending = false
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val body = response.body()!!
                                        // Generic anti-enumeration message — always shown as-is.
                                        message = body.message
                                            ?: "If an account exists for $cleanIdentifier, a code has been sent via WhatsApp."
                                        isError = false
                                        codeSent = true
                                        resendCooldown = 60
                                        // Dev-only simulated echo (never present against production).
                                        if (!body.code.isNullOrEmpty()) code = body.code
                                    } else {
                                        message = parseErrorMessage(response.errorBody()?.string())
                                            ?: "Could not send the code. Please try again."
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    isSending = false
                                    message = "Network error: ${e.localizedMessage}"
                                    isError = true
                                }
                            }
                        } else {
                            message = "Please enter your email or phone"
                            isError = true
                        }
                    } else {
                        // ── Step 2: verify the code ──
                        if (code.length == 6) {
                            isVerifying = true
                            message = null
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.verifyWhatsAppCode(
                                        WhatsAppVerifyRequest(cleanIdentifier, code)
                                    )
                                    isVerifying = false
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        message = response.body()?.message ?: "Your WhatsApp number has been verified."
                                        isError = false
                                        verified = true
                                    } else {
                                        message = parseErrorMessage(response.errorBody()?.string())
                                            ?: "Invalid or expired code. Please request a new one."
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    isVerifying = false
                                    message = "Network error: ${e.localizedMessage}"
                                    isError = true
                                }
                            }
                        } else {
                            message = "Enter the full 6-digit code"
                            isError = true
                        }
                    }
                },
                enabled = !isSending && !isVerifying && !verified,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSending || isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (!codeSent) "SEND CODE" else "VERIFY CODE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Pulls { success, message } error bodies (400/429/500/502) into a readable string. */
private fun parseErrorMessage(errorBody: String?): String? {
    val gson = Gson()
    val codeMsg = try {
        gson.fromJson(errorBody, WhatsAppCodeResponse::class.java)?.message
    } catch (e: Exception) { null }
    if (codeMsg != null) return codeMsg
    return try {
        gson.fromJson(errorBody, WhatsAppVerifyResponse::class.java)?.message
    } catch (e: Exception) { null }
}

@Preview(showBackground = true)
@Composable
fun OtpVerificationScreenPreview() {
    GreggoryPortalTheme {
        OtpVerificationScreen(onBackToLogin = {})
    }
}
