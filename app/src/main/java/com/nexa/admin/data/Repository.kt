package com.nexa.admin.data

import com.nexa.admin.data.ApiClient.api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class Repository(private val prefs: Prefs) {
    private fun authHeader() = "Bearer ${prefs.token ?: ""}"

    private fun err(e: Throwable): String = when (e) {
        is HttpException -> try {
            val b = e.response()?.errorBody()?.string() ?: ""
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(b)?.groupValues?.get(1) ?: "HTTP ${e.code()}"
        } catch (_: Exception) { "HTTP ${e.code()}" }
        else -> e.message ?: e.javaClass.simpleName
    }

    private inline fun <T> wrap(block: () -> T): Result<T> =
        runCatching(block).recoverCatching { throw Exception(err(it)) }

    suspend fun loginPin(email: String, pin: String, code: String? = null): Result<AuthResponse> =
        withContext(Dispatchers.IO) { wrap { api.loginPin(LoginPinRequest(email, pin, code)) } }

    suspend fun me(): Result<MeResponse> =
        withContext(Dispatchers.IO) { wrap { api.me(authHeader()) } }

    suspend fun saveFcmToken(token: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.saveFcmToken(authHeader(), FcmTokenRequest(token, "admin")) } }

    suspend fun stats(): Result<AdminStats> =
        withContext(Dispatchers.IO) { wrap { api.adminStats(authHeader()) } }

    suspend fun listUsers(q: String = "", page: Int = 1, banned: String? = null): Result<UserListResponse> =
        withContext(Dispatchers.IO) { wrap { api.listUsers(authHeader(), q, page, 20, banned) } }

    suspend fun getUser(id: String): Result<UserDetailResponse> =
        withContext(Dispatchers.IO) { wrap { api.getUser(authHeader(), id) } }

    suspend fun adjustBalance(userId: String, currency: String, amount: Double, note: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.adjustBalance(authHeader(), AdjustBalanceRequest(userId, currency, amount, note)) } }

    suspend fun setBan(userId: String, ban: Boolean, reason: String?): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.setBan(authHeader(), BanRequest(userId, ban, reason)) } }

    suspend fun deleteUser(id: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.deleteUser(authHeader(), id) } }

    suspend fun sendNotify(userId: String, title: String, body: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.sendUserNotification(authHeader(), SendNotifyRequest(userId, title, body)) } }

    suspend fun pendingKyc(): Result<KycListResponse> =
        withContext(Dispatchers.IO) { wrap { api.pendingKyc(authHeader()) } }

    suspend fun approveKyc(kycId: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.approveKyc(authHeader(), ApproveKycRequest(kycId)) } }

    suspend fun rejectKyc(kycId: String, note: String): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.rejectKyc(authHeader(), RejectKycRequest(kycId, note)) } }

    suspend fun pendingTx(type: String? = null): Result<TxListResponse> =
        withContext(Dispatchers.IO) { wrap { api.pendingTx(authHeader(), type) } }

    suspend fun approveDeposit(txId: String, action: String, note: String?): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.approveDeposit(authHeader(), ApproveActionRequest(txId, action, note)) } }

    suspend fun approveCashout(txId: String, action: String, note: String?): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.approveCashout(authHeader(), ApproveActionRequest(txId, action, note)) } }

    suspend fun getNotifications(): Result<NotificationsResponse> =
        withContext(Dispatchers.IO) { wrap { api.getNotifications(authHeader()) } }

    suspend fun markAllRead(): Result<SimpleResponse> =
        withContext(Dispatchers.IO) { wrap { api.markAllRead(authHeader()) } }

    fun saveSession(token: String?, user: AdminUser?) {
        prefs.token = token
        prefs.email = user?.email
        prefs.name = user?.fullName
        prefs.role = user?.role
        prefs.fcmTokenSynced = false
    }

    fun clearSession() = prefs.logout()
}
