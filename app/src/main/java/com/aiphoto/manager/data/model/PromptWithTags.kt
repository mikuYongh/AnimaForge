package com.aiphoto.manager.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.PromptImageEntity
import com.aiphoto.manager.data.local.entity.TagEntity

data class PromptWithTags(
    @Embedded val prompt: PromptEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = com.aiphoto.manager.data.local.entity.PromptTagCrossRef::class,
            parentColumn = "promptId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "promptId"
    )
    val images: List<PromptImageEntity> = emptyList()
)
