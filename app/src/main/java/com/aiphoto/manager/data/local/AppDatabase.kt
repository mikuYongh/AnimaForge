package com.aiphoto.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aiphoto.manager.data.local.dao.FavoritePromptDao
import com.aiphoto.manager.data.local.dao.GeneratedImageDao
import com.aiphoto.manager.data.local.dao.PromptDao
import com.aiphoto.manager.data.local.dao.TagDao
import com.aiphoto.manager.data.local.dao.WorkflowDao
import com.aiphoto.manager.data.local.entity.FavoritePromptEntity
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.PromptImageEntity
import com.aiphoto.manager.data.local.entity.PromptTagCrossRef
import com.aiphoto.manager.data.local.entity.TagEntity
import com.aiphoto.manager.data.local.entity.WorkflowEntity

@Database(
    entities = [
        PromptEntity::class,
        TagEntity::class,
        PromptTagCrossRef::class,
        PromptImageEntity::class,
        WorkflowEntity::class,
        GeneratedImageEntity::class,
        FavoritePromptEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun tagDao(): TagDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun generatedImageDao(): GeneratedImageDao
    abstract fun favoritePromptDao(): FavoritePromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 从版本3迁移到版本4：为prompts表添加width, height, steps, cfgScale字段
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE prompts ADD COLUMN width INTEGER NOT NULL DEFAULT 896")
                database.execSQL("ALTER TABLE prompts ADD COLUMN height INTEGER NOT NULL DEFAULT 1088")
                database.execSQL("ALTER TABLE prompts ADD COLUMN steps INTEGER NOT NULL DEFAULT 30")
                database.execSQL("ALTER TABLE prompts ADD COLUMN cfgScale REAL NOT NULL DEFAULT 5.5")
            }
        }

        // 从版本4迁移到版本5：添加 favorite_prompts 表
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_prompts (
                        id TEXT NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        type TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_prompt_manager.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
