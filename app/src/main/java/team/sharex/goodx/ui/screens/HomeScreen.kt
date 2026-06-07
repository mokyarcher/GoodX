package team.sharex.goodx.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import team.sharex.goodx.data.remote.CreateGoodItemRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import java.io.File

enum class HomeTab {
    DISCOVER, ALL, PROFILE
}

@Composable
fun HomeScreen(
    initialTab: HomeTab = HomeTab.DISCOVER,
    onTabChanged: (HomeTab) -> Unit = {},
    onLogout: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onGoodItemClick: (String) -> Unit = {},
    onMyPostsClick: () -> Unit = {},
    onPublishClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    
    // 当外部传入的 initialTab 变化时，同步更新
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    
    // tab 变化时通知外部
    DisposableEffect(selectedTab) {
        onTabChanged(selectedTab)
        onDispose { }
    }

    Scaffold(
        bottomBar = {
            CustomBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onPublishClick = onPublishClick
            )
        },
        containerColor = Background
    ) { padding ->
        when (selectedTab) {
            HomeTab.DISCOVER -> DiscoverTab(onGoodItemClick = onGoodItemClick, modifier = Modifier.padding(padding))
            HomeTab.ALL -> AllCategoriesTab(onCategoryClick = onCategoryClick, modifier = Modifier.padding(padding))
            HomeTab.PROFILE -> ProfileTab(onLogout = onLogout, onMyPostsClick = onMyPostsClick, onPublishClick = onPublishClick, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun CustomBottomNav(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onPublishClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 发现 (weight 1)
            NavItem(
                icon = "◧",
                label = "发现",
                isSelected = selectedTab == HomeTab.DISCOVER,
                onClick = { onTabSelected(HomeTab.DISCOVER) },
                modifier = Modifier.weight(1f)
            )
            // 全部 (weight 1)
            NavItem(
                icon = "◉",
                label = "全部",
                isSelected = selectedTab == HomeTab.ALL,
                onClick = { onTabSelected(HomeTab.ALL) },
                modifier = Modifier.weight(1f)
            )
            // 发布 + 按钮（凸起效果）
            var publishPressed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (publishPressed) Accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        publishPressed = true
                        onPublishClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .size(36.dp)
                        .background(if (publishPressed) Accent.copy(alpha = 0.7f) else Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Light)
                }
                // 自动恢复
                LaunchedEffect(publishPressed) {
                    if (publishPressed) {
                        kotlinx.coroutines.delay(150)
                        publishPressed = false
                    }
                }
            }
            // 搜索（占位）(weight 1)
            NavItem(
                icon = "⌕",
                label = "搜索",
                isSelected = false,
                onClick = { /* TODO: 搜索功能 */ },
                modifier = Modifier.weight(1f)
            )
            // 我的 (weight 1)
            NavItem(
                icon = "☐",
                label = "我的",
                isSelected = selectedTab == HomeTab.PROFILE,
                onClick = { onTabSelected(HomeTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            color = if (isSelected) Accent else TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Accent else TextSecondary
        )
    }
}

// ========== 发现页 ==========
@Composable
fun DiscoverTab(
    onGoodItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var goodItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getGoodItems(sort = "newest")
                if (response.isSuccessful) {
                    goodItems = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadItems() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Text(
            text = "发现",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (goodItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无好物", color = TextSecondary, fontSize = 16.sp)
                    Text("去发布第一个好物吧", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goodItems) { item ->
                    GoodItemCard(item = item, onClick = { onGoodItemClick(item.id) })
                }
            }
        }
    }
}

@Composable
fun GoodItemCard(item: GoodItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧小图
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(TextSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (item.images.isNotEmpty()) {
                AsyncImage(
                    model = "http://124.223.50.79:3002${item.images.first()}",
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(item.category.iconEmoji(), fontSize = 28.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category.displayName(),
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!item.subCategory.isNullOrBlank()) {
                    Text(
                        text = " · ",
                        color = TextSecondary.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "🛒 ${item.subCategory}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("♥", color = Accent, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${item.likes}", color = TextSecondary, fontSize = 11.sp)
                }
                Text(
                    text = formatTimeAgo(item.createdAt),
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 30 -> "${days}天前"
        else -> {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            format.format(date)
        }
    }
}

// ========== 全部页（品类网格） ==========
@Composable
fun AllCategoriesTab(
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Text(
            text = "全部品类",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Category.values().toList()) { category ->
                CategoryGridItem(category = category, onClick = { onCategoryClick(category) })
            }
        }
    }
}

@Composable
fun CategoryGridItem(category: Category, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Surface)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = category.iconEmoji(),
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = category.displayName(),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 我的页 ==========
@Composable
fun ProfileTab(
    onLogout: () -> Unit,
    onMyPostsClick: () -> Unit = {},
    onPublishClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var refreshTrigger by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "◆",
            color = Accent,
            fontSize = 64.sp,
            modifier = Modifier.padding(top = 48.dp, bottom = 16.dp)
        )
        Text(
            text = "GoodX",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "v0.1.0",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        Button(
            onClick = onPublishClick,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("+ 发布好物", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onMyPostsClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("📋 我的发布", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("退出登录", fontSize = 16.sp)
        }
    }

}

// ========== 发布好物对话框 ==========
@Composable
fun CreateGoodItemDialog(
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ELECTRONICS) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // 自动上传
            scope.launch {
                isUploadingImage = true
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    inputStream?.use { stream ->
                        tempFile.outputStream().use { out ->
                            stream.copyTo(out)
                        }
                    }

                    val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

                    val response = RetrofitClient.apiService.uploadImage(body)
                    if (response.isSuccessful) {
                        uploadedImageUrl = response.body()?.url
                    } else {
                        error = "图片上传失败: ${response.errorMessage()}"
                    }
                    tempFile.delete()
                } catch (e: Exception) {
                    error = "图片上传错误: ${e.message}"
                }
                isUploadingImage = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("发布好物", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 图片上传区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(TextSecondary.copy(alpha = 0.1f))
                        .clickable(enabled = !isUploadingImage) { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploadingImage -> CircularProgressIndicator(color = Accent, modifier = Modifier.size(32.dp))
                        uploadedImageUrl != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✓", color = Accent, fontSize = 32.sp)
                                Text("图片已上传", color = Accent, fontSize = 12.sp)
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+", color = TextSecondary, fontSize = 32.sp)
                                Text("点击上传图片", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 品类选择
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedCategory.iconEmoji()} ${selectedCategory.displayName()}")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        Category.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.iconEmoji()} ${cat.displayName()}", color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("标题", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述", color = TextSecondary) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("平台/品牌（可选）", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "请输入标题"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val response = RetrofitClient.apiService.createGoodItem(
                                CreateGoodItemRequest(
                                    title = title,
                                    description = description,
                                    category = selectedCategory.name,
                                    subCategory = platform.takeIf { it.isNotBlank() },
                                    images = uploadedImageUrl?.let { listOf(it) } ?: emptyList()
                                )
                            )
                            if (response.isSuccessful) {
                                // 显示成功提示
                                android.widget.Toast.makeText(context, "✓ 发布成功！", android.widget.Toast.LENGTH_SHORT).show()
                                onCreated()
                                return@launch
                            } else {
                                error = "发布失败: ${response.errorMessage()}"
                            }
                        } catch (e: Exception) {
                            error = "网络错误: ${e.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && !isUploadingImage,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("发布")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("取消")
            }
        }
    )
}
