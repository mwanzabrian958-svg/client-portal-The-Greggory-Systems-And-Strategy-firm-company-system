package com.greggory.portal.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Live Production Backend - Wired to Render Cloud & Aiven MySQL
    const val BASE_URL = "https://w-the-greggory-systems-and-strategy-firm-vik4.onrender.com/"

    private var authTokenProvider: (() -> String?)? = null

    /**
     * Set in stone: Initializes the global network client with a dynamic token provider.
     * This guarantees that every request automatically carries the DB-generated token,
     * ensuring uniform routing of client data across any device or access point.
     */
    fun initialize(context: Context) {
        val prefs = com.greggory.portal.data.local.PreferencesManager(context.applicationContext)
        authTokenProvider = { prefs.getToken() }
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = authTokenProvider?.invoke()
        
        val newRequest = if (!token.isNullOrEmpty() && originalRequest.header("Authorization") == null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // SSL Pinning for "Set in Stone" Security
    // NOTE: Before production deployment, update the hash below with the actual SHA-256 of your certificate.
    // You can get this by running: 
    // openssl s_client -connect w-the-greggory-systems-and-strategy-firm-vik4.onrender.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
    private val certificatePinner = okhttp3.CertificatePinner.Builder()
        .add("w-the-greggory-systems-and-strategy-firm-vik4.onrender.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build()

    private val httpClient = OkHttpClient.Builder()
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
