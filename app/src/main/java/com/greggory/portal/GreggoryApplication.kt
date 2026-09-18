package com.greggory.portal

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

class GreggoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Retrofit with context
        com.greggory.portal.data.api.RetrofitClient.initialize(this)

        // Initialize Background AI functioning monitor
        com.greggory.portal.utils.AiIssueMonitor.initialize(this)
        
        // Enable Crashlytics collection in production
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
