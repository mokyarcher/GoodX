package team.sharex.goodx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.RegisterRequest
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "注册 GoodX",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Text(
            text = "创建你的账号",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
            label = { Text("用户名", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("昵称（可选）", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("密码", color = TextSecondary) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Accent,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "请填写用户名和密码"
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val response = RetrofitClient.apiService.register(
                            RegisterRequest(username, password, nickname.takeIf { it.isNotBlank() })
                        )
                        if (response.isSuccessful) {
                            val auth = response.body()
                            if (auth != null) {
                                TokenManager.saveToken(auth.token)
                                onRegisterSuccess()
                                return@launch
                            }
                        } else {
                            errorMessage = "注册失败：${response.errorMessage()}"
                        }
                    } catch (e: Exception) {
                        errorMessage = "网络错误：${e.message}"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Accent.copy(alpha = 0.5f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("注册", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
        ) {
            Text("已有账号？去登录")
        }
    }
}
