package com.aiphoto.manager.util

import android.content.Context
import android.util.Log
import com.aiphoto.manager.api.ComfyUIClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ComfyUIHistory(
    @SerializedName("prompt") val prompt: List<Any>,
    @SerializedName("outputs") val outputs: JsonObject,
    @SerializedName("status") val status: Status
)

data class Status(
    @SerializedName("status_str") val statusStr: String,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("messages") val messages: List<List<Any>>
)

data class WorkflowInfo(
    val promptId: String,
    val createTime: Long,
    val positivePrompt: String?,
    val negativePrompt: String?,
    val sampler: String?,
    val scheduler: String?,
    val steps: Int?,
    val cfg: Double?,
    val seed: Long?,
    val width: Int?,
    val height: Int?,
    val modelName: String?,
    val outputImages: List<String>,
    val status: String,
    val completed: Boolean
)

object ComfyUILogAnalyzer {

    private const val TAG = "ComfyUILogAnalyzer"
    private val gson = Gson()

    suspend fun getHistory(
        context: Context,
        comfyUiUrl: String,
        maxItems: Int = 20
    ): Map<String, WorkflowInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始获取历史记录，URL: $comfyUiUrl")
            val client = ComfyUIClient(context)
            client.setServerUrl(comfyUiUrl)
            val response = client.getHistory(maxItems)

            Log.d(TAG, "获取到历史记录，条目数: ${response.size()}")
            val historyMap = mutableMapOf<String, WorkflowInfo>()

            response.entrySet().forEach { entry ->
                val promptId = entry.key
                val historyData = entry.value.asJsonObject

                try {
                    val workflowInfo = parseHistoryEntry(promptId, historyData)
                    historyMap[promptId] = workflowInfo
                    Log.d(TAG, "成功解析历史记录: $promptId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing history entry $promptId", e)
                }
            }

            Log.d(TAG, "最终历史记录数: ${historyMap.size}")
            historyMap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history", e)
            emptyMap()
        }
    }

    private fun parseHistoryEntry(promptId: String, historyData: JsonObject): WorkflowInfo {
        val promptArray = historyData.getAsJsonArray("prompt")
        val workflowJson = promptArray[2].asJsonObject

        // 提取正向提示词
        var positivePrompt: String? = null
        var negativePrompt: String? = null
        var sampler: String? = null
        var scheduler: String? = null
        var steps: Int? = null
        var cfg: Double? = null
        var seed: Long? = null
        var width: Int? = null
        var height: Int? = null
        var modelName: String? = null

        // 遍历所有节点
        workflowJson.entrySet().forEach { nodeEntry ->
            val nodeId = nodeEntry.key
            val node = nodeEntry.value.asJsonObject
            val classType = node.get("class_type")?.asString

            when (classType) {
                "CLIPTextEncode" -> {
                    val inputs = node.getAsJsonObject("inputs")
                    val textElement = inputs.get("text")

                    // text 可能是字符串，也可能是数组引用（如 ["38", 0]）
                    val text = if (textElement?.isJsonPrimitive == true) {
                        textElement.asString
                    } else {
                        null // 数组引用，跳过
                    }

                    val title = node.getAsJsonObject("_meta")?.get("title")?.asString

                    if (title?.contains("Positive", ignoreCase = true) == true && text != null) {
                        positivePrompt = text
                    } else if (title?.contains("Negative", ignoreCase = true) == true && text != null) {
                        negativePrompt = text
                    }
                }

                "Simple String" -> {
                    val inputs = node.getAsJsonObject("inputs")
                    val stringElement = inputs.get("string")
                    val string = if (stringElement?.isJsonPrimitive == true) {
                        stringElement.asString
                    } else {
                        null
                    }
                    if (positivePrompt == null && string != null) {
                        positivePrompt = string
                    }
                }

                "ClownsharKSampler_Beta", "KSampler", "KSamplerAdvanced" -> {
                    val inputs = node.getAsJsonObject("inputs")
                    sampler = inputs.get("sampler_name")?.asString
                    scheduler = inputs.get("scheduler")?.asString
                    steps = inputs.get("steps")?.asInt
                    cfg = inputs.get("cfg")?.asDouble
                    seed = inputs.get("seed")?.asLong
                }

                "SDXLEmptyLatentSizePicker+", "EmptyLatentImage" -> {
                    val inputs = node.getAsJsonObject("inputs")
                    width = inputs.get("width")?.asInt
                    height = inputs.get("height")?.asInt

                    // 尝试从 resolution 字段解析
                    val resolution = inputs.get("resolution")?.asString
                    if (resolution != null && width == null) {
                        val match = Regex("(\\d+)x(\\d+)").find(resolution)
                        if (match != null) {
                            width = match.groupValues[1].toIntOrNull()
                            height = match.groupValues[2].toIntOrNull()
                        }
                    }
                }

                "UNETLoader", "CheckpointLoaderSimple" -> {
                    val inputs = node.getAsJsonObject("inputs")
                    modelName = inputs.get("unet_name")?.asString
                        ?: inputs.get("ckpt_name")?.asString
                }
            }
        }

        // 提取输出图片
        val outputImages = mutableListOf<String>()
        val outputs = historyData.getAsJsonObject("outputs")
        outputs.entrySet().forEach { outputEntry ->
            val outputNode = outputEntry.value.asJsonObject
            val images = outputNode.getAsJsonArray("images")
            images?.forEach { imageElement ->
                val imageObj = imageElement.asJsonObject
                val filename = imageObj.get("filename")?.asString
                if (filename != null) {
                    outputImages.add(filename)
                }
            }
        }

        // 提取状态信息
        val statusObj = historyData.getAsJsonObject("status")
        val statusStr = statusObj.get("status_str")?.asString ?: "unknown"
        val completed = statusObj.get("completed")?.asBoolean ?: false

        // 提取创建时间 - prompt 数组结构: [数字, promptId字符串, 工作流对象, 元数据对象, 输出节点数组]
        val createTime = if (promptArray.size() > 3 && promptArray[3].isJsonObject) {
            val promptMeta = promptArray[3].asJsonObject
            promptMeta.get("create_time")?.asLong ?: 0L
        } else {
            0L
        }

        return WorkflowInfo(
            promptId = promptId,
            createTime = createTime,
            positivePrompt = positivePrompt,
            negativePrompt = negativePrompt,
            sampler = sampler,
            scheduler = scheduler,
            steps = steps,
            cfg = cfg,
            seed = seed,
            width = width,
            height = height,
            modelName = modelName,
            outputImages = outputImages,
            status = statusStr,
            completed = completed
        )
    }

    fun formatWorkflowInfo(info: WorkflowInfo): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流信息 ===")
        sb.appendLine("ID: ${info.promptId}")
        sb.appendLine("状态: ${info.status} ${if (info.completed) "✓" else "⏳"}")
        sb.appendLine("创建时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(info.createTime))}")

        info.modelName?.let { sb.appendLine("模型: $it") }
        info.positivePrompt?.let { sb.appendLine("\n正向提示词:\n$it") }
        info.negativePrompt?.let { sb.appendLine("\n负向提示词:\n$it") }

        sb.appendLine("\n采样参数:")
        info.sampler?.let { sb.appendLine("  采样器: $it") }
        info.scheduler?.let { sb.appendLine("  调度器: $it") }
        info.steps?.let { sb.appendLine("  步数: $it") }
        info.cfg?.let { sb.appendLine("  CFG: $it") }
        info.seed?.let { sb.appendLine("  种子: $it") }

        info.width?.let { w ->
            info.height?.let { h ->
                sb.appendLine("  尺寸: ${w}x${h}")
            }
        }

        if (info.outputImages.isNotEmpty()) {
            sb.appendLine("\n输出图片:")
            info.outputImages.forEach { sb.appendLine("  - $it") }
        }

        return sb.toString()
    }
}
