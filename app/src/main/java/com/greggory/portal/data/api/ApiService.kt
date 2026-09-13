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

    @POST("api/users/push-token")
    suspend fun updatePushToken(@Body request: PushTokenRequest): Response<SimpleResponse>
}

data class PushTokenRequest(val token: String, val platform: String = "android")

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String?, val token: String?, val user: UserInfo?)
data class UserInfo(val id: Int, val email: String, val first_name: String)

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
    val projects: List<Project>?,
    val invoices: List<Invoice>?,
    val kpiMetrics: List<KpiMetric>?
)

data class Project(val id: Int, val name: String, val status: String, val progress: Int)
data class Invoice(val id: Int, val amount: Double, val status: String)
data class KpiMetric(val label: String, val value: String)
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
    val report_date: String,
    val project_name: String
)

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
