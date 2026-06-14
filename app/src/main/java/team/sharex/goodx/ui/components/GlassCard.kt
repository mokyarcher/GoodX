package team.sharex.goodx.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页面级液态玻璃光场背景。
 * 纯色背景会让玻璃效果很弱，所以这里提供柔和光斑与暗角，让卡片有“可折射”的底色。
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    baseColor: Color,
    accentColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(baseColor)
            .drawBehind {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.82f),
                            Color.White.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.10f, h * 0.03f),
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = Offset(w * 0.10f, h * 0.03f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.24f),
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.94f, h * 0.12f),
                        radius = w * 0.68f
                    ),
                    radius = w * 0.68f,
                    center = Offset(w * 0.94f, h * 0.12f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD4F4F2).copy(alpha = 0.28f),
                            Color(0xFFE0F6F4).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.36f, h * 0.54f),
                        radius = w * 0.88f
                    ),
                    radius = w * 0.88f,
                    center = Offset(w * 0.36f, h * 0.54f)
                )

                // 额外的高光点：底部左侧小光斑
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.18f, h * 0.82f),
                        radius = w * 0.34f
                    ),
                    radius = w * 0.34f,
                    center = Offset(w * 0.18f, h * 0.82f)
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF7AB5B2).copy(alpha = 0.10f)
                        ),
                        startY = h * 0.50f,
                        endY = h
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
            },
        content = content
    )
}

/**
 * 液态玻璃背景层 - 增强版。更通透、更炫的高光与折射。
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 32.dp,
    tintColor: Color = Color.White.copy(alpha = 0.32f),
    accentColor: Color = Color(0xFF0ABAB5)
) {
    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val useBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (useBlur) {
                    try {
                        val blurPx = with(density) { blurRadius.toPx() }
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } catch (e: Exception) {
                        Modifier
                    }
                } else {
                    Modifier
                }
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.52f),
                        tintColor,
                        accentColor.copy(alpha = 0.14f),
                        Color(0xFFE8F8F6).copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.22f)
                    ),
                    start = Offset.Zero,
                    end = Offset(1100f, 900f)
                ),
                shape = shape
            )
            .drawBehind {
                val w = size.width
                val h = size.height

                // 顶部玻璃釉面
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.64f),
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.48f
                    ),
                    size = Size(w, h * 0.48f)
                )

                // 左上角液态高光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.54f),
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.08f, h * 0.06f),
                        radius = w * 0.56f
                    ),
                    radius = w * 0.56f,
                    center = Offset(w * 0.08f, h * 0.06f)
                )

                // 右下角青色折射
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.94f, h * 0.94f),
                        radius = w * 0.58f
                    ),
                    radius = w * 0.58f,
                    center = Offset(w * 0.94f, h * 0.94f)
                )

                // 底部内阴影
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF3A6764).copy(alpha = 0.06f),
                            Color(0xFF1A3835).copy(alpha = 0.14f)
                        ),
                        startY = h * 0.45f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.45f),
                    size = Size(w, h * 0.55f)
                )

                // 顶部锐利亮线
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.82f),
                            Color.White.copy(alpha = 0.54f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.06f, 0.6f),
                    end = Offset(w * 0.94f, 0.6f),
                    strokeWidth = 1.6f
                )

                // 斜向玻璃光带1
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(w * 0.03f, h * 0.26f),
                    end = Offset(w * 0.68f, 0f),
                    strokeWidth = 2.8f
                )

                // 斜向玻璃光带2（反向）
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(w * 0.88f, h * 0.88f),
                    end = Offset(w * 0.24f, h * 0.56f),
                    strokeWidth = 3.2f
                )

                // 中间水平光带（更明显的液态流动感）
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.15f, h * 0.35f),
                    end = Offset(w * 0.85f, h * 0.35f),
                    strokeWidth = 1.8f
                )
            }
    )
}

/**
 * 液态玻璃卡片容器（玻璃层与内容分离，内容保持清晰）。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 28.dp,
    tintColor: Color = Color.White.copy(alpha = 0.28f),
    accentColor: Color = Color(0xFF0ABAB5),
    borderAlpha: Float = 0.42f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        accentColor.copy(alpha = borderAlpha * 0.36f),
                        Color.White.copy(alpha = borderAlpha * 0.22f)
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 900f)
                ),
                shape = shape
            )
    ) {
        LiquidGlassBackground(
            modifier = Modifier.matchParentSize(),
            cornerRadius = cornerRadius,
            blurRadius = blurRadius,
            tintColor = tintColor,
            accentColor = accentColor
        )
        content()
    }
}
