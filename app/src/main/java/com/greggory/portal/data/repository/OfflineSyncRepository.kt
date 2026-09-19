package com.greggory.portal.data.repository

import android.content.Context
import android.util.Log
import com.greggory.portal.data.api.Project
import com.greggory.portal.data.api.Invoice
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineSyncRepository(context: Context) {
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
    suspend fun syncProjects(): Boolean {
        return try {
            val response = RetrofitClient.instance.getProjects()
            if (response.isSuccessful && response.body() != null) {
                val apiProjects = response.body()!!
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
            invoiceDao.clearInvoices()
            invoiceDao.insertInvoices(apiInvoices.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SyncRepository", "Invoice synchronization cache step failed: ${e.localizedMessage}")
        }
    }

    suspend fun syncReports() {
        try {
            val response = RetrofitClient.instance.getReports()
            if (response.isSuccessful && response.body() != null) {
                val apiReports = response.body()!!.reports
                reportDao.clearReports()
                reportDao.insertReports(apiReports.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Reports archive background sync failed: ${e.localizedMessage}")
        }
    }
}
