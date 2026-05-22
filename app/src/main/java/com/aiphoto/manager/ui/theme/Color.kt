package com.aiphoto.manager.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// Tag 预设色 (所有主题共用，保持半透明高对比二次元风格)
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
// 1. 初音未来樱花粉 (Sakura Miku) [默认主题]
// ============================================
val MikuSakuraPrimary = Color(0xFFFF74A3)         // 柔嫩樱花粉
val MikuSakuraSecondary = Color(0xFFBA84FC)       // 梦幻紫罗兰
val MikuSakuraTertiary = Color(0xFFFFB8D0)        // 软萌樱粉
val MikuSakuraBgStart = Color(0xFFFFEFF4)         // 渐变粉白起
val MikuSakuraBgEnd = Color(0xFFF3E9FF)           // 渐变紫白终

// ============================================
// 2. 初音未来经典蓝 (Miku Teal/Blue)
// ============================================
val MikuBluePrimary = Color(0xFF39C5BB)           // 标志性初音蓝绿 (Teal)
val MikuBlueSecondary = Color(0xFF22B9EE)         // 歌姬霓虹亮蓝
val MikuBlueTertiary = Color(0xFF67E8F9)          // 亮丽青蓝
val MikuBlueBgStart = Color(0xFFE8FAF7)           // 渐变青蓝白起
val MikuBlueBgEnd = Color(0xFFE3F5FF)             // 渐变浅蓝白终

// ============================================
// 3. 初音未来葱绿 (Miku Leek Green)
// ============================================
val MikuGreenPrimary = Color(0xFF7DC876)          // 活力大葱绿
val MikuGreenSecondary = Color(0xFFFFD54F)        // 柠檬黄
val MikuGreenTertiary = Color(0xFFA5F3FC)         // 莹润青蓝
val MikuGreenBgStart = Color(0xFFF1FAF0)          // 葱绿浅白起
val MikuGreenBgEnd = Color(0xFFFFFDE7)            // 暖洋柠檬白终

// ============================================
// 4. 暗夜星辰 (Abyss Midnight) [暗色调]
// ============================================
val MikuDarkPrimary = Color(0xFFBB86FC)           // 罗兰紫色
val MikuDarkSecondary = Color(0xFF39C5BB)         // 葱蓝绿点缀
val MikuDarkTertiary = Color(0xFFFF7597)          // 霓虹荧光粉
val MikuDarkBgStart = Color(0xFF0A0515)           // 深邃暗紫底
val MikuDarkBgEnd = Color(0xFF1B0D2E)             // 暗夜星空终

// ============================================
// 5. 赛博极光 (Cyber Neon) [高对比暗色调]
// ============================================
val CyberNeonPrimary = Color(0xFFFF8500)          // 赛博霓虹橙
val CyberNeonSecondary = Color(0xFF00E5FF)        // 荧光极光青
val CyberNeonTertiary = Color(0xFFBA68C8)         // 霓虹紫
val CyberNeonBgStart = Color(0xFF070B18)          // 赛博黑底
val CyberNeonBgEnd = Color(0xFF1B112D)            // 极光星雨底

// ============================================
// 通用状态色
// ============================================
val ErrorSoft = Color(0xFFFF8A80)
val SuccessSoft = Color(0xFF81C784)
val WarningSoft = Color(0xFFFFD54F)
