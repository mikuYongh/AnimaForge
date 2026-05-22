package com.aiphoto.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================
// 兼容旧代码的静态渐变定义，现已映射至初音未来定制配色
// ============================================

// 按钮渐变 - 蓝白/经典蓝
val ButtonGradientBlueWhite = Brush.horizontalGradient(
    colors = listOf(MikuBluePrimary, MikuBlueSecondary)
)

// 按钮渐变 - 樱花粉
val ButtonGradientSakura = Brush.horizontalGradient(
    colors = listOf(MikuSakuraPrimary, MikuSakuraSecondary)
)

// 辅助渐变
val ButtonGradientSecondary = Brush.horizontalGradient(
    colors = listOf(MikuSakuraSecondary, MikuBluePrimary)
)

// 卡片渐变
val CardGradientPink = Brush.linearGradient(
    colors = listOf(
        MikuSakuraTertiary,
        MikuSakuraPrimary
    )
)

val CardGradientPurple = Brush.linearGradient(
    colors = listOf(
        Color(0xFFEEDDFF),
        MikuSakuraSecondary
    )
)

val CardGradientOcean = Brush.linearGradient(
    colors = listOf(
        MikuBlueTertiary,
        MikuBluePrimary
    )
)

val CardGradientSky = Brush.linearGradient(
    colors = listOf(
        Color(0xFFD2F5F3),
        MikuBlueSecondary
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
        Color.Black.copy(alpha = 0.15f)
    )
)

val AccentGradient = Brush.radialGradient(
    colors = listOf(
        MikuSakuraPrimary.copy(alpha = 0.25f),
        Color.Transparent
    )
)

// ============================================
// 主题感知渐变工厂 (Theme-Aware Gradient Factory)
// ============================================
object ThemeGradients {
    fun backgroundGradient(colorSet: AppColorSet): Brush =
        Brush.verticalGradient(listOf(colorSet.bgGradientStart, colorSet.bgGradientEnd))

    fun buttonGradient(colorSet: AppColorSet): Brush =
        Brush.horizontalGradient(listOf(colorSet.btnGradientStart, colorSet.btnGradientEnd))

    fun cardGlowGradient(colorSet: AppColorSet): Brush =
        Brush.radialGradient(
            colors = listOf(
                colorSet.colorScheme.primary.copy(alpha = 0.12f),
                Color.Transparent
            )
        )

    fun heroGradient(colorSet: AppColorSet): Brush =
        Brush.verticalGradient(
            colors = listOf(
                colorSet.colorScheme.primaryContainer.copy(alpha = 0.8f),
                colorSet.colorScheme.background
            )
        )
}

// 兼容旧代码的 BackgroundGradient
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        MikuSakuraBgStart,
        MikuSakuraBgEnd
    )
)
