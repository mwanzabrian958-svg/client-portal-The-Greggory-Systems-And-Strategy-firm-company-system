package com.greggory.portal.data.repository

import android.content.Context
import android.util.Log
import com.greggory.portal.data.api.Project
import com.greggory.portal.data.api.Invoice
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.local.*
import com.greggory.portal.utils.DataRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineSyncRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = PreferencesManager.getInstance(appContext)
    private val database = AppDatabase.getDatabase(context)
    private val projectDao = database.projectDao()
    private val invoiceDao = database.invoiceDao()
    private val reportDao = database.reportDao()

    // --- Stream Data from Room Cache directly to UI ---
    val projectsFlow: Flow<List<Project>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toApi() }
    }

    val invoicesFlow: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { entities ->
        entities.map { it.toApi() }
    }

    val reportsFlow: Flow<List<Report>> = reportDao.getAllReports().map { entities ->
        entities.map { it.toApi() }
    }

    // --- Asynchronous Network Sync Hooks ---
    // Each device syncs ONLY its own userId silo. Foreign rows are dropped before
    // Room insert so Phone A can never cache Phone B's projects/invoices.
    suspend fun syncProjects(): Boolean {
        return try {
            val response = RetrofitClient.instance.getProjects()
            if (response.isSuccessful && response.body() != null) {
                val userId = prefs.getUserId()
                val apiProjects = response.body()!!
                    .filter { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }
                projectDao.clearProjects()
                projectDao.insertProjects(apiProjects.map { it.toEntity() })
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Project cache synchronization delta dropped: ${e.localizedMessage}")
            false
        }
    }

    suspend fun syncInvoices(apiInvoices: List<Invoice>) {
        try {
            val userId = prefs.getUserId()
            val mine = apiInvoices.filter { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }
            invoiceDao.clearInvoices()
            invoiceDao.insertInvoices(mine.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SyncRepository", "Invoice synchronization cache step failed: ${e.localizedMessage}")
        }
    }

    suspend fun syncReports() {
        try {
            val response = RetrofitClient.instance.getReports()
            if (response.isSuccessful && response.body() != null) {
                val userId = prefs.getUserId()
                val apiReports = response.body()!!.reports
                    .filter { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }
                reportDao.clearReports()
                reportDao.insertReports(apiReports.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Reports archive background sync failed: ${e.localizedMessage}")
        }
    }
}
