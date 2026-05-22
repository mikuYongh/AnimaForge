package com.aiphoto.manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColorSet = compositionLocalOf { colorSetFor(ThemeMode.MIKU_SAKURA, false) }
val LocalThemeMode = compositionLocalOf { ThemeMode.MIKU_SAKURA }

@Composable
fun AIPromptManagerTheme(
    themeMode: ThemeMode = ThemeMode.MIKU_SAKURA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorSet = remember(themeMode, darkTheme) {
        colorSetFor(themeMode, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            window.statusBarColor = colorSet.colorScheme.surface.toArgb()
            window.navigationBarColor = colorSet.colorScheme.surface.toArgb()

            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColorSet provides colorSet,
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorSet.colorScheme,
            typography = AnimeTypography,
            shapes = Shapes,
            content = content
        )
    }
}
