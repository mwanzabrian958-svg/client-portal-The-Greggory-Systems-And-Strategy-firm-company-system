package com.greggory.portal

import com.google.gson.Gson
import com.greggory.portal.data.api.RegisterRequest
import com.greggory.portal.data.api.RegisterResponse
import org.junit.Assert.*
import org.junit.Test

class RegisterTest {

    private val gson = Gson()

    @Test
    fun `Verify Test Account Registration Payload and Response`() {
        // Create test account registration request payload
        val request = RegisterRequest(
            first_name = "Test",
            last_name = "User",
            email = "testuser_new@greggory.com",
            phone = "0712345678",
            password = "SecurePassword123!"
        )

        // Serialize payload to JSON
        val jsonPayload = gson.toJson(request)
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.contains("testuser_new@greggory.com"))
        assertTrue(jsonPayload.contains("Test"))
        assertTrue(jsonPayload.contains("User"))

        // Simulate successful registration response from backend
        val responseJson = """
            {
                "success": true,
                "message": "Test account registered successfully",
                "loginInstead": false
            }
        """.trimIndent()

        val response = gson.fromJson(responseJson, RegisterResponse::class.java)
        assertTrue(response.success)
        assertEquals("Test account registered successfully", response.message)
        assertEquals(false, response.loginInstead)
    }
}
