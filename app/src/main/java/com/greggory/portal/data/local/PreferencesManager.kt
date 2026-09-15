package com.greggory.portal.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "GreggoryPrefsSecure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun saveUserInfo(userId: Int, email: String, name: String) {
        sharedPreferences.edit().apply {
            putInt("user_id", userId)
            putString("user_email", email)
            putString("user_name", name)
        }.apply()
    }

    fun getUserId(): Int = sharedPreferences.getInt("user_id", -1)
    fun getUserEmail(): String? = sharedPreferences.getString("user_email", null)
    fun getUserName(): String? = sharedPreferences.getString("user_name", null)

    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }

    fun getFcmToken(): String? = sharedPreferences.getString("fcm_token", null)
}
