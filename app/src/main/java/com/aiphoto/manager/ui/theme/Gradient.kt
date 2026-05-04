package com.aiphoto.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Background gradients
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFF0F5),  // Light pink
        Color(0xFFF5F0FF)   // Light lavender
    )
)

// Button gradients
val ButtonGradientPrimary = Brush.horizontalGradient(
    colors = listOf(
        SakuraPink,
        LavenderPurple
    )
)

val ButtonGradientSecondary = Brush.horizontalGradient(
    colors = listOf(
        LavenderPurple,
        MintBlue
    )
)

// Card gradients (for special highlighted cards)
val CardGradientPink = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFB8D0),  // Pastel pink
        Color(0xFFFF8FAB)   // Vibrant pink
    )
)

val CardGradientPurple = Brush.linearGradient(
    colors = listOf(
        Color(0xFFEBCCFF),  // Pastel mauve
        Color(0xFFD4B5FE)   // Vibrant purple
    )
)

val CardGradientOcean = Brush.linearGradient(
    colors = listOf(
        Color(0xFFA5F3FC),  // Light cyan
        Color(0xFF67E8F9)   // Mint blue
    )
)

// Shimmer/Loading gradients
val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.0f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.0f)
    )
)

// Decorative gradients (for overlays and accents)
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
