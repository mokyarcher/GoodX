package team.sharex.goodx.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ============================================
// 淡蓝绿色系背景 - 液态玻璃优化版
// 背景：极淡的蒂芙尼蓝 | 卡片：玻璃质感 | 文字：深色
// ============================================

// 背景层
var Background by mutableStateOf(Color(0xFFEFF8F7))          // 极淡蓝绿
var BackgroundSecondary by mutableStateOf(Color(0xFFE0F2F1)) // 稍深一点的淡蓝绿

// 卡片/表面层
var Surface by mutableStateOf(Color(0xFFFFFFFF))
var SurfaceElevated by mutableStateOf(Color(0xFFFFFFFF))

// 文字（深色，适配浅色背景）
var TextPrimary by mutableStateOf(Color(0xFF1C1C1E))         // 近黑
var TextSecondary by mutableStateOf(Color(0xFF6E6E73))       // 中灰
var TextTertiary by mutableStateOf(Color(0xFFADADB0))        // 浅灰

// 强调色（深一点的蒂芙尼蓝）
var Accent by mutableStateOf(Color(0xFF0ABAB5))              // 蒂芙尼蓝
var AccentSecondary by mutableStateOf(Color(0xFF30D158))     // 绿色

// 边框/分隔线
var Border by mutableStateOf(Color(0xFFD0ECEA))
var Divider by mutableStateOf(Color(0xFFD0ECEA))

// 功能色
var LikeRed by mutableStateOf(Color(0xFF0ABAB5))

// ============================================
// 颜色方案数据类
// ============================================

data class ColorScheme(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val divider: Color,
    val name: String
)

fun applyColorScheme(scheme: ColorScheme) {
    Accent = scheme.accent
    Background = scheme.background
    Surface = scheme.surface
    SurfaceElevated = scheme.surfaceElevated
    TextPrimary = scheme.textPrimary
    TextSecondary = scheme.textSecondary
    TextTertiary = scheme.textTertiary
    Border = scheme.border
    Divider = scheme.divider
}

object AppColors {
    val TiffanyLight = ColorScheme(
        accent = Color(0xFF0ABAB5),
        background = Color(0xFFEFF8F7),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF1C1C1E),
        textSecondary = Color(0xFF6E6E73),
        textTertiary = Color(0xFFADADB0),
        border = Color(0xFFD0ECEA),
        divider = Color(0xFFD0ECEA),
        name = "淡蓝绿"
    )

    fun getScheme(index: Int): ColorScheme = TiffanyLight
}
