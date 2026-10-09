package com.greggory.portal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.ui.navigation.AppNavigation
import com.greggory.portal.ui.navigation.Screen
import com.greggory.portal.ui.theme.GreggoryPortalTheme
import com.greggory.portal.utils.BiometricHelper
import com.greggory.portal.utils.NotificationHelper

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled by system
    }

    private lateinit var prefs: PreferencesManager
    private val themeModeState = mutableStateOf("system")

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "theme_mode") {
            themeModeState.value = sharedPreferences.getString("theme_mode", "system") ?: "system"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        prefs = PreferencesManager.getInstance(this)
        themeModeState.value = prefs.getThemeMode()
        prefs.registerListener(prefListener)

        // ── Unique-account session gate (parity with the website's AuthContext):
        //    a session is authenticated ONLY when a real token for a real user
        //    exists. Older builds injected a hardcoded demo identity here
        //    (user id 1 "Brian Mwanza" / token gf_lock_offline_demo_token_999),
        //    which let the portal open as someone who never logged in — purge
        //    it so every device shows the account that actually signed in.
        if (prefs.getToken() == "gf_lock_offline_demo_token_999") {
            prefs.clear()
        }
        
        NotificationHelper.requestPermission(this, requestPermissionLauncher)
        
        setContent {
            val systemInDarkTheme = isSystemInDarkTheme()
            
            val themeMode by remember { themeModeState }
            
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemInDarkTheme
            }

            // Portal only for an existing signed-in account; first run shows
            // onboarding, everyone else lands on Login (same rule as the
            // website portal's PrivateRoute: no token -> /login).
            val startDestination = when {
                !prefs.getToken().isNullOrEmpty() && prefs.getUserId() > 0 -> Screen.Portal.route
                prefs.isFirstLaunch() -> Screen.Onboarding.route
                else -> Screen.Login.route
            }

            GreggoryPortalTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController, startDestination = startDestination)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterListener(prefListener)
    }
}
