package team.sharex.goodx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import team.sharex.goodx.data.remote.TokenManager
import team.sharex.goodx.model.Category
import team.sharex.goodx.ui.screens.CategoryDetailScreen
import team.sharex.goodx.ui.screens.CreateGoodItemScreen
import team.sharex.goodx.ui.screens.GoodItemDetailScreen
import team.sharex.goodx.ui.screens.HomeScreen
import team.sharex.goodx.ui.screens.HomeTab
import team.sharex.goodx.ui.screens.LoginScreen
import team.sharex.goodx.ui.screens.MyPostsScreen
import team.sharex.goodx.ui.screens.RegisterScreen
import team.sharex.goodx.ui.theme.GoodXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(this)
        enableEdgeToEdge()
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
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    data class CategoryDetail(val category: Category) : Screen()
    data class GoodItemDetail(val itemId: String) : Screen()
    object MyPosts : Screen()
    object CreateGoodItem : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(
        if (TokenManager.isLoggedIn()) Screen.Home else Screen.Login
    ) }
    var navStack by remember { mutableStateOf<List<Screen>>(emptyList()) }
    var homeTab by remember { mutableStateOf(HomeTab.DISCOVER) }
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // 注册系统返回键拦截
    DisposableEffect(currentScreen, navStack) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navStack.isNotEmpty()) {
                    val previous = navStack.last()
                    when (previous) {
                        is Screen.CategoryDetail -> homeTab = HomeTab.ALL
                        is Screen.MyPosts -> homeTab = HomeTab.PROFILE
                        else -> {}
                    }
                    currentScreen = previous
                    navStack = navStack.dropLast(1)
                } else {
                    // 栈为空，执行默认返回（退出 App）
                    isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
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

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val isForward = when {
                initialState is Screen.Home && targetState is Screen.CategoryDetail -> true
                initialState is Screen.Home && targetState is Screen.GoodItemDetail -> true
                initialState is Screen.Home && targetState is Screen.MyPosts -> true
                initialState is Screen.Home && targetState is Screen.CreateGoodItem -> true
                initialState is Screen.CategoryDetail && targetState is Screen.GoodItemDetail -> true
                initialState is Screen.Login && targetState is Screen.Home -> true
                initialState is Screen.Register && targetState is Screen.Home -> true
                else -> false
            }

            // 从发布页返回Home时，只做淡入淡出，避免标题漂移感
            val isReturnToHome = targetState is Screen.Home && (
                initialState is Screen.CreateGoodItem ||
                initialState is Screen.GoodItemDetail ||
                initialState is Screen.MyPosts ||
                initialState is Screen.CategoryDetail
            )

            if (isReturnToHome) {
                fadeIn(animationSpec = tween(250), initialAlpha = 0.0f) togetherWith
                fadeOut(animationSpec = tween(200), targetAlpha = 0.0f)
            } else if (isForward) {
                // 进入：从右侧滑入 + 淡入（快一点，减少等待感）
                fadeIn(animationSpec = tween(200), initialAlpha = 0.0f) +
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) togetherWith
                // 退出：轻微淡出，不做位移（减少GPU负担）
                fadeOut(animationSpec = tween(200), targetAlpha = 0.4f)
            } else {
                // 返回：从左侧滑入 + 淡入
                fadeIn(animationSpec = tween(200), initialAlpha = 0.0f) +
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(280)) togetherWith
                // 退出：轻微淡出
                fadeOut(animationSpec = tween(200), targetAlpha = 0.4f)
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Login -> LoginScreen(
                onLoginSuccess = { navigateTo(Screen.Home) },
                onNavigateToRegister = { navigateTo(Screen.Register) }
            )
            is Screen.Register -> RegisterScreen(
                onRegisterSuccess = { navigateTo(Screen.Home) },
                onNavigateToLogin = { navigateTo(Screen.Login) }
            )
            is Screen.Home -> HomeScreen(
                initialTab = homeTab,
                onTabChanged = { homeTab = it },
                onLogout = {
                    TokenManager.clearToken()
                    navStack = emptyList()
                    currentScreen = Screen.Login
                },
                onCategoryClick = { category ->
                    navigateTo(Screen.CategoryDetail(category))
                },
                onGoodItemClick = { itemId ->
                    navigateTo(Screen.GoodItemDetail(itemId))
                },
                onMyPostsClick = {
                    navigateTo(Screen.MyPosts)
                },
                onPublishClick = {
                    navigateTo(Screen.CreateGoodItem)
                }
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
            is Screen.MyPosts -> MyPostsScreen(
                onBack = {
                    if (navStack.isNotEmpty()) {
                        val previous = navStack.last()
                        if (previous is Screen.MyPosts) homeTab = HomeTab.PROFILE
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
}
