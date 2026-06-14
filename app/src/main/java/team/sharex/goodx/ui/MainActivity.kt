package team.sharex.goodx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.model.Category
import team.sharex.goodx.model.ContentType
import team.sharex.goodx.ui.screens.CategoryDetailScreen
import team.sharex.goodx.ui.screens.ContentTypePostsScreen
import team.sharex.goodx.data.remote.AdminPost
import team.sharex.goodx.data.remote.AdminUser
import team.sharex.goodx.ui.screens.AdminPostDetailScreen
import team.sharex.goodx.ui.screens.AdminScreen
import team.sharex.goodx.ui.screens.AdminUserPostsScreen
import team.sharex.goodx.ui.screens.CreateGoodItemScreen
import team.sharex.goodx.ui.screens.EditGoodItemScreen
import team.sharex.goodx.ui.screens.EditProfileScreen
import team.sharex.goodx.ui.screens.GoodItemDetailScreen
import team.sharex.goodx.ui.screens.HomeScreen
import team.sharex.goodx.ui.screens.HomeTab
import team.sharex.goodx.ui.screens.LoginScreen
import team.sharex.goodx.ui.screens.SplashScreen
import team.sharex.goodx.ui.screens.MyPostsScreen
import team.sharex.goodx.ui.screens.NotificationsScreen
import team.sharex.goodx.ui.screens.RegisterScreen
import team.sharex.goodx.ui.theme.AppColors
import team.sharex.goodx.ui.theme.Background
import team.sharex.goodx.ui.theme.GoodXTheme
import team.sharex.goodx.ui.theme.applyColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(this)
        // 应用保存的主题
        applyColorScheme(AppColors.getScheme(TokenManager.getTheme()))
        enableEdgeToEdge()
        // 状态栏：透明背景 + 深色图标文字（适配浅色背景）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        setContent {
            GoodXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    data class ContentTypePosts(val contentType: ContentType) : Screen()
    data class CategoryDetail(val category: Category) : Screen()
    data class GoodItemDetail(val itemId: String) : Screen()
    object Notifications : Screen()
    data class MyPosts(val showRemoved: Boolean = false) : Screen()
    data class EditGoodItem(val itemId: String) : Screen()
    object EditProfile : Screen()
    object Admin : Screen()
    data class AdminUserPosts(val user: AdminUser) : Screen()
    data class AdminPostDetail(val post: AdminPost, val user: AdminUser) : Screen()
    object CreateGoodItem : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(
        if (TokenManager.isLoggedIn()) Screen.Splash else Screen.Login
    ) }
    var navStack by remember { mutableStateOf<List<Screen>>(emptyList()) }
    var homeTab by remember { mutableStateOf(HomeTab.DISCOVER) }
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var lastBackTime by remember { mutableStateOf(0L) }

    // 注册系统返回键拦截
    DisposableEffect(currentScreen, navStack) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navStack.isNotEmpty()) {
                    val previous = navStack.last()
                    when (previous) {
                        is Screen.ContentTypePosts -> homeTab = HomeTab.ALL
                        is Screen.CategoryDetail -> homeTab = HomeTab.ALL
                        is Screen.EditGoodItem -> homeTab = HomeTab.PROFILE
                        is Screen.Notifications -> homeTab = HomeTab.PROFILE
                        is Screen.MyPosts -> homeTab = HomeTab.PROFILE
                        else -> {}
                    }
                    currentScreen = previous
                    navStack = navStack.dropLast(1)
                } else {
                    // 栈为空，防误触：2秒内再次返回才退出
                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < 2000) {
                        isEnabled = false
                        activity.finish()
                    } else {
                        lastBackTime = now
                        android.widget.Toast.makeText(context, "再次点击返回退出", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }

    fun navigateTo(screen: Screen) {
        navStack = navStack + currentScreen
        currentScreen = screen
    }

    // 登录/注册等入口跳转到主页面时，不应保留返回栈
    fun navigateAndReset(screen: Screen) {
        navStack = emptyList()
        currentScreen = screen
    }

    // Home 常驻底层，详情覆盖上层，保证返回时滚动位置不丢失
    Box(modifier = Modifier.fillMaxSize()) {
        if (currentScreen !is Screen.Login && currentScreen !is Screen.Register) {
            HomeScreen(
                initialTab = homeTab,
                onTabChanged = { homeTab = it },
                onLogout = {
                    TokenManager.clearToken()
                    navStack = emptyList()
                    currentScreen = Screen.Login
                },
                onCategoryClick = { category -> navigateTo(Screen.CategoryDetail(category)) },
                onContentTypeClick = { ct -> navigateTo(Screen.ContentTypePosts(ct)) },
                onGoodItemClick = { itemId -> navigateTo(Screen.GoodItemDetail(itemId)) },
                onMyPostsClick = { navigateTo(Screen.MyPosts()) },
                onNotificationsClick = { navigateTo(Screen.Notifications) },
                onPublishClick = { navigateTo(Screen.CreateGoodItem) },
                onEditProfileClick = { navigateTo(Screen.EditProfile) },
                onAdminClick = { navigateTo(Screen.Admin) }
            )
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val isForward = when {
                initialState is Screen.Home && targetState is Screen.ContentTypePosts -> true
                initialState is Screen.Home && targetState is Screen.CategoryDetail -> true
                initialState is Screen.Home && targetState is Screen.GoodItemDetail -> true
                initialState is Screen.Home && targetState is Screen.Notifications -> true
                initialState is Screen.Notifications && targetState is Screen.AdminUserPosts -> true
                initialState is Screen.Notifications && targetState is Screen.EditGoodItem -> true
                initialState is Screen.Home && targetState is Screen.MyPosts -> true
                initialState is Screen.MyPosts && targetState is Screen.EditGoodItem -> true
                initialState is Screen.Home && targetState is Screen.EditProfile -> true
                initialState is Screen.Home && targetState is Screen.Admin -> true
                initialState is Screen.Admin && targetState is Screen.AdminUserPosts -> true
                initialState is Screen.AdminUserPosts && targetState is Screen.AdminPostDetail -> true
                initialState is Screen.Home && targetState is Screen.CreateGoodItem -> true
                initialState is Screen.CategoryDetail && targetState is Screen.GoodItemDetail -> true
                initialState is Screen.Login && targetState is Screen.Home -> true
                initialState is Screen.Register && targetState is Screen.Home -> true
                else -> false
            }

            // 从发布页返回Home时，只做淡入淡出，避免标题漂移感
            val isReturnToHome = targetState is Screen.Home && (
                initialState is Screen.CreateGoodItem ||
                initialState is Screen.EditProfile ||
                initialState is Screen.Admin ||
                initialState is Screen.GoodItemDetail ||
                initialState is Screen.Notifications ||
                initialState is Screen.MyPosts ||
                initialState is Screen.EditGoodItem ||
                initialState is Screen.ContentTypePosts ||
                initialState is Screen.CategoryDetail
            )

            if (isReturnToHome) {
                fadeIn(animationSpec = tween(160), initialAlpha = 0.0f) togetherWith
                fadeOut(animationSpec = tween(120), targetAlpha = 0.0f)
            } else if (isForward) {
                // 进入详情时只做小幅滑入 + 快速淡入，减少大面积位移带来的掉帧感。
                fadeIn(animationSpec = tween(150), initialAlpha = 0.0f) +
                slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(180)) togetherWith
                fadeOut(animationSpec = tween(120), targetAlpha = 0.72f)
            } else {
                fadeIn(animationSpec = tween(150), initialAlpha = 0.0f) +
                slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(180)) togetherWith
                fadeOut(animationSpec = tween(120), targetAlpha = 0.72f)
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> SplashScreen(
                onReady = { navigateAndReset(Screen.Home) }
            )
            is Screen.Login -> LoginScreen(
                onLoginSuccess = { navigateAndReset(Screen.Home) },
                onNavigateToRegister = { navigateTo(Screen.Register) }
            )
            is Screen.Register -> RegisterScreen(
                onRegisterSuccess = { navigateAndReset(Screen.Home) },
                onNavigateToLogin = { navigateTo(Screen.Login) }
            )
            is Screen.Home -> { /* Home 已在底层渲染 */ }
            is Screen.ContentTypePosts -> ContentTypePostsScreen(
                contentType = screen.contentType,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        if (previous is Screen.ContentTypePosts) homeTab = HomeTab.ALL
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onGoodItemClick = { itemId -> navigateTo(Screen.GoodItemDetail(itemId)) }
            )
            is Screen.CategoryDetail -> CategoryDetailScreen(
                category = screen.category,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        if (previous is Screen.CategoryDetail) homeTab = HomeTab.ALL
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onGoodItemClick = { itemId ->
                    navigateTo(Screen.GoodItemDetail(itemId))
                }
            )
            is Screen.GoodItemDetail -> GoodItemDetailScreen(
                itemId = screen.itemId,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                }
            )
            is Screen.EditGoodItem -> EditGoodItemScreen(
                itemId = screen.itemId,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        currentScreen = navStack.last()
                        navStack = navStack.dropLast(1)
                    }
                },
                onUpdated = {
                    if (navStack.isNotEmpty()) {
                        currentScreen = navStack.last()
                        navStack = navStack.dropLast(1)
                    }
                }
            )
            is Screen.Notifications -> NotificationsScreen(
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onPostClick = { postId, extra ->
                    val extraData = try {
                        extra?.let { org.json.JSONObject(it) }
                    } catch (_: Exception) { null }

                    if (extraData?.optString("action") == "review_posts") {
                        val authorId = extraData.optString("authorId", "")
                        val authorName = extraData.optString("authorName", "用户")
                        navigateTo(Screen.AdminUserPosts(
                            AdminUser(authorId, authorName, authorName, false, false, 0)
                        ))
                    } else {
                        navigateTo(Screen.MyPosts(showRemoved = true))
                    }
                }
            )
            is Screen.MyPosts -> MyPostsScreen(
                initialShowRemoved = screen.showRemoved,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        if (previous is Screen.MyPosts) homeTab = HomeTab.PROFILE
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onEditItem = { itemId ->
                    navigateTo(Screen.EditGoodItem(itemId))
                }
            )
            is Screen.Admin -> AdminScreen(
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onUserClick = { user ->
                    navigateTo(Screen.AdminUserPosts(user))
                }
            )
            is Screen.AdminUserPosts -> AdminUserPostsScreen(
                user = screen.user,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onPostClick = { post ->
                    navigateTo(Screen.AdminPostDetail(post, screen.user))
                }
            )
            is Screen.AdminPostDetail -> AdminPostDetailScreen(
                post = screen.post,
                user = screen.user,
                onBack = {
                    if (navStack.isNotEmpty()) {
                        currentScreen = navStack.last()
                        navStack = navStack.dropLast(1)
                    }
                },
                onAction = {
                    if (navStack.isNotEmpty()) {
                        currentScreen = navStack.last()
                        navStack = navStack.dropLast(1)
                    }
                }
            )
            is Screen.EditProfile -> EditProfileScreen(
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onUpdated = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                }
            )
            is Screen.CreateGoodItem -> CreateGoodItemScreen(
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        currentScreen = previous
                        navStack = navStack.dropLast(1)
                    }
                },
                onPublished = {
                    homeTab = HomeTab.DISCOVER
                    navStack = emptyList()
                    currentScreen = Screen.Home
                }
            )
        }
    }
    } // Box end
}
