package com.greggory.portal.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Portal : Screen("portal")
    object Projects : Screen("projects")
    object Billing : Screen("billing")
    object Notifications : Screen("notifications")
    object ForgotPassword : Screen("forgot_password")
    object Services : Screen("services")
    object Documents : Screen("documents")
    object Feedback : Screen("feedback")
    object Requests : Screen("requests")
    object PdfViewer : Screen("pdf_viewer?url={url}&title={title}") {
        fun createRoute(url: String, title: String) = "pdf_viewer?url=$url&title=$title"
    }
}
