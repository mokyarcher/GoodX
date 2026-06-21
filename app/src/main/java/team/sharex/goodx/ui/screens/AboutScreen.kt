package team.sharex.goodx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.sharex.goodx.data.remote.UpdateManager
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Border
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var downloadProgress by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 32.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Text(
                text = "关于GoodX",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // 功能按钮区域（横排样式，类似"我的"页面）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    UpdateManager.checkForUpdate(context) { v, n, u ->
                        updateInfo = Triple(v, n, u)
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Border.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("检查更新", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 版本信息
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GoodX",
                color = Accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "v0.7.7",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "分享值得被看见的东西",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "© 2026 GoodX",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }

    // 更新对话框
    updateInfo?.let { (version, note, url) ->
        val isDownloading = downloadProgress >= 0
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) {
                    updateInfo = null
                    downloadProgress = -1
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isDownloading) "正在下载..." else "发现新版本 $version",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                if (isDownloading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$downloadProgress%",
                            color = Accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            color = Accent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            url,
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(note, color = TextSecondary, fontSize = 14.sp)
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(
                        onClick = {
                            downloadProgress = 0
                            UpdateManager.downloadAndInstall(context, url) { pct ->
                                downloadProgress = pct
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                    ) {
                        Text("立即更新", fontSize = 15.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        updateInfo = null
                        downloadProgress = -1
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(if (isDownloading) "后台下载" else "稍后", fontSize = 15.sp)
                }
            }
        )
    }
}
