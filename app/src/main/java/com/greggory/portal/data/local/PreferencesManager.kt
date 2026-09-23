package com.greggory.portal.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager private constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "GreggoryPrefsEncrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(
            "GreggoryPrefsSecure",
            Context.MODE_PRIVATE
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).commit()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove("auth_token").remove("fcm_token").commit()
    }

    fun clear() {
        clearToken()
        sharedPreferences.edit().clear().commit()
    }

    fun saveUserInfo(userId: Int, email: String, name: String, phone: String? = null, role: String? = "user", briefing: String? = null, photoData: String? = null) {
        sharedPreferences.edit().apply {
            putInt("user_id", userId)
            putString("user_email", email)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_role", role)
            putString("user_briefing", briefing)
            putString("user_photo_data", photoData)
        }.commit()
    }

    fun getUserId(): Int = sharedPreferences.getInt("user_id", -1)
    fun getUserEmail(): String? = sharedPreferences.getString("user_email", null)
    fun getUserName(): String? = sharedPreferences.getString("user_name", null)
    fun getUserPhone(): String? = sharedPreferences.getString("user_phone", null)
    fun getUserRole(): String? = sharedPreferences.getString("user_role", "user")
    fun getUserBriefing(): String? = sharedPreferences.getString("user_briefing", null)
    fun getUserPhotoData(): String? = sharedPreferences.getString("user_photo_data", null)

    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }

    fun getFcmToken(): String? = sharedPreferences.getString("fcm_token", null)

    fun saveBackgroundType(type: String) {
        sharedPreferences.edit().putString("bg_type", type).apply()
    }

    fun getBackgroundType(): String = sharedPreferences.getString("bg_type", "color") ?: "color"

    fun saveBackgroundUri(uri: String) {
        sharedPreferences.edit().putString("bg_uri", uri).apply()
    }

    fun getBackgroundUri(): String? = sharedPreferences.getString("bg_uri", null)

    fun saveBackgroundSource(source: String) {
        sharedPreferences.edit().putString("bg_source", source).apply()
    }

    fun getBackgroundSource(): String = sharedPreferences.getString("bg_source", "none") ?: "none"

    // New Settings
    fun saveThemeMode(mode: String) { sharedPreferences.edit().putString("theme_mode", mode).apply() }
    fun getThemeMode(): String = sharedPreferences.getString("theme_mode", "system") ?: "system"

    fun saveBiometricEnabled(enabled: Boolean) { sharedPreferences.edit().putBoolean("biometric_enabled", enabled).apply() }
    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)

    fun saveNotificationPref(key: String, enabled: Boolean) { sharedPreferences.edit().putBoolean("notif_$key", enabled).apply() }
    fun getNotificationPref(key: String): Boolean = sharedPreferences.getBoolean("notif_$key", true)

    fun saveCurrency(currency: String) { sharedPreferences.edit().putString("currency", currency).apply() }
    fun getCurrency(): String = sharedPreferences.getString("currency", "KSH") ?: "KSH"

    fun saveFirstLaunchCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean("first_launch_completed", completed).apply()
    }
    fun isFirstLaunch(): Boolean = !sharedPreferences.getBoolean("first_launch_completed", false)

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
