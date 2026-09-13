package com.greggory.portal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.ui.navigation.AppNavigation
import com.greggory.portal.ui.theme.GreggoryPortalTheme
import com.greggory.portal.utils.BiometricHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val prefs = PreferencesManager(this)
        val hasToken = prefs.getToken() != null
        val startDestination = if (hasToken) "portal" else "login"

        setContent {
            GreggoryPortalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Show Biometric prompt if token exists and biometric is available
                    var isAuthenticated by remember { mutableStateOf(!hasToken) }
                    var finalStartDestination by remember { mutableStateOf(startDestination) }
                    
                    if (isAuthenticated) {
                        AppNavigation(navController = navController, startDestination = finalStartDestination)
                    } else {
                        // Routing Screen (Black/Brand background while biometric loads)
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    }

                    LaunchedEffect(Unit) {
                        if (hasToken && BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                            BiometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = {
                                    isAuthenticated = true
                                },
                                onError = { 
                                    // If biometric fails, clear token and force login for security
                                    prefs.clear()
                                    finalStartDestination = "login"
                                    isAuthenticated = true
                                }
                            )
                        } else {
                            isAuthenticated = true
                        }
                    }
                }
            }
        }
    }
}
