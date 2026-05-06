package com.aiphoto.manager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    BLUE_WHITE("蓝白二次元"),
    SAKURA_PINK("樱花物语"),
    DARK_PURPLE("暗夜幻境"),
    MINT_OCEAN("薄荷深海");

    companion object {
        fun fromName(name: String): ThemeMode =
            entries.find { it.name == name } ?: BLUE_WHITE
    }
}

data class AppColorSet(
    val colorScheme: ColorScheme,

    // 背景渐变色
    val bgGradientStart: Color,
    val bgGradientEnd: Color,

    // 主按钮渐变
    val btnGradientStart: Color,
    val btnGradientEnd: Color,

    // 卡片背景替补色
    val cardBackgroundAlt: Color,

    // 自定义元素色
    val shimmerBase: Color,
    val shimmerHighlight: Color,

    // 正向/负向/画师提示词 Chip 色
    val promptChipPositive: String,
    val promptChipNegative: String,
    val promptChipArtist: String
)

fun colorSetFor(mode: ThemeMode, darkTheme: Boolean): AppColorSet {
    return when (mode) {
        ThemeMode.BLUE_WHITE -> if (darkTheme) blueWhiteDark else blueWhiteLight
        ThemeMode.SAKURA_PINK -> if (darkTheme) sakuraDark else sakuraLight
        ThemeMode.DARK_PURPLE -> darkPurpleSet
        ThemeMode.MINT_OCEAN -> if (darkTheme) mintDark else mintLight
    }
}

// ============================================
// 蓝白二次元 (默认)
// ============================================
private val blueWhiteLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = Color(0xFF4A90D9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6EBFF),
        onPrimaryContainer = Color(0xFF1A3A5C),
        secondary = Color(0xFF6EC6F8),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD6F0FF),
        onSecondaryContainer = Color(0xFF1B4050),
        tertiary = Color(0xFFFF8FAB),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFD6E3),
        onTertiaryContainer = Color(0xFF4A2030),
        background = Color(0xFFF0F5FF),
        onBackground = Color(0xFF1A2A3A),
        surface = Color.White,
        onSurface = Color(0xFF1A2A3A),
        surfaceVariant = Color(0xFFE8F0FE),
        onSurfaceVariant = Color(0xFF607088),
        error = Color(0xFFFF6B7A),
        onError = Color.White,
        outline = Color(0xFFB3C8E0),
        outlineVariant = Color(0xFFD6E4F4)
    ),
    bgGradientStart = Color(0xFFE8F2FF),
    bgGradientEnd = Color(0xFFFFF0F5),
    btnGradientStart = Color(0xFF4A90D9),
    btnGradientEnd = Color(0xFF6EC6F8),
    cardBackgroundAlt = Color(0xFFF5F9FF),
    shimmerBase = Color(0xFFE0E8F0),
    shimmerHighlight = Color(0xFFF0F5FF),
    promptChipPositive = "#4A90D9",
    promptChipNegative = "#B0A0D0",
    promptChipArtist = "#6EC6F8"
)

private val blueWhiteDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = Color(0xFF6EB8FF),
        onPrimary = Color(0xFF0A243C),
        primaryContainer = Color(0xFF2A5080),
        onPrimaryContainer = Color(0xFFD6EBFF),
        secondary = Color(0xFF7DD3FC),
        onSecondary = Color(0xFF0A2A3C),
        secondaryContainer = Color(0xFF2A5890),
        onSecondaryContainer = Color(0xFFD6F0FF),
        tertiary = Color(0xFFFF8FAB),
        onTertiary = Color(0xFF4A2030),
        tertiaryContainer = Color(0xFF6A4050),
        onTertiaryContainer = Color(0xFFFFD6E3),
        background = Color(0xFF0D1420),
        onBackground = Color(0xFFE8F0FE),
        surface = Color(0xFF192435),
        onSurface = Color(0xFFE8F0FE),
        surfaceVariant = Color(0xFF243040),
        onSurfaceVariant = Color(0xFFB0C0D4),
        error = Color(0xFFFF8A94),
        onError = Color(0xFF301020),
        outline = Color(0xFF506080),
        outlineVariant = Color(0xFF344460)
    ),
    bgGradientStart = Color(0xFF0D1420),
    bgGradientEnd = Color(0xFF152030),
    btnGradientStart = Color(0xFF4A90D9),
    btnGradientEnd = Color(0xFF6EB8FF),
    cardBackgroundAlt = Color(0xFF1E2A3C),
    shimmerBase = Color(0xFF1A2638),
    shimmerHighlight = Color(0xFF243040),
    promptChipPositive = "#4A90D9",
    promptChipNegative = "#A090C8",
    promptChipArtist = "#6EC6F8"
)

// ============================================
// 樱花物语 (保留原有粉色调)
// ============================================
private val sakuraLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = Color(0xFFFF6B9D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFB3CC),
        onPrimaryContainer = Color(0xFF1F1035),
        secondary = Color(0xFFC084FC),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDDD6FE),
        onSecondaryContainer = Color(0xFF1F1035),
        tertiary = Color(0xFF67E8F9),
        onTertiary = Color(0xFF1F1035),
        tertiaryContainer = Color(0xFFA5F3FC),
        onTertiaryContainer = Color(0xFF1F1035),
        background = Color(0xFFFAF5FF),
        onBackground = Color(0xFF1F1035),
        surface = Color(0xFFFFF0F5),
        onSurface = Color(0xFF1F1035),
        surfaceVariant = Color(0xFFFDF2F8),
        onSurfaceVariant = Color(0xFF6B5B7B),
        error = Color(0xFFFF8A80),
        onError = Color.White,
        outline = Color(0xFFFFB3CC),
        outlineVariant = Color(0xFFDDD6FE)
    ),
    bgGradientStart = Color(0xFFFFF0F5),
    bgGradientEnd = Color(0xFFF5F0FF),
    btnGradientStart = Color(0xFFFF6B9D),
    btnGradientEnd = Color(0xFFC084FC),
    cardBackgroundAlt = Color(0xFFFDF2F8),
    shimmerBase = Color(0xFFF5E8F0),
    shimmerHighlight = Color(0xFFFFF0F5),
    promptChipPositive = "#FFC0D0",
    promptChipNegative = "#D8B4FE",
    promptChipArtist = "#B0D8FF"
)

private val sakuraDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = Color(0xFFFF8FAB),
        onPrimary = Color(0xFF1F1035),
        primaryContainer = Color(0xFFE6457A),
        onPrimaryContainer = Color(0xFF2A1F35),
        secondary = Color(0xFFD4B5FE),
        onSecondary = Color(0xFF1F1035),
        secondaryContainer = Color(0xFF9F5FE0),
        onSecondaryContainer = Color(0xFF2A1F35),
        tertiary = Color(0xFF67E8F9),
        onTertiary = Color(0xFF1F1035),
        tertiaryContainer = Color(0xFF22D3EE),
        onTertiaryContainer = Color(0xFF2A1F35),
        background = Color(0xFF0D0815),
        onBackground = Color(0xFFF5F0FF),
        surface = Color(0xFF1A0F24),
        onSurface = Color(0xFFF5F0FF),
        surfaceVariant = Color(0xFF322840),
        onSurfaceVariant = Color(0xFFB8A0C8),
        error = Color(0xFFFF8A80),
        onError = Color(0xFFF5F0FF),
        outline = Color(0xFFE6457A),
        outlineVariant = Color(0xFF9F5FE0)
    ),
    bgGradientStart = Color(0xFF0D0815),
    bgGradientEnd = Color(0xFF1A0F24),
    btnGradientStart = Color(0xFFFF8FAB),
    btnGradientEnd = Color(0xFFD4B5FE),
    cardBackgroundAlt = Color(0xFF322840),
    shimmerBase = Color(0xFF1A1228),
    shimmerHighlight = Color(0xFF242036),
    promptChipPositive = "#FFC0D0",
    promptChipNegative = "#D8B4FE",
    promptChipArtist = "#B0D8FF"
)

// ============================================
// 暗夜幻境 (始终暗色)
// ============================================
private val darkPurpleSet = AppColorSet(
    colorScheme = darkColorScheme(
        primary = Color(0xFFC084FC),
        onPrimary = Color(0xFF1A0F24),
        primaryContainer = Color(0xFF6030A0),
        onPrimaryContainer = Color(0xFFE8D8FF),
        secondary = Color(0xFFFF7597),
        onSecondary = Color(0xFF1A0F24),
        secondaryContainer = Color(0xFF804060),
        onSecondaryContainer = Color(0xFFFFD0DE),
        tertiary = Color(0xFF00E5FF),
        onTertiary = Color(0xFF1A0F24),
        tertiaryContainer = Color(0xFF007880),
        onTertiaryContainer = Color(0xFFB0F8FF),
        background = Color(0xFF0B0815),
        onBackground = Color(0xFFEDE0FF),
        surface = Color(0xFF1A0F28),
        onSurface = Color(0xFFEDE0FF),
        surfaceVariant = Color(0xFF261840),
        onSurfaceVariant = Color(0xFFC0B0D8),
        error = Color(0xFFFF6080),
        onError = Color(0xFF1A0F24),
        outline = Color(0xFF8050B0),
        outlineVariant = Color(0xFF402868)
    ),
    bgGradientStart = Color(0xFF0B0815),
    bgGradientEnd = Color(0xFF140D24),
    btnGradientStart = Color(0xFFC084FC),
    btnGradientEnd = Color(0xFFFF7597),
    cardBackgroundAlt = Color(0xFF221838),
    shimmerBase = Color(0xFF1A102C),
    shimmerHighlight = Color(0xFF261C40),
    promptChipPositive = "#C084FC",
    promptChipNegative = "#FF8A80",
    promptChipArtist = "#00BCD4"
)

// ============================================
// 薄荷深海
// ============================================
private val mintLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = Color(0xFF0891B2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCF3F8),
        onPrimaryContainer = Color(0xFF002A38),
        secondary = Color(0xFF4ADE80),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD4F5E0),
        onSecondaryContainer = Color(0xFF003010),
        tertiary = Color(0xFFF59E0B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF402800),
        background = Color(0xFFF0FDFA),
        onBackground = Color(0xFF162820),
        surface = Color.White,
        onSurface = Color(0xFF162820),
        surfaceVariant = Color(0xFFE0F5F0),
        onSurfaceVariant = Color(0xFF506860),
        error = Color(0xFFEF4444),
        onError = Color.White,
        outline = Color(0xFFA0D0C8),
        outlineVariant = Color(0xFFCCE8E4)
    ),
    bgGradientStart = Color(0xFFF0FDFA),
    bgGradientEnd = Color(0xFFE0F8F8),
    btnGradientStart = Color(0xFF0891B2),
    btnGradientEnd = Color(0xFF4ADE80),
    cardBackgroundAlt = Color(0xFFF5FCFA),
    shimmerBase = Color(0xFFE0F0EC),
    shimmerHighlight = Color(0xFFF0FDF8),
    promptChipPositive = "#0891B2",
    promptChipNegative = "#C084FC",
    promptChipArtist = "#4ADE80"
)

private val mintDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = Color(0xFF22D3EE),
        onPrimary = Color(0xFF002A38),
        primaryContainer = Color(0xFF005060),
        onPrimaryContainer = Color(0xFFCCF3F8),
        secondary = Color(0xFF86EFAC),
        onSecondary = Color(0xFF003010),
        secondaryContainer = Color(0xFF205830),
        onSecondaryContainer = Color(0xFFD4F5E0),
        tertiary = Color(0xFFFCD34D),
        onTertiary = Color(0xFF402800),
        tertiaryContainer = Color(0xFF604800),
        onTertiaryContainer = Color(0xFFFEF3C7),
        background = Color(0xFF0A1510),
        onBackground = Color(0xFFD8F8F0),
        surface = Color(0xFF152A20),
        onSurface = Color(0xFFD8F8F0),
        surfaceVariant = Color(0xFF203830),
        onSurfaceVariant = Color(0xFFA0C0B4),
        error = Color(0xFFF87171),
        onError = Color(0xFF301010),
        outline = Color(0xFF507868),
        outlineVariant = Color(0xFF304C40)
    ),
    bgGradientStart = Color(0xFF0A1510),
    bgGradientEnd = Color(0xFF102018),
    btnGradientStart = Color(0xFF22D3EE),
    btnGradientEnd = Color(0xFF86EFAC),
    cardBackgroundAlt = Color(0xFF1A3028),
    shimmerBase = Color(0xFF182820),
    shimmerHighlight = Color(0xFF203830),
    promptChipPositive = "#22D3EE",
    promptChipNegative = "#C084FC",
    promptChipArtist = "#86EFAC"
)
