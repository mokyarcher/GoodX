package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.AdminPost
import team.sharex.goodx.data.remote.AdminUser
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun AdminUserPostsScreen(
    user: AdminUser,
    onBack: () -> Unit,
    onPostClick: (AdminPost) -> Unit = {}
) {
    BackHandler { onBack() }

    var posts by remember { mutableStateOf<List<AdminPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRemoveDialog by remember { mutableStateOf<AdminPost?>(null) }
    var removeReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadPosts() {
        scope.launch {
            isLoading = true
            try {
                val resp = RetrofitClient.apiService.getAdminUserPosts(user.id)
                if (resp.isSuccessful) posts = resp.body() ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(user.id) { loadPosts() }

    val activePosts = posts.filter { it.status == "active" }
    val removedPosts = posts.filter { it.status == "removed" }
    val pendingPosts = posts.filter { it.status == "pending_review" }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("返回", fontSize = 14.sp)
            }
            Text("${user.nickname ?: user.username} 的帖子", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 待审核
                if (pendingPosts.isNotEmpty()) {
                    item { Text("⏳ 待审核 (${pendingPosts.size})", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(pendingPosts, key = { it.id }) { post ->
                        AdminPostCard(post, "pending", scope, { loadPosts() }, { onPostClick(it) }, { showRemoveDialog = it })
                    }
                }
                // 正常
                item { Text("✓ 已发布 (${activePosts.size})", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                items(activePosts, key = { it.id }) { post ->
                    AdminPostCard(post, "active", scope, { loadPosts() }, { onPostClick(it) }, { showRemoveDialog = it })
                }
                // 已下架
                if (removedPosts.isNotEmpty()) {
                    item { Text("⚠️ 已下架 (${removedPosts.size})", color = LikeRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(removedPosts, key = { it.id }) { post ->
                        AdminPostCard(post, "removed", scope, { loadPosts() }, { onPostClick(it) }, { showRemoveDialog = it })
                    }
                }
            }
        }
    }

    // 下架理由弹窗
    showRemoveDialog?.let { post ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null; removeReason = "" },
            containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 24.dp),
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("下架帖子", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } },
            text = {
                Column {
                    Text("「${post.title}」", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = removeReason, onValueChange = { removeReason = it },
                        label = { Text("下架理由", color = TextSecondary) },
                        minLines = 2, maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (removeReason.isBlank()) return@TextButton
                        scope.launch {
                            try { RetrofitClient.apiService.adminRemovePost(post.id, mapOf("reason" to removeReason)); showRemoveDialog = null; removeReason = ""; loadPosts() } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)
                ) { Text("确认下架") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = null; removeReason = "" }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消") } }
        )
    }

}

@Composable
private fun AdminPostCard(
    post: AdminPost,
    type: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onReload: () -> Unit,
    onView: (AdminPost) -> Unit,
    onRemoveClick: (AdminPost) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.clickable { onView(post) }.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.category ?: "", color = Accent, fontSize = 11.sp)
                        if (post.status == "pending_review") Text(" · 待审核", color = Accent, fontSize = 11.sp)
                        if (post.status == "removed") Text(" · ⚠️", color = LikeRed, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("♥${post.likes} 💬${post.commentsCount}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Text("查看 ›", color = TextSecondary, fontSize = 13.sp)
            }
            // 下架理由
            post.removeReason?.takeIf { it.isNotBlank() }?.let {
                Text("下架理由：$it", color = LikeRed.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            // 操作按钮
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                when (type) {
                    "active" -> {
                        TextButton(onClick = { onRemoveClick(post) }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("下架", fontSize = 12.sp) }
                    }
                    "pending" -> {
                        TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminApprovePost(post.id); onReload() } catch (_: Exception) { } } }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("通过", fontSize = 12.sp) }
                        TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminRejectPost(post.id); onReload() } catch (_: Exception) { } } }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("拒绝", fontSize = 12.sp) }
                    }
                    "removed" -> {}
                }
                TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminDeletePost(post.id); onReload() } catch (_: Exception) { } } }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("删除", fontSize = 12.sp) }
            }
        }
    }
}
