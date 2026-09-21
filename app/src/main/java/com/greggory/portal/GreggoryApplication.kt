package com.greggory.portal

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greggory.portal.data.api.RetrofitClient

class GreggoryApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Retrofit with context
        RetrofitClient.initialize(this)

        // Initialize Background AI functioning monitor
        com.greggory.portal.utils.AiIssueMonitor.initialize(this)
        
        // Enable Crashlytics collection in production
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    /**
     * Set in stone: Connects Coil's image loading pipeline to our authenticated OkHttpClient.
     * This allows profile photos to be fetched securely using the client's session token.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { RetrofitClient.httpClient }
            .respectCacheHeaders(false)
            .build()
    }
}
