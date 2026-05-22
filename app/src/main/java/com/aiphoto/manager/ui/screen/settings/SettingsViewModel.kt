package com.aiphoto.manager.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.data.SettingsManager
import com.aiphoto.manager.data.local.AppDatabase
import com.aiphoto.manager.ui.theme.ThemeMode
import com.aiphoto.manager.util.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel : ViewModel() {

    private val _backupFiles = MutableStateFlow<List<File>>(emptyList())
    val backupFiles: StateFlow<List<File>> = _backupFiles.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private val _currentTheme = MutableStateFlow(ThemeMode.MIKU_SAKURA)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()

    private val _comfyUIUrl = MutableStateFlow("")
    val comfyUIUrl: StateFlow<String> = _comfyUIUrl.asStateFlow()

    private val _comfyUIVideoUrl = MutableStateFlow("")
    val comfyUIVideoUrl: StateFlow<String> = _comfyUIVideoUrl.asStateFlow()

    private var settingsManager: SettingsManager? = null

    fun initSettings(context: Context) {
        if (settingsManager == null) {
            settingsManager = SettingsManager(context)
            viewModelScope.launch {
                settingsManager!!.themeMode.collect { mode ->
                    _currentTheme.value = mode
                }
            }
            viewModelScope.launch {
                settingsManager!!.comfyUiUrl.collect { url ->
                    _comfyUIUrl.value = url
                }
            }
            viewModelScope.launch {
                settingsManager!!.comfyUiVideoUrl.collect { url ->
                    _comfyUIVideoUrl.value = url
                }
            }
        }
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        _currentTheme.value = mode
        val sm = settingsManager ?: SettingsManager(context).also { settingsManager = it }
        viewModelScope.launch {
            sm.saveThemeMode(mode)
        }
    }

    fun saveComfyUIUrl(url: String) {
        _comfyUIUrl.value = url
        val sm = settingsManager ?: return
        viewModelScope.launch { sm.saveComfyUiUrl(url) }
    }

    fun saveComfyUIVideoUrl(url: String) {
        _comfyUIVideoUrl.value = url
        val sm = settingsManager ?: return
        viewModelScope.launch { sm.saveComfyUiVideoUrl(url) }
    }

    fun loadBackupFiles(context: Context) {
        _backupFiles.value = AutoBackupManager.getBackupFiles(context)
        _lastBackupTime.value = AutoBackupManager.getLastBackupTime(context)
    }

    fun createBackup(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            val success = AutoBackupManager.createAutoBackup(
                context = context,
                promptDao = database.promptDao(),
                tagDao = database.tagDao()
            )
            kotlinx.coroutines.Dispatchers.Main.run {
                onComplete(success)
                if (success) {
                    loadBackupFiles(context)
                }
            }
        }
    }

    fun restoreBackup(context: Context, backupFile: File, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            val success = AutoBackupManager.restoreFromBackup(
                context = context,
                backupFile = backupFile,
                promptDao = database.promptDao(),
                tagDao = database.tagDao()
            )
            kotlinx.coroutines.Dispatchers.Main.run {
                onComplete(success)
            }
        }
    }
}
