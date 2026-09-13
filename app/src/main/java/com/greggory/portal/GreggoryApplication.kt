package com.greggory.portal

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

class GreggoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Retrofit with context
        com.greggory.portal.data.api.RetrofitClient.initialize(this)
        
        // Enable Crashlytics collection in production
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
