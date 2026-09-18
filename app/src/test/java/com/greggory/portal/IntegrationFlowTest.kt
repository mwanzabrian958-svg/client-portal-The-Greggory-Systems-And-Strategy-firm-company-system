package com.greggory.portal

import com.google.gson.Gson
import com.greggory.portal.data.api.*
import com.greggory.portal.utils.DataRouter
import org.junit.Assert.*
import org.junit.Test

class IntegrationFlowTest {

    private val gson = Gson()

    @Test
    fun `Full Hand-in-Hand Application Journey Flow Simulator`() {
        // --- STEP 1: CLIENT SIGNUP ---
        val registerResponseJson = """
            {
                "success": true,
                "message": "User registered successfully",
                "loginInstead": false
            }
        """.trimIndent()
        
        val signUpResponse = gson.fromJson(registerResponseJson, RegisterResponse::class.java)
        assertTrue(signUpResponse.success)
        assertEquals("User registered successfully", signUpResponse.message)

        // --- STEP 2: SECURE LOG IN ---
        val loginResponseJson = """
            {
                "id": 6,
                "email": "client@greggory.com",
                "first_name": "Brian",
                "last_name": "Mwanza",
                "display_name": "Brian Mwanza",
                "token": "gf_lock_session_token_xyz"
            }
        """.trimIndent()

        val loginResponse = gson.fromJson(loginResponseJson, LoginResponse::class.java)
        assertEquals("gf_lock_session_token_xyz", loginResponse.token)
        assertEquals(6, loginResponse.id)

        // --- STEP 3: PORTAL LAUNCH & DATA SYNC (HAND-IN-HAND CORRECTION LOAD) ---
        val dashboardResponseJson = """
            {
                "success": true,
                "dashboard": {
                    "user": { "id": 6, "first_name": "Brian", "mission_briefing": "Modernize infrastructure." },
                    "projects": [
                        { "id": 101, "project_name": "Cloud Architecture Optimization", "status": "active", "progress_percentage": 75, "user_id": 6 }
                    ],
                    "invoices": [
                        { "id": 201, "amount": 125000.0, "status": "pending", "user_id": 6 }
                    ],
                    "businessSummary": { "activeProjects": 1, "openInvoices": 1, "openMessages": 2, "nextMilestone": "Database Migration" }
                }
            }
        """.trimIndent()

        val dashboardResponse = gson.fromJson(dashboardResponseJson, DashboardResponse::class.java)
        assertTrue(dashboardResponse.success == true)
        
        val dashboardData = dashboardResponse.dashboard!!
        assertEquals("Modernize infrastructure.", dashboardData.user?.missionBriefing)
        assertEquals(1, dashboardData.projects?.size)
        assertEquals("Cloud Architecture Optimization", dashboardData.projects?.get(0)?.name)
        assertEquals(75, dashboardData.projects?.get(0)?.progress)

        // Validate the "Set in Stone" routing rule hand-in-hand match
        val remoteClientId = dashboardData.projects?.get(0)?.clientId ?: -1
        val localAuthenticatedUserId = loginResponse.id

        // Verify Data Integrity check passes hand-in-hand
        assertTrue(DataRouter.verifyRoutingIntegrity(remoteClientId, localAuthenticatedUserId))
    }
}
