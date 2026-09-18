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
                "dashboard": {
                    "user": {
                        "id": 1,
                        "first_name": "Test Client",
                        "mission_briefing": "Scale operations."
                    },
                    "projects": [
                        { "id": 101, "project_name": "Web Audit", "status": "active", "progress_percentage": 45, "user_id": 1 }
                    ],
                    "invoices": [
                        { "id": 201, "amount": 50000.0, "status": "pending", "user_id": 1 }
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
            }
        """.trimIndent()

        val response = Gson().fromJson(json, DashboardResponse::class.java)

        assertTrue(response.success == true)
        val data = response.dashboard
        assertNotNull(data)
        assertEquals("Test Client", data?.user?.firstName)
        assertEquals("Scale operations.", data?.user?.missionBriefing)
        assertEquals(1, data?.projects?.size)
        assertEquals("Web Audit", data?.projects?.get(0)?.name)
        assertEquals(45, data?.projects?.get(0)?.progress)
        assertEquals(1, data?.businessSummary?.activeProjects)
        assertEquals("Code Review", data?.businessSummary?.nextMilestone)
        assertEquals(100000.0, data?.budgetOverview?.planned ?: 0.0, 0.0)
    }
}
