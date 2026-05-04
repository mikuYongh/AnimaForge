package com.aiphoto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "prompt_images",
    foreignKeys = [
        ForeignKey(
            entity = PromptEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PromptImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val promptId: String,
    val imagePath: String
)
