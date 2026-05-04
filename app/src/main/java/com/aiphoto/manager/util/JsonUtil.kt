package com.aiphoto.manager.util

import android.content.Context
import android.net.Uri
import com.aiphoto.manager.data.local.dao.PromptDao
import com.aiphoto.manager.data.local.dao.TagDao
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.PromptTagCrossRef
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

data class ExportData(
    @SerializedName("prompts") val prompts: List<PromptExport>,
    @SerializedName("tags") val tags: List<TagExport>,
    @SerializedName("version") val version: Int = 2
)

data class PromptExport(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("positivePrompt") val positivePrompt: String,
    @SerializedName("negativePrompt") val negativePrompt: String,
    @SerializedName("seed") val seed: String,
    @SerializedName("parameters") val parameters: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("steps") val steps: Int,
    @SerializedName("cfgScale") val cfgScale: Double,
    @SerializedName("isFavorite") val isFavorite: Boolean,
    @SerializedName("isPinned") val isPinned: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("imagePaths") val imagePaths: List<String>
)

data class TagExport(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String
)

object JsonUtil {

    private val gson = Gson()

    suspend fun exportToJsonString(
        context: Context,
        promptDao: PromptDao,
        tagDao: TagDao
    ): String {
        val allTags = tagDao.getAllTagsSync()
        val allPrompts = promptDao.getAllPromptsSync()

        val exportData = ExportData(
            prompts = allPrompts.map { pwt ->
                PromptExport(
                    id = pwt.prompt.id,
                    title = pwt.prompt.title,
                    description = pwt.prompt.description,
                    positivePrompt = pwt.prompt.positivePrompt,
                    negativePrompt = pwt.prompt.negativePrompt,
                    seed = pwt.prompt.seed,
                    parameters = pwt.prompt.parameters,
                    width = pwt.prompt.width,
                    height = pwt.prompt.height,
                    steps = pwt.prompt.steps,
                    cfgScale = pwt.prompt.cfgScale,
                    isFavorite = pwt.prompt.isFavorite,
                    isPinned = pwt.prompt.isPinned,
                    createdAt = pwt.prompt.createdAt,
                    tags = pwt.tags.map { it.name },
                    imagePaths = pwt.images.map { it.imagePath }
                )
            },
            tags = allTags.map { TagExport(it.id, it.name, it.color) }
        )

        return gson.toJson(exportData)
    }

    suspend fun importFromJsonString(
        context: Context,
        json: String,
        promptDao: PromptDao,
        tagDao: TagDao
    ) {
        try {
            val type = object : TypeToken<ExportData>() {}.type
            val data = gson.fromJson<ExportData>(json, type) ?: return

            // Import tags first
            data.tags.forEach { tagExport ->
                val existing = tagDao.getTagByName(tagExport.name)
                if (existing == null) {
                    tagDao.insertTag(
                        com.aiphoto.manager.data.local.entity.TagEntity(
                            id = tagExport.id,
                            name = tagExport.name,
                            color = tagExport.color
                        )
                    )
                }
            }

            // Import prompts
            data.prompts.forEach { promptExport ->
                promptDao.insertPrompt(
                    PromptEntity(
                        id = promptExport.id,
                        title = promptExport.title,
                        description = promptExport.description,
                        positivePrompt = promptExport.positivePrompt,
                        negativePrompt = promptExport.negativePrompt,
                        seed = promptExport.seed,
                        parameters = promptExport.parameters,
                        width = promptExport.width,
                        height = promptExport.height,
                        steps = promptExport.steps,
                        cfgScale = promptExport.cfgScale,
                        isFavorite = promptExport.isFavorite,
                        isPinned = promptExport.isPinned,
                        createdAt = promptExport.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                // Link tags
                promptExport.tags.forEach { tagName ->
                    val tag = tagDao.getTagByName(tagName)
                    tag?.let {
                        promptDao.insertPromptTagCrossRef(
                            PromptTagCrossRef(promptExport.id, it.id)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToJson(
        context: Context,
        uri: Uri,
        promptDao: PromptDao,
        tagDao: TagDao
    ) {
        runBlocking(Dispatchers.IO) {
            val json = exportToJsonString(context, promptDao, tagDao)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray())
            }
        }
    }

    fun importFromJson(
        context: Context,
        uri: Uri,
        promptDao: PromptDao,
        tagDao: TagDao
    ) {
        runBlocking(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    ?.use { it.readText() } ?: return@runBlocking

                importFromJsonString(context, json, promptDao, tagDao)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
