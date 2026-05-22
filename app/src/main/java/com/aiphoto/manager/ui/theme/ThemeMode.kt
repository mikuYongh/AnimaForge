package com.aiphoto.manager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    MIKU_SAKURA("初音樱花粉"),
    MIKU_BLUE("初音经典蓝"),
    MIKU_GREEN("初音葱绿色"),
    DARK_PURPLE("暗夜星辰"),
    CYBER_NEON("赛博极光");

    companion object {
        fun fromName(name: String): ThemeMode =
            entries.find { it.name == name } ?: MIKU_SAKURA
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
        ThemeMode.MIKU_SAKURA -> if (darkTheme) mikuSakuraDark else mikuSakuraLight
        ThemeMode.MIKU_BLUE -> if (darkTheme) mikuBlueDark else mikuBlueLight
        ThemeMode.MIKU_GREEN -> if (darkTheme) mikuGreenDark else mikuGreenLight
        ThemeMode.DARK_PURPLE -> mikuDarkSet
        ThemeMode.CYBER_NEON -> cyberNeonSet
    }
}

// ============================================
// 1. 初音未来樱花粉 (MIKU_SAKURA)
// ============================================
private val mikuSakuraLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = MikuSakuraPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD6E4),
        onPrimaryContainer = Color(0xFF6A0C35),
        secondary = MikuSakuraSecondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF5E8FF),
        onSecondaryContainer = Color(0xFF381060),
        tertiary = MikuSakuraTertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFF0F5),
        onTertiaryContainer = Color(0xFF5A1A32),
        background = Color(0xFFFFFAFB),
        onBackground = Color(0xFF2C101E),
        surface = Color.White,
        onSurface = Color(0xFF2C101E),
        surfaceVariant = Color(0xFFFFEFF3),
        onSurfaceVariant = Color(0xFF8C586E),
        error = ErrorSoft,
        onError = Color.White,
        outline = Color(0xFFFFB8D0),
        outlineVariant = Color(0xFFFFE5EE)
    ),
    bgGradientStart = MikuSakuraBgStart,
    bgGradientEnd = MikuSakuraBgEnd,
    btnGradientStart = MikuSakuraPrimary,
    btnGradientEnd = MikuSakuraSecondary,
    cardBackgroundAlt = Color(0xFFFFEFF3),
    shimmerBase = Color(0xFFFBE4EC),
    shimmerHighlight = Color(0xFFFFF0F5),
    promptChipPositive = "#FF74A3",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#FFB8D0"
)

private val mikuSakuraDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = MikuSakuraPrimary,
        onPrimary = Color(0xFF4C0322),
        primaryContainer = Color(0xFF8F1E4A),
        onPrimaryContainer = Color(0xFFFFD6E4),
        secondary = MikuSakuraSecondary,
        onSecondary = Color(0xFF38006B),
        secondaryContainer = Color(0xFF5A2A9A),
        onSecondaryContainer = Color(0xFFF5E8FF),
        tertiary = MikuSakuraTertiary,
        onTertiary = Color(0xFF5A1A32),
        tertiaryContainer = Color(0xFF6E2845),
        onTertiaryContainer = Color(0xFFFFF0F5),
        background = Color(0xFF1C0A12),
        onBackground = Color(0xFFFFE5EE),
        surface = Color(0xFF2D1420),
        onSurface = Color(0xFFFFE5EE),
        surfaceVariant = Color(0xFF3E1D2D),
        onSurfaceVariant = Color(0xFFD6A0B8),
        error = ErrorSoft,
        onError = Color(0xFF4C0322),
        outline = Color(0xFF8F1E4A),
        outlineVariant = Color(0xFF5E1232)
    ),
    bgGradientStart = Color(0xFF1C0A12),
    bgGradientEnd = Color(0xFF2C1024),
    btnGradientStart = MikuSakuraPrimary,
    btnGradientEnd = MikuSakuraSecondary,
    cardBackgroundAlt = Color(0xFF3E1D2D),
    shimmerBase = Color(0xFF301524),
    shimmerHighlight = Color(0xFF3E1D2D),
    promptChipPositive = "#FF74A3",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#FFB8D0"
)

// ============================================
// 2. 初音未来经典蓝 (MIKU_BLUE)
// ============================================
private val mikuBlueLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = MikuBluePrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD2F5F3),
        onPrimaryContainer = Color(0xFF003835),
        secondary = MikuBlueSecondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F7FF),
        onSecondaryContainer = Color(0xFF003B52),
        tertiary = MikuBlueTertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFECFDFF),
        onTertiaryContainer = Color(0xFF00444A),
        background = Color(0xFFF6FDFD),
        onBackground = Color(0xFF002927),
        surface = Color.White,
        onSurface = Color(0xFF002927),
        surfaceVariant = Color(0xFFE6F5F4),
        onSurfaceVariant = Color(0xFF4FA09D),
        error = ErrorSoft,
        onError = Color.White,
        outline = Color(0xFFA2E5E1),
        outlineVariant = Color(0xFFD2F5F3)
    ),
    bgGradientStart = MikuBlueBgStart,
    bgGradientEnd = MikuBlueBgEnd,
    btnGradientStart = MikuBluePrimary,
    btnGradientEnd = MikuBlueSecondary,
    cardBackgroundAlt = Color(0xFFE6F5F4),
    shimmerBase = Color(0xFFD2EDE9),
    shimmerHighlight = Color(0xFFE2FAF7),
    promptChipPositive = "#39C5BB",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#22B9EE"
)

private val mikuBlueDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = MikuBluePrimary,
        onPrimary = Color(0xFF003835),
        primaryContainer = Color(0xFF005A56),
        onPrimaryContainer = Color(0xFFD2F5F3),
        secondary = MikuBlueSecondary,
        onSecondary = Color(0xFF003347),
        secondaryContainer = Color(0xFF004E6C),
        onSecondaryContainer = Color(0xFFE0F7FF),
        tertiary = MikuBlueTertiary,
        onTertiary = Color(0xFF00373C),
        tertiaryContainer = Color(0xFF00565F),
        onTertiaryContainer = Color(0xFFECFDFF),
        background = Color(0xFF061414),
        onBackground = Color(0xFFD2EDE9),
        surface = Color(0xFF102626),
        onSurface = Color(0xFFD2EDE9),
        surfaceVariant = Color(0xFF183B3B),
        onSurfaceVariant = Color(0xFF86CBC7),
        error = ErrorSoft,
        onError = Color(0xFF003835),
        outline = Color(0xFF005A56),
        outlineVariant = Color(0xFF003E3B)
    ),
    bgGradientStart = Color(0xFF061414),
    bgGradientEnd = Color(0xFF0D2529),
    btnGradientStart = MikuBluePrimary,
    btnGradientEnd = MikuBlueSecondary,
    cardBackgroundAlt = Color(0xFF183B3B),
    shimmerBase = Color(0xFF122C2C),
    shimmerHighlight = Color(0xFF183B3B),
    promptChipPositive = "#39C5BB",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#22B9EE"
)

// ============================================
// 3. 初音未来葱绿 (MIKU_GREEN)
// ============================================
private val mikuGreenLight = AppColorSet(
    colorScheme = lightColorScheme(
        primary = MikuGreenPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2F8DE),
        onPrimaryContainer = Color(0xFF153C10),
        secondary = MikuGreenSecondary,
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFFFFF9C4),
        onSecondaryContainer = Color(0xFF5D4037),
        tertiary = MikuGreenTertiary,
        onTertiary = Color(0xFF003538),
        tertiaryContainer = Color(0xFFE0F7FA),
        onTertiaryContainer = Color(0xFF004D40),
        background = Color(0xFFF9FDF9),
        onBackground = Color(0xFF10250E),
        surface = Color.White,
        onSurface = Color(0xFF10250E),
        surfaceVariant = Color(0xFFEDF8EA),
        onSurfaceVariant = Color(0xFF679E61),
        error = ErrorSoft,
        onError = Color.White,
        outline = Color(0xFFB5E5B0),
        outlineVariant = Color(0xFFE2F8DE)
    ),
    bgGradientStart = MikuGreenBgStart,
    bgGradientEnd = MikuGreenBgEnd,
    btnGradientStart = MikuGreenPrimary,
    btnGradientEnd = MikuGreenSecondary,
    cardBackgroundAlt = Color(0xFFEDF8EA),
    shimmerBase = Color(0xFFDEEDE0),
    shimmerHighlight = Color(0xFFEEFAF1),
    promptChipPositive = "#7DC876",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#FFD54F"
)

private val mikuGreenDark = AppColorSet(
    colorScheme = darkColorScheme(
        primary = MikuGreenPrimary,
        onPrimary = Color(0xFF153C10),
        primaryContainer = Color(0xFF2D5C26),
        onPrimaryContainer = Color(0xFFE2F8DE),
        secondary = MikuGreenSecondary,
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFF8C7B1E),
        onSecondaryContainer = Color(0xFFFFF9C4),
        tertiary = MikuGreenTertiary,
        onTertiary = Color(0xFF003538),
        tertiaryContainer = Color(0xFF205E64),
        onTertiaryContainer = Color(0xFFE0F7FA),
        background = Color(0xFF091408),
        onBackground = Color(0xFFDEEDE0),
        surface = Color(0xFF152613),
        onSurface = Color(0xFFDEEDE0),
        surfaceVariant = Color(0xFF223B20),
        onSurfaceVariant = Color(0xFF90C28A),
        error = ErrorSoft,
        onError = Color(0xFF153C10),
        outline = Color(0xFF2D5C26),
        outlineVariant = Color(0xFF1D3B18)
    ),
    bgGradientStart = Color(0xFF091408),
    bgGradientEnd = Color(0xFF1A2612),
    btnGradientStart = MikuGreenPrimary,
    btnGradientEnd = MikuGreenSecondary,
    cardBackgroundAlt = Color(0xFF223B20),
    shimmerBase = Color(0xFF162914),
    shimmerHighlight = Color(0xFF223B20),
    promptChipPositive = "#7DC876",
    promptChipNegative = "#BA84FC",
    promptChipArtist = "#FFD54F"
)

// ============================================
// 4. 暗夜星辰 (DARK_PURPLE)
// ============================================
private val mikuDarkSet = AppColorSet(
    colorScheme = darkColorScheme(
        primary = MikuDarkPrimary,
        onPrimary = Color(0xFF280056),
        primaryContainer = Color(0xFF4F278C),
        onPrimaryContainer = Color(0xFFEEDDFF),
        secondary = MikuDarkSecondary,
        onSecondary = Color(0xFF003835),
        secondaryContainer = Color(0xFF005652),
        onSecondaryContainer = Color(0xFFD2F5F3),
        tertiary = MikuDarkTertiary,
        onTertiary = Color(0xFF4C0320),
        tertiaryContainer = Color(0xFF7A2548),
        onTertiaryContainer = Color(0xFFFFD6E1),
        background = MikuDarkBgStart,
        onBackground = Color(0xFFEEDDFF),
        surface = Color(0xFF170D28),
        onSurface = Color(0xFFEEDDFF),
        surfaceVariant = Color(0xFF22113A),
        onSurfaceVariant = Color(0xFFC0A2E2),
        error = ErrorSoft,
        onError = Color(0xFF280056),
        outline = Color(0xFF6B45A8),
        outlineVariant = Color(0xFF3D2168)
    ),
    bgGradientStart = MikuDarkBgStart,
    bgGradientEnd = MikuDarkBgEnd,
    btnGradientStart = MikuDarkPrimary,
    btnGradientEnd = MikuDarkSecondary,
    cardBackgroundAlt = Color(0xFF22113A),
    shimmerBase = Color(0xFF1D0E31),
    shimmerHighlight = Color(0xFF2A1545),
    promptChipPositive = "#BB86FC",
    promptChipNegative = "#FF7597",
    promptChipArtist = "#39C5BB"
)

// ============================================
// 5. 赛博极光 (CYBER_NEON)
// ============================================
private val cyberNeonSet = AppColorSet(
    colorScheme = darkColorScheme(
        primary = CyberNeonPrimary,
        onPrimary = Color(0xFF3A1F00),
        primaryContainer = Color(0xFF704400),
        onPrimaryContainer = Color(0xFFFFE0B2),
        secondary = CyberNeonSecondary,
        onSecondary = Color(0xFF00383B),
        secondaryContainer = Color(0xFF00646B),
        onSecondaryContainer = Color(0xFFE0F7FA),
        tertiary = CyberNeonTertiary,
        onTertiary = Color(0xFF310047),
        tertiaryContainer = Color(0xFF5D1E80),
        onTertiaryContainer = Color(0xFFF3E5F5),
        background = CyberNeonBgStart,
        onBackground = Color(0xFFE0E6ED),
        surface = Color(0xFF10162B),
        onSurface = Color(0xFFE0E6ED),
        surfaceVariant = Color(0xFF1B2342),
        onSurfaceVariant = Color(0xFF8CA5CF),
        error = ErrorSoft,
        onError = Color(0xFF3A1F00),
        outline = Color(0xFF905900),
        outlineVariant = Color(0xFF543400)
    ),
    bgGradientStart = CyberNeonBgStart,
    bgGradientEnd = CyberNeonBgEnd,
    btnGradientStart = CyberNeonPrimary,
    btnGradientEnd = CyberNeonSecondary,
    cardBackgroundAlt = Color(0xFF1B2342),
    shimmerBase = Color(0xFF141B32),
    shimmerHighlight = Color(0xFF202A4C),
    promptChipPositive = "#FF8500",
    promptChipNegative = "#BA68C8",
    promptChipArtist = "#00E5FF"
)
