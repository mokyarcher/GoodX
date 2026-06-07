package team.sharex.goodx.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import team.sharex.goodx.data.remote.CreateGoodItemRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import java.io.File

val Platforms = listOf("京东", "淘宝", "拼多多", "抖音", "其他")

@Composable
fun CreateGoodItemScreen(
    onBack: () -> Unit,
    onPublished: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ELECTRONICS) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 多图选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.let {
            selectedImages = it
            scope.launch {
                isUploading = true
                val urls = mutableListOf<String>()
                it.forEach { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${urls.size}.jpg")
                        inputStream?.use { stream ->
                            tempFile.outputStream().use { out ->
                                stream.copyTo(out)
                            }
                        }

                        val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

                        val response = RetrofitClient.apiService.uploadImage(body)
                        if (response.isSuccessful) {
                            response.body()?.url?.let { url -> urls.add(url) }
                        }
                        tempFile.delete()
                    } catch (e: Exception) {
                        // ignore single image fail
                    }
                }
                uploadedImageUrls = urls
                isUploading = false
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
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("取消", fontSize = 14.sp)
            }
            Text(
                text = "发布好物",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "请填写好物名称"
                        return@Button
                    }
                    scope.launch {
                        isPublishing = true
                        error = null
                        try {
                            val response = RetrofitClient.apiService.createGoodItem(
                                CreateGoodItemRequest(
                                    title = title,
                                    description = content,
                                    category = selectedCategory.name,
                                    subCategory = platform.takeIf { it.isNotBlank() },
                                    images = uploadedImageUrls
                                )
                            )
                            if (response.isSuccessful) {
                                android.widget.Toast.makeText(context, "✓ 发布成功！", android.widget.Toast.LENGTH_SHORT).show()
                                onPublished()
                                return@launch
                            } else {
                                error = "发布失败: ${response.errorMessage()}"
                            }
                        } catch (e: Exception) {
                            error = "网络错误: ${e.message}"
                        }
                        isPublishing = false
                    }
                },
                enabled = !isPublishing && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("发布", fontSize = 14.sp)
            }
        }

        // 错误提示
        if (error != null) {
            Text(
                text = error!!,
                color = Accent,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 可滚动内容区
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 图片预览区
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(TextSecondary.copy(alpha = 0.1f))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            if (isUploading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Accent,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                    // 添加更多按钮
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(TextSecondary.copy(alpha = 0.05f))
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = TextSecondary, fontSize = 32.sp)
                        }
                    }
                }
            } else {
                // 添加图片按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(TextSecondary.copy(alpha = 0.05f))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+", color = TextSecondary, fontSize = 40.sp)
                        Text("添加图片", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = null },
                placeholder = { Text("填写好物名称", color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 正文输入
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("添加正文：购买理由、优缺点、使用体验...", color = TextSecondary.copy(alpha = 0.5f)) },
                minLines = 6,
                maxLines = 12,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 平台选择
            Text(
                text = "购买平台",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            PlatformSelector(
                selectedPlatform = platform,
                onPlatformSelected = { platform = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 品类选择
            Text(
                text = "选择分类",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { categoryExpanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${selectedCategory.iconEmoji()} ${selectedCategory.displayName()}")
                }
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(Surface)
                ) {
                    Category.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.iconEmoji()} ${cat.displayName()}", color = TextPrimary) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformSelector(
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedPlatform.isNotBlank()) "🛒 $selectedPlatform" else "选择购买平台",
                color = if (selectedPlatform.isNotBlank()) TextPrimary else TextSecondary.copy(alpha = 0.5f)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Surface)
        ) {
            Platforms.forEach { plat ->
                DropdownMenuItem(
                    text = { Text("🛒 $plat", color = TextPrimary) },
                    onClick = {
                        onPlatformSelected(plat)
                        expanded = false
                    }
                )
            }
        }
    }
}
