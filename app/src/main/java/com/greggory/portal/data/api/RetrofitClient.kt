package com.greggory.portal.data.api

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Live Production Backend - Wired to Render Cloud & Aiven MySQL
    // NOTE: only the w-the-greggory-systems-and-strategy-firm-1vf9 host is live
    // (it matches the website sitemap and serves /api/auth/whatsapp). The old
    // jz7i / vik4 services 404 every route — that dead host is what broke login.
    // Base URL comes from BuildConfig: debug -> local backend (10.0.2.2:10000),
    // release -> Render cloud (HTTPS). Keeps one source of truth per build type.
    val BASE_URL: String = com.greggory.portal.BuildConfig.BASE_URL

    private var appContext: Context? = null

    /**
     * Set in stone: Initializes the global network client.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Always dynamically get the singleton instance using the application context
        val context = appContext
        val (token, userId) = if (context != null) {
            val prefs = com.greggory.portal.data.local.PreferencesManager.getInstance(context)
            Pair(prefs.getToken(), prefs.getUserId())
        } else {
            Pair(null, -1)
        }
        
        val requestBuilder = originalRequest.newBuilder()
        
        // Identification header for the "Company Pipeline" load balancer
        requestBuilder.header("User-Agent", "GreggoryClientPortal/1.0.0 (Android; " + android.os.Build.VERSION.RELEASE + ")")
        
        // Always use the latest token from PreferencesManager dynamically
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        // Inject Set in Stone Routing Headers (Critical for Load Balancing & Data Partitioning)
        if (userId > 0) {
            requestBuilder.header("X-Greggory-Client-ID", userId.toString())
        }
        requestBuilder.header("X-Routing-Policy", "set-in-stone-v1")
        
        chain.proceed(requestBuilder.build())
    }

    /**
     * Retry Interceptor: Handles transient "Company Pipeline" glitches (502, 503, 504),
     * and triggers automatic session expulsion redirects on 401 token authentication rejections.
     *
     * Per-device independence rule: a 401 from an AUTH endpoint (login / register /
     * forgot-password / whatsapp request-code / verify-code) means "wrong credentials",
     * NOT "session expired" — it must NEVER wipe tokens or Room caches. Only a 401
     * from an AUTHENTICATED endpoint (with a Bearer token attached) triggers the
     * nuclear wipe + logout redirect. This is what lets every downloaded app work
     * independently: bad login on one phone can't nuke another session, and each
     * device keeps only its own userId silo.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response = chain.proceed(request)

        if (response.code == 401) {
            val path = request.url.encodedPath.lowercase()
            val isAuthEndpoint = path.contains("/login") ||
                path.contains("/register") ||
                path.contains("/forgot-password") ||
                path.contains("/request-code") ||
                path.contains("/verify-code") ||
                path.contains("/whatsapp/status") ||
                path.contains("/notify/status")
            // Only expel when the device actually holds a session token.
            // NOTE: retryInterceptor runs BEFORE authInterceptor, so the request
            // object here does NOT have the Authorization header yet — read the
            // token straight from encrypted prefs instead.
            val heldToken: String? = try {
                appContext?.let {
                    com.greggory.portal.data.local.PreferencesManager.getInstance(it).getToken()
                }
            } catch (ignored: Exception) { null }
            val hadToken = !heldToken.isNullOrEmpty()

            if (isAuthEndpoint || !hadToken) {
                // Wrong password / unknown account / cold-start probe — show the
                // server error message, keep local cache + token untouched.
                android.util.Log.d("SESSION_GUARD", "401 on public/auth endpoint $path — no wipe")
                return@Interceptor response
            }
            // Expelled session: Wipe auth tokens and redirect instantly
            appContext?.let { ctx ->
                val prefs = com.greggory.portal.data.local.PreferencesManager.getInstance(ctx)
                prefs.clearToken()
                prefs.clear()
                
                // --- TOKEN PURGE: Clear all auth tokens in local Room database ---
                val db = com.greggory.portal.data.local.AppDatabase.getDatabase(ctx)
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        db.userDao().purgeAllTokens()
                        db.projectDao().clearProjects()
                        db.invoiceDao().clearInvoices()
                        db.reportDao().clearReports()
                        android.util.Log.d("NUCLEAR_WIPE", "Auth tokens and active cache purged on 401 expulsion")
                    } catch (e: Exception) {
                        android.util.Log.e("NUCLEAR_WIPE", "Purge failed during 401", e)
                    }
                }
            }
            com.greggory.portal.utils.SessionEventBus.triggerUnauthorizedLogout()
            return@Interceptor response
        }

        var tryCount = 0
        val maxLimit = 3

        while (!response.isSuccessful && (response.code in 502..504) && tryCount < maxLimit) {
            tryCount++
            Thread.sleep(1000L * tryCount)
            response.close()
            response = chain.proceed(request)
        }
        response
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (com.greggory.portal.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // SSL Pinning: Using DEFAULT system trust store to prevent certificate rotation failures on cloud hosts
    private val certificatePinner = CertificatePinner.DEFAULT

    internal val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        // Order matters: auth headers must be attached BEFORE the session-guard
        // sees the request, so the 401 handler can tell "logged-in call expired"
        // apart from "wrong password on login screen".
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(logging)
        .certificatePinner(certificatePinner)
        .build()

    /**
     * Lightweight pipeline probe — call before login/dashboard sync to tell the
     * user "server waking up / down" instead of a generic connection error.
     * Returns true when the Render backend + Aiven DB answer.
     */
    suspend fun isBackendReachable(): Boolean {
        return try {
            val url = BASE_URL.trimEnd('/') + "/api/auth/whatsapp/status"
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("X-Routing-Policy", "set-in-stone-v1")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                // Any real HTTP answer (even 401/429) = pipeline alive.
                // 502/503/504 = Render asleep or crashed. Exception = no route.
                resp.code !in listOf(502, 503, 504)
            }
        } catch (ignored: Exception) {
            false
        }
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
