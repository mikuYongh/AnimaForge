package com.aiphoto.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 按钮渐变 - 蓝白主题
val ButtonGradientBlueWhite = Brush.horizontalGradient(
    colors = listOf(BlueWhitePrimary, BlueWhiteSecondary)
)

// 按钮渐变 - 樱花粉
val ButtonGradientSakura = Brush.horizontalGradient(
    colors = listOf(SakuraPink, LavenderPurple)
)

// 辅助渐变
val ButtonGradientSecondary = Brush.horizontalGradient(
    colors = listOf(LavenderPurple, MintBlue)
)

// 卡片渐变
val CardGradientPink = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFB8D0),
        Color(0xFFFF8FAB)
    )
)

val CardGradientPurple = Brush.linearGradient(
    colors = listOf(
        Color(0xFFEBCCFF),
        Color(0xFFD4B5FE)
    )
)

val CardGradientOcean = Brush.linearGradient(
    colors = listOf(
        Color(0xFFA5F3FC),
        Color(0xFF67E8F9)
    )
)

val CardGradientSky = Brush.linearGradient(
    colors = listOf(
        Color(0xFFD6EBFF),
        Color(0xFFA0DDFA)
    )
)

// 加载闪烁
val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.0f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.0f)
    )
)

// 覆盖层
val OverlayGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.1f)
    )
)

val AccentGradient = Brush.radialGradient(
    colors = listOf(
        SakuraPink.copy(alpha = 0.3f),
        Color.Transparent
    )
)

// ============================================
// 主题感知渐变工厂
// ============================================
object ThemeGradients {
    fun backgroundGradient(colorSet: AppColorSet): Brush =
        Brush.verticalGradient(listOf(colorSet.bgGradientStart, colorSet.bgGradientEnd))

    fun buttonGradient(colorSet: AppColorSet): Brush =
        Brush.horizontalGradient(listOf(colorSet.btnGradientStart, colorSet.btnGradientEnd))

    fun cardGlowGradient(colorSet: AppColorSet): Brush =
        Brush.radialGradient(
            colors = listOf(
                colorSet.colorScheme.primary.copy(alpha = 0.08f),
                Color.Transparent
            )
        )

    fun heroGradient(colorSet: AppColorSet): Brush =
        Brush.verticalGradient(
            colors = listOf(
                colorSet.colorScheme.primaryContainer,
                colorSet.colorScheme.background
            )
        )
}

// 兼容旧代码的 BackgroundGradient
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFF0F5),
        Color(0xFFF5F0FF)
    )
)
