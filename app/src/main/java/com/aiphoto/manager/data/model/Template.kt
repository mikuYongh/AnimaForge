package com.aiphoto.manager.data.model

data class Template(
    val id: String,
    val name: String,
    val category: String,
    val positivePrompt: String,
    val negativePrompt: String = "",
    val tags: List<String> = emptyList(),
    val previewDescription: String = ""
)
