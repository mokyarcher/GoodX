package team.sharex.goodx.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import team.sharex.goodx.data.remote.CreateGoodItemRequest
import team.sharex.goodx.data.remote.RetrofitClient
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.data.remote.UpdateManager
import team.sharex.goodx.data.remote.errorMessage
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.model.GoodItem
import team.sharex.goodx.model.categories
import team.sharex.goodx.model.description
import team.sharex.goodx.model.displayName
import team.sharex.goodx.model.iconEmoji
import team.sharex.goodx.model.subtitle
import team.sharex.goodx.ui.components.LiquidGlassBackdrop
import team.sharex.goodx.ui.components.LiquidGlassCard
import team.sharex.goodx.ui.theme.Accent
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.BackgroundSecondary
import team.sharex.goodx.ui.theme.Border
import team.sharex.goodx.ui.theme.LikeRed
import team.sharex.goodx.ui.theme.Surface
import team.sharex.goodx.ui.theme.TextPrimary
import team.sharex.goodx.ui.theme.TextSecondary
import team.sharex.goodx.ui.theme.TextTertiary
import java.io.File

private const val GOODX_BASE_URL = "http://124.223.50.79:3002"

private fun thumbnailImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val filename = path.substringAfterLast('/')
    return "$GOODX_BASE_URL/api/upload/thumb/$filename"
}

enum class HomeTab { DISCOVER, ALL, CIRCLES, PROFILE }

@Composable
fun HomeScreen(
    initialTab: HomeTab = HomeTab.DISCOVER,
    onTabChanged: (HomeTab) -> Unit = {},
    onLogout: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onContentTypeClick: (ContentType) -> Unit = {},
    onGoodItemClick: (String) -> Unit = {},
    onMyPostsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onPublishClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onAdminClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }
    DisposableEffect(selectedTab) { onTabChanged(selectedTab); onDispose { } }

    Scaffold(
        bottomBar = {
            CustomBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, onPublishClick = onPublishClick)
        },
        containerColor = Background
    ) { padding ->
        val tabs = HomeTab.values()
        val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = tabs.indexOf(selectedTab))

        var disablePagerSync by remember { mutableStateOf(false) }

        LaunchedEffect(selectedTab) {
            val idx = tabs.indexOf(selectedTab)
            if (idx != pagerState.currentPage) {
                disablePagerSync = true
                pagerState.animateScrollToPage(idx)
                // 动画完成后恢复 pager→tab 同步
                disablePagerSync = false
            }
        }
        LaunchedEffect(pagerState.currentPage) {
            if (!disablePagerSync) {
                selectedTab = tabs[pagerState.currentPage]
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
            when (tabs[page]) {
                HomeTab.DISCOVER -> DiscoverTab(onGoodItemClick = onGoodItemClick, modifier = Modifier.fillMaxSize())
                HomeTab.ALL -> AllCategoriesTab(onGoodItemClick = onGoodItemClick, modifier = Modifier.fillMaxSize())
                HomeTab.CIRCLES -> CirclesTab(modifier = Modifier.fillMaxSize())
                HomeTab.PROFILE -> ProfileTab(onLogout = onLogout, onMyPostsClick = onMyPostsClick, onPublishClick = onPublishClick, onEditProfileClick = onEditProfileClick, onAdminClick = onAdminClick, onNotificationsClick = onNotificationsClick, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ============================================
// 底部导航
// ============================================
@Composable
fun CustomBottomNav(selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit, onPublishClick: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth().height(64.dp), cornerRadius = 0.dp, tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            NavItem("◧", "发现", selectedTab == HomeTab.DISCOVER, { onTabSelected(HomeTab.DISCOVER) }, Modifier.weight(1f))
            NavItem("◉", "全部", selectedTab == HomeTab.ALL, { onTabSelected(HomeTab.ALL) }, Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onPublishClick() }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(44.dp).shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Accent.copy(alpha = 0.3f), spotColor = Accent.copy(alpha = 0.3f)).background(Accent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("+", color = androidx.compose.ui.graphics.Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
            }
            NavItem("◇", "圈子", selectedTab == HomeTab.CIRCLES, { onTabSelected(HomeTab.CIRCLES) }, Modifier.weight(1f))
            NavItem("☐", "我的", selectedTab == HomeTab.PROFILE, { onTabSelected(HomeTab.PROFILE) }, Modifier.weight(1f))
        }
    }
}

@Composable
fun NavItem(icon: String, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = modifier.fillMaxHeight().clickable { onClick() }) {
        Text(icon, fontSize = 22.sp, color = if (isSelected) Accent else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = if (isSelected) Accent else TextSecondary, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ============================================
// 发现页
// ============================================
@Composable
fun DiscoverTab(onGoodItemClick: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    var goodItems by remember { mutableStateOf<List<GoodItem>>(RetrofitClient.goodItemsCache ?: emptyList()) }
    var isLoading by remember { mutableStateOf(goodItems.isEmpty()) }
    val scope = rememberCoroutineScope()

    fun loadItems() { scope.launch {
        isLoading = true
        try { val r = RetrofitClient.apiService.getGoodItems(sort = "newest"); if (r.isSuccessful) goodItems = r.body() ?: emptyList() } catch (_: Exception) {}; isLoading = false }
    }
    LaunchedEffect(Unit) { if (goodItems.isEmpty()) loadItems() }

    LiquidGlassBackdrop(modifier = modifier.fillMaxSize(), baseColor = Background, accentColor = Accent) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("发现", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { loadItems() }, enabled = !isLoading, colors = ButtonDefaults.textButtonColors(contentColor = if (isLoading) TextSecondary else Accent)) { Text("↻", fontSize = 24.sp) }
            }
            if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent, strokeWidth = 2.dp) }
            else if (goodItems.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("暂无好物", color = TextSecondary, fontSize = 16.sp); Text("去发布第一个好物吧", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) } }
            else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(goodItems, key = { it.id }) { item -> GoodItemCard(item = item, onClick = { cacheGoodItemPreview(item); onGoodItemClick(item.id) }) }
            }
        }
    }
}

// ============================================
// 发现页卡片
// ============================================
@Composable
fun GoodItemCard(item: GoodItem, onClick: () -> Unit = {}) {
    val cardShape = RoundedCornerShape(22.dp)
    Row(modifier = Modifier.fillMaxWidth().height(120.dp).shadow(12.dp, cardShape, ambientColor = androidx.compose.ui.graphics.Color(0xFF5CA9A5).copy(alpha = 0.22f), spotColor = androidx.compose.ui.graphics.Color(0xFF0ABAB5).copy(alpha = 0.28f)).clip(cardShape).background(brush = Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.28f), BackgroundSecondary.copy(alpha = 0.48f), androidx.compose.ui.graphics.Color(0xFFE8FDFB).copy(alpha = 0.22f)))).border(1.2.dp, Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.70f), Accent.copy(alpha = 0.24f), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.30f))), cardShape).drawBehind { drawLine(brush = Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.30f), Color.Transparent)), start = Offset(size.width * 0.06f, 1f), end = Offset(size.width * 0.94f, 1f), strokeWidth = 1.6f) }.clickable { onClick() }
    ) {
        Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
            if (item.images.orEmpty().isNotEmpty()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(thumbnailImageUrl(item.images!!.first())).crossfade(120).size(360, 360).scale(Scale.FILL).memoryCacheKey("detail-thumb:${item.images!!.first()}:180").diskCacheKey("detail-thumb:${item.images!!.first()}:180").build(), contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.22f), BackgroundSecondary.copy(alpha = 0.65f)))), contentAlignment = Alignment.Center) { Text(item.category.iconEmoji(), fontSize = 36.sp) }
            Box(modifier = Modifier.align(Alignment.CenterEnd).width(30.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f), BackgroundSecondary.copy(alpha = 0.38f)))))
        }
        LiquidGlassCard(modifier = Modifier.fillMaxHeight().weight(1f), cornerRadius = 0.dp, blurRadius = 30.dp, tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.30f), accentColor = Accent, borderAlpha = 0.0f) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${item.contentType.displayName()} · ${item.category.displayName()}", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (!item.description.isNullOrBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text(item.description, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                        val authorAvatar = item.author?.avatar
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(TextSecondary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            if (!authorAvatar.isNullOrBlank()) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data("$GOODX_BASE_URL/api/upload/thumb/${authorAvatar.substringAfterLast('/')}").size(72,72).scale(Scale.FILL).crossfade(80).memoryCacheKey("avatar-thumb:$authorAvatar").diskCacheKey("avatar-thumb:$authorAvatar").build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text((item.author?.nickname ?: "?").first().uppercase(), color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(item.author?.nickname ?: item.author?.username ?: "匿名", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (item.likes > 0) "♥" else "♡", color = if (item.likes > 0) LikeRed else TextTertiary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp)); Text("${item.likes}", color = if (item.likes > 0) LikeRed else TextSecondary, fontSize = 11.sp)
                        if (item.commentsCount > 0) { Spacer(modifier = Modifier.width(6.dp)); Text("💬", fontSize = 11.sp); Spacer(modifier = Modifier.width(2.dp)); Text("${item.commentsCount}", color = TextSecondary, fontSize = 11.sp) }
                        val inter = item.latestInteraction; if (inter != null && inter.user?.nickname != null) { Spacer(modifier = Modifier.width(6.dp)); Text(if (inter.type == "comment") "${inter.user.nickname} 刚刚评论了" else "${inter.user.nickname} 刚刚点赞了", color = TextTertiary, fontSize = 10.sp, maxLines = 1) }
                    }
                    Text(formatTimeAgo(item.createdAt), color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000; val minutes = seconds / 60; val hours = minutes / 60; val days = hours / 24
    return when { seconds < 60 -> "刚刚"; minutes < 60 -> "${minutes}分钟前"; hours < 24 -> "${hours}小时前"; days < 30 -> "${days}天前"; else -> { val d = java.util.Date(timestamp); java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(d) } }
}

// ============================================
// 全部品类页
// ============================================
@Composable
fun AllCategoriesTab(onGoodItemClick: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    var selectedType by rememberSaveable { mutableStateOf<ContentType?>(ContentType.GOODS) }
    var goodItems by remember { mutableStateOf<List<GoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                val r = RetrofitClient.apiService.getGoodItems(sort = "newest", contentType = selectedType?.name)
                if (r.isSuccessful) goodItems = r.body() ?: emptyList()
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(selectedType) { loadItems() }

    Column(modifier = modifier.fillMaxSize().background(Background)) {
        Text("全部", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp))

        // 顶部大类选择器
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ContentType.values().forEach { type ->
                val selected = selectedType == type
                Box(modifier = Modifier.weight(1f).height(38.dp).background(if (selected) Accent.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(10.dp)).clickable { selectedType = if (selected) null else type }, contentAlignment = Alignment.Center) {
                    Text(type.displayName(), color = if (selected) Accent else TextSecondary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }

        if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent, strokeWidth = 2.dp) }
        else if (goodItems.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无内容", color = TextSecondary, fontSize = 14.sp) }
        else LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(goodItems, key = { it.id }) { item -> GoodItemCard(item = item, onClick = { cacheGoodItemPreview(item); onGoodItemClick(item.id) }) }
        }
    }
}

@Composable
fun ContentTypeCard(contentType: ContentType, onClick: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth().height(112.dp).clickable { onClick() }, cornerRadius = 22.dp, tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(58.dp).background(Accent.copy(alpha = 0.12f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Text(contentType.iconEmoji(), fontSize = 30.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(contentType.displayName(), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(4.dp)); Text(contentType.subtitle(), color = TextSecondary, fontSize = 13.sp) }
            Text("›", color = TextSecondary.copy(alpha = 0.7f), fontSize = 28.sp)
        }
    }
}

@Composable
fun CategoryGridItem(category: Category, onClick: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.aspectRatio(1f).clickable { onClick() }, cornerRadius = 24.dp, tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(category.iconEmoji(), fontSize = 36.sp, modifier = Modifier.padding(bottom = 10.dp)); Text(category.displayName(), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================
// 圈子页
// ============================================
@Composable
fun CirclesTab(modifier: Modifier = Modifier) {
    var circles by remember { mutableStateOf(listOf<CircleData>()) }
    var showCreate by remember { mutableStateOf(false) }
    var selectedCircle by remember { mutableStateOf<CircleData?>(null) }
    var newCircleName by remember { mutableStateOf("") }
    val myName = TokenManager.getNickname() ?: TokenManager.getUsername() ?: "我"

    Column(modifier = modifier.fillMaxSize().background(Background).padding(horizontal = 20.dp)) {
        Text("圈子", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Text("创建或加入兴趣圈子", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = { showCreate = true }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("创建圈子", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(20.dp))
        if (circles.isEmpty()) Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("暂无圈子，创建第一个吧", color = TextSecondary, fontSize = 14.sp) }
        else LazyColumn(modifier = Modifier.weight(1f)) {
            items(circles, key = { it.name }) { circle -> LiquidGlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selectedCircle = circle }, cornerRadius = 16.dp, tintColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(circle.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text("${circle.members.size} 人", color = TextSecondary, fontSize = 12.sp) }
                    Text("›", color = TextSecondary, fontSize = 22.sp)
                }
            }}
        }
    }
    if (showCreate) AlertDialog(onDismissRequest = { showCreate = false }, containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 32.dp), title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("创建圈子", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }, text = { OutlinedTextField(value = newCircleName, onValueChange = { newCircleName = it }, placeholder = { Text("圈子名称", color = TextSecondary.copy(alpha = 0.5f)) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { if (newCircleName.isNotBlank()) { circles = circles + CircleData(newCircleName.trim(), mutableListOf(myName)); newCircleName = ""; showCreate = false } }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("创建", fontSize = 15.sp) } }, dismissButton = { TextButton(onClick = { showCreate = false; newCircleName = "" }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消", fontSize = 15.sp) } })
    selectedCircle?.let { circle -> AlertDialog(onDismissRequest = { selectedCircle = null }, containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 32.dp), title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(circle.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }, text = { Column { Text("成员 (${circle.members.size})", color = TextSecondary, fontSize = 13.sp); Spacer(modifier = Modifier.height(8.dp)); circle.members.forEach { m -> Text("◇ $m", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp)) } } }, confirmButton = { TextButton(onClick = { selectedCircle = null }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("关闭", fontSize = 15.sp) } }) }
}

data class CircleData(val name: String, val members: MutableList<String>)

// ============================================
// 我的页
// ============================================
@Composable
fun ProfileTab(
    onLogout: () -> Unit, onMyPostsClick: () -> Unit = {}, onPublishClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {}, onAdminClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val nickname = TokenManager.getNickname() ?: TokenManager.getUsername() ?: "GoodX"
    var isAdmin by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableStateOf(0) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var downloadProgress by remember { mutableStateOf(-1) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { try { val r = RetrofitClient.apiService.checkAdmin(); if (r.isSuccessful) isAdmin = r.body()?.isAdmin ?: false; val u = RetrofitClient.apiService.getUnreadCount(); if (u.isSuccessful) unreadCount = u.body()?.count ?: 0 } catch (_: Exception) {} }

    Column(modifier = modifier.fillMaxSize().background(Background).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(60.dp))
        val avatarUrl = TokenManager.getAvatar()
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)).clickable { onEditProfileClick() }, contentAlignment = Alignment.Center) {
            if (!avatarUrl.isNullOrBlank()) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(if (avatarUrl.startsWith("http")) avatarUrl else "$GOODX_BASE_URL$avatarUrl").size(240, 240).scale(Scale.FILL).crossfade(120).build(), contentDescription = "头像", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(nickname.firstOrNull()?.uppercase() ?: "◆", color = Accent, fontSize = 40.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(nickname, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("@${TokenManager.getUsername() ?: ""}", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        TextButton(onClick = onEditProfileClick, colors = ButtonDefaults.textButtonColors(contentColor = Accent), modifier = Modifier.padding(bottom = 24.dp)) { Text("编辑资料", fontSize = 13.sp) }

        Button(onClick = onPublishClick, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("发布好物", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onMyPostsClick, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border)), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("我的发布", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onNotificationsClick, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border)), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (unreadCount > 0) " 消息中心 ($unreadCount)" else "消息中心", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        Spacer(modifier = Modifier.height(12.dp))

        if (isAdmin) {
            OutlinedButton(onClick = onAdminClick, colors = ButtonDefaults.outlinedButtonColors(contentColor = LikeRed), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(LikeRed.copy(alpha = 0.3f))), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("🛡️ 后台管理", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(onClick = {
            UpdateManager.checkForUpdate(context) { v, n, u -> updateInfo = Triple(v, n, u) }
        }, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border.copy(alpha = 0.5f))), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("检查更新", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = { showAbout = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border.copy(alpha = 0.5f))), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("关于GoodX", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = { showLogoutConfirm = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), shape = RoundedCornerShape(12.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border.copy(alpha = 0.5f))), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("退出登录", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
    }

    if (showLogoutConfirm) AlertDialog(onDismissRequest = { showLogoutConfirm = false }, containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 48.dp), title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("退出登录", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }, text = { Spacer(modifier = Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = { showLogoutConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消", fontSize = 15.sp) }; TextButton(onClick = { showLogoutConfirm = false; onLogout() }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("确认", fontSize = 15.sp) } } }, confirmButton = {}, dismissButton = {})

    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false }, containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 32.dp), text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("GoodX", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); Text("v0.6.0", color = TextSecondary, fontSize = 14.sp); Spacer(modifier = Modifier.height(4.dp)); Text("分享值得被看见的东西", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp); Spacer(modifier = Modifier.height(16.dp)); Text("© 2026 GoodX", color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) } }, confirmButton = { TextButton(onClick = { showAbout = false }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("知道了", fontSize = 15.sp) } })

    updateInfo?.let { (version, note, url) ->
        val isDownloading = downloadProgress >= 0
        AlertDialog(onDismissRequest = { if (!isDownloading) { updateInfo = null; downloadProgress = -1 } }, containerColor = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(horizontal = 32.dp), title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (isDownloading) "正在下载..." else "发现新版本 $version", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }, text = { if (isDownloading) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$downloadProgress%", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Spacer(modifier = Modifier.height(8.dp)); LinearProgressIndicator(progress = { downloadProgress / 100f }, color = Accent, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Text(url, color = TextSecondary.copy(alpha = 0.4f), fontSize = 9.sp, maxLines = 1) } else Text(note, color = TextSecondary, fontSize = 14.sp) }, confirmButton = { if (!isDownloading) TextButton(onClick = { downloadProgress = 0; UpdateManager.downloadAndInstall(context, url) { pct -> downloadProgress = pct } }, colors = ButtonDefaults.textButtonColors(contentColor = Accent)) { Text("立即更新", fontSize = 15.sp) } }, dismissButton = { TextButton(onClick = { updateInfo = null; downloadProgress = -1 }, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text(if (isDownloading) "后台下载" else "稍后", fontSize = 15.sp) } })
    }
}

// ========== 发布好物对话框 ==========
@Composable
fun CreateGoodItemDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var platform by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ELECTRONICS) }; var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }; var isUploadingImage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { scope.launch { isUploadingImage = true; try { val inputStream = context.contentResolver.openInputStream(it); val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg"); inputStream?.use { s -> tempFile.outputStream().use { o -> s.copyTo(o) } }; val body = MultipartBody.Part.createFormData("image", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull())); val r = RetrofitClient.apiService.uploadImage(body); if (r.isSuccessful) uploadedImageUrl = r.body()?.url; tempFile.delete() } catch (_: Exception) {}; isUploadingImage = false } } }

    AlertDialog(onDismissRequest = onDismiss, containerColor = Surface, shape = RoundedCornerShape(20.dp), title = { Text("发布好物", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }, text = {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(BackgroundSecondary).border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable(enabled = !isUploadingImage) { imagePicker.launch("image/*") }, contentAlignment = Alignment.Center) { when { isUploadingImage -> CircularProgressIndicator(color = Accent, modifier = Modifier.size(32.dp), strokeWidth = 2.dp); uploadedImageUrl != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("✓", color = Accent, fontSize = 36.sp, fontWeight = FontWeight.Bold); Text("图片已上传", color = Accent, fontSize = 13.sp) }; else -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("+", color = TextTertiary, fontSize = 40.sp, fontWeight = FontWeight.Light); Text("点击上传图片", color = TextSecondary, fontSize = 13.sp) } } }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it; error = null }, label = { Text("标题", color = TextSecondary) }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Border, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = BackgroundSecondary, unfocusedContainerColor = BackgroundSecondary), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述", color = TextSecondary) }, maxLines = 3, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Border, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = BackgroundSecondary, unfocusedContainerColor = BackgroundSecondary), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("平台/品牌（可选）", color = TextSecondary) }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Border, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = BackgroundSecondary, unfocusedContainerColor = BackgroundSecondary), modifier = Modifier.fillMaxWidth())
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                OutlinedButton(onClick = { expanded = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary), shape = RoundedCornerShape(10.dp), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Border)), modifier = Modifier.fillMaxWidth()) { Text("${selectedCategory.iconEmoji()} ${selectedCategory.displayName()}") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Surface)) { Category.values().forEach { cat -> DropdownMenuItem(text = { Text("${cat.iconEmoji()} ${cat.displayName()}", color = TextPrimary) }, onClick = { selectedCategory = cat; expanded = false }) } }
            }
            if (error != null) Text(error!!, color = LikeRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }, confirmButton = {
        Button(onClick = { if (title.isBlank()) { error = "请输入标题"; return@Button }; scope.launch { isLoading = true; error = null; try { val r = RetrofitClient.apiService.createGoodItem(CreateGoodItemRequest(title = title, description = description, category = selectedCategory.name, subCategory = platform.takeIf { it.isNotBlank() }, images = uploadedImageUrl?.let { listOf(it) } ?: emptyList())); if (r.isSuccessful) { Toast.makeText(context, "✓ 发布成功！", Toast.LENGTH_SHORT).show(); onCreated() } else error = "发布失败: ${r.errorMessage()}" } catch (e: Exception) { error = "网络错误: ${e.message}" }; isLoading = false } }, enabled = !isLoading && !isUploadingImage, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(12.dp)) { if (isLoading) CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("发布", fontWeight = FontWeight.SemiBold) }
    }, dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("取消") } })
}
