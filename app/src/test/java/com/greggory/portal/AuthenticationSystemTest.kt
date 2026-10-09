package com.greggory.portal

import com.google.gson.Gson
import com.greggory.portal.data.api.ChannelStatusResponse
import com.greggory.portal.data.api.LoginResponse
import com.greggory.portal.data.api.OtpRequestResponse
import com.greggory.portal.data.api.RegisterResponse
import com.greggory.portal.data.api.SimpleResponse
import org.junit.Assert.*
import org.junit.Test

class AuthenticationSystemTest {

    private val gson = Gson()

    @Test
    fun `Verify Login Response Deserialization`() {
        val json = """
            {
                "id": 6,
                "email": "test@greggory.com",
                "first_name": "Brian",
                "last_name": "Mwanza",
                "display_name": "Brian Mwanza",
                "token": "gf_lock_test_token_123"
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertEquals(6, response.id)
        assertEquals("test@greggory.com", response.email)
        assertEquals("Brian", response.firstName)
        assertEquals("gf_lock_test_token_123", response.token)
    }

    @Test
    fun `Verify Registration Response Deserialization`() {
        val json = """
            {
                "success": true,
                "message": "User registered successfully",
                "loginInstead": false
            }
        """.trimIndent()

        val response = gson.fromJson(json, RegisterResponse::class.java)

        assertTrue(response.success)
        assertEquals("User registered successfully", response.message)
        assertEquals(false, response.loginInstead)
    }

    @Test
    fun `Verify Auth Failure Parsing`() {
        val json = """
            {
                "success": false,
                "message": "Invalid or expired authentication token",
                "error": "Unauthorized"
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertFalse(response.success == true)
        assertEquals("Invalid or expired authentication token", response.message)
        assertEquals("Unauthorized", response.error)
    }

    @Test
    fun `Verify Simple Response Success`() {
        val json = """
            {
                "success": true,
                "message": "Operation completed"
            }
        """.trimIndent()

        val response = gson.fromJson(json, SimpleResponse::class.java)
        assertTrue(response.success)
        assertEquals("Operation completed", response.message)
    }

    @Test
    fun `Verify Multi-Channel OTP Request and Status Deserialization`() {
        val requestResponseJson = """
            {
                "success": true,
                "message": "Code sent successfully",
                "expiresInMinutes": 10,
                "simulated": true,
                "provider": "sms:infobip",
                "channel": "sms"
            }
        """.trimIndent()

        val otpResp = gson.fromJson(requestResponseJson, OtpRequestResponse::class.java)
        assertTrue(otpResp.success)
        assertEquals(10, otpResp.expiresInMinutes)
        assertEquals("sms:infobip", otpResp.provider)

        val statusJson = """
            {
                "success": true,
                "chain": ["whatsapp", "sms", "email"],
                "whatsapp": { "configured": true, "provider": "infobip" }
            }
        """.trimIndent()

        val statusResp = gson.fromJson(statusJson, ChannelStatusResponse::class.java)
        assertTrue(statusResp.success)
        assertEquals(3, statusResp.chain.size)
        assertTrue(statusResp.whatsapp?.configured == true)
    }
}
