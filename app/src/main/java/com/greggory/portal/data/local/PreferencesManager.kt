@file:Suppress("DEPRECATION")

package com.greggory.portal.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager private constructor(context: Context) {
    @Suppress("DEPRECATION")
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "GreggoryPrefsEncrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // SECURITY (M1): the silent downgrade is gone — a broken encrypted
        // store is now LOUD (logcat + crash report), and the degraded path
        // holds secrets in memory ONLY (never plaintext on disk), forcing a
        // fresh login instead of persisting a stealable token.
        android.util.Log.e("PreferencesManager", "EncryptedSharedPreferences unavailable — token persistence DISABLED, session is memory-only", e)
        InMemoryFallback(context.getSharedPreferences("GreggoryPrefsBootstrap", Context.MODE_PRIVATE))
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

    /**
     * SECURITY (M1): degraded store used ONLY when EncryptedSharedPreferences
     * cannot be created. Non-secret UI settings still persist (via a separate
     * plain prefs file), but SECRET keys (auth_token, fcm_token, user_*) are
     * held in RAM only — a cold reboot loses the session, which is exactly the
     * safe failure mode (forces re-login instead of a plaintext token on disk).
     */
    private class InMemoryFallback(private val uiPrefs: SharedPreferences) : SharedPreferences by uiPrefs {
        private val memory = java.util.concurrent.ConcurrentHashMap<String, Any?>()

        private fun isSecretKey(key: String?): Boolean {
            if (key == null) return false
            return key == "auth_token" || key == "fcm_token" || key.startsWith("user_")
        }

        private inner class SecretAwareEditor(private val base: SharedPreferences.Editor) : SharedPreferences.Editor by base {
            private val pendingSecrets = mutableMapOf<String, Any?>()
            private val pendingSecretRemovals = mutableSetOf<String>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = value; return this }
                base.putString(key, value); return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = values; return this }
                base.putStringSet(key, values); return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = value; return this }
                base.putInt(key, value); return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = value; return this }
                base.putLong(key, value); return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = value; return this }
                base.putFloat(key, value); return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (isSecretKey(key)) { pendingSecrets[key!!] = value; return this }
                base.putBoolean(key, value); return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (isSecretKey(key)) { memory.remove(key); pendingSecretRemovals.add(key!!); pendingSecrets.remove(key); return this }
                base.remove(key); return this
            }
            override fun clear(): SharedPreferences.Editor {
                memory.clear(); pendingSecrets.clear(); base.clear(); return this
            }
            override fun commit(): Boolean {
                pendingSecrets.forEach { (k, v) -> if (v == null) memory.remove(k) else memory[k] = v }
                pendingSecrets.clear(); pendingSecretRemovals.clear()
                return base.commit()
            }
            override fun apply() {
                pendingSecrets.forEach { (k, v) -> if (v == null) memory.remove(k) else memory[k] = v }
                pendingSecrets.clear(); pendingSecretRemovals.clear()
                base.apply()
            }
        }

        override fun edit(): SharedPreferences.Editor = SecretAwareEditor(uiPrefs.edit())

        @Suppress("UNCHECKED_CAST")
        override fun getString(key: String?, defValue: String?): String? =
            if (isSecretKey(key)) (memory[key] as? String) ?: defValue else uiPrefs.getString(key, defValue)
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            if (isSecretKey(key)) (memory[key] as? MutableSet<String>) ?: defValues else uiPrefs.getStringSet(key, defValues)
        override fun getInt(key: String?, defValue: Int): Int =
            if (isSecretKey(key)) (memory[key] as? Int) ?: defValue else uiPrefs.getInt(key, defValue)
        override fun getLong(key: String?, defValue: Long): Long =
            if (isSecretKey(key)) (memory[key] as? Long) ?: defValue else uiPrefs.getLong(key, defValue)
        override fun getFloat(key: String?, defValue: Float): Float =
            if (isSecretKey(key)) (memory[key] as? Float) ?: defValue else uiPrefs.getFloat(key, defValue)
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            if (isSecretKey(key)) (memory[key] as? Boolean) ?: defValue else uiPrefs.getBoolean(key, defValue)
        override fun contains(key: String?): Boolean =
            if (isSecretKey(key)) memory.containsKey(key) else uiPrefs.contains(key)
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
        if (userId <= 0) return
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

    fun getBackgroundType(): String {
        val type = sharedPreferences.getString("bg_type", "image") ?: "image"
        return if (type == "color") "image" else type
    }

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
    fun getThemeMode(): String {
        val mode = sharedPreferences.getString("theme_mode", "dark") ?: "dark"
        return if (mode == "system") "dark" else mode
    }

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
