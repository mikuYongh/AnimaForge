package com.aiphoto.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aiphoto.manager.ui.theme.AIPromptManagerTheme
import com.aiphoto.manager.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = (application as App).settingsManager

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState(initial = null)
            val useDarkTheme = isSystemInDarkTheme()

            AIPromptManagerTheme(
                themeMode = themeMode ?: ThemeMode.BLUE_WHITE,
                darkTheme = useDarkTheme,
            ) {
                com.aiphoto.manager.ui.navigation.AppNavGraph()
            }
        }
    }
}
