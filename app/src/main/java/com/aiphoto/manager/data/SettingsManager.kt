package com.aiphoto.manager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiphoto.manager.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val COMFYUI_URL = stringPreferencesKey("comfyui_url")
        private val COMFYUI_WORKFLOW = stringPreferencesKey("comfyui_workflow")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val comfyUiUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[COMFYUI_URL] ?: "http://192.168.123.178:8188"
        }

    val comfyUiWorkflow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[COMFYUI_WORKFLOW] ?: ""
        }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            ThemeMode.fromName(preferences[THEME_MODE] ?: ThemeMode.BLUE_WHITE.name)
        }

    suspend fun saveComfyUiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[COMFYUI_URL] = url
        }
    }

    suspend fun saveComfyUiWorkflow(workflow: String) {
        context.dataStore.edit { preferences ->
            preferences[COMFYUI_WORKFLOW] = workflow
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
}
