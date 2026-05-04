package com.aiphoto.manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Color Scheme
private val AnimeColorScheme = lightColorScheme(
    primary = SakuraPink,
    onPrimary = CardBackground,
    primaryContainer = SakuraPinkLight,
    onPrimaryContainer = TextPrimary,
    secondary = LavenderPurple,
    onSecondary = CardBackground,
    secondaryContainer = LavenderPurpleLight,
    onSecondaryContainer = TextPrimary,
    tertiary = MintBlue,
    onTertiary = TextPrimary,
    tertiaryContainer = MintBlueLight,
    onTertiaryContainer = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundAlt,
    onSurfaceVariant = TextSecondary,
    error = ErrorSoft,
    onError = CardBackground,
    outline = SakuraPinkLight,
    outlineVariant = LavenderPurpleLight
)

// Dark Theme Color Scheme
private val AnimeDarkColorScheme = darkColorScheme(
    primary = SakuraPinkDarkTheme,
    onPrimary = TextPrimaryDark,
    primaryContainer = SakuraPinkDark,
    onPrimaryContainer = CardBackgroundDark,
    secondary = LavenderPurpleDarkTheme,
    onSecondary = TextPrimaryDark,
    secondaryContainer = LavenderPurpleDark,
    onSecondaryContainer = CardBackgroundDark,
    tertiary = MintBlueDarkTheme,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = MintBlueDark,
    onTertiaryContainer = CardBackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardBackgroundAltDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorSoft,
    onError = TextPrimaryDark,
    outline = SakuraPinkDark,
    outlineVariant = LavenderPurpleDark
)

@Composable
fun AIPromptManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain consistent anime theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        AnimeDarkColorScheme
    } else {
        AnimeColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnimeTypography,
        shapes = Shapes,  // Integrate shape system
        content = content
    )
}
