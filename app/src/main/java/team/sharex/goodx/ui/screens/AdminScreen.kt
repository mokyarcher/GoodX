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
import team.sharex.goodx.data.remote.AdminUpdateRequest
import team.sharex.goodx.data.remote.AdminUser
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onUserClick: (AdminUser) -> Unit = {},
    onAllPosts: () -> Unit = {}
) {
    BackHandler { onBack() }

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var posts by remember { mutableStateOf<List<AdminPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var mainTab by remember { mutableStateOf("users") }
    var userTab by remember { mutableStateOf("all") }
    var postTab by remember { mutableStateOf("all") }
    var editingUser by remember { mutableStateOf<AdminUser?>(null) }
    var deletingUser by remember { mutableStateOf<AdminUser?>(null) }
    val scope = rememberCoroutineScope()

    fun loadAll() {
        scope.launch {
            isLoading = true
            try {
                val ur = RetrofitClient.apiService.getAdminUsers()
                if (ur.isSuccessful) users = ur.body() ?: emptyList()
                val pr = RetrofitClient.apiService.getAdminAllPosts()
                if (pr.isSuccessful) posts = pr.body() ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("返回", fontSize = 14.sp) }
            Text("后台管理", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        // 主 Tab
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminTabButton("用户", mainTab == "users", Modifier.weight(1f)) { mainTab = "users" }
            AdminTabButton("帖子", mainTab == "posts", Modifier.weight(1f)) { mainTab = "posts" }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            return@Column
        }

        if (mainTab == "users") {
            // 用户子 Tab：全部 / 封禁 / 注销（占位）
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminSubTabButton("全部", userTab == "all", Modifier.weight(1f)) { userTab = "all" }
                AdminSubTabButton("封禁", userTab == "banned", Modifier.weight(1f)) { userTab = "banned" }
                AdminSubTabButton("注销", userTab == "deleted", Modifier.weight(1f)) { userTab = "deleted" }
            }

            val shownUsers = when (userTab) {
                "banned" -> users.filter { it.banned }
                "deleted" -> emptyList() // 先留位置，不做逻辑
                else -> users
            }

            Text("共 ${shownUsers.size} 个用户", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shownUsers, key = { it.id }) { user ->
                    AdminUserCard(
                        user = user,
                        onClick = { onUserClick(user) },
                        onEdit = { editingUser = user },
                        onBanToggle = {
                            scope.launch {
                                try { RetrofitClient.apiService.adminUpdateUser(user.id, AdminUpdateRequest(banned = !user.banned)); loadAll() } catch (_: Exception) { }
                            }
                        },
                        onDelete = { deletingUser = user }
                    )
                }
            }
        } else {
            // 帖子子 Tab：所有 / 匿名 / 下架
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminSubTabButton("所有", postTab == "all", Modifier.weight(1f)) { postTab = "all" }
                AdminSubTabButton("匿名", postTab == "anonymous", Modifier.weight(1f)) { postTab = "anonymous" }
                AdminSubTabButton("下架", postTab == "removed", Modifier.weight(1f)) { postTab = "removed" }
            }

            val shownPosts = when (postTab) {
                "anonymous" -> posts.filter { it.authorId == null }
                "removed" -> posts.filter { it.status == "removed" }
                else -> posts
            }

            Text("共 ${shownPosts.size} 篇帖子", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shownPosts, key = { it.id }) { post ->
                    AdminPostCard(post = post, scope = scope, onReload = { loadAll() })
                }
            }
        }
    }

    // 编辑用户
    editingUser?.let { user ->
        var nickname by remember { mutableStateOf(user.nickname ?: "") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editingUser = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("编辑 ${user.username}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } },
            text = {
                Column {
                    OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("昵称") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("新密码（留空不修改）") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { try { RetrofitClient.apiService.adminUpdateUser(user.id, AdminUpdateRequest(nickname = nickname.takeIf { it.isNotBlank() }, password = password.takeIf { it.length >= 6 })); editingUser = null; loadAll() } catch (_: Exception) { } }
                }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingUser = null }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消") } }
        )
    }

    // 删除用户
    deletingUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("删除用户", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } },
            text = { Text("确定删除 @${user.username}？此操作不可撤销。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminDeleteUser(user.id); deletingUser = null; loadAll() } catch (_: Exception) { } } }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingUser = null }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消") } }
        )
    }
}

@Composable
private fun AdminTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(42.dp).background(if (selected) Accent.copy(alpha = 0.14f) else Surface, RoundedCornerShape(12.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Accent else TextSecondary, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun AdminSubTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(34.dp).background(if (selected) Accent.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Accent else TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AdminUserCard(user: AdminUser, onClick: () -> Unit, onEdit: () -> Unit, onBanToggle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.nickname ?: user.username, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (user.isAdmin) Text(" · 管理员", color = Accent, fontSize = 11.sp)
                    if (user.banned) Text(" · 已封禁", color = LikeRed, fontSize = 11.sp)
                }
                Text("@${user.username}", color = TextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)) { Text("编辑", fontSize = 12.sp) }
            TextButton(onClick = onBanToggle, colors = ButtonDefaults.textButtonColors(contentColor = if (user.banned) Accent else LikeRed)) { Text(if (user.banned) "解封" else "封禁", fontSize = 12.sp) }
            if (!user.isAdmin) TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("删除", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun AdminPostCard(post: AdminPost, scope: kotlinx.coroutines.CoroutineScope, onReload: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Row {
                        Text(post.category ?: "", color = Accent, fontSize = 11.sp)
                        if (post.status == "removed") Text(" · 已下架", color = LikeRed, fontSize = 11.sp)
                        if (post.status == "pending_review") Text(" · 待审核", color = Accent, fontSize = 11.sp)
                        if (post.authorId == null) Text(" · 匿名用户", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
            post.removeReason?.takeIf { it.isNotBlank() }?.let { Text("下架理由：$it", color = LikeRed.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (post.status == "active") TextButton(onClick = { /* 下架从详情页处理 */ }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("下架", fontSize = 12.sp) }
                if (post.status == "pending_review") {
                    TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminApprovePost(post.id); onReload() } catch (_: Exception) {} } }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("通过", fontSize = 12.sp) }
                    TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminRejectPost(post.id); onReload() } catch (_: Exception) {} } }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("拒绝", fontSize = 12.sp) }
                }
                TextButton(onClick = { scope.launch { try { RetrofitClient.apiService.adminDeletePost(post.id); onReload() } catch (_: Exception) {} } }, colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)) { Text("删除", fontSize = 12.sp) }
            }
        }
    }
}
