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

    private var authTokenProvider: (() -> String?)? = null
    private var userIdProvider: (() -> Int)? = null

    /**
     * Set in stone: Initializes the global network client with a dynamic token provider.
     * This guarantees that every request automatically carries the DB-generated token,
     * ensuring uniform routing of client data across any device or access point.
     */
    fun initialize(context: Context) {
        val prefs = com.greggory.portal.data.local.PreferencesManager(context.applicationContext)
        authTokenProvider = { prefs.getToken() }
        userIdProvider = { prefs.getUserId() }
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = authTokenProvider?.invoke()
        val userId = userIdProvider?.invoke() ?: -1
        
        val requestBuilder = originalRequest.newBuilder()
        
        if (!token.isNullOrEmpty() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        // Inject Set in Stone Routing Headers
        requestBuilder.header("X-Greggory-Client-ID", userId.toString())
        requestBuilder.header("X-Routing-Policy", "set-in-stone-v1")
        
        chain.proceed(requestBuilder.build())
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
