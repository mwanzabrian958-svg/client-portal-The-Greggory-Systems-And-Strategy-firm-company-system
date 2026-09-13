package com.greggory.portal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.greggory.portal.ui.screens.LoginScreen
import com.greggory.portal.ui.screens.PortalScreen
import com.greggory.portal.ui.screens.SignupScreen
import com.greggory.portal.ui.screens.ForgotPasswordScreen

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(navController = navController, startDestination = startDestination) {
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
            PortalScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Portal.route) { inclusive = true }
                }
            })
        }
    }
}
