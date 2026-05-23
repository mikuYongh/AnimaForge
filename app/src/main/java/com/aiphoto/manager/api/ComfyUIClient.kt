package com.aiphoto.manager.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class ComfyUIClient(private val context: Context) {

    private var baseUrl: String = "http://192.168.123.178:8188/"
    private var api: ComfyUIApi? = null
    private var ws: WebSocket? = null
    private val gson = Gson()

    // WebSocket 事件
    private val _wsEvent = MutableSharedFlow<Pair<String, JsonObject>>(replay = 16, extraBufferCapacity = 64)
    val wsEvents: SharedFlow<Pair<String, JsonObject>> = _wsEvent

    fun connectWebSocket(clientId: String) {
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws?clientId=$clientId"
        Log.d("ComfyUI_WS", "WS连接地址: $wsUrl")

        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(wsUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ComfyUI_WS", "WS已连接: ${response.code}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ComfyUI_WS", "WS收到: $text")
                try {
                    val obj = JsonParser.parseString(text).asJsonObject
                    val type = obj.get("type")?.asString ?: return
                    Log.d("ComfyUI_WS", "WS类型: $type")
                    _wsEvent.tryEmit(type to obj)
                } catch (e: Exception) {
                    Log.e("ComfyUI_WS", "WS解析失败: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ComfyUI_WS", "WS关闭中: code=$code reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ComfyUI_WS", "WS已关闭: code=$code reason=$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ComfyUI_WS", "WS错误: ${t.message}", t)
            }
        })
    }

    fun disconnectWebSocket() {
        ws?.close(1000, "client disconnect")
        ws = null
    }

    fun setServerUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        initApi()
    }

    private fun initApi() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ComfyUIApi::class.java)
    }

    suspend fun submitPrompt(
        positivePrompt: String,
        negativePrompt: String = "",
        width: Int = 896,
        height: Int = 1088,
        steps: Int = 30,
        cfgScale: Double = 5.5,
        seed: Long = -1,
        samplerName: String = "multistep",
        scheduler: String = "beta57",
        ksamplerName: String = "euler_ancestral",
        kscheduler: String = "normal",
        customWorkflow: String? = null,
        denoise: Double = 1.0,
        inputImageFilename: String? = null,
        useWorkflowDimensions: Boolean = false,
        artistPrompt: String = "",
        clientId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val actualSeed = if (seed == -1L) System.currentTimeMillis() else seed

            // 拼接画师到正向提示词
            val fullPositivePrompt = if (artistPrompt.isNotBlank()) {
                val trimmed = positivePrompt.trimEnd(',', ' ')
                if (trimmed.isNotBlank()) "$trimmed, $artistPrompt" else artistPrompt
            } else {
                positivePrompt
            }

            val workflowJson = if (customWorkflow != null) {
                val workflowObj = JsonParser.parseString(customWorkflow).asJsonObject
                smartReplaceWorkflowParams(workflowObj, fullPositivePrompt, negativePrompt, actualSeed, steps, cfgScale, width, height, samplerName, scheduler, ksamplerName, kscheduler, denoise, inputImageFilename, useWorkflowDimensions)
                gson.toJson(workflowObj)
            } else {
                createDefaultWorkflow(fullPositivePrompt, negativePrompt, actualSeed, width, height, steps, cfgScale, samplerName, scheduler)
            }

            Log.d("ComfyUIClient", "发送的工作流: $workflowJson")

            // 构建请求 JSON
            val cid = clientId ?: UUID.randomUUID().toString()
            val requestJson = """{"prompt":$workflowJson,"client_id":"$cid"}"""

            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            val response = api!!.queuePrompt(requestBody)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!.string()
                val promptId = JsonParser.parseString(responseBody).asJsonObject.get("prompt_id")?.asString
                if (promptId != null) {
                    Result.success(promptId)
                } else {
                    Result.failure(Exception("无法获取 prompt_id"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ComfyUIClient", "提交失败: ${response.code()}, 错误: $errorBody")
                Result.failure(Exception("提交失败: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ComfyUIClient", "提交异常", e)
            Result.failure(e)
        }
    }

    /**
     * 智能替换工作流参数（使用 JsonObject 保持数字类型）
     */
    private fun smartReplaceWorkflowParams(
        workflow: JsonObject,
        positivePrompt: String,
        negativePrompt: String,
        seed: Long,
        steps: Int,
        cfgScale: Double,
        width: Int = 512,
        height: Int = 512,
        samplerName: String = "multistep",
        scheduler: String = "beta57",
        ksamplerName: String = "euler_ancestral",
        kscheduler: String = "normal",
        denoise: Double = 1.0,
        inputImageName: String? = null,
        useWorkflowDimensions: Boolean = false
    ) {
        for ((nodeId, nodeElement) in workflow.entrySet()) {
            if (nodeElement.isJsonObject) {
                val nodeObj = nodeElement.asJsonObject
                val classType = nodeObj.get("class_type")?.asString ?: continue

                if (nodeObj.has("inputs") && nodeObj.get("inputs").isJsonObject) {
                    val inputs = nodeObj.getAsJsonObject("inputs")

                    when (classType) {
                        // Simple String 节点 - 替换正向提示词
                        "Simple String" -> {
                            inputs.addProperty("string", positivePrompt)
                        }

                        // CLIPTextEncode 节点 - 根据标题判断正向/负向
                        "CLIPTextEncode" -> {
                            val meta = nodeObj.getAsJsonObject("_meta")
                            val title = meta?.get("title")?.asString ?: ""

                            when {
                                title.contains("Positive", ignoreCase = true) -> {
                                    if (inputs.has("text") && inputs.get("text").isJsonPrimitive) {
                                        inputs.addProperty("text", positivePrompt)
                                    }
                                }
                                title.contains("Negative", ignoreCase = true) -> {
                                    inputs.addProperty("text", negativePrompt)
                                }
                            }
                        }

                        // ClownsharKSampler - 使用Clownshar专用采样器配置
                        "ClownsharKSampler_Beta" -> {
                            if (inputs.has("seed")) {
                                inputs.addProperty("seed", seed)
                            }
                            if (inputs.has("steps")) {
                                inputs.addProperty("steps", steps)
                            }
                            if (inputs.has("cfg")) {
                                inputs.addProperty("cfg", cfgScale)
                            }
                            if (inputs.has("sampler_name")) {
                                inputs.addProperty("sampler_name", samplerName)
                            }
                            if (inputs.has("scheduler")) {
                                inputs.addProperty("scheduler", scheduler)
                            }
                            if (inputImageName != null && inputs.has("denoise")) {
                                inputs.addProperty("denoise", denoise)
                            }
                            if (!useWorkflowDimensions) {
                                if (inputs.has("width")) {
                                    inputs.addProperty("width", width)
                                }
                                if (inputs.has("height")) {
                                    inputs.addProperty("height", height)
                                }
                            }
                        }

                        // KSampler - 标准采样器配置
                        "KSampler" -> {
                            if (inputs.has("seed")) {
                                inputs.addProperty("seed", seed)
                            }
                            if (inputs.has("steps")) {
                                inputs.addProperty("steps", steps)
                            }
                            if (inputs.has("cfg")) {
                                inputs.addProperty("cfg", cfgScale)
                            }
                            if (inputs.has("sampler_name")) {
                                inputs.addProperty("sampler_name", ksamplerName)
                            }
                            if (inputs.has("scheduler")) {
                                inputs.addProperty("scheduler", kscheduler)
                            }
                            if (inputImageName != null && inputs.has("denoise")) {
                                inputs.addProperty("denoise", denoise)
                            }
                        }

                        // KSamplerAdvanced - 高级采样器配置（noise_seed 替代 seed，无 denoise）
                        "KSamplerAdvanced" -> {
                            if (inputs.has("noise_seed")) {
                                inputs.addProperty("noise_seed", seed)
                            }
                            if (inputs.has("steps")) {
                                inputs.addProperty("steps", steps)
                            }
                            if (inputs.has("cfg")) {
                                inputs.addProperty("cfg", cfgScale)
                            }
                            if (inputs.has("sampler_name")) {
                                inputs.addProperty("sampler_name", ksamplerName)
                            }
                            if (inputs.has("scheduler")) {
                                inputs.addProperty("scheduler", kscheduler)
                            }
                            if (inputs.has("start_at_step")) {
                                inputs.addProperty("start_at_step", 0)
                            }
                            if (inputs.has("end_at_step")) {
                                inputs.addProperty("end_at_step", steps)
                            }
                            if (inputImageName != null && inputs.has("return_with_leftover_noise")) {
                                inputs.addProperty("return_with_leftover_noise", "disable")
                            }
                        }

                        // EmptyLatentImage - 替换尺寸
                        "EmptyLatentImage" -> {
                            if (!useWorkflowDimensions) {
                                if (inputs.has("width")) {
                                    inputs.addProperty("width", width)
                                }
                                if (inputs.has("height")) {
                                    inputs.addProperty("height", height)
                                }
                            }
                        }

                        // SDXLEmptyLatentSizePicker+ - 替换尺寸
                        "SDXLEmptyLatentSizePicker+" -> {
                            if (!useWorkflowDimensions) {
                                if (inputs.has("width_override")) {
                                    inputs.addProperty("width_override", width)
                                }
                                if (inputs.has("height_override")) {
                                    inputs.addProperty("height_override", height)
                                }
                            }
                        }

                        // LoadImage - 图生图输入图片
                        "LoadImage" -> {
                            if (inputImageName != null) {
                                inputs.addProperty("image", inputImageName)
                            }
                        }

                        // image_scale_pixel_v2 - 图片缩放节点
                        "image_scale_pixel_v2" -> {
                            if (!useWorkflowDimensions) {
                                if (inputs.has("width")) {
                                    inputs.addProperty("width", width)
                                }
                                if (inputs.has("height")) {
                                    inputs.addProperty("height", height)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createDefaultWorkflow(
        positivePrompt: String,
        negativePrompt: String,
        seed: Long,
        width: Int,
        height: Int,
        steps: Int,
        cfgScale: Double,
        samplerName: String = "multistep",
        scheduler: String = "beta57"
    ): String {
        return """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {
                    "seed": $seed,
                    "steps": $steps,
                    "cfg": $cfgScale,
                    "sampler_name": "$samplerName",
                    "scheduler": "$scheduler",
                    "denoise": 1.0,
                    "model": ["4", 0],
                    "positive": ["6", 0],
                    "negative": ["7", 0],
                    “bongmath”: false,
                    "latent_image": ["5", 0]
                }
            },
            "4": {
                "class_type": "CheckpointLoaderSimple",
                "inputs": {
                    "ckpt_name": "novaAnimeXL_ilV180.safetensors"
                }
            },
            "5": {
                "class_type": "EmptyLatentImage",
                "inputs": {
                    "width": $width,
                    "height": $height,
                    "batch_size": 1
                }
            },
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {
                    "text": "$positivePrompt",
                    "clip": ["4", 1]
                }
            },
            "7": {
                "class_type": "CLIPTextEncode",
                "inputs": {
                    "text": "$negativePrompt",
                    "clip": ["4", 1]
                }
            },
            "8": {
                "class_type": "VAEDecode",
                "inputs": {
                    "samples": ["3", 0],
                    "vae": ["4", 2]
                }
            },
            "9": {
                "class_type": "SaveImage",
                "inputs": {
                    "filename_prefix": "ComfyUI",
                    "images": ["8", 0]
                }
            }
        }
        """.trimIndent()
    }

    suspend fun waitForImage(promptId: String, maxWaitMs: Long = 120000): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                val historyResponse = api!!.getHistory(promptId)

                if (historyResponse.isSuccessful && historyResponse.body() != null) {
                    val history = historyResponse.body()!![promptId]
                    if (history?.outputs != null) {
                        val images = mutableListOf<File>()

                        for ((_, output) in history.outputs) {
                            output.images?.forEach { imageInfo ->
                                val imageFile = downloadImage(imageInfo)
                                if (imageFile != null) {
                                    images.add(imageFile)
                                }
                            }
                        }

                        if (images.isNotEmpty()) {
                            return@withContext Result.success(images)
                        }
                    }
                }

                Thread.sleep(1000)
            }

            Result.failure(Exception("等待超时"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadImage(imageInfo: ImageInfo): File? = withContext(Dispatchers.IO) {
        try {
            val response = api!!.getImage(
                filename = imageInfo.filename,
                subfolder = imageInfo.subfolder,
                type = imageInfo.type
            )

            if (response.isSuccessful && response.body() != null) {
                val imagesDir = File(context.filesDir, "prompt_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val imageFile = File(imagesDir, "${UUID.randomUUID()}.png")
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(imageFile).use { output ->
                        input.copyTo(output)
                    }
                }
                imageFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检测工作流类型
     * 通过检查是否包含LoadImage节点来判断
     */
    fun detectWorkflowType(workflowJson: String): String {
        return try {
            val workflowObj = JsonParser.parseString(workflowJson).asJsonObject

            for ((_, nodeElement) in workflowObj.entrySet()) {
                if (nodeElement.isJsonObject) {
                    val nodeObj = nodeElement.asJsonObject
                    val classType = nodeObj.get("class_type")?.asString ?: continue

                    if (classType == "LoadImage") {
                        return "img2img"
                    }
                }
            }

            "text2img"
        } catch (e: Exception) {
            "text2img"
        }
    }

    /**
     * 上传图片到ComfyUI服务器
     * @param imageUri 图片URI
     * @return 上传后的文件名（服务器返回）
     */
    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("无法打开图片"))

            val tempFile = File.createTempFile("upload_", ".png", context.cacheDir)
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val requestBody = RequestBody.create(
                "image/*".toMediaType(),
                tempFile
            )
            val multipartBody = MultipartBody.Part.createFormData(
                "image",
                tempFile.name,
                requestBody
            )

            val response = api!!.uploadImage(multipartBody)

            tempFile.delete()

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val filename = responseBody["name"]
                if (filename != null) {
                    Result.success(filename)
                } else {
                    Result.failure(Exception("上传失败：未返回文件名"))
                }
            } else {
                Result.failure(Exception("上传失败：${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ComfyUIClient", "上传图片失败", e)
            Result.failure(e)
        }
    }

    init {
        initApi()
    }

    suspend fun getHistory(maxItems: Int = 20): JsonObject = withContext(Dispatchers.IO) {
        try {
            Log.d("ComfyUIClient", "获取历史记录，maxItems: $maxItems")
            val response = api!!.getHistory(maxItems = maxItems)
            Log.d("ComfyUIClient", "响应码: ${response.code()}, 成功: ${response.isSuccessful}")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("ComfyUIClient", "历史记录条目数: ${body.size()}")
                body
            } else {
                Log.e("ComfyUIClient", "获取失败: ${response.errorBody()?.string()}")
                JsonObject()
            }
        } catch (e: Exception) {
            Log.e("ComfyUIClient", "获取历史记录失败", e)
            JsonObject()
        }
    }

    suspend fun getQueueStatus(): JsonObject = withContext(Dispatchers.IO) {
        try {
            val response = api!!.getQueue()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                JsonObject()
            }
        } catch (e: Exception) {
            Log.e("ComfyUIClient", "获取队列状态失败", e)
            JsonObject()
        }
    }

    suspend fun interrupt(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api!!.interrupt()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ComfyUIClient", "中断生成失败", e)
            false
        }
    }

    suspend fun submitRawWorkflow(workflowJson: String, clientId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cid = clientId ?: UUID.randomUUID().toString()
            val requestJson = """{"prompt":$workflowJson,"client_id":"$cid"}"""
            Log.d("ComfyUIClient", "提交 Raw 工作流: $requestJson")
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())
            val response = api!!.queuePrompt(requestBody)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!.string()
                val promptId = JsonParser.parseString(body).asJsonObject.get("prompt_id")?.asString
                if (promptId != null) Result.success(promptId)
                else Result.failure(Exception("无法获取 prompt_id"))
            } else {
                Result.failure(Exception("提交失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistoryById(promptId: String): Result<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val response = api!!.getHistoryRaw(promptId)
            if (response.isSuccessful && response.body() != null) {
                val raw = response.body()!!.string()
                val root = com.google.gson.JsonParser.parseString(raw).asJsonObject
                val entry = root[promptId]?.asJsonObject
                if (entry != null) {
                    Result.success(entry)
                } else {
                    Result.failure(Exception("未找到: $promptId"))
                }
            } else {
                Result.failure(Exception("查询失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(filename: String, subfolder: String, type: String, outputType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api!!.getImage(filename = filename, subfolder = subfolder, type = type)
            if (response.isSuccessful && response.body() != null) {
                val dir = when (outputType) {
                    "video" -> File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "AnimaForge")
                    else -> File(context.filesDir, outputType)
                }
                if (!dir.exists()) dir.mkdirs()
                val ext = filename.substringAfterLast('.', "png")
                val uniqueName = "AnimaForge_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}_${java.util.UUID.randomUUID().toString().take(8)}.$ext"
                val file = File(dir, uniqueName)
                response.body()!!.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                Log.d("ComfyUIClient", "下载完成: ${file.absolutePath}")
                Result.success(file.absolutePath)
            } else {
                Result.failure(Exception("下载失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
