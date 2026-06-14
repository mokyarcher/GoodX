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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.AdminUpdateRequest
import team.sharex.goodx.data.remote.AdminUser
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun AdminScreen(onBack: () -> Unit, onUserClick: (AdminUser) -> Unit = {}, onAllPosts: () -> Unit = {}) {
    BackHandler { onBack() }

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("users") }
    var editingUser by remember { mutableStateOf<AdminUser?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AdminUser?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadUsers() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getAdminUsers()
                if (response.isSuccessful) users = response.body() ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("返回", fontSize = 14.sp)
            }
            Text("后台管理", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            // 顶部 Tab：用户 / 帖子
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminTabButton("用户", selectedTab == "users", Modifier.weight(1f)) { selectedTab = "users" }
                AdminTabButton("帖子", selectedTab == "posts", Modifier.weight(1f)) { selectedTab = "posts"; onAllPosts() }
            }
            Text(
                text = "共 ${users.size} 个用户",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onUserClick(user) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.nickname ?: user.username, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (user.isAdmin) {
                                        Text(" · 管理员", color = Accent, fontSize = 11.sp)
                                    }
                                    if (user.banned) {
                                        Text(" · 已封禁", color = LikeRed, fontSize = 11.sp)
                                    }
                                }
                                Text("@${user.username}", color = TextSecondary, fontSize = 12.sp)
                            }
                            // 编辑
                            TextButton(
                                onClick = { editingUser = user },
                                colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
                            ) {
                                Text("编辑", fontSize = 12.sp)
                            }
                            // 封禁/解封
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            RetrofitClient.apiService.adminUpdateUser(
                                                user.id, AdminUpdateRequest(banned = !user.banned)
                                            )
                                            loadUsers()
                                        } catch (_: Exception) { }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = if (user.banned) Accent else LikeRed)
                            ) {
                                Text(if (user.banned) "解封" else "封禁", fontSize = 12.sp)
                            }
                            // 删除
                            if (!user.isAdmin) {
                                TextButton(
                                    onClick = { showDeleteConfirm = user },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)
                                ) {
                                    Text("删除", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    editingUser?.let { user ->
        var nickname by remember { mutableStateOf(user.nickname ?: "") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editingUser = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("编辑 ${user.username}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nickname, onValueChange = { nickname = it },
                        label = { Text("昵称") }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("新密码（留空不修改）") }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.apiService.adminUpdateUser(
                                    user.id, AdminUpdateRequest(
                                        nickname = nickname.takeIf { it.isNotBlank() },
                                        password = password.takeIf { it.length >= 6 }
                                    )
                                )
                                editingUser = null
                                loadUsers()
                            } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认
    showDeleteConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("删除用户", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }},
            text = { Text("确定删除 @${user.username}？此操作不可撤销。", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try { RetrofitClient.apiService.adminDeleteUser(user.id); showDeleteConfirm = null; loadUsers() } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LikeRed)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AdminTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(42.dp)
            .background(if (selected) Accent.copy(alpha = 0.14f) else Surface, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Accent else TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
