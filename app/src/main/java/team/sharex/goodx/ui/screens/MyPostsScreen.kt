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
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.TokenManager
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
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

private const val GOODX_BASE_URL = "http://124.223.50.79:3002"

private fun originalImageUrl(path: String): String =
    if (path.startsWith("http")) path else "$GOODX_BASE_URL$path"

private fun thumbnailImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val filename = path.substringAfterLast('/')
    return "$GOODX_BASE_URL/api/upload/thumb/$filename"
}

@Composable
fun MyPostsScreen(
    onBack: () -> Unit,
    onEditItem: (String) -> Unit = {},
    initialShowRemoved: Boolean = false
) {
    BackHandler { onBack() }

    var myItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRemoved by remember { mutableStateOf(initialShowRemoved) }
    var showDeleteConfirm by remember { mutableStateOf<GoodItem?>(null) }
    var showSubmitConfirm by remember { mutableStateOf<GoodItem?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun loadMyItems() {
        scope.launch {
            isLoading = true
            try {
                val meResponse = RetrofitClient.apiService.getMe()
                val userId = meResponse.body()?.id
                val response = RetrofitClient.apiService.getGoodItems(
                    author = userId,
                    sort = "newest",
                    status = if (showRemoved) "removed" else null
                )
                if (response.isSuccessful) {
                    myItems = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }
            isLoading = false
        }
    }

    LaunchedEffect(refreshTrigger, showRemoved) { loadMyItems() }

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
            Text(
                text = "我的发布",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Tab 切换
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = !showRemoved,
                onClick = { showRemoved = false },
                label = { Text("已发布", fontSize = 15.sp) },
                modifier = Modifier.height(40.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent.copy(alpha = 0.15f),
                    selectedLabelColor = Accent
                )
            )
            FilterChip(
                selected = showRemoved,
                onClick = { showRemoved = true },
                label = { Text("已下架", fontSize = 15.sp) },
                modifier = Modifier.height(40.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent.copy(alpha = 0.15f),
                    selectedLabelColor = Accent
                )
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (myItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无发布", color = TextSecondary, fontSize = 16.sp)
                    Text("去发布你的第一个好物吧", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myItems) { item ->
                    MyPostItemCard(
                        item = item,
                        onEdit = {
                            cacheGoodItemPreview(item)
                            onEditItem(item.id)
                        },
                        onDelete = { showDeleteConfirm = item },
                        onSubmitReview = {
                            showSubmitConfirm = item
                        }
                    )
                }
            }
        }
    }

    // 提交审核确认
    showSubmitConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = null },
            containerColor = Surface,
            title = { Text("提交审核", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("确定要将「${item.title}」提交管理员审核吗？\n\n提交后帖子将进入审核状态，审核通过后会自动重新上架。", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitConfirm = null
                        scope.launch {
                            try {
                                val resp = RetrofitClient.apiService.submitForReview(item.id)
                                if (resp.isSuccessful) {
                                    refreshTrigger++
                                    android.widget.Toast.makeText(context, "已提交审核", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, resp.errorMessage(), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) { }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent)
                ) { Text("确认提交") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = null }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = Surface,
            title = { Text("确认删除", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${item.title}」吗？", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteGoodItem(item.id)
                                if (response.isSuccessful) {
                                    showDeleteConfirm = null
                                    refreshTrigger++
                                    android.widget.Toast.makeText(context, "✓ 删除成功！", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MyPostItemCard(
    item: GoodItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSubmitReview: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图片缩略
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(TextSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.images.isNullOrEmpty()) {
                val imagePath = item.images.first()
                var useOriginalImage by remember(imagePath) { mutableStateOf(false) }
                val imageUrl = if (useOriginalImage) originalImageUrl(imagePath) else thumbnailImageUrl(imagePath)

                coil.compose.AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(imageUrl)
                        .crossfade(120)
                        .size(240, 240)
                        .scale(Scale.FILL)
                        .memoryCacheKey(if (useOriginalImage) "original:${imagePath}" else "thumb:${imagePath}")
                        .diskCacheKey(if (useOriginalImage) "original:${imagePath}" else "thumb:${imagePath}")
                        .listener(
                            onError = { _, _ ->
                                if (!useOriginalImage) useOriginalImage = true
                            }
                        )
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(item.category.iconEmoji(), fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 信息
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                when (item.status) {
                    "removed" -> Text(" ⚠️", fontSize = 12.sp)
                    "pending_review" -> Text(" ⏳", fontSize = 12.sp)
                }
            }
            // 下架理由
            item.removeReason?.takeIf { it.isNotBlank() }?.let {
                Text("理由：$it", color = LikeRed.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            // 审核中
            if (item.status == "pending_review") {
                Text("审核中...", color = Accent, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = item.category.displayName(),
                    color = Accent,
                    fontSize = 11.sp
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
            Text(
                text = "♥ ${item.likes}",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 操作按钮
        Column {
            // 编辑（审核中不可编辑）
            if (item.status == "pending_review") {
                Text("审核中，无法编辑", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp)
            } else {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("编辑", fontSize = 12.sp)
                }
            }
            // 提交审核（仅已下架）
            if (item.status == "removed") {
                TextButton(
                    onClick = onSubmitReview,
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("提交审核", fontSize = 12.sp)
                }
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = Accent),
                modifier = Modifier.height(32.dp)
            ) {
                Text("删除", fontSize = 12.sp)
            }
        }
    }
}

