package com.aiphoto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.PromptImageEntity
import com.aiphoto.manager.data.local.entity.PromptTagCrossRef
import com.aiphoto.manager.data.model.PromptWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    @Transaction
    @Query("SELECT * FROM prompts ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllPrompts(): Flow<List<PromptWithTags>>

    @Transaction
    @Query("SELECT * FROM prompts ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllPromptsSync(): List<PromptWithTags>

    @Transaction
    @Query("SELECT * FROM prompts WHERE id = :promptId")
    fun getPromptById(promptId: String): Flow<PromptWithTags?>

    @Transaction
    @Query("""
        SELECT * FROM prompts
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR positivePrompt LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchPrompts(query: String): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM prompts p
        INNER JOIN prompt_tag_cross_ref pt ON p.id = pt.promptId
        WHERE pt.tagId IN (:tagIds)
        ORDER BY p.isPinned DESC, p.updatedAt DESC
    """)
    fun filterByTags(tagIds: List<String>): Flow<List<PromptWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT p.* FROM prompts p
        INNER JOIN prompt_tag_cross_ref pt ON p.id = pt.promptId
        WHERE pt.tagId IN (:tagIds)
           AND (p.title LIKE '%' || :query || '%'
             OR p.description LIKE '%' || :query || '%'
             OR p.positivePrompt LIKE '%' || :query || '%')
        ORDER BY p.isPinned DESC, p.updatedAt DESC
    """)
    fun searchAndFilter(query: String, tagIds: List<String>): Flow<List<PromptWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity)

    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)

    @Query("UPDATE prompts SET width = :width, height = :height, steps = :steps, cfgScale = :cfgScale, updatedAt = :updatedAt WHERE id = :promptId")
    suspend fun updatePromptDimensions(promptId: String, width: Int, height: Int, steps: Int, cfgScale: Double, updatedAt: Long)

    @Query("DELETE FROM prompts WHERE id = :promptId")
    suspend fun deletePromptById(promptId: String)

    @Query("UPDATE prompts SET isFavorite = :favorite WHERE id = :promptId")
    suspend fun setFavorite(promptId: String, favorite: Boolean)

    @Query("UPDATE prompts SET isPinned = :pinned WHERE id = :promptId")
    suspend fun setPinned(promptId: String, pinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPromptTagCrossRef(crossRef: PromptTagCrossRef)

    @Query("DELETE FROM prompt_tag_cross_ref WHERE promptId = :promptId")
    suspend fun deletePromptTags(promptId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(image: PromptImageEntity)

    @Query("DELETE FROM prompt_images WHERE promptId = :promptId")
    suspend fun deletePromptImages(promptId: String)

    @Query("DELETE FROM prompt_images WHERE id = :imageId")
    suspend fun deleteImage(imageId: Long)

    @Transaction
    suspend fun updatePromptTags(promptId: String, tagIds: List<String>) {
        deletePromptTags(promptId)
        tagIds.forEach { tagId ->
            insertPromptTagCrossRef(PromptTagCrossRef(promptId, tagId))
        }
    }

    @Transaction
    suspend fun insertPromptFull(
        prompt: PromptEntity,
        tagIds: List<String>,
        images: List<PromptImageEntity>
    ) {
        insertPrompt(prompt)
        tagIds.forEach { tagId ->
            insertPromptTagCrossRef(PromptTagCrossRef(prompt.id, tagId))
        }
        images.forEach { image ->
            insertImage(image)
        }
    }
}
