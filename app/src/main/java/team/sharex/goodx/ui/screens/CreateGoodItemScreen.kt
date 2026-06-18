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
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.model.categories
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import java.io.File

val Platforms = listOf("京东", "淘宝", "拼多多", "抖音", "其他")

private fun ContentType.titlePlaceholder(): String = when (this) {
    ContentType.GOODS -> "填写好物名称"
    ContentType.MOMENTS -> "这一刻是什么？"
    ContentType.ENTERTAINMENT -> "这部作品叫什么？"
}

private fun ContentType.contentPlaceholder(): String = when (this) {
    ContentType.GOODS -> "添加正文：购买理由、优缺点、使用体验..."
    ContentType.MOMENTS -> "记录一下当时看到/感受到的东西..."
    ContentType.ENTERTAINMENT -> "为什么值得看/听/读/玩？"
}

private fun ContentType.extraLabel(): String = when (this) {
    ContentType.GOODS -> "品牌 / 平台 / 场景"
    ContentType.MOMENTS -> "地点 / 场景"
    ContentType.ENTERTAINMENT -> "作者 / 平台 / 状态"
}

private fun ContentType.extraPlaceholder(): String = when (this) {
    ContentType.GOODS -> "选择或填写品牌 / 平台 / 场景"
    ContentType.MOMENTS -> "填写地点 / 场景"
    ContentType.ENTERTAINMENT -> "填写作者 / 平台 / 状态"
}

@Composable
fun CreateGoodItemScreen(
    onBack: () -> Unit,
    onPublished: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var selectedContentType by remember { mutableStateOf(ContentType.GOODS) }
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
    val availableCategories = selectedContentType.categories()

    LaunchedEffect(selectedContentType) {
        selectedCategory = availableCategories.first()
        platform = ""
    }

    // 多图选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.let {
            val total = selectedImages.size + it.size
            if (total > 20) {
                error = "最多选择20张图片，已截取前${20 - selectedImages.size}张"
                selectedImages = selectedImages + it.take(20 - selectedImages.size)
            } else {
                selectedImages = selectedImages + it
            }
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
        val publish: () -> Unit = {
            if (title.isBlank()) {
                error = "请填写${selectedContentType.displayName()}标题"
            } else {
                scope.launch {
                    isPublishing = true
                    error = null
                    try {
                        val response = RetrofitClient.apiService.createGoodItem(
                            CreateGoodItemRequest(
                                title = title,
                                description = content,
                                contentType = selectedContentType.name,
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
                    } catch (_: Exception) {
                        error = "网络错误，点击重试"
                    }
                    isPublishing = false
                }
            }
        }

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
                text = "发布${selectedContentType.displayName()}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = publish,
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .then(if (error == "网络错误，点击重试") Modifier.clickable { publish() } else Modifier)
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

            ContentTypeSelector(
                selectedType = selectedContentType,
                onTypeSelected = { selectedContentType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标题输入
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 10) { title = it; error = null } },
                    placeholder = { Text(selectedContentType.titlePlaceholder(), color = TextSecondary.copy(alpha = 0.5f)) },
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
                Text("${title.length}/10", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 正文输入
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                placeholder = { Text(selectedContentType.contentPlaceholder(), color = TextSecondary.copy(alpha = 0.5f)) },
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
                Text("${content.length}/500", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 补充词条
            Text(
                text = selectedContentType.extraLabel(),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (selectedContentType == ContentType.GOODS) {
                PlatformSelector(
                    selectedPlatform = platform,
                    onPlatformSelected = { platform = it }
                )
            } else {
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    placeholder = { Text(selectedContentType.extraPlaceholder(), color = TextSecondary.copy(alpha = 0.5f)) },
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
            }

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
                    availableCategories.forEach { cat ->
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
fun ContentTypeSelector(
    selectedType: ContentType,
    onTypeSelected: (ContentType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ContentType.values().forEach { type ->
            val selected = selectedType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(
                        if (selected) Accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.displayName(),
                    color = if (selected) Accent else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
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
