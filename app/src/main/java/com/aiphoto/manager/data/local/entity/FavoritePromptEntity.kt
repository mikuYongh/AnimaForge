package com.aiphoto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_prompts")
data class FavoritePromptEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val type: String, // "positive" or "negative"
    val createdAt: Long = System.currentTimeMillis()
)
