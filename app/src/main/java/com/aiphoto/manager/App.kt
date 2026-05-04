package com.aiphoto.manager

import android.app.Application
import com.aiphoto.manager.util.AutoBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { com.aiphoto.manager.data.local.AppDatabase.getInstance(this) }
    val settingsManager by lazy { com.aiphoto.manager.data.SettingsManager(this) }

    override fun onCreate() {
        super.onCreate()

        // 自动备份：每次启动应用时创建备份
        applicationScope.launch {
            try {
                AutoBackupManager.createAutoBackup(
                    context = this@App,
                    promptDao = database.promptDao(),
                    tagDao = database.tagDao()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
