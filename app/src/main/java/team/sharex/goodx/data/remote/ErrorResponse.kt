package team.sharex.goodx.data.remote

import com.google.gson.Gson
import retrofit2.Response

data class ErrorResponse(
    val message: String? = null,
    val error: String? = null
)

fun <T> Response<T>.errorMessage(): String {
    return try {
        val body = errorBody()?.string()
        if (!body.isNullOrBlank()) {
            val err = Gson().fromJson(body, ErrorResponse::class.java)
            err.message ?: err.error ?: message()
        } else {
            message()
        }
    } catch (e: Exception) {
        message()
    }
}
