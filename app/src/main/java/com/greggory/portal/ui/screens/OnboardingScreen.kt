package com.greggory.portal.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greggory.portal.data.local.PreferencesManager
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "STRATEGIC COMMAND",
                description = "Welcome to Your Enterprise Hub. Real-time tracking for premium delivery metrics, active workflows, and full corporate oversight parameters.",
                icon = Icons.Default.Star,
                color = Color(0xFF002D62)
            ),
            OnboardingPageData(
                title = "REAL-TIME TELEMETRY",
                description = "Stay informed instantly. We use push notifications to deliver secure milestone metrics, change request confirmations, and live pipeline sync updates.",
                icon = Icons.Default.NotificationsActive,
                color = Color(0xFF2A9D8F)
            ),
            OnboardingPageData(
                title = "SECURE DATA VAULT",
                description = "Your strategy artifacts are completely locked. Experience military-grade endpoint isolation with hardware biometrics and securely structured storage schemes.",
                icon = Icons.Default.Security,
                color = Color(0xFF1D3557)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Intent Permission Launcher for API 33+ Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Callback handled gracefully; proceed to the final destination regardless
        PreferencesManager.getInstance(context).saveFirstLaunchCompleted(true)
        onOnboardingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(page.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = page.color
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = page.color,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Bottom interactive overlay components
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (active) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) pages[pagerState.currentPage].color 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // CTA Navigation Action Trigger Button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        // Request Runtime Push Notifications Permission natively for Android 13+ (API 33)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PreferencesManager.getInstance(context).saveFirstLaunchCompleted(true)
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].color
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "AUTHORIZE & ENTER" else "CONTINUE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
