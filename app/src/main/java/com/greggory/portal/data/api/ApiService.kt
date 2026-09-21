package com.greggory.portal.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/users/client-dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @POST("api/users/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<SimpleResponse>

    @GET("api/user-projects")
    suspend fun getProjects(): Response<List<Project>>

    @GET("api/users/notifications/me")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @GET("api/users/my-reports")
    suspend fun getReports(): Response<ReportsResponse>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<SimpleResponse>

    @POST("api/mpesa/stkpush")
    suspend fun initiateSTKPush(@Body request: MpesaStkPushRequest): Response<MpesaStkPushResponse>

    @GET("api/mpesa/status/{checkoutRequestId}")
    suspend fun getMpesaStatus(@Path("checkoutRequestId") checkoutRequestId: String): Response<MpesaStatusResponse>

    @Multipart
    @POST("api/users/profile-photo")
    suspend fun uploadProfilePhoto(
        @Part photo: okhttp3.MultipartBody.Part
    ): Response<ImageUploadResponse>

    @Multipart
    @POST("api/reports/upload-asset")
    suspend fun uploadAsset(
        @Part asset: okhttp3.MultipartBody.Part
    ): Response<SimpleResponse>

    @Streaming
    @GET("api/users/my-reports/{id}/download")
    suspend fun downloadReport(@Path("id") reportId: Int): Response<okhttp3.ResponseBody>

    @Streaming
    @GET("api/users/my-invoices/{id}/pdf")
    suspend fun downloadInvoicePdf(@Path("id") invoiceId: Int): Response<okhttp3.ResponseBody>

    @POST("api/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<SimpleResponse>

    @POST("api/users/push-token")
    suspend fun updatePushToken(@Body request: PushTokenRequest): Response<SimpleResponse>

    @GET("api/users/search")
    suspend fun search(@Query("q") query: String): Response<SearchResponse>

    @POST("api/mpesa/report-payment")
    suspend fun reportManualPayment(@Body request: PaymentReportRequest): Response<SimpleResponse>

    // Communication & Feedback
    @GET("api/users/client-feedback")
    suspend fun getFeedback(): Response<FeedbackResponse>

    @POST("api/users/client-feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<SimpleResponse>

    // Requests (Quotes & Signatures)
    @GET("api/users/my-quotes")
    suspend fun getQuotes(): Response<QuotesResponse>

    @POST("api/users/my-quotes/{id}/decision")
    suspend fun quoteDecision(@Path("id") quoteId: Int, @Body request: DecisionRequest): Response<SimpleResponse>

    @GET("api/users/my-signature-requests")
    suspend fun getSignatureRequests(): Response<SignatureRequestsResponse>

    @POST("api/users/my-signature-requests/{id}/decision")
    suspend fun signatureDecision(@Path("id") requestId: Int, @Body request: DecisionRequest): Response<SimpleResponse>

    @GET("api/users/my-change-requests")
    suspend fun getChangeRequests(): Response<ChangeRequestsResponse>

    @POST("api/users/my-change-requests")
    suspend fun submitChangeRequest(@Body request: ChangeRequestAction): Response<SimpleResponse>

    // Notification Operations
    @PUT("api/users/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<SimpleResponse>

    @PUT("api/users/notifications/read-all/me")
    suspend fun markAllNotificationsRead(): Response<SimpleResponse>
}

data class FeedbackRequest(
    val title: String,
    val message: String,
    val type: String, // 'bug', 'feature', 'general'
    val rating: Int,
    val priority: String // 'low', 'medium', 'high'
)

data class FeedbackResponse(val success: Boolean, val feedback: List<FeedbackItem>)
data class FeedbackItem(
    val id: Int,
    val title: String,
    val message: String,
    @SerializedName("feedback_type") val type: String,
    val rating: Int,
    val priority: String,
    val author: String,
    @SerializedName("created_at") val createdAt: String
) {
    val created_at: String get() = createdAt
}

data class QuotesResponse(val success: Boolean, val quotes: List<Quote>)
data class Quote(
    val id: Int,
    @SerializedName("project_id") val projectId: Int,
    @SerializedName("project_name") val projectName: String,
    val amount: Double,
    val description: String,
    val status: String,
    @SerializedName("user_id") val clientId: Int = 0,
    @SerializedName("created_at") val createdAt: String
) {
    val project_name: String get() = projectName
    val created_at: String get() = createdAt
}

data class SignatureRequestsResponse(val success: Boolean, val requests: List<SignatureRequest>)
data class SignatureRequest(
    val id: Int,
    @SerializedName("document_name") val documentName: String,
    @SerializedName("project_name") val projectName: String,
    val status: String,
    @SerializedName("user_id") val clientId: Int = 0,
    @SerializedName("created_at") val createdAt: String
) {
    val document_name: String get() = documentName
    val project_name: String get() = projectName
    val created_at: String get() = createdAt
}

data class DecisionRequest(val decision: String, val note: String? = null)

data class ChangeRequestsResponse(val success: Boolean, val requests: List<ChangeRequest>)
data class ChangeRequest(
    val id: Int,
    @SerializedName("project_id") val projectId: Int,
    @SerializedName("project_name") val projectName: String,
    val title: String,
    val description: String,
    val status: String,
    @SerializedName("user_id") val clientId: Int = 0,
    @SerializedName("created_at") val createdAt: String
) {
    val project_name: String get() = projectName
    val created_at: String get() = createdAt
}

data class ChangeRequestAction(val project_id: Int, val title: String, val description: String)

data class PaymentReportRequest(val invoiceId: String, val mpesaMessage: String)

data class PushTokenRequest(val token: String, val platform: String = "android")

data class LoginRequest(val email: String, val password: String)

/**
 * AUTH PROTOCOL: Terminal Lock response.
 * The backend returns user fields at the top level and uses a persistent DB token.
 */
data class LoginResponse(
    val id: Int,
    val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val phone: String? = null,
    @SerializedName("primary_role") val primaryRole: String? = "user",
    val token: String?,
    // Error fields for failed login
    val success: Boolean? = null,
    val message: String? = null,
    val error: String? = null
)

data class UserInfo(
    val id: Int, 
    val email: String, 
    @SerializedName("first_name") val firstName: String, 
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val phone: String? = null,
    @SerializedName("mission_briefing") val missionBriefing: String? = null,
    @SerializedName("primary_role") val primaryRole: String? = null,
    @SerializedName("profilePhotoData") val profilePhotoData: String? = null
)

data class RegisterRequest(
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String,
    val password: String,
    val profile_photo_base64: String? = null,
    val profile_photo_mime_type: String? = null,
    val profile_photo_file_name: String? = null
)
data class RegisterResponse(val success: Boolean, val message: String?, val loginInstead: Boolean? = false)

data class DashboardResponse(
    val success: Boolean,
    val dashboard: DashboardData?
)

data class DashboardData(
    val user: UserInfo?,
    val projects: List<Project>?,
    val invoices: List<Invoice>?,
    val tasks: List<Task>?,
    @SerializedName("teamMembers") val teamMembers: List<TeamMember>?,
    val messages: List<Message>?,
    @SerializedName("budgetOverview") val budgetOverview: BudgetOverview?,
    @SerializedName("businessSummary") val businessSummary: BusinessSummary?,
    @SerializedName("kpiMetrics") val kpiMetrics: List<KpiMetric>?
)

data class Project(
    val id: Int, 
    @SerializedName("project_name") val name: String, 
    val status: String, 
    @SerializedName("progress_percentage") val progress: Int, 
    @SerializedName("user_id", alternate = ["client_id"]) val clientId: Int,
    val priority: String? = "Medium",
    @SerializedName("manager_name") val manager: String? = "Team Lead",
    @SerializedName("end_date") val deadline: String? = null,
    @SerializedName("estimated_budget") val plannedBudget: Double? = 0.0,
    @SerializedName("actual_budget") val actualBudget: Double? = 0.0
)

data class Task(
    val id: Int,
    @SerializedName("task_name") val title: String,
    @SerializedName("project_name") val project: String,
    @SerializedName("assignee_name") val assignee: String,
    val priority: String,
    @SerializedName("progress_percentage") val progress: Int,
    val status: String,
    @SerializedName("due_date") val dueDate: String?
)

data class TeamMember(
    val id: Int,
    val name: String,
    val role: String,
    val duties: String,
    val email: String?,
    val phone: String? = null,
    @SerializedName("project_name") val projectName: String?
)

data class Message(
    val id: String,
    val sender: String,
    val subject: String,
    val message: String,
    val time: String,
    val unread: Boolean,
    val feedback: Boolean? = false
)

data class BudgetOverview(
    val planned: Double,
    val spent: Double,
    val forecast: Double,
    val variance: Int
)

data class BusinessSummary(
    @SerializedName("activeProjects") val activeProjects: Int,
    @SerializedName("openInvoices") val openInvoices: Int,
    @SerializedName("openMessages") val openMessages: Int,
    @SerializedName("nextMilestone") val nextMilestone: String?
)

data class Invoice(
    val id: Int,
    val amount: Double,
    val status: String,
    @SerializedName("user_id", alternate = ["client_id"]) val clientId: Int,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("project_name") val projectName: String? = null,
    @SerializedName("due_date") val dueDate: String? = null
)

data class KpiMetric(val label: String, val value: String, val trend: String? = "neutral")

data class SearchResponse(
    val success: Boolean,
    val results: SearchResults?
)

data class SearchResults(
    val projects: List<Project>?,
    val tasks: List<Task>?,
    val invoices: List<Invoice>?,
    val documents: List<Report>?
)

data class NotificationsResponse(val success: Boolean, val notifications: List<Notification>)
data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val priority: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
) {
    val created_at: String get() = createdAt
}

data class ReportsResponse(val success: Boolean, val reports: List<Report>)
data class Report(
    val id: Int,
    val title: String,
    val summary: String,
    @SerializedName("file_type") val file_type: String,
    @SerializedName("file_size") val file_size: Long,
    @SerializedName("report_date") val report_date: String,
    @SerializedName("project_name") val project_name: String,
    @SerializedName("user_id", alternate = ["client_id"]) val clientId: Int
) {
    val fileType: String get() = file_type
    val fileSize: Long get() = file_size
    val reportDate: String get() = report_date
    val projectName: String get() = project_name
}

data class ProjectsResponse(val success: Boolean, val projects: List<Project>)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

data class ForgotPasswordRequest(val email: String)

data class ProfileUpdateRequest(val display_name: String, val phone_number: String)
data class SimpleResponse(val success: Boolean, val message: String?)

data class MpesaStkPushRequest(
    val phoneNumber: String,
    val amount: Double,
    val accountReference: String,
    val description: String,
    val userId: Int? = null
)

data class MpesaStkPushResponse(
    val success: Boolean,
    val message: String,
    val checkoutRequestId: String?,
    val simulated: Boolean?
)

data class MpesaStatusResponse(
    val success: Boolean,
    val status: String,
    val result_desc: String?,
    val mpesa_receipt: String?
)

data class ImageUploadResponse(
    val success: Boolean,
    val message: String?,
    val imageUrl: String?
)
