package com.aiphoto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey val id: String,
    val promptId: String?,
    val imagePath: String,
    val positivePrompt: String,
    val negativePrompt: String,
    val seed: Long,
    val workflowId: String?,
    val createdAt: Long = System.currentTimeMillis()
)
