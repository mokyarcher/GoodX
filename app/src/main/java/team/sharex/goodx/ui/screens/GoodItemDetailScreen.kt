package team.sharex.goodx.ui.screens

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import team.sharex.goodx.data.remote.CommentRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Comment
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.displayName
import team.sharex.goodx.ui.components.LiquidGlassCard
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

private val GOODX_BASE_URL = RetrofitClient.BASE_URL.removeSuffix("/")
private val viewedOriginalImages = mutableStateSetOf<String>()
private val goodItemPreviewCache = mutableStateMapOf<String, GoodItem>()

fun cacheGoodItemPreview(item: GoodItem) {
    goodItemPreviewCache[item.id] = item
}

private fun originalImageUrl(path: String): String =
    if (path.startsWith("http")) path else "$GOODX_BASE_URL$path"

private fun thumbnailImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val filename = path.substringAfterLast('/')
    return "$GOODX_BASE_URL/api/upload/thumb/$filename"
}

private fun previewImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val filename = path.substringAfterLast('/')
    return "$GOODX_BASE_URL/api/upload/preview/$filename"
}

// 原图预加载已移除：慢带宽下后台下载 20MB 原图会堵塞 Coil 队列，导致 preview/thumb 加载失败。
// 原图仅在用户主动点击"查看原图"时按需加载。

@Composable
fun GoodItemDetailScreen(
    itemId: String,
    onBack: () -> Unit
) {
    var item by remember(itemId) { mutableStateOf(goodItemPreviewCache[itemId]) }
    var isLoading by remember(itemId) { mutableStateOf(item == null) }
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<Comment?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadDetail() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getGoodItemDetail(itemId)
                if (response.isSuccessful) {
                    response.body()?.let { detail ->
                        goodItemPreviewCache[itemId] = detail
                        item = detail
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            isLoading = false
        }
    }

    LaunchedEffect(itemId) { loadDetail() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("好物不存在", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            val goodItem = item!!
            
            // Top Bar
            Text(
                text = "详情",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 48.dp, bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图片轮播
                item {
                    if (!goodItem.images.isNullOrEmpty()) {
                        ImageCarousel(images = goodItem.images)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(TextSecondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无图片", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                // 图片与正文分隔线
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.8.dp,
                        color = TextSecondary.copy(alpha = 0.15f)
                    )
                }

                // 标题和分类
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${goodItem.contentType.displayName()} · ${goodItem.category.displayName()}",
                                    color = Accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!goodItem.subCategory.isNullOrBlank()) {
                                    Text(
                                        text = " · ",
                                        color = TextSecondary.copy(alpha = 0.3f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "🛒 ${goodItem.subCategory}",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Text(
                                text = formatTimeAgo(goodItem.createdAt),
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // 发布者
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val authorAvatar = goodItem.author?.avatar
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(TextSecondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!authorAvatar.isNullOrBlank()) {
                                    val avatarThumbUrl = if (authorAvatar.startsWith("http")) authorAvatar
                                        else "$GOODX_BASE_URL/api/upload/thumb/${authorAvatar.substringAfterLast('/')}"
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(avatarThumbUrl)
                                            .size(72, 72)
                                            .scale(Scale.FIT)
                                            .crossfade(80)
                                            .memoryCacheKey("avatar-thumb:$authorAvatar")
                                            .diskCacheKey("avatar-thumb:$authorAvatar")
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = (goodItem.author?.nickname ?: goodItem.author?.username ?: "?").first().uppercase(),
                                        color = Accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = goodItem.author?.nickname ?: goodItem.author?.username ?: "匿名",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = goodItem.title,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!goodItem.description.isNullOrBlank()) {
                            Text(
                                text = goodItem.description,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // 点赞区域
                item {
                    LikeSection(
                        item = goodItem,
                        onLikeToggle = {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.likeGoodItem(goodItem.id)
                                    if (response.isSuccessful) {
                                        item = response.body()
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }
                    )
                }

                // 正文与评论分隔线
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.8.dp,
                        color = TextSecondary.copy(alpha = 0.15f)
                    )
                }

                // 评论区域：统一卡片容器
                item {
                    CommentSection(
                        comments = goodItem.comments.orEmpty(),
                        commentsCount = goodItem.commentsCount,
                        onLike = { commentId ->
                            scope.launch {
                                try {
                                    val r = RetrofitClient.apiService.likeComment(goodItem.id, commentId)
                                    if (r.isSuccessful) item = r.body()
                                } catch (_: Exception) { }
                            }
                        },
                        onReply = { replyTarget = it }
                    )
                }
            }

            // 回复评论弹窗
            if (replyTarget != null) {
                AlertDialog(
                    onDismissRequest = { replyTarget = null; replyText = "" },
                    containerColor = Surface,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = "回复 ${replyTarget?.user?.nickname ?: replyTarget?.user?.username ?: "匿名"}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("写回复...", color = TextSecondary, fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (replyText.isBlank()) return@Button
                                scope.launch {
                                    isSendingReply = true
                                    try {
                                        val response = RetrofitClient.apiService.addComment(
                                            id = goodItem.id,
                                            request = CommentRequest(
                                                content = replyText.trim(),
                                                parentId = replyTarget?.id
                                            )
                                        )
                                        if (response.isSuccessful) {
                                            replyText = ""
                                            replyTarget = null
                                            item = response.body()
                                        }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    isSendingReply = false
                                }
                            },
                            enabled = !isSendingReply && replyText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSendingReply) {
                                CircularProgressIndicator(
                                    color = TextPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("发送", fontSize = 13.sp)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { replyTarget = null; replyText = "" },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("取消", fontSize = 14.sp)
                        }
                    }
                )
            }

            // 底部评论输入
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("写评论...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (commentText.isBlank()) return@Button
                        scope.launch {
                            isSending = true
                            try {
                                val response = RetrofitClient.apiService.addComment(
                                    id = goodItem.id,
                                    request = CommentRequest(commentText.trim())
                                )
                                if (response.isSuccessful) {
                                    commentText = ""
                                    item = response.body()
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                            isSending = false
                        }
                    },
                    enabled = !isSending && commentText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("发送", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageCarousel(images: List<String>) {
    var selectedIndex by remember { mutableStateOf(0) }
    var viewerVisible by remember { mutableStateOf(false) }
    var originalLoaded by remember(images) { mutableStateOf(images.filter { it in viewedOriginalImages }.toSet()) }
    val context = LocalContext.current

    // 不再预加载原图，避免堵塞图片队列

    val pagerState = rememberPagerState(pageCount = { images.size })

    Column {
        // 主图显示区：支持左右滑动切图
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { images[it] },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TextSecondary.copy(alpha = 0.1f))
                .clickable { viewerVisible = true }
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DetailCompressedImage(
                    imagePath = images[page],
                    modifier = Modifier.fillMaxSize(),
                    size = 1280,
                    usePreview = true
                )
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            selectedIndex = pagerState.currentPage
        }
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(selectedIndex)
            }
        }

        // 图片计数器
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Background.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 缩略图列表
        if (images.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TextSecondary.copy(alpha = 0.1f))
                            .clickable { selectedIndex = index }
                            .then(
                                if (index == selectedIndex) {
                                    Modifier.border(2.dp, Accent, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        DetailCompressedImage(
                            imagePath = images[index],
                            modifier = Modifier.fillMaxSize(),
                            size = 180,
                            usePreview = false
                        )
                    }
                }
            }
        }
    }

    if (viewerVisible && images.isNotEmpty()) {
        FullscreenImageViewer(
            images = images,
            initialIndex = selectedIndex,
            originalLoaded = originalLoaded,
            onOriginalLoaded = {
                viewedOriginalImages.add(it)
                originalLoaded = originalLoaded + it
            },
            onIndexChanged = { selectedIndex = it },
            onDismiss = { viewerVisible = false }
        )
    }
}

@Composable
private fun DetailCompressedImage(
    imagePath: String,
    modifier: Modifier = Modifier,
    size: Int,
    usePreview: Boolean
) {
    var retryKey by remember(imagePath, usePreview) { mutableStateOf(0) }
    var useOriginalFallback by remember(imagePath, usePreview) { mutableStateOf(false) }
    val compressedUrl = if (usePreview) previewImageUrl(imagePath) else thumbnailImageUrl(imagePath)
    val imageUrl = if (useOriginalFallback) originalImageUrl(imagePath) else compressedUrl
    val cachePrefix = if (usePreview) "detail-preview" else "detail-thumb"

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(120)
            .size(size, size)
            .scale(Scale.FILL)
            .memoryCacheKey(if (useOriginalFallback) "detail-original-fallback:$imagePath" else "$cachePrefix:$imagePath:$size")
            .diskCacheKey(if (useOriginalFallback) "detail-original-fallback:$imagePath" else "$cachePrefix:$imagePath:$size")
            .listener(
                onError = { _, _ ->
                    if (!useOriginalFallback) {
                        if (retryKey < 1) {
                            retryKey += 1
                        } else {
                            useOriginalFallback = true
                        }
                    }
                }
            )
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

@Composable
private fun FullscreenImageViewer(
    images: List<String>,
    initialIndex: Int,
    originalLoaded: Set<String>,
    onOriginalLoaded: (String) -> Unit,
    onIndexChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { images.size }, initialPage = initialIndex.coerceIn(0, images.lastIndex))
    val selectedIndex = pagerState.currentPage
    val context = LocalContext.current

    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, key = { images[it] }, userScrollEnabled = true, modifier = Modifier.fillMaxSize()) { page ->
            val imagePath = images[page]
            var originalReady by remember(imagePath) { mutableStateOf(false) }
            var previewReady by remember(imagePath) { mutableStateOf(false) }
            var thumbnailReady by remember(imagePath) { mutableStateOf(false) }
            var imageScale by remember(imagePath) { mutableStateOf(1f) }
            var imageOffsetX by remember(imagePath) { mutableStateOf(0f) }
            var imageOffsetY by remember(imagePath) { mutableStateOf(0f) }

            Box(
                modifier = Modifier.fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            ) {
                // 缩略图兜底
                if (!previewReady && !originalReady) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailImageUrl(imagePath)).crossfade(80).scale(Scale.FIT)
                            .memoryCacheKey("detail-thumb:$imagePath:180").diskCacheKey("detail-thumb:$imagePath:180")
                            .listener(onSuccess = { _, _ -> thumbnailReady = true }).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                // 预览图默认显示
                if (!originalReady) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(previewImageUrl(imagePath)).crossfade(160).scale(Scale.FIT)
                            .memoryCacheKey("detail-preview:$imagePath:1280").diskCacheKey("detail-preview:$imagePath:1280")
                            .listener(onStart = { thumbnailReady = true }, onSuccess = { _, _ -> previewReady = true }).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                // 原图自动加载，成功显示后覆盖 preview
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(originalImageUrl(imagePath)).crossfade(false).scale(Scale.FIT)
                        .memoryCacheKey("viewer-original:$imagePath").diskCacheKey("viewer-original:$imagePath").build(),
                    onSuccess = { originalReady = true; onOriginalLoaded(imagePath) },
                    onError = { originalReady = false },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = imageScale; scaleY = imageScale; translationX = imageOffsetX; translationY = imageOffsetY }
                        .pointerInput(originalReady, imageScale) {
                            if (originalReady && imageScale > 1.01f) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val ns = (imageScale * zoom).coerceIn(1f, 5f)
                                    imageScale = ns
                                    if (ns <= 1.01f) { imageScale = 1f; imageOffsetX = 0f; imageOffsetY = 0f }
                                    else { imageOffsetX += pan.x; imageOffsetY += pan.y }
                                }
                            }
                        },
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                // 加载指示器：只在预览/缩略/原图都未就绪时显示
                if (!originalReady && !previewReady && !thumbnailReady) {
                    Box(
                        modifier = Modifier.align(Alignment.Center).size(54.dp)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.34f), RoundedCornerShape(27.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        Text(
            text = "关闭",
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 36.dp)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.34f), RoundedCornerShape(16.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 42.dp, end = 42.dp, bottom = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerOutlineButton(
                text = "⇩",
                minWidth = 40.dp,
                onClick = { downloadOriginalImage(context, images[pagerState.currentPage]) }
            )

            Spacer(modifier = Modifier.width(88.dp))

            ViewerOutlineButton(
                text = "${selectedIndex + 1} / ${images.size}",
                minWidth = 52.dp,
                onClick = { }
            )
        }
    }
}
}

@Composable
private fun ViewerOutlineButton(
    text: String,
    minWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .defaultMinSize(minWidth = minWidth)
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun downloadOriginalImage(context: Context, imagePath: String) {
    val appContext = context.applicationContext
    val url = originalImageUrl(imagePath)
    val filename = imagePath.substringAfterLast('/').ifBlank { "goodx-image.jpg" }

    Toast.makeText(context, "开始下载原图", Toast.LENGTH_SHORT).show()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val bytes = URL(url).openStream().use { it.readBytes() }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, guessImageMimeType(filename))
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = appContext.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("无法创建相册文件")

                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("无法写入相册文件")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, filename)
                file.writeBytes(bytes)
                MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), arrayOf(guessImageMimeType(filename)), null)
            }

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(appContext, "成功保存到相册", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(appContext, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun guessImageMimeType(filename: String): String = when (filename.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    else -> "image/jpeg"
}

@Composable
private fun ImageNavButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                androidx.compose.ui.graphics.Color.Black.copy(alpha = if (enabled) 0.36f else 0.12f),
                RoundedCornerShape(23.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = if (enabled) 0.92f else 0.28f),
            fontSize = 34.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun LikeSection(
    item: GoodItem,
    onLikeToggle: () -> Unit
) {
    val isLiked = !item.likedBy.isNullOrEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onLikeToggle() }
        ) {
            Text(
                text = if (isLiked) "❤️" else "🤍",
                fontSize = 32.sp
            )
            Text(
                text = "${item.likes}",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CommentSection(
    comments: List<Comment>,
    commentsCount: Int,
    onLike: (String) -> Unit,
    onReply: (Comment) -> Unit
) {
    Column {
        Text(
            text = "评论 ($commentsCount)",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (comments.isEmpty()) {
            Text(
                text = "暂无评论，来说两句吧",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        } else {
            comments.forEachIndexed { index, comment ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    blurRadius = 8.dp,
                    tintColor = Color.White.copy(alpha = 0.10f),
                    accentColor = Accent.copy(alpha = 0.15f),
                    borderAlpha = 0.18f
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        CommentItem(
                            comment = comment,
                            onLike = onLike,
                            onReply = onReply
                        )
                        comment.replies?.forEach { reply ->
                            CommentItem(
                                comment = reply,
                                parentComment = comment,
                                onLike = onLike,
                                onReply = { onReply(comment) },
                                isReply = true
                            )
                        }
                    }
                }
                if (index < comments.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    parentComment: Comment? = null,
    onLike: (String) -> Unit = {},
    onReply: (Comment) -> Unit = {},
    isReply: Boolean = false
) {
    val commentId = comment.id.orEmpty()
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 32.dp else 0.dp,
                end = 0.dp,
                top = if (isReply) 4.dp else 6.dp,
                bottom = if (isReply) 4.dp else 6.dp
            )
            .clickable { onReply(comment) },
        verticalAlignment = Alignment.Top
    ) {
        // 头像
        val avatarSize = if (isReply) 26.dp else 36.dp
        val avatarUrl = comment.user?.avatar
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(TextSecondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                val avatarThumbUrl = if (avatarUrl.startsWith("http")) avatarUrl
                else "$GOODX_BASE_URL/api/upload/thumb/${avatarUrl.substringAfterLast('/')}"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarThumbUrl)
                        .size(72, 72)
                        .scale(Scale.FIT)
                        .crossfade(80)
                        .memoryCacheKey("avatar-thumb:$avatarUrl")
                        .diskCacheKey("avatar-thumb:$avatarUrl")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = (comment.user?.nickname ?: comment.user?.username ?: "?").first().uppercase(),
                    color = Accent,
                    fontSize = if (isReply) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 中间内容
        Column(modifier = Modifier.weight(1f)) {
            // 昵称行：回复显示「回复者 ▶ 被回复者」
            if (isReply && parentComment != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.nickname ?: comment.user?.username ?: "匿名",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▶",
                        color = Accent.copy(alpha = 0.8f),
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = parentComment.user?.nickname ?: parentComment.user?.username ?: "匿名",
                        color = Accent.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = comment.user?.nickname ?: comment.user?.username ?: "匿名用户",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.content.orEmpty(),
                color = TextPrimary,
                fontSize = if (isReply) 12.sp else 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimeAgo(comment.createdAt),
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "回复",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onReply(comment) }
                )
            }
        }

        // 点赞
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onLike(commentId) }
        ) {
            Text(
                text = if (comment.likesCount > 0) "♥" else "♡",
                color = if (comment.likesCount > 0) LikeRed else TextSecondary.copy(alpha = 0.35f),
                fontSize = if (isReply) 13.sp else 15.sp
            )
            if (comment.likesCount > 0) {
                Text(
                    text = "${comment.likesCount}",
                    color = if (comment.likesCount > 0) LikeRed else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }

    // 渲染嵌套回复
    comment.replies?.forEach { reply ->
        CommentItem(
            comment = reply,
            parentComment = comment,
            onLike = onLike,
            onReply = { onReply(comment) },
            isReply = true
        )
    }
}
