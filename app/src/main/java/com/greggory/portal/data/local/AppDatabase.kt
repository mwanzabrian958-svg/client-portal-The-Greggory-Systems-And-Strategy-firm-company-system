package com.greggory.portal.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val progress: Int
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: Int,
    val amount: Double,
    val status: String
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

@Database(entities = [ProjectEntity::class, InvoiceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun invoiceDao(): InvoiceDao

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
fun com.greggory.portal.data.api.Project.toEntity() = ProjectEntity(id, name, status, progress)
fun ProjectEntity.toApi() = com.greggory.portal.data.api.Project(id, name, status, progress)

fun com.greggory.portal.data.api.Invoice.toEntity() = InvoiceEntity(id, amount, status)
fun InvoiceEntity.toApi() = com.greggory.portal.data.api.Invoice(id, amount, status)
