package team.sharex.goodx.data.remote

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import team.sharex.goodx.model.GoodItem
import java.io.File

object RetrofitClient {
    // AVD 模拟器用 10.0.2.2 访问本机
    // 真机测试时改为服务器地址: http://111.229.166.216:3002/
    const val BASE_URL = "http://111.229.166.216:3002/"
    private const val CACHE_SIZE = 20L * 1024 * 1024 // 20MB OkHttp cache

    // 内存数据缓存（Splash 预加载共享给 DiscoverTab）
    var goodItemsCache: List<GoodItem>? = null
    var cacheTimestamp: Long = 0L
    const val CACHE_VALID_MS = 60_000L // 60秒内有效

    lateinit var apiService: ApiService
        private set

    fun init(context: Context) {
        val cacheDir = File(context.cacheDir, "okhttp")
        val cache = Cache(cacheDir, CACHE_SIZE)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor())
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
