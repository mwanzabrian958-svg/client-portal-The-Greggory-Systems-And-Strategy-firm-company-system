package com.greggory.portal.data.api

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
    suspend fun getProjects(): Response<ProjectsResponse>

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
    val type: String,
    val rating: Int,
    val priority: String,
    val author: String,
    val created_at: String
)

data class QuotesResponse(val success: Boolean, val quotes: List<Quote>)
data class Quote(
    val id: Int,
    val project_id: Int,
    val project_name: String,
    val amount: Double,
    val description: String,
    val status: String,
    val created_at: String
)

data class SignatureRequestsResponse(val success: Boolean, val requests: List<SignatureRequest>)
data class SignatureRequest(
    val id: Int,
    val document_name: String,
    val project_name: String,
    val status: String,
    val created_at: String
)

data class DecisionRequest(val decision: String, val note: String? = null)

data class ChangeRequestsResponse(val success: Boolean, val requests: List<ChangeRequest>)
data class ChangeRequest(
    val id: Int,
    val project_id: Int,
    val project_name: String,
    val title: String,
    val description: String,
    val status: String,
    val created_at: String
)

data class ChangeRequestAction(val project_id: Int, val title: String, val description: String)

data class PaymentReportRequest(val invoiceId: String, val mpesaMessage: String)

data class PushTokenRequest(val token: String, val platform: String = "android")

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String?, val token: String?, val user: UserInfo?)
data class UserInfo(
    val id: Int, 
    val email: String, 
    val first_name: String, 
    val last_name: String? = null,
    val display_name: String? = null,
    val phone: String? = null,
    val mission_briefing: String? = null,
    val primary_role: String? = null
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
    val user: UserInfo?,
    val projects: List<Project>?,
    val invoices: List<Invoice>?,
    val tasks: List<Task>?,
    val teamMembers: List<TeamMember>?,
    val messages: List<Message>?,
    val budgetOverview: BudgetOverview?,
    val businessSummary: BusinessSummary?,
    val kpiMetrics: List<KpiMetric>?
)

data class Project(
    val id: Int, 
    val name: String, 
    val status: String, 
    val progress: Int, 
    val client_id: Int,
    val priority: String? = "Medium",
    val manager: String? = "Team Lead",
    val deadline: String? = null,
    val plannedBudget: Double? = 0.0,
    val actualBudget: Double? = 0.0
)

data class Task(
    val id: Int,
    val title: String,
    val project: String,
    val assignee: String,
    val priority: String,
    val progress: Int,
    val status: String,
    val dueDate: String?
)

data class TeamMember(
    val id: Int,
    val name: String,
    val role: String,
    val duties: String,
    val email: String?,
    val projectName: String?
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
    val activeProjects: Int,
    val completedProjects: Int,
    val openInvoices: Int,
    val openMessages: Int,
    val nextMilestone: String?
)
data class Invoice(val id: Int, val amount: Double, val status: String, val client_id: Int)
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

data class ProjectsResponse(val success: Boolean, val projects: List<Project>)

data class NotificationsResponse(val success: Boolean, val notifications: List<Notification>)
data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val priority: String,
    val status: String,
    val created_at: String
)

data class ReportsResponse(val success: Boolean, val reports: List<Report>)
data class Report(
    val id: Int,
    val title: String,
    val summary: String,
    val file_type: String,
    val file_size: Long,
    val report_date: String,
    val project_name: String,
    val client_id: Int
)

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
