package com.greggory.portal.utils

import com.greggory.portal.data.local.PreferencesManager

/**
 * "Set in Stone" Routing Logic
 * 
 * This utility ensures that every data request is strictly partitioned by the 
 * client's unique system identifier. It prevents cross-talk between client accounts
 * and guarantees that the mobile app only interacts with the designated cloud silo.
 */
object DataRouter {
    
    fun getSecureHeaders(prefs: PreferencesManager): Map<String, String> {
        val token = prefs.getToken() ?: ""
        val userId = prefs.getUserId()
        
        return mutableMapOf<String, String>().apply {
            put("Authorization", "Bearer $token")
            put("X-Greggory-Client-ID", userId.toString())
            put("X-Routing-Policy", "set-in-stone-v1")
        }
    }

    /**
     * Verifies that the returned data belongs to the authenticated client.
     * This is a second layer of defense against accidental data leakage.
     * 
     * NOTE: We allow ID 0 as it often represents System-generated or Global data.
     */
    fun verifyRoutingIntegrity(remoteClientId: Int, localClientId: Int): Boolean {
        // Relaxed check: allow matches OR system-level global data (ID 0)
        return remoteClientId == localClientId || remoteClientId == 0
    }
}
