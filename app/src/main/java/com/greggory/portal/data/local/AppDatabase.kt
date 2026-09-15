package com.greggory.portal.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val progress: Int,
    val client_id: Int
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: Int,
    val amount: Double,
    val status: String,
    val client_id: Int
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    val file_type: String,
    val file_size: Long,
    val report_date: String,
    val project_name: String,
    val client_id: Int
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects")
    suspend fun clearProjects()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)

    @Query("DELETE FROM reports")
    suspend fun clearReports()
}

@Database(entities = [ProjectEntity::class, InvoiceEntity::class, ReportEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "greggory_portal_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Mappers to convert between API models and Database entities
fun com.greggory.portal.data.api.Project.toEntity() = ProjectEntity(id, name, status, progress, clientId)
fun ProjectEntity.toApi() = com.greggory.portal.data.api.Project(id, name, status, progress, client_id)

fun com.greggory.portal.data.api.Invoice.toEntity() = InvoiceEntity(id, amount, status, clientId)
fun InvoiceEntity.toApi() = com.greggory.portal.data.api.Invoice(id, amount, status, client_id)

fun com.greggory.portal.data.api.Report.toEntity() = ReportEntity(id, title, summary, fileType, fileSize, reportDate, projectName, clientId)
fun ReportEntity.toApi() = com.greggory.portal.data.api.Report(id, title, summary, file_type, file_size, report_date, project_name, client_id)
