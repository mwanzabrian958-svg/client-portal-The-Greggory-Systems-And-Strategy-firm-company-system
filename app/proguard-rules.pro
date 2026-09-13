# The Greggory Client Portal - ProGuard Rules

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Gson (keeps data models from being obfuscated so JSON mapping works)
# Keep only the data models, allow obfuscation of logic/clients
-keepclassmembers class com.greggory.portal.data.api.** {
    <fields>;
}
-keep class com.greggory.portal.data.api.*Request
-keep class com.greggory.portal.data.api.*Response
-keep class com.greggory.portal.data.api.UserInfo
-keep class com.greggory.portal.data.api.Project
-keep class com.greggory.portal.data.api.Invoice
-keep class com.greggory.portal.data.api.KpiMetric
-keep class com.greggory.portal.data.api.Notification
-keep class com.greggory.portal.data.api.Report
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements com.google.gson.TypeAdapterFactory
-keep public class * implements com.google.gson.JsonSerializer
-keep public class * implements com.google.gson.JsonDeserializer

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class com.greggory.portal.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Firebase & Crashlytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Coil (Image Loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Jetpack Compose
-keep class androidx.compose.material.icons.** { *; }

# "Set in Stone" Routing Logic - HIGHEST PROTECTION
# We keep the class name for internal reference but obfuscate all internal logic
-keep class com.greggory.portal.utils.DataRouter {
    public static ** getSecureHeaders(...);
    public static boolean verifyRoutingIntegrity(...);
}
-keepclassmembernames class com.greggory.portal.utils.DataRouter {
    <methods>;
}

# General Android
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keep class androidx.core.app.CoreComponentFactory
