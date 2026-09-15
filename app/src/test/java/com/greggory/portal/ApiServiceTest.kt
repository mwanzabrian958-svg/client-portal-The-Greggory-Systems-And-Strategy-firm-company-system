package com.greggory.portal

import com.google.gson.Gson
import com.greggory.portal.data.api.DashboardResponse
import org.junit.Assert.*
import org.junit.Test

class ApiServiceTest {

    @Test
    fun deserializeDashboardResponse_Success() {
        val json = """
            {
                "success": true,
                "user": {
                    "id": 1,
                    "first_name": "Lydia",
                    "mission_briefing": "Scale operations."
                },
                "projects": [
                    { "id": 101, "name": "Web Audit", "status": "active", "progress": 45, "client_id": 1 }
                ],
                "invoices": [
                    { "id": 201, "amount": 50000.0, "status": "pending", "client_id": 1 }
                ],
                "businessSummary": {
                    "activeProjects": 1,
                    "openInvoices": 1,
                    "openMessages": 0,
                    "nextMilestone": "Code Review"
                },
                "budgetOverview": {
                    "planned": 100000.0,
                    "spent": 45000.0,
                    "forecast": 55000.0,
                    "variance": 0
                }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, DashboardResponse::class.java)

        assertTrue(response.success)
        assertEquals("Lydia", response.user?.first_name)
        assertEquals("Scale operations.", response.user?.mission_briefing)
        assertEquals(1, response.projects?.size)
        assertEquals("Web Audit", response.projects?.get(0)?.name)
        assertEquals(45, response.projects?.get(0)?.progress)
        assertEquals(1, response.businessSummary?.activeProjects)
        assertEquals("Code Review", response.businessSummary?.nextMilestone)
        assertEquals(100000.0, response.budgetOverview?.planned ?: 0.0, 0.0)
    }
}
