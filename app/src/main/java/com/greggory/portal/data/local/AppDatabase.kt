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

@Entity(tableName = "messages")
data class MessageCacheEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val subject: String,
    val message: String,
    val time: String,
    val unread: Boolean,
    val feedback: Boolean,
    val attachmentUrl: String? = null
)

@Entity(tableName = "chat_messages")
data class StrategyChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String,
    val senderId: Int,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val id: Int,
    val firstName: String,
    val lastName: String? = null,
    val displayName: String? = null,
    val phone: String? = null,
    val primaryRole: String? = "user",
    val profilePhotoData: String? = null,
    val missionBriefing: String? = null,
    val authToken: String? = null
) {
    fun toUserInfo() = com.greggory.portal.data.api.UserInfo(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        displayName = displayName,
        phone = phone,
        missionBriefing = missionBriefing,
        primaryRole = primaryRole,
        profilePhotoData = profilePhotoData
    )
}

@Entity(tableName = "client_agreements")
data class ClientAgreementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val companyName: String,
    val email: String,
    val phoneNumber: String,
    val serviceCategory: String,
    val declarationAccepted: Boolean,
    val signatureName: String,
    val timestamp: Long
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: Int,
    val project_id: Int,
    val project_name: String,
    val amount: Double,
    val description: String,
    val status: String,
    val client_id: Int,
    val created_at: String
)

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val rating: Int,
    val priority: String,
    val author: String,
    val created_at: String
)

@Entity(tableName = "change_requests")
data class ChangeRequestEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val status: String,
    val client_id: Int,
    val created_at: String
)

@Entity(tableName = "signature_requests")
data class SignatureRequestEntity(
    @PrimaryKey val id: Int,
    val document_name: String,
    val project_name: String,
    val status: String,
    val client_id: Int,
    val created_at: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val read: Boolean,
    val client_id: Int
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserSync(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET profilePhotoData = :photoData WHERE email = :email")
    suspend fun updateProfilePhoto(email: String, photoData: String?)

    @Query("UPDATE users SET authToken = NULL")
    suspend fun purgeAllTokens()

    @Query("UPDATE users SET authToken = NULL WHERE email = :email")
    suspend fun purgeTokenForEmail(email: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

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

@Dao
interface MessageCacheDao {
    @Query("SELECT * FROM messages ORDER BY id DESC")
    fun getAllMessages(): Flow<List<MessageCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageCacheEntity>)

    @Query("DELETE FROM messages")
    suspend fun clearMessages()
}

@Dao
interface StrategyChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<StrategyChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: StrategyChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface AgreementDao {
    @Query("SELECT * FROM client_agreements ORDER BY timestamp DESC")
    fun getAllAgreements(): Flow<List<ClientAgreementEntity>>

    @Query("SELECT * FROM client_agreements WHERE email = :email ORDER BY timestamp DESC LIMIT 1")
    suspend fun getAgreementByEmailSync(email: String): ClientAgreementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgreement(agreement: ClientAgreementEntity)

    @Query("DELETE FROM client_agreements")
    suspend fun clearAgreements()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Query("DELETE FROM quotes")
    suspend fun clearQuotes()
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: List<FeedbackEntity>)

    @Query("DELETE FROM feedback")
    suspend fun clearFeedback()
}

@Dao
interface ChangeRequestDao {
    @Query("SELECT * FROM change_requests")
    fun getAllChangeRequests(): Flow<List<ChangeRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeRequests(requests: List<ChangeRequestEntity>)

    @Query("DELETE FROM change_requests")
    suspend fun clearChangeRequests()
}

@Dao
interface SignatureRequestDao {
    @Query("SELECT * FROM signature_requests")
    fun getAllSignatureRequests(): Flow<List<SignatureRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatureRequests(requests: List<SignatureRequestEntity>)

    @Query("DELETE FROM signature_requests")
    suspend fun clearSignatureRequests()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()
}

@Database(
    entities = [
        ProjectEntity::class,
        InvoiceEntity::class,
        ReportEntity::class,
        MessageCacheEntity::class,
        StrategyChatMessageEntity::class,
        UserEntity::class,
        ClientAgreementEntity::class,
        QuoteEntity::class,
        FeedbackEntity::class,
        ChangeRequestEntity::class,
        SignatureRequestEntity::class,
        NotificationEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun reportDao(): ReportDao
    abstract fun messageCacheDao(): MessageCacheDao
    abstract fun strategyChatDao(): StrategyChatDao
    abstract fun userDao(): UserDao
    abstract fun agreementDao(): AgreementDao
    abstract fun quoteDao(): QuoteDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun changeRequestDao(): ChangeRequestDao
    abstract fun signatureRequestDao(): SignatureRequestDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "greggory_portal_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
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

fun com.greggory.portal.data.api.Quote.toEntity() = QuoteEntity(id, projectId, projectName, amount, description, status, clientId, createdAt)
fun QuoteEntity.toApi() = com.greggory.portal.data.api.Quote(id, project_id, project_name, amount, description, status, client_id, created_at)

fun com.greggory.portal.data.api.FeedbackItem.toEntity() = FeedbackEntity(id, title, message, type, rating, priority, author, createdAt)
fun FeedbackEntity.toApi() = com.greggory.portal.data.api.FeedbackItem(id, title, message, type, rating, priority, author, created_at)

fun com.greggory.portal.data.api.UserInfo.toEntity(authToken: String? = null) = UserEntity(
    email = email,
    id = id,
    firstName = firstName,
    lastName = lastName,
    displayName = displayName,
    phone = phone,
    primaryRole = primaryRole,
    profilePhotoData = profilePhotoData,
    missionBriefing = missionBriefing,
    authToken = authToken
)

fun com.greggory.portal.data.api.LoginResponse.toUserEntity() = UserEntity(
    email = email,
    id = id,
    firstName = firstName,
    lastName = lastName,
    displayName = displayName,
    phone = phone,
    primaryRole = primaryRole,
    profilePhotoData = profilePhotoData,
    missionBriefing = missionBriefing,
    authToken = token
)
