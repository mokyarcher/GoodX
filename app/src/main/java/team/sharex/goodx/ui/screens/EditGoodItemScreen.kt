package team.sharex.goodx.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.UpdateGoodItemRequest
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.categories
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import java.io.File

private const val GOODX_BASE_URL = "http://124.223.50.79:3002"

private fun thumbUrl(path: String): String =
    if (path.startsWith("http")) path else "$GOODX_BASE_URL/api/upload/thumb/${path.substringAfterLast('/')}"

@Composable
fun EditGoodItemScreen(
    itemId: String,
    onBack: () -> Unit,
    onUpdated: () -> Unit
) {
    BackHandler { onBack() }

    var item by remember { mutableStateOf<GoodItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(itemId) {
        try {
            val resp = RetrofitClient.apiService.getGoodItemDetail(itemId)
            if (resp.isSuccessful) item = resp.body()
        } catch (_: Exception) { }
        loading = false
    }

    if (loading || item == null) {
        Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }

    val goodItem = item!!
    var title by remember { mutableStateOf(goodItem.title) }
    var description by remember { mutableStateOf(goodItem.description ?: "") }
    var platform by remember { mutableStateOf(goodItem.subCategory ?: "") }
    var selectedCategory by remember { mutableStateOf(goodItem.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currentImages by remember { mutableStateOf(goodItem.images.orEmpty()) }
    val hasChanges = title != goodItem.title ||
            description != (goodItem.description ?: "") ||
            platform != (goodItem.subCategory ?: "") ||
            selectedCategory != goodItem.category ||
            currentImages != goodItem.images.orEmpty()
    var newImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val availableCategories = goodItem.contentType.categories()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris?.let {
            val totalAfter = currentImages.size + it.size
            if (totalAfter > 20) {
                error = "图片最多20张，已截取前${20 - currentImages.size}张"
                newImageUris = it.take(20 - currentImages.size)
            } else {
                newImageUris = it
            }
            isUploading = true
        }
    }

    // 上传新图片
    LaunchedEffect(isUploading) {
        if (!isUploading || newImageUris.isEmpty()) return@LaunchedEffect
        val urls = mutableListOf<String>()
        newImageUris.forEachIndexed { index, uri ->
            try {
                val input = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                val temp = File(context.cacheDir, "edit_upload_${index}_${uri.hashCode()}.jpg")
                input.use { s -> temp.outputStream().use { o -> s.copyTo(o) } }
                val body = MultipartBody.Part.createFormData("image", temp.name, temp.asRequestBody("image/*".toMediaTypeOrNull()))
                val resp = RetrofitClient.apiService.uploadImage(body)
                if (resp.isSuccessful) resp.body()?.url?.let { urls.add(it) }
                temp.delete()
            } catch (_: Exception) { }
        }
        currentImages = currentImages + urls
        newImageUris = emptyList()
        isUploading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("取消", fontSize = 14.sp)
            }
            Text("编辑内容", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    if (title.isBlank()) { error = "请填写标题"; return@Button }
                    if (!hasChanges) { error = "未做任何修改"; return@Button }
                    scope.launch {
                        isSaving = true; error = null
                        try {
                            val resp = RetrofitClient.apiService.updateGoodItem(
                                id = goodItem.id,
                                request = UpdateGoodItemRequest(
                                    title = title,
                                    description = description,
                                    contentType = goodItem.contentType.name,
                                    category = selectedCategory.name,
                                    subCategory = platform.takeIf { it.isNotBlank() },
                                    images = currentImages
                                )
                            )
                            if (resp.isSuccessful) {
                                onUpdated()
                            } else error = "保存失败: ${resp.errorMessage()}"
                        } catch (e: Exception) { error = "网络错误: ${e.message}" }
                        isSaving = false
                    }
                },
                enabled = !isSaving && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            ) { Text("保存", fontSize = 14.sp) }
        }

        if (error != null) {
            Text(error!!, color = Accent, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            // 类型（锁定）
            Text("类型：${goodItem.contentType.displayName()}（不可修改）", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // 图片区
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(currentImages) { img ->
                    Box(modifier = Modifier.size(100.dp).background(TextSecondary.copy(alpha = 0.1f))) {
                        AsyncImage(
                            model = thumbUrl(img), contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        // 删除按钮
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                .background(Accent, androidx.compose.foundation.shape.CircleShape)
                                .clickable { currentImages = currentImages - img },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                        }
                    }
                }
                // 添加按钮
                item {
                    Box(
                        modifier = Modifier.size(100.dp).background(TextSecondary.copy(alpha = 0.05f))
                            .clickable(enabled = !isUploading && currentImages.size < 20) { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else if (currentImages.size >= 20) Text("已满", color = TextSecondary, fontSize = 13.sp)
                        else Text("+", color = TextSecondary, fontSize = 32.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            OutlinedTextField(
                value = title, onValueChange = { if (it.length <= 10) { title = it; error = null } },
                placeholder = { Text("标题", color = TextSecondary.copy(alpha = 0.5f)) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 描述
            OutlinedTextField(
                value = description, onValueChange = { if (it.length <= 500) description = it },
                placeholder = { Text("描述", color = TextSecondary.copy(alpha = 0.5f)) },
                minLines = 4, maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 补充词条
            Text(platformLabel(goodItem.contentType), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = platform, onValueChange = { platform = it },
                placeholder = { Text(platformPlaceholder(goodItem.contentType), color = TextSecondary.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 品类
            Text("选择分类", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { categoryExpanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("${selectedCategory.iconEmoji()} ${selectedCategory.displayName()}") }
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(Surface)) {
                    availableCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.iconEmoji()} ${cat.displayName()}", color = TextPrimary) },
                            onClick = { selectedCategory = cat; categoryExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun platformLabel(ct: ContentType): String = when (ct) {
    ContentType.GOODS -> "品牌 / 平台 / 场景"
    ContentType.MOMENTS -> "地点 / 场景"
    ContentType.ENTERTAINMENT -> "作者 / 平台 / 状态"
}
private fun platformPlaceholder(ct: ContentType): String = when (ct) {
    ContentType.GOODS -> "选择或填写品牌 / 平台 / 场景"
    ContentType.MOMENTS -> "填写地点 / 场景"
    ContentType.ENTERTAINMENT -> "填写作者 / 平台 / 状态"
}
