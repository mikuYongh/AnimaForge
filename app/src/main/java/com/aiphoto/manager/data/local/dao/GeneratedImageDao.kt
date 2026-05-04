package com.aiphoto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images ORDER BY createdAt DESC")
    fun getAllGeneratedImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE promptId = :promptId ORDER BY createdAt DESC")
    fun getImagesByPromptId(promptId: String): Flow<List<GeneratedImageEntity>>

    @Insert
    suspend fun insertGeneratedImage(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteGeneratedImage(id: String)

    @Query("DELETE FROM generated_images")
    suspend fun deleteAllGeneratedImages()
}
