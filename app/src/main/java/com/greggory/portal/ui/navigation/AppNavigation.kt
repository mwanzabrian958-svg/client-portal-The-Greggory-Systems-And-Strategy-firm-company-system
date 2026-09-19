package com.greggory.portal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.greggory.portal.ui.screens.LoginScreen
import com.greggory.portal.ui.screens.OnboardingScreen
import com.greggory.portal.ui.screens.PortalScreen
import com.greggory.portal.ui.screens.SignupScreen
import com.greggory.portal.ui.screens.ForgotPasswordScreen
import com.greggory.portal.ui.screens.PdfViewerScreen
import com.greggory.portal.ui.screens.ProjectDetailsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Portal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Portal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Portal.route) {
            PortalScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Portal.route) { inclusive = true }
                    }
                },
                onViewPdf = { url, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(java.net.URLEncoder.encode(url, "UTF-8"), title))
                },
                onViewProject = { projectId ->
                    // Navigation handled via state inside PortalScreen for instant data availability
                }
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Document"
            PdfViewerScreen(
                url = java.net.URLDecoder.decode(url, "UTF-8"),
                title = title,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
