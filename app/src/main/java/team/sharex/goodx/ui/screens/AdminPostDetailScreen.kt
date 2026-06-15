package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.AdminPost
import team.sharex.goodx.data.remote.AdminUser
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.model.displayName
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

private val GOODX_BASE_URL = RetrofitClient.BASE_URL.removeSuffix("/")

private fun thumbUrl(path: String): String =
    if (path.startsWith("http")) path else "$GOODX_BASE_URL/api/upload/thumb/${path.substringAfterLast('/')}"

@Composable
fun AdminPostDetailScreen(
    post: AdminPost,
    user: AdminUser,
    onBack: () -> Unit,
    onAction: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 把 contentType 字符串映射到 ContentType
    val ct = remember(post.contentType) {
        when (post.contentType?.lowercase()) {
            "moments" -> ContentType.MOMENTS
            "entertainment" -> ContentType.ENTERTAINMENT
            else -> ContentType.GOODS
        }
    }
    val images = post.images.orEmpty()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("返回", fontSize = 14.sp)
            }
            if (post.status == "pending_review") {
                Text("待审核", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("帖子详情", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            // 图片轮播
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { img ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TextSecondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = thumbUrl(img),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // 类型
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${ct.displayName()} · ${post.category ?: ""}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(formatTimeAgo(post.createdAt), color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 标题
                Text(post.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                // 发布者
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (user.nickname ?: user.username).first().uppercase(),
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(TextSecondary.copy(alpha = 0.12f))
                            .wrapContentSize(Alignment.Center)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(user.nickname ?: user.username, color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 内容
                if (!post.description.isNullOrBlank()) {
                    Text(post.description, color = TextSecondary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 下架理由
                post.removeReason?.takeIf { it.isNotBlank() }?.let {
                    Surface(color = LikeRed.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("下架理由", color = LikeRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it, color = LikeRed.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 互动数据
                Row {
                    Text("♥ ${post.likes}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("💬 ${post.commentsCount}", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        // 底部操作区（仅待审核时显示）
        if (post.status == "pending_review") {
            Surface(color = Surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    RetrofitClient.apiService.adminRejectPost(post.id)
                                    onAction()
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LikeRed),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("拒绝", fontSize = 16.sp) }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    RetrofitClient.apiService.adminApprovePost(post.id)
                                    onAction()
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("通过审核", fontSize = 16.sp) }
                }
            }
        }
    }
}
