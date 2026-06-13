package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.AppNotification
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun NotificationsScreen(onBack: () -> Unit, onPostClick: (String, String?) -> Unit = { _, _ -> }) {
    BackHandler { onBack() }

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            try {
                val resp = RetrofitClient.apiService.getNotifications()
                if (resp.isSuccessful) notifications = resp.body()?.notifications ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("消息中心", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            try { RetrofitClient.apiService.markAllRead(); load() } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text("全部已读", fontSize = 13.sp) }
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无消息", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick = {
                            if (!notif.read) {
                                scope.launch {
                                    try { RetrofitClient.apiService.markNotificationRead(notif.id); load() } catch (_: Exception) { }
                                }
                            }
                            notif.relatedPostId?.let { onPostClick(it, notif.extra) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    Surface(
        color = if (notification.read) Surface else Accent.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 未读标记
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .offset(y = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1
                )
                if (notification.message.isNotBlank()) {
                    Text(
                        notification.message,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    formatTimeAgo(notification.createdAt),
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
