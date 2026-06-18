package team.sharex.goodx.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background

private val GOODX_BASE_URL = RetrofitClient.BASE_URL.removeSuffix("/")

@Composable
fun SplashScreen(onReady: () -> Unit) {
    var isReady by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(targetValue = if (isReady) 0f else 1f, animationSpec = tween(300))
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        awaitAll(
            async(Dispatchers.IO) {
                try {
                    val r = RetrofitClient.apiService.getGoodItems(sort = "newest", limit = 30)
                    r.body()?.let { items ->
                        RetrofitClient.goodItemsCache = items
                        RetrofitClient.cacheTimestamp = System.currentTimeMillis()
                        val loader = context.imageLoader
                        items.take(6).forEach { item ->
                            item.images?.firstOrNull()?.let { path ->
                                val filename = path.substringAfterLast('/')
                                val url = "$GOODX_BASE_URL/api/upload/thumb/$filename"
                                loader.enqueue(ImageRequest.Builder(context).data(url).build())
                            }
                        }
                    }
                } catch (_: Exception) {}
            },
            async(Dispatchers.IO) {
                try { RetrofitClient.apiService.getMe() } catch (_: Exception) {}
                try { RetrofitClient.apiService.getUnreadCount() } catch (_: Exception) {}
            }
        )

        val elapsed = System.currentTimeMillis() - startTime
        val remaining = 1000L - elapsed
        if (remaining > 0) delay(remaining)

        isReady = true
        delay(300)
        onReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "◆",
                color = Accent,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "GoodX",
                color = Accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "分享值得被看见的东西",
                color = team.sharex.goodx.ui.theme.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
