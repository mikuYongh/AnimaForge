package com.aiphoto.manager.data.repository

import com.aiphoto.manager.data.model.Template

class TemplateRepository {

    fun getTemplates(): List<Template> = predefinedTemplates

    fun getTemplatesByCategory(category: String): List<Template> =
        predefinedTemplates.filter { it.category == category }

    fun getCategories(): List<String> = predefinedTemplates.map { it.category }.distinct()

    companion object {
        private const val DEFAULT_NEGATIVE_PROMPT = "lazyneg, lazyhand, censored, mosaic censoring, photorealistic, realistic, artist name, signature, lowres, bad anatomy, bad hands, text, error, missing fingers, extra fingers, fewer digits, cropped, worst quality, low quality, jpeg artifacts, watermark, username, sketch, jpeg Closed eyes, artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, shiny clothes, shiny skin, gold skin, white hair, halo,three hands"

        private val predefinedTemplates = listOf(
            Template(
                id = "anime_default",
                name = "动漫默认",
                category = "动漫",
                positivePrompt = "masterpiece, best quality, highly detailed, anime style, beautiful face, detailed eyes, vibrant colors",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("动漫", "默认"),
                previewDescription = "标准动漫风格，高质量设置"
            ),
            Template(
                id = "anime_cute",
                name = "可爱动漫",
                category = "动漫",
                positivePrompt = "masterpiece, best quality, 1girl, cute, kawaii, smile, blush, big eyes, pastel colors, soft lighting, detailed face, colorful",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("动漫", "可爱", "kawaii"),
                previewDescription = "可爱卡哇伊动漫女孩风格"
            ),
            Template(
                id = "anime_dark",
                name = "暗黑动漫",
                category = "动漫",
                positivePrompt = "masterpiece, best quality, dark theme, gothic, dramatic lighting, detailed shadows, anime style, mysterious atmosphere",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("动漫", "暗黑", "哥特"),
                previewDescription = "暗黑哥特动漫风格"
            ),
            Template(
                id = "realistic_portrait",
                name = "写实人像",
                category = "写实",
                positivePrompt = "masterpiece, best quality, photorealistic, ultra detailed, professional photography, studio lighting, 8k, raw photo, dslr",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("写实", "人像"),
                previewDescription = "照片级真实人像风格"
            ),
            Template(
                id = "realistic_landscape",
                name = "写实风景",
                category = "写实",
                positivePrompt = "masterpiece, best quality, photorealistic, landscape, nature, ultra detailed, 8k uhd, dslr, high quality, natural lighting, scenic",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("写实", "风景"),
                previewDescription = "照片级真实风景风格"
            ),
            Template(
                id = "watercolor",
                name = "水彩画",
                category = "水彩",
                positivePrompt = "masterpiece, best quality, watercolor painting, soft colors, artistic, flowing, dreamy, delicate brushstrokes, paper texture",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("水彩", "艺术"),
                previewDescription = "柔和水彩画风格"
            ),
            Template(
                id = "pixel_art",
                name = "像素艺术",
                category = "像素",
                positivePrompt = "masterpiece, pixel art, 16bit, retro game style, clean pixels, detailed sprite, vibrant colors, nostalgic",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("像素", "复古"),
                previewDescription = "复古像素游戏风格"
            ),
            Template(
                id = "oil_painting",
                name = "油画",
                category = "油画",
                positivePrompt = "masterpiece, best quality, oil painting, rich colors, thick brushstrokes, canvas texture, classical art, dramatic lighting",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("油画", "古典"),
                previewDescription = "古典油画风格"
            ),
            Template(
                id = "cyberpunk",
                name = "赛博朋克",
                category = "科幻",
                positivePrompt = "masterpiece, best quality, cyberpunk, neon lights, futuristic city, rain, dark atmosphere, high tech, blade runner style",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("赛博朋克", "科幻", "霓虹"),
                previewDescription = "霓虹灯赛博朋克风格"
            ),
            Template(
                id = "fantasy",
                name = "奇幻艺术",
                category = "奇幻",
                positivePrompt = "masterpiece, best quality, fantasy art, magical, ethereal, detailed, epic, dramatic lighting, mystical atmosphere",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("奇幻", "魔法"),
                previewDescription = "史诗奇幻艺术风格"
            ),
            Template(
                id = "chibi",
                name = "Q版",
                category = "动漫",
                positivePrompt = "masterpiece, best quality, chibi, super deformed, cute, small body, big head, kawaii, simple, adorable",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("动漫", "Q版", "可爱"),
                previewDescription = "可爱Q版超变形风格"
            ),
            Template(
                id = "ghibli",
                name = "吉卜力风格",
                category = "动漫",
                positivePrompt = "masterpiece, best quality, studio ghibli style, miyazaki, watercolor background, detailed nature, warm colors, peaceful, whimsical",
                negativePrompt = DEFAULT_NEGATIVE_PROMPT,
                tags = listOf("动漫", "吉卜力"),
                previewDescription = "吉卜力工作室风格"
            )
        )
    }
}
