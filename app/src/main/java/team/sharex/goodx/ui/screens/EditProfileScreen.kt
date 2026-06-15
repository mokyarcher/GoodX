package team.sharex.goodx.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import team.sharex.goodx.data.remote.ChangePasswordRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.data.remote.UpdateProfileRequest
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import java.io.File

private val GOODX_BASE_URL = RetrofitClient.BASE_URL.removeSuffix("/")

private fun avatarUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("http")) path else "$GOODX_BASE_URL$path"
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onUpdated: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nickname by remember { mutableStateOf(TokenManager.getNickname() ?: "") }
    var avatar by remember { mutableStateOf(TokenManager.getAvatar()) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingAvatar = true
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                        ?: return@launch
                    val tempFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                    inputStream.use { stream ->
                        tempFile.outputStream().use { out -> stream.copyTo(out) }
                    }
                    val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                    val response = RetrofitClient.apiService.uploadImage(body)
                    if (response.isSuccessful) {
                        val url = response.body()?.url
                        if (url != null) {
                            avatar = url
                            // 立刻保存头像到后端
                            val profileResp = RetrofitClient.apiService.updateProfile(
                                UpdateProfileRequest(avatar = url)
                            )
                            if (profileResp.isSuccessful) {
                                TokenManager.updateAvatar(url)
                            }
                        }
                    } else {
                        error = "头像上传失败: ${response.errorMessage()}"
                    }
                    tempFile.delete()
                } catch (e: Exception) {
                    error = "头像上传出错: ${e.message}"
                }
                isUploadingAvatar = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Bar
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
            Text(
                text = "编辑资料",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        error = null
                        try {
                            val response = RetrofitClient.apiService.updateProfile(
                                UpdateProfileRequest(nickname = nickname.takeIf { it.isNotBlank() })
                            )
                            if (response.isSuccessful) {
                                response.body()?.let { user ->
                                    TokenManager.updateNickname(user.nickname ?: nickname)
                                }
                                onUpdated()
                            } else {
                                error = "保存失败: ${response.errorMessage()}"
                            }
                        } catch (e: Exception) {
                            error = "网络错误: ${e.message}"
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving && !isUploadingAvatar,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            ) {
                Text("保存", fontSize = 14.sp)
            }
        }

        if (error != null) {
            Text(
                text = error!!,
                color = Accent,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 头像
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.15f))
                    .clickable(enabled = !isUploadingAvatar) { avatarPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingAvatar) {
                    CircularProgressIndicator(
                        color = Accent,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                } else if (!avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl(avatar),
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (TokenManager.getNickname() ?: TokenManager.getUsername() ?: "?")
                            .take(1).uppercase(),
                        color = Accent,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击更换头像",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 用户名（不可修改）
            OutlinedTextField(
                value = TokenManager.getUsername() ?: "",
                onValueChange = {},
                label = { Text("用户名", color = TextSecondary) },
                enabled = false,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = TextSecondary.copy(alpha = 0.15f),
                    disabledTextColor = TextPrimary.copy(alpha = 0.5f),
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 昵称（可修改）
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("昵称", color = TextSecondary) },
                placeholder = { Text("输入展示昵称", color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "用户名用于登录，不可修改。昵称用于展示。",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 修改密码入口
            OutlinedButton(
                onClick = { showPasswordDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔒 修改密码", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 修改密码对话框
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("修改密码", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            if (success) {
                Text("✓ 密码修改成功", color = Accent, fontSize = 15.sp)
            } else {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; error = null },
                        label = { Text("旧密码", color = TextSecondary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; error = null },
                        label = { Text("新密码（至少6位）", color = TextSecondary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        label = { Text("确认新密码", color = TextSecondary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error!!, color = Accent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!success) {
                Button(
                    onClick = {
                        if (oldPassword.isBlank() || newPassword.isBlank()) {
                            error = "请填写所有密码字段"
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            error = "新密码不能少于6位"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            error = "两次输入的新密码不一致"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            error = null
                            try {
                                val response = RetrofitClient.apiService.changePassword(
                                    ChangePasswordRequest(oldPassword, newPassword)
                                )
                                if (response.isSuccessful) {
                                    success = true
                                } else {
                                    error = response.errorMessage()
                                }
                            } catch (e: Exception) {
                                error = "网络错误: ${e.message}"
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("确认修改")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text(if (success) "关闭" else "取消")
            }
        }
    )
}