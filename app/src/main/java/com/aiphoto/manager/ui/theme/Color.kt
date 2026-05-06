package com.aiphoto.manager.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// Tag 预设色 (所有主题共用)
// ============================================
val TagPink = Color(0xFFFFC0D0)
val TagPurple = Color(0xFFD8B4FE)
val TagBlue = Color(0xFF93C5FD)
val TagGreen = Color(0xFF86EFAC)
val TagYellow = Color(0xFFFDE68A)
val TagOrange = Color(0xFFFDBA74)
val TagRed = Color(0xFFFCA5A5)
val TagCyan = Color(0xFF67E8F9)

val TagColors = listOf(TagPink, TagPurple, TagBlue, TagGreen, TagYellow, TagOrange, TagRed, TagCyan)

val TagColorHexes = listOf(
    "#93C5FD", "#A5B4FC", "#C4B5FD", "#86EFAC",
    "#FDE68A", "#FDBA74", "#FCA5A5", "#67E8F9"
)

fun tagColorFor(tagName: String): String {
    val index = kotlin.math.abs(tagName.hashCode()) % TagColorHexes.size
    return TagColorHexes[index]
}

// ============================================
// 蓝白二次元主题色 (默认)
// ============================================
val BlueWhitePrimary = Color(0xFF4A90D9)
val BlueWhitePrimaryLight = Color(0xFF7BB5F0)
val BlueWhitePrimaryDark = Color(0xFF2A70B0)
val BlueWhitePrimaryContainer = Color(0xFFD6EBFF)

val BlueWhiteSecondary = Color(0xFF6EC6F8)
val BlueWhiteSecondaryLight = Color(0xFFA0DDFA)
val BlueWhiteSecondaryDark = Color(0xFF3AA8E0)

val BlueWhiteTertiary = Color(0xFFFF8FAB)
val BlueWhiteTertiaryLight = Color(0xFFFFB8D0)

val BlueWhiteBackground = Color(0xFFF0F5FF)
val BlueWhiteSurface = Color(0xFFFFFFFF)
val BlueWhiteSurfaceVariant = Color(0xFFE8F0FE)

val BlueWhiteTextPrimary = Color(0xFF1A2A3A)
val BlueWhiteTextSecondary = Color(0xFF607088)
val BlueWhiteTextHint = Color(0xFFA0B0C8)

// ============================================
// 樱花粉 (保留兼容)
// ============================================
val SakuraPink = Color(0xFFFF6B9D)
val SakuraPinkLight = Color(0xFFFFB3CC)
val SakuraPinkDark = Color(0xFFE6457A)
val SakuraPinkVibrant = Color(0xFFFF8FAB)
val SakuraPinkSoft = Color(0xFFFFB8D0)

val LavenderPurple = Color(0xFFC084FC)
val LavenderPurpleLight = Color(0xFFDDD6FE)
val LavenderPurpleDark = Color(0xFF9F5FE0)
val LavenderPurpleVibrant = Color(0xFFD4B5FE)
val LavenderPurpleDeep = Color(0xFF9F7AEA)

val MintBlue = Color(0xFF67E8F9)
val MintBlueLight = Color(0xFFA5F3FC)
val MintBlueDark = Color(0xFF22D3EE)
val SkyBlue = Color(0xFF7DD3FC)
val CyanLight = Color(0xFFA5F3FC)

val CoralPeach = Color(0xFFFFB4A2)
val LemonCream = Color(0xFFFDE68A)
val MintGreen = Color(0xFF86EFAC)

// ============================================
// 通用状态色
// ============================================
val ErrorSoft = Color(0xFFFF8A80)
val SuccessSoft = Color(0xFF81C784)
val WarningSoft = Color(0xFFFFD54F)

// ============================================
// 暗色主题背景色 (保留兼容)
// ============================================
val SakuraPinkDarkTheme = Color(0xFFFF8FAB)
val LavenderPurpleDarkTheme = Color(0xFFD4B5FE)
val MintBlueDarkTheme = Color(0xFF67E8F9)

val SurfaceDark = Color(0xFF1A0F24)
val BackgroundDark = Color(0xFF0D0815)
val CardBackgroundDark = Color(0xFF2A1F35)
val CardBackgroundAltDark = Color(0xFF322840)

val TextPrimaryDark = Color(0xFFF5F0FF)
val TextSecondaryDark = Color(0xFFB8A0C8)
val TextHintDark = Color(0xFF7A6888)

// ============================================
// 亮色主题表面色 (保留兼容)
// ============================================
val SurfaceLight = Color(0xFFFFF0F5)
val BackgroundLight = Color(0xFFFAF5FF)
val CardBackground = Color(0xFFFFFFFF)
val CardBackgroundAlt = Color(0xFFFDF2F8)
val BackgroundGradientStart = Color(0xFFFFF0F5)
val BackgroundGradientEnd = Color(0xFFF5F0FF)

val CardElevation1 = Color(0xFFFFFBFC)
val CardElevation2 = Color(0xFFFFF5F8)
val CardElevation3 = Color(0xFFFFF0F5)

val TextPrimary = Color(0xFF1F1035)
val TextSecondary = Color(0xFF6B5B7B)
val TextHint = Color(0xFFB0A0C0)
