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
import com.greggory.portal.data.api.ChannelStatusResponse
import com.greggory.portal.data.api.OtpRequest
import com.greggory.portal.data.api.OtpVerify
import com.greggory.portal.data.api.OtpVerifyResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Resend cooldown ticker (60s window)
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        }
    }

    // Success → hold confirmation briefly, then return to Login
    LaunchedEffect(verified) {
        if (verified) {
            delay(1500)
            onBackToLogin()
        }
    }

    // Channel status health probe
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.channelStatus()
            val body: ChannelStatusResponse? = response.body()
            if (response.isSuccessful && body?.success == true) {
                val whatsappConfigured = body.whatsapp?.configured == true
                val smsConfigured = body.sms?.configured == true
                if (!whatsappConfigured && !smsConfigured) {
                    deliveryNote = "No primary OTP delivery channels (WhatsApp/SMS) are fully configured on the server."
                }
            }
        } catch (ignored: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Channel Verification") },
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
                text = "Verify Your Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email or phone number. We'll send a 6-digit verification code through the active delivery channel.",
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
                                            val response = RetrofitClient.instance.requestCode(
                                                OtpRequest(cleanIdentifier)
                                            )
                                            isSending = false
                                            if (response.isSuccessful && response.body()?.success == true) {
                                                val body = response.body()!!
                                                message = body.message.ifEmpty { "Code resent successfully." }
                                                isError = false
                                                resendCooldown = 60
                                            } else {
                                                if (response.code() == 429) {
                                                    message = "Too many requests. Please wait before trying again."
                                                } else if (response.code() == 502) {
                                                    message = "Could not send the code, please try again."
                                                } else {
                                                    message = "Could not resend the code. Please try again."
                                                }
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
                        if (cleanIdentifier.isNotEmpty()) {
                            isSending = true
                            message = null
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.requestCode(
                                        OtpRequest(cleanIdentifier)
                                    )
                                    isSending = false
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val body = response.body()!!
                                        val provider = body.provider ?: "whatsapp"
                                        val announcement = when {
                                            provider.contains("sms", ignoreCase = true) -> "Code sent by SMS"
                                            provider.contains("voice", ignoreCase = true) -> "You will receive a phone call reading your code"
                                            provider.contains("email", ignoreCase = true) -> "Check your email for the verification code"
                                            else -> "Check WhatsApp for the verification code"
                                        }
                                        message = "$announcement (Expires in ${body.expiresInMinutes}m)"
                                        isError = false
                                        codeSent = true
                                        resendCooldown = 60
                                    } else {
                                        if (response.code() == 429) {
                                            message = "Too many requests. Please wait before trying again."
                                        } else if (response.code() == 502) {
                                            message = "Could not send the code, please try again."
                                        } else {
                                            message = "Could not send code. Please check your identifier."
                                        }
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
                        if (code.length == 6) {
                            isVerifying = true
                            message = null
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.verifyCode(
                                        OtpVerify(cleanIdentifier, code)
                                    )
                                    isVerifying = false
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        message = response.body()?.message ?: "Verification successful!"
                                        isError = false
                                        verified = true
                                    } else {
                                        message = "Invalid or expired code. Please check and try again."
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    isVerifying = false
                                    message = "Verification error: ${e.localizedMessage}"
                                    isError = true
                                }
                            }
                        } else {
                            message = "Please enter the 6-digit code"
                            isError = true
                        }
                    }
                },
                enabled = !isSending && !isVerifying && !verified,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSending || isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (!codeSent) "SEND CODE" else "VERIFY CODE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
