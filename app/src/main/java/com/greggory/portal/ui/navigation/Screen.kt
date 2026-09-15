package com.greggory.portal.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Portal : Screen("portal")
    object Projects : Screen("projects")
    object Billing : Screen("billing")
    object Notifications : Screen("notifications")
    object ForgotPassword : Screen("forgot_password")
}
