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
                            Color.White.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.04f),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(w * 0.12f, h * 0.04f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.92f, h * 0.14f),
                        radius = w * 0.62f
                    ),
                    radius = w * 0.62f,
                    center = Offset(w * 0.92f, h * 0.14f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBDEDEA).copy(alpha = 0.22f),
                            Color(0xFFCFEFED).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.38f, h * 0.58f),
                        radius = w * 0.82f
                    ),
                    radius = w * 0.82f,
                    center = Offset(w * 0.38f, h * 0.58f)
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF88BDBA).copy(alpha = 0.08f)
                        ),
                        startY = h * 0.55f,
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
 * 液态玻璃背景层。
 * RenderEffect 在 Compose 中不是完整 backdrop blur，因此主要靠“半透明渐变 + 边缘高光 + 内阴影”建立玻璃厚度。
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 28.dp,
    tintColor: Color = Color.White.copy(alpha = 0.28f),
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
                        Color.White.copy(alpha = 0.42f),
                        tintColor,
                        accentColor.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.18f)
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 900f)
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
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.42f
                    ),
                    size = Size(w, h * 0.42f)
                )

                // 左上角液态高光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.10f),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(w * 0.12f, h * 0.10f)
                )

                // 右下角青色折射
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            accentColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.92f, h * 0.92f),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(w * 0.92f, h * 0.92f)
                )

                // 底部内阴影，制造厚度
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF4A7774).copy(alpha = 0.05f),
                            Color(0xFF234845).copy(alpha = 0.10f)
                        ),
                        startY = h * 0.50f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.50f),
                    size = Size(w, h * 0.50f)
                )

                // 顶部锐利亮线
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.48f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.08f, 0.8f),
                    end = Offset(w * 0.92f, 0.8f),
                    strokeWidth = 1.4f
                )

                // 斜向玻璃光带
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(w * 0.05f, h * 0.22f),
                    end = Offset(w * 0.72f, 0f),
                    strokeWidth = 2.2f
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
