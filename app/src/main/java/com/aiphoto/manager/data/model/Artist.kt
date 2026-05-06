package com.aiphoto.manager.data.model

data class Artist(
    val slug: String,
    val tag: String,
    val imageId: String,
    val postCount: Int,
    val shard: String,
    val hasImage: Boolean
)
