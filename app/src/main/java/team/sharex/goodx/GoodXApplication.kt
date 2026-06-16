package team.sharex.goodx

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.OkHttpClient
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.TokenManager
import java.io.File
import java.util.concurrent.TimeUnit

class GoodXApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        RetrofitClient.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        // 与 Retrofit 共享磁盘缓存目录，避免多份缓存浪费空间
        val cacheDir = File(cacheDir, "okhttp").apply { mkdirs() }
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 100L * 1024 * 1024)) // 100MB 图片缓存
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_disk"))
                    .maxSizeBytes(200L * 1024 * 1024) // 200MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // 忽略服务器缓存头，按 Coil 自身策略缓存
            .build()
    }
}
