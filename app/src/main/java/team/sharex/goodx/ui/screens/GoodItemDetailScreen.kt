package team.sharex.goodx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import team.sharex.goodx.data.remote.CommentRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Comment
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.displayName
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary

@Composable
fun GoodItemDetailScreen(
    itemId: String,
    onBack: () -> Unit
) {
    var item by remember { mutableStateOf<GoodItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadDetail() {
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getGoodItemDetail(itemId)
                if (response.isSuccessful) {
                    item = response.body()
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
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "好物详情",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

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
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图片轮播
                item {
                    if (goodItem.images.isNotEmpty()) {
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
                                    text = goodItem.category.displayName(),
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

                // 评论列表
                item {
                    Text(
                        text = "评论 (${goodItem.commentsCount})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (goodItem.comments.isEmpty()) {
                    item {
                        Text(
                            text = "暂无评论，来说两句吧",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    items(goodItem.comments) { comment ->
                        CommentItem(comment = comment)
                    }
                }
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
                    placeholder = { Text("写评论...", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
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
                    shape = RoundedCornerShape(0.dp)
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
    
    Column {
        // 主图显示区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(TextSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (images.isNotEmpty()) {
                AsyncImage(
                    model = "http://124.223.50.79:3002${images[selectedIndex]}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            // 图片计数器
            if (images.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Background.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedIndex + 1} / ${images.size}",
                        color = TextPrimary,
                        fontSize = 12.sp
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
                            .size(40.dp)
                            .background(TextSecondary.copy(alpha = 0.1f))
                            .clickable { selectedIndex = index }
                            .then(
                                if (index == selectedIndex) {
                                    Modifier.border(2.dp, Accent)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        AsyncImage(
                            model = "http://124.223.50.79:3002${images[index]}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LikeSection(
    item: GoodItem,
    onLikeToggle: () -> Unit
) {
    val isLiked = item.likedBy.isNotEmpty()
    
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
fun CommentItem(comment: Comment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comment.user?.nickname ?: comment.user?.username ?: "匿名用户",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTimeAgo(comment.createdAt),
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}
