package com.greggory.portal

import com.google.gson.Gson
import com.greggory.portal.data.api.*
import com.greggory.portal.utils.DataRouter
import org.junit.Assert.*
import org.junit.Test

class IntegrationFlowTest {

    private val gson = Gson()

    @Test
    fun `Execute Register and Login Flow for Test User`() {
        // --- STEP 1: REGISTER NEW TEST USER ---
        val registerRequest = RegisterRequest(
            first_name = "DynamicTest",
            last_name = "Client",
            email = "dynamic_testuser@greggory.com",
            phone = "0798765432",
            password = "TestPassword123!"
        )

        val registerPayloadJson = gson.toJson(registerRequest)
        assertTrue(registerPayloadJson.contains("dynamic_testuser@greggory.com"))
        assertTrue(registerPayloadJson.contains("DynamicTest"))

        // Simulate backend register response
        val registerResponseJson = """
            {
                "success": true,
                "message": "Test account registered successfully",
                "loginInstead": false
            }
        """.trimIndent()
        
        val signUpResponse = gson.fromJson(registerResponseJson, RegisterResponse::class.java)
        assertTrue(signUpResponse.success)
        assertEquals("Test account registered successfully", signUpResponse.message)

        // --- STEP 2: LOG IN WITH NEW TEST USER ---
        val loginRequest = LoginRequest(
            email = "dynamic_testuser@greggory.com",
            password = "TestPassword123!"
        )
        val loginPayloadJson = gson.toJson(loginRequest)
        assertTrue(loginPayloadJson.contains("dynamic_testuser@greggory.com"))

        // Simulate backend login response returning token and user metadata
        val loginResponseJson = """
            {
                "id": 99,
                "email": "dynamic_testuser@greggory.com",
                "first_name": "DynamicTest",
                "last_name": "Client",
                "display_name": "DynamicTest Client",
                "primary_role": "user",
                "token": "gf_lock_test_token_dynamic_99"
            }
        """.trimIndent()

        val loginResponse = gson.fromJson(loginResponseJson, LoginResponse::class.java)
        assertEquals("gf_lock_test_token_dynamic_99", loginResponse.token)
        assertEquals(99, loginResponse.id)
        assertEquals("dynamic_testuser@greggory.com", loginResponse.email)

        // --- STEP 3: PORTAL DASHBOARD SYNC & ROUTING INTEGRITY ---
        val dashboardResponseJson = """
            {
                "success": true,
                "dashboard": {
                    "user": { 
                        "id": 99, 
                        "email": "dynamic_testuser@greggory.com",
                        "first_name": "DynamicTest", 
                        "mission_briefing": "Automated pipeline verification." 
                    },
                    "projects": [
                        { "id": 501, "project_name": "Automated Security Audit", "status": "active", "progress_percentage": 90, "user_id": 99 }
                    ],
                    "invoices": [
                        { "id": 601, "amount": 250000.0, "status": "paid", "user_id": 99, "invoice_number": "INV-99-01" }
                    ],
                    "businessSummary": { "activeProjects": 1, "openInvoices": 0, "openMessages": 0, "nextMilestone": "Final Handover" }
                }
            }
        """.trimIndent()

        val dashboardResponse = gson.fromJson(dashboardResponseJson, DashboardResponse::class.java)
        assertTrue(dashboardResponse.success == true)
        
        val dashboardData = dashboardResponse.dashboard!!
        assertEquals(99, dashboardData.user?.id)
        assertEquals("Automated pipeline verification.", dashboardData.user?.missionBriefing)
        assertEquals(1, dashboardData.projects?.size)
        assertEquals("Automated Security Audit", dashboardData.projects?.get(0)?.name)
        assertEquals(90, dashboardData.projects?.get(0)?.progress)
        assertEquals("INV-99-01", dashboardData.invoices?.get(0)?.invoiceNumber)

        // Verify Set-in-Stone Routing Integrity check passes for the newly registered user
        val remoteClientId = dashboardData.projects?.get(0)?.clientId ?: -1
        val localAuthenticatedUserId = loginResponse.id
        assertTrue(DataRouter.verifyRoutingIntegrity(remoteClientId, localAuthenticatedUserId))
    }
}
