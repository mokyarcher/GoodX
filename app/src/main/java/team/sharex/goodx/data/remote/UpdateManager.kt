package team.sharex.goodx.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    fun checkForUpdate(context: Context, onResult: (version: String, note: String, apkUrl: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val info = RetrofitClient.apiService.getVersion()
                if (!info.isSuccessful || info.body() == null) return@launch
                val remote = info.body()!!
                val localCode = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .longVersionCode.toInt()

                if (remote.versionCode <= localCode) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(remote.version, remote.note, remote.apkUrl)
                }
            } catch (_: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun downloadAndInstall(context: Context, url: String, onProgress: (Int) -> Unit) {
        val appContext = context.applicationContext
        // 每次用唯一文件名，避免 FileProvider 缓存旧 content URI
        val file = File(appContext.cacheDir, "goodx_v${System.currentTimeMillis()}.apk")
        // 清理旧更新包
        appContext.cacheDir.listFiles { f -> f.name.startsWith("goodx_v") && f.name.endsWith(".apk") }
            ?.forEach { it.delete() }

        Toast.makeText(appContext, "开始下载更新...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.useCaches = false
                connection.setRequestProperty("Cache-Control", "no-cache, no-store")
                connection.connect()

                val total = connection.contentLength
                val input = connection.inputStream

                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var lastPct = -1
                    var bytes: Int

                    while (true) {
                        bytes = input.read(buffer)
                        if (bytes < 0) break
                        output.write(buffer, 0, bytes)
                        downloaded += bytes

                        if (total > 0) {
                            val pct = ((downloaded * 100) / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                CoroutineScope(Dispatchers.Main).launch { onProgress(pct) }
                            }
                        }
                    }
                }
                input.close()
                connection.disconnect()

                // 验证下载完整性
                if (total > 0 && file.length() < total * 0.95) {
                    file.delete()
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(appContext, "下载不完整，请重试", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                CoroutineScope(Dispatchers.Main).launch {
                    onProgress(100)
                    Toast.makeText(appContext, "下载完成，准备安装...", Toast.LENGTH_SHORT).show()
                    installApk(appContext, file)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(appContext, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists() || file.length() < 1000) {
            Toast.makeText(context, "安装文件无效", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        // 使用 ACTION_VIEW 而非 ACTION_INSTALL_PACKAGE，兼容 ColorOS 等严格系统
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        context.startActivity(intent)
    }
}
