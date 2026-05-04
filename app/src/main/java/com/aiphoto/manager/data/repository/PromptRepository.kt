package com.aiphoto.manager.data.repository

import com.aiphoto.manager.data.local.dao.PromptDao
import com.aiphoto.manager.data.local.dao.TagDao
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.PromptImageEntity
import com.aiphoto.manager.data.local.entity.TagEntity
import com.aiphoto.manager.data.model.PromptWithTags
import kotlinx.coroutines.flow.Flow

class PromptRepository(
    private val promptDao: PromptDao,
    private val tagDao: TagDao
) {

    fun getAllPrompts(): Flow<List<PromptWithTags>> = promptDao.getAllPrompts()

    fun getPromptById(id: String): Flow<PromptWithTags?> = promptDao.getPromptById(id)

    fun searchPrompts(query: String): Flow<List<PromptWithTags>> =
        if (query.isBlank()) getAllPrompts() else promptDao.searchPrompts(query)

    fun filterByTags(tagIds: List<String>): Flow<List<PromptWithTags>> =
        if (tagIds.isEmpty()) getAllPrompts() else promptDao.filterByTags(tagIds)

    fun searchAndFilter(query: String, tagIds: List<String>): Flow<List<PromptWithTags>> =
        if (query.isBlank() && tagIds.isEmpty()) getAllPrompts()
        else if (tagIds.isEmpty()) searchPrompts(query)
        else if (query.isBlank()) filterByTags(tagIds)
        else promptDao.searchAndFilter(query, tagIds)

    suspend fun savePrompt(
        prompt: PromptEntity,
        tagIds: List<String>,
        imagePaths: List<String>
    ) {
        val images = imagePaths.map { PromptImageEntity(promptId = prompt.id, imagePath = it) }
        promptDao.insertPromptFull(prompt, tagIds, images)
    }

    suspend fun updatePrompt(
        prompt: PromptEntity,
        tagIds: List<String>,
        imagePaths: List<String>
    ) {
        promptDao.updatePrompt(prompt)
        promptDao.updatePromptTags(prompt.id, tagIds)
        promptDao.deletePromptImages(prompt.id)
        imagePaths.forEach { path ->
            promptDao.insertImage(PromptImageEntity(promptId = prompt.id, imagePath = path))
        }
    }

    suspend fun deletePrompt(promptId: String) = promptDao.deletePromptById(promptId)

    suspend fun toggleFavorite(promptId: String, isFavorite: Boolean) =
        promptDao.setFavorite(promptId, !isFavorite)

    suspend fun togglePinned(promptId: String, isPinned: Boolean) =
        promptDao.setPinned(promptId, !isPinned)

    suspend fun clonePrompt(promptWithTags: PromptWithTags): String {
        val originalPrompt = promptWithTags.prompt
        val newId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // 创建克隆的提示词
        val clonedPrompt = originalPrompt.copy(
            id = newId,
            title = "${originalPrompt.title} (克隆)",
            isFavorite = false,
            isPinned = false,
            createdAt = now,
            updatedAt = now
        )

        // 获取原始提示词的图片
        val imagePaths = promptWithTags.images.map { it.imagePath }

        // 确保所有标签都存在于数据库中
        val tagIds = promptWithTags.tags.map { tag ->
            // 如果标签不存在，则创建它
            val existingTag = tagDao.getTagByName(tag.name)
            if (existingTag != null) {
                existingTag.id
            } else {
                // 创建新标签
                tagDao.insertTag(tag)
                tag.id
            }
        }

        // 保存克隆的提示词
        savePrompt(
            prompt = clonedPrompt,
            tagIds = tagIds,
            imagePaths = imagePaths
        )

        return newId
    }

    // Tag operations
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun getOrCreateTag(name: String, color: String = "#FF6B9D"): TagEntity {
        val existing = tagDao.getTagByName(name)
        if (existing != null) return existing

        val tag = TagEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            color = color
        )
        tagDao.insertTag(tag)

        // 重新查询以确保返回数据库中的对象
        return tagDao.getTagByName(name) ?: tag
    }

    suspend fun insertTag(tag: TagEntity) = tagDao.insertTag(tag)
    suspend fun deleteTag(tagId: String) = tagDao.deleteTagById(tagId)
}
