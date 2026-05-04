package com.aiphoto.manager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val COMFYUI_URL = stringPreferencesKey("comfyui_url")
        private val COMFYUI_WORKFLOW = stringPreferencesKey("comfyui_workflow")
    }

    val comfyUiUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[COMFYUI_URL] ?: "http://192.168.123.178:8188"
        }

    val comfyUiWorkflow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[COMFYUI_WORKFLOW] ?: ""
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
}
