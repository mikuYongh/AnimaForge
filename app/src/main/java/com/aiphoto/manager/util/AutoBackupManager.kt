package com.aiphoto.manager.util

import android.content.Context
import android.util.Log
import com.aiphoto.manager.data.local.dao.PromptDao
import com.aiphoto.manager.data.local.dao.TagDao
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AutoBackupManager {
    private const val TAG = "AutoBackupManager"
    private const val BACKUP_DIR = "backups"
    private const val MAX_BACKUPS = 10 // 保留最近10个备份
    private const val BACKUP_PREFIX = "auto_backup_"

    suspend fun createAutoBackup(context: Context, promptDao: PromptDao, tagDao: TagDao): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val backupDir = File(context.filesDir, BACKUP_DIR)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "${BACKUP_PREFIX}${timestamp}.json")

                // 导出数据
                val jsonData = JsonUtil.exportToJsonString(context, promptDao, tagDao)

                // 写入备份文件
                backupFile.writeText(jsonData)

                Log.d(TAG, "自动备份创建成功: ${backupFile.name}")

                // 清理旧备份
                cleanOldBackups(backupDir)

                true
            } catch (e: Exception) {
                Log.e(TAG, "自动备份失败", e)
                false
            }
        }
    }

    private fun cleanOldBackups(backupDir: File) {
        try {
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() } ?: return

            // 删除超过最大数量的旧备份
            if (backupFiles.size > MAX_BACKUPS) {
                backupFiles.drop(MAX_BACKUPS).forEach { file ->
                    file.delete()
                    Log.d(TAG, "删除旧备份: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧备份失败", e)
        }
    }

    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getLastBackupTime(context: Context): Long? {
        val backupFiles = getBackupFiles(context)
        return backupFiles.firstOrNull()?.lastModified()
    }

    suspend fun restoreFromBackup(context: Context, backupFile: File, promptDao: PromptDao, tagDao: TagDao): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = backupFile.readText()
                JsonUtil.importFromJsonString(context, jsonData, promptDao, tagDao)
                Log.d(TAG, "从备份恢复成功: ${backupFile.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "从备份恢复失败", e)
                false
            }
        }
    }
}
