package com.greggory.portal.utils

import android.content.Context
import android.util.Log
import com.greggory.portal.data.api.FeedbackRequest
import com.greggory.portal.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

object AiIssueMonitor {
    private const val TAG = "AiIssueMonitor"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var isConfigured = false

    /**
     * Initializes the background AI-driven functioning monitor.
     * Hooks into the global thread exception handler to intercept crashes as they occur.
     */
    fun initialize(context: Context) {
        if (isConfigured) return
        isConfigured = true

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Intercept and handle via AI priority heuristics
            processAndReportCrash(throwable)

            // Let the standard framework handler take over so crash reporting (Firebase/OS) functions normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Public method to allow developers or ViewModels to report caught non-fatal operations/errors
     * to the feedback loop automatically.
     */
    fun logHandledIssue(title: String, exception: Throwable, extraContext: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                val fullTrace = sw.toString()

                val messageText = buildString {
                    appendLine("Context: $extraContext")
                    appendLine("Exception Message: ${exception.localizedMessage}")
                    appendLine("Stack Trace:")
                    append(fullTrace)
                }

                val priority = evaluatePriority(exception, fullTrace)
                sendFeedbackToServer(title, messageText, priority)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to compile handled issue feedback", e)
            }
        }
    }

    private fun processAndReportCrash(throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val title = "Automated AI Crash Alert: ${throwable.javaClass.simpleName}"
            val messageText = buildString {
                appendLine("An unhandled exception occurred in the application background pipeline.")
                appendLine("Exception Class: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.localizedMessage}")
                appendLine("Full Stack Trace:")
                append(stackTrace)
            }

            val priority = evaluatePriority(throwable, stackTrace)

            // Run on a blocking or immediate thread pool since the application process might exit shortly
            val job = CoroutineScope(Dispatchers.IO).launch {
                sendFeedbackToServer(title, messageText, priority)
            }
            // Give it a brief moment to dispatch the network call before the VM completely halts
            Thread.sleep(800)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging automated background crash report", e)
        }
    }

    /**
     * Autonomous Heuristics Engine (AI Pattern Matcher):
     * Dynamically determines severity and priority metrics based on operational criteria.
     */
    private fun evaluatePriority(throwable: Throwable, stackTrace: String): String {
        val className = throwable.javaClass.name.lowercase()
        val traceLower = stackTrace.lowercase()

        return when {
            // High Priority: Data leaks, security issues, null-pointers in core business layers, or database corruption
            className.contains("nullpointerexception") ||
            className.contains("sqliteexception") ||
            className.contains("securityexception") ||
            traceLower.contains("preferencesmanager") ||
            traceLower.contains("retrofitclient") -> "high"

            // Medium Priority: Network timeouts, parsing mismatches, connectivity drops
            className.contains("timeoutexception") ||
            className.contains("unknownhostexception") ||
            className.contains("ioexception") ||
            className.contains("jsonparseexception") -> "medium"

            // Low Priority: UI state anomalies, view drawing glitches, generic caught exceptions
            else -> "low"
        }
    }

    private suspend fun sendFeedbackToServer(title: String, message: String, priority: String) {
        try {
            val trimmedMessage = if (message.length > 2000) message.take(2000) + "\n[Truncated due to length]" else message
            val request = FeedbackRequest(
                title = title,
                message = trimmedMessage,
                type = "bug",
                rating = 1,
                priority = priority
            )
            val response = RetrofitClient.instance.submitFeedback(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Background AI issue report dispatched successfully. Priority: $priority")
            } else {
                Log.w(TAG, "Background monitor server rejected report: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transmit automated feedback to the central firm pipeline", e)
        }
    }
}
