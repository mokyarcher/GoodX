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
    val category: String,
    val subCategory: String? = null,
    val images: List<String> = emptyList(),
    val link: String? = null
)

data class UpdateGoodItemRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val images: List<String>? = null,
    val link: String? = null
)

data class CommentRequest(val content: String)

// ========== Upload ==========
data class UploadResponse(val url: String, val filename: String, val size: Int)

interface ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<User>

    // Good Items
    @GET("api/good-items")
    suspend fun getGoodItems(
        @Query("category") category: String? = null,
        @Query("sort") sort: String = "newest",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("author") author: String? = null
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

    // Upload
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
}
