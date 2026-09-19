package com.greggory.portal.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Live Production Backend - Wired to Render Cloud & Aiven MySQL
    const val BASE_URL = "https://the-greggory-systems-and-strategy-firm-jz7i.onrender.com/"

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
        requestBuilder.header("X-Greggory-Client-ID", userId.toString())
        requestBuilder.header("X-Routing-Policy", "set-in-stone-v1")
        
        chain.proceed(requestBuilder.build())
    }

    /**
     * Retry Interceptor: Handles transient "Company Pipeline" glitches (502, 503, 504),
     * and triggers automatic session expulsion redirects on 401 token authentication rejections.
     */
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response = chain.proceed(request)
        
        if (response.code == 401) {
            // Expelled session: Wipe state and redirect instantly
            appContext?.let { ctx ->
                com.greggory.portal.data.local.PreferencesManager.getInstance(ctx).clear()
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

    // SSL Pinning for "Set in Stone" Security
    // IMPORTANT: To prevent crashes, this is only active in RELEASE builds.
    private val certificatePinner = if (!com.greggory.portal.BuildConfig.DEBUG) {
        okhttp3.CertificatePinner.Builder()
            .add("the-greggory-systems-and-strategy-firm-jz7i.onrender.com", "sha256/fizfE9JVlzlRplEx7epXfqW9enrbLvwF/LU26XTPEG4=")
            .build()
    } else {
        okhttp3.CertificatePinner.DEFAULT
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(retryInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .certificatePinner(certificatePinner)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
