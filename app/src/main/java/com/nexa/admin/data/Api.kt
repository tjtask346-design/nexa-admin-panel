package com.nexa.admin.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://nexa-backend-w3xb.onrender.com"

data class AdminUser(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String? = null,
    val email: String = "",
    val fullName: String = "",
    val accountNumber: String = "",
    val role: String = "user",
    val balance: Double = 0.0,
    val ltcBalance: Double = 0.0,
    val balanceUSD: Double = 0.0,
    val uid: String? = null,
    val kycStatus: String = "unverified",
    val totpEnabled: Boolean = false,
    val avatarUrl: String? = null,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val ltcAddress: String? = null,
    val wallets: Wallets? = null,
    val createdAt: String? = null
)

data class Wallets(
    val bscAddress: String? = null,
    val walletIndex: Int? = null,
    val xpub: String? = null
)

data class AdminStats(
    val success: Boolean = false,
    val totalUsers: Int = 0,
    val bannedUsers: Int = 0,
    val pendingKyc: Int = 0,
    val pendingTx: Int = 0,
    val totalBalance: Double = 0.0
)

data class UserListResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val page: Int = 1,
    val users: List<AdminUser> = emptyList()
)

data class UserDetailResponse(
    val success: Boolean = false,
    val user: AdminUser? = null
)

data class KycItem(
    @SerializedName(value = "_id", alternate = ["id"])
    val id: String? = null,
    val user: AdminUser? = null,
    val nidNumber: String = "",
    val frontUrl: String = "",
    val backUrl: String = "",
    val selfieUrl: String = "",
    val status: String = "pending",
    val adminNote: String? = null,
    val createdAt: String? = null
)

data class KycListResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val kycs: List<KycItem> = emptyList()
)

data class TxItem(
    @SerializedName(value = "_id", alternate = ["id"])
    val id: String? = null,
    val user: AdminUser? = null,
    val type: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    val trxId: String? = null,
    val paymentMethodNumber: String? = null,
    val senderUid: String? = null,
    val receiverUid: String? = null,
    val adminNote: String? = null,
    val note: String? = null,
    val createdAt: String? = null
)

data class TxListResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val transactions: List<TxItem> = emptyList()
)

data class AuthResponse(
    val success: Boolean = false,
    val token: String? = null,
    val user: AdminUser? = null,
    val message: String? = null,
    val requiresTotp: Boolean = false,
    val requiresTotpSetup: Boolean = false,
    val banned: Boolean = false,
    val reason: String? = null
)

data class MeResponse(
    val success: Boolean = false,
    val user: AdminUser? = null,
    val message: String? = null,
    val banned: Boolean = false,
    val reason: String? = null
)

data class SimpleResponse(
    val success: Boolean = false,
    val message: String? = null
)

data class NotificationItem(
    @SerializedName(value = "_id", alternate = ["id"])
    val id: String? = null,
    val title: String = "",
    val body: String = "",
    val type: String = "system",
    val read: Boolean = false,
    val createdAt: String? = null
)

data class NotificationsResponse(
    val success: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val unread: Int = 0
)

data class LoginPinRequest(val email: String, val pin: String, val code: String? = null)
data class AdjustBalanceRequest(val userId: String, val currency: String, val amount: Double, val note: String)
data class BanRequest(val userId: String, val ban: Boolean, val reason: String? = null)
data class SendNotifyRequest(val userId: String, val title: String, val body: String, val type: String = "system")
data class ApproveActionRequest(val transactionId: String, val action: String, val adminNote: String? = null)
data class ApproveKycRequest(val kycId: String)
data class RejectKycRequest(val kycId: String, val adminNote: String)
data class FcmTokenRequest(val token: String)

interface NexaAdminApi {
    @POST("/api/auth/login-pin")
    suspend fun loginPin(@Body body: LoginPinRequest): AuthResponse

    @GET("/api/auth/me")
    suspend fun me(@Header("Authorization") token: String): MeResponse

    @POST("/api/auth/save-fcm-token")
    suspend fun saveFcmToken(@Header("Authorization") token: String, @Body body: FcmTokenRequest): SimpleResponse

    @GET("/api/admin/stats")
    suspend fun adminStats(@Header("Authorization") token: String): AdminStats

    @GET("/api/admin/users")
    suspend fun listUsers(
        @Header("Authorization") token: String,
        @Query("q") q: String = "",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("banned") banned: String? = null
    ): UserListResponse

    @GET("/api/admin/users/{id}")
    suspend fun getUser(@Header("Authorization") token: String, @Path("id") id: String): UserDetailResponse

    @POST("/api/admin/users/adjust")
    suspend fun adjustBalance(@Header("Authorization") token: String, @Body body: AdjustBalanceRequest): SimpleResponse

    @POST("/api/admin/users/ban")
    suspend fun setBan(@Header("Authorization") token: String, @Body body: BanRequest): SimpleResponse

    @DELETE("/api/admin/users/{id}")
    suspend fun deleteUser(@Header("Authorization") token: String, @Path("id") id: String): SimpleResponse

    @POST("/api/admin/notify")
    suspend fun sendUserNotification(@Header("Authorization") token: String, @Body body: SendNotifyRequest): SimpleResponse

    @GET("/api/kyc/pending")
    suspend fun pendingKyc(@Header("Authorization") token: String): KycListResponse

    @POST("/api/kyc/approve")
    suspend fun approveKyc(@Header("Authorization") token: String, @Body body: ApproveKycRequest): SimpleResponse

    @POST("/api/kyc/reject")
    suspend fun rejectKyc(@Header("Authorization") token: String, @Body body: RejectKycRequest): SimpleResponse

    @GET("/api/admin/pending")
    suspend fun pendingTx(@Header("Authorization") token: String, @Query("type") type: String? = null): TxListResponse

    @POST("/api/admin/approve-deposit")
    suspend fun approveDeposit(@Header("Authorization") token: String, @Body body: ApproveActionRequest): SimpleResponse

    @POST("/api/admin/approve-cashout")
    suspend fun approveCashout(@Header("Authorization") token: String, @Body body: ApproveActionRequest): SimpleResponse

    @GET("/api/notifications")
    suspend fun getNotifications(@Header("Authorization") token: String): NotificationsResponse

    @PUT("/api/notifications/read-all")
    suspend fun markAllRead(@Header("Authorization") token: String): SimpleResponse
}

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    val api: NexaAdminApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NexaAdminApi::class.java)
}
