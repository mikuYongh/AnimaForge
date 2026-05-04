package com.aiphoto.manager.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aiphoto.manager.data.local.AppDatabase
import com.aiphoto.manager.util.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

    fun loadBackupFiles(context: Context) {
        _backupFiles.value = AutoBackupManager.getBackupFiles(context)
        _lastBackupTime.value = AutoBackupManager.getLastBackupTime(context)
    }

    fun createBackup(context: Context, onComplete: (Boolean) -> Unit) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
