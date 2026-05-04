package com.aiphoto.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aiphoto.manager.ui.theme.AIPromptManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPromptManagerTheme {
                com.aiphoto.manager.ui.navigation.AppNavGraph()
            }
        }
    }
}
