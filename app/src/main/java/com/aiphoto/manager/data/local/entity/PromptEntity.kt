package com.aiphoto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val positivePrompt: String = "",
    val negativePrompt: String = "lazyneg, lazyhand, censored, mosaic censoring, photorealistic, realistic, artist name, signature, lowres, bad anatomy, bad hands, text, error, missing fingers, extra fingers, fewer digits, cropped, worst quality, low quality, jpeg artifacts, watermark, username, sketch, jpeg Closed eyes, artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, shiny clothes, shiny skin, gold skin, white hair, halo,three hands",
    val seed: String = "",
    val parameters: String = "",
    // 生成参数
    val width: Int = 896,
    val height: Int = 1088,
    val steps: Int = 30,
    val cfgScale: Double = 5.5,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val artistPrompt: String = "",
    // 工作流模型/LoRA 配置
    val baseModel: String? = null,        // 选用的基础模型 (UNETLoader.unet_name)
    val loraConfigs: String? = null,      // LoRA 配置 JSON: [{"name":"...","strength":1.0,"enabled":true},...]
    val clipModel: String? = null,        // CLIP 模型 (CLIPLoader)
    val vaeModel: String? = null,         // VAE 模型 (VAELoader)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
