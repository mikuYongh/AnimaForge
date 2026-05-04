package com.aiphoto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiphoto.manager.data.local.entity.FavoritePromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePromptDao {
    @Query("SELECT * FROM favorite_prompts WHERE type = :type ORDER BY createdAt DESC")
    fun getByType(type: String): Flow<List<FavoritePromptEntity>>

    @Query("SELECT * FROM favorite_prompts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FavoritePromptEntity>>

    @Query("SELECT * FROM favorite_prompts WHERE content = :content AND type = :type LIMIT 1")
    suspend fun findByContentAndType(content: String, type: String): FavoritePromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoritePrompt: FavoritePromptEntity)

    @Delete
    suspend fun delete(favoritePrompt: FavoritePromptEntity)

    @Query("DELETE FROM favorite_prompts WHERE id = :id")
    suspend fun deleteById(id: String)
}
