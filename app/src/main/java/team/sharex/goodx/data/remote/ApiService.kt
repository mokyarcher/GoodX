package team.sharex.goodx.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.User

// ========== Auth ==========
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String, val nickname: String?)
data class AuthResponse(val token: String, val user: User)

// ========== GoodItem ==========
data class CreateGoodItemRequest(
    val title: String,
    val description: String,
    val contentType: String = "GOODS",
    val category: String,
    val subCategory: String? = null,
    val images: List<String> = emptyList(),
    val link: String? = null
)

data class UpdateGoodItemRequest(
    val title: String? = null,
    val description: String? = null,
    val contentType: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val images: List<String>? = null,
    val link: String? = null
)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)
data class UpdateProfileRequest(val nickname: String? = null, val avatar: String? = null)
data class CommentRequest(
    val content: String,
    val parentId: String? = null
)

// ========== Upload ==========
data class VersionInfo(val version: String, val versionCode: Int, val apkUrl: String, val note: String)
data class AdminCheckResponse(val isAdmin: Boolean)
data class AdminUser(val id: String, val username: String, val nickname: String?, val isAdmin: Boolean, val banned: Boolean, val createdAt: Long)
data class AdminUpdateRequest(val nickname: String? = null, val password: String? = null, val banned: Boolean? = null)
data class AdminPost(val id: String, val title: String, val description: String?, val category: String?, val contentType: String?, val images: List<String>?, val status: String, val removeReason: String?, val authorId: String? = null, val likes: Int, val commentsCount: Int, val createdAt: Long)
data class UnreadCountResponse(val count: Int)
data class AppNotification(val id: String, val type: String, val title: String, val message: String, val relatedPostId: String?, val extra: String?, val read: Boolean, val createdAt: Long)
data class NotificationListResponse(val notifications: List<AppNotification>, val total: Int, val unread: Int)
data class UploadResponse(val url: String, val filename: String, val size: Int)

interface ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<User>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @PUT("api/auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    // Good Items
    @GET("api/good-items")
    suspend fun getGoodItems(
        @Query("contentType") contentType: String? = null,
        @Query("category") category: String? = null,
        @Query("sort") sort: String = "newest",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("author") author: String? = null,
        @Query("status") status: String? = null
    ): Response<List<GoodItem>>

    @GET("api/good-items/{id}")
    suspend fun getGoodItemDetail(@Path("id") id: String): Response<GoodItem>

    @POST("api/good-items")
    suspend fun createGoodItem(@Body request: CreateGoodItemRequest): Response<GoodItem>

    @PUT("api/good-items/{id}")
    suspend fun updateGoodItem(@Path("id") id: String, @Body request: UpdateGoodItemRequest): Response<GoodItem>

    @DELETE("api/good-items/{id}")
    suspend fun deleteGoodItem(@Path("id") id: String): Response<Unit>

    @POST("api/good-items/{id}/like")
    suspend fun likeGoodItem(@Path("id") id: String): Response<GoodItem>

    @POST("api/good-items/{id}/comment")
    suspend fun addComment(@Path("id") id: String, @Body request: CommentRequest): Response<GoodItem>

    @POST("api/good-items/{id}/comment/{commentId}/like")
    suspend fun likeComment(@Path("id") id: String, @Path("commentId") commentId: String): Response<GoodItem>

    // Version
    @GET("api/version")
    suspend fun getVersion(): Response<VersionInfo>

    // Admin
    @GET("api/admin/check")
    suspend fun checkAdmin(): Response<AdminCheckResponse>

    @GET("api/admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUser>>

    @PUT("api/admin/users/{id}")
    suspend fun adminUpdateUser(@Path("id") id: String, @Body request: AdminUpdateRequest): Response<Unit>

    @DELETE("api/admin/users/{id}")
    suspend fun adminDeleteUser(@Path("id") id: String): Response<Unit>

    @GET("api/admin/all-posts")
    suspend fun getAdminAllPosts(): Response<List<AdminPost>>

    @GET("api/admin/users/{id}/posts")
    suspend fun getAdminUserPosts(@Path("id") id: String): Response<List<AdminPost>>

    @PUT("api/admin/posts/{id}/remove")
    suspend fun adminRemovePost(@Path("id") id: String, @Body request: Map<String, String>): Response<Unit>

    @PUT("api/admin/posts/{id}/approve")
    suspend fun adminApprovePost(@Path("id") id: String): Response<Unit>

    @PUT("api/admin/posts/{id}/reject")
    suspend fun adminRejectPost(@Path("id") id: String): Response<Unit>

    @DELETE("api/admin/posts/{id}")
    suspend fun adminDeletePost(@Path("id") id: String): Response<Unit>

    @PUT("api/good-items/{id}/submit-review")
    suspend fun submitForReview(@Path("id") id: String): Response<Unit>

    // Notifications
    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @GET("api/notifications")
    suspend fun getNotifications(@Query("page") page: Int = 1): Response<NotificationListResponse>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @PUT("api/notifications/read-all")
    suspend fun markAllRead(): Response<Unit>

    // Upload
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
}
