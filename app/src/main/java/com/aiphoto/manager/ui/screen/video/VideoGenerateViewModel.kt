package com.aiphoto.manager.ui.screen.video

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.api.ComfyUIClient
import com.aiphoto.manager.data.SettingsManager
import com.aiphoto.manager.data.local.entity.WorkflowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoGenerateViewModel(application: Application) : AndroidViewModel(application) {

    private val workflowDao = (application as App).database.workflowDao()
    private val settingsManager = SettingsManager(application.applicationContext)
    private val comfyUIClient = ComfyUIClient(application.applicationContext)
    private val prefs = application.getSharedPreferences("video_generate", Context.MODE_PRIVATE)

    var lastFps: Int
        get() = prefs.getInt("last_fps", 25); set(v) { prefs.edit().putInt("last_fps", v).apply() }
    var lastDuration: Int
        get() = prefs.getInt("last_duration", 5); set(v) { prefs.edit().putInt("last_duration", v).apply() }
    var lastWidth: Int
        get() = prefs.getInt("last_width", 960); set(v) { prefs.edit().putInt("last_width", v).apply() }
    var lastHeight: Int
        get() = prefs.getInt("last_height", 544); set(v) { prefs.edit().putInt("last_height", v).apply() }
    var lastPrompt: String
        get() = prefs.getString("last_prompt", "") ?: ""; set(v) { prefs.edit().putString("last_prompt", v).apply() }

    val workflows: StateFlow<List<WorkflowEntity>> =
        workflowDao.getWorkflowsByType("img2video")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _generatedVideos = MutableStateFlow<List<String>>(emptyList())
    val generatedVideos: StateFlow<List<String>> = _generatedVideos
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress
    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent
    private val _estimatedTime = MutableStateFlow<String?>(null)
    val estimatedTime: StateFlow<String?> = _estimatedTime
    private var generationJob: kotlinx.coroutines.Job? = null

    fun stopGeneration() {
        Log.d("VideoGen", "=== stopGeneration 被调用 ===")
        Log.d("VideoGen", "job 状态: isActive=${generationJob?.isActive}, isCancelled=${generationJob?.isCancelled}")
        generationJob?.cancel()
        Log.d("VideoGen", "cancel 后: isActive=${generationJob?.isActive}, isCancelled=${generationJob?.isCancelled}")
        viewModelScope.launch { comfyUIClient.interrupt() }
        comfyUIClient.disconnectWebSocket()
        _isGenerating.value = false; _progress.value = ""
    }

    fun clearResults() {
        if (!_isGenerating.value) {
            _generatedVideos.value = emptyList()
            _progressPercent.value = 0f
            _estimatedTime.value = ""
            _progress.value = ""
        }
    }

    override fun onCleared() { super.onCleared(); comfyUIClient.disconnectWebSocket() }

    fun startGeneration(imageUri: Uri, prompt: String, workflowId: String, fps: Int, duration: Int, width: Int, height: Int, batchCount: Int = 1, onError: (String) -> Unit) {
        lastFps = fps; lastDuration = duration; lastWidth = width; lastHeight = height; lastPrompt = prompt
        _isGenerating.value = true; _progress.value = "正在准备..."; _progressPercent.value = 0f; _generatedVideos.value = emptyList()
        val ctx = getApplication<Application>().applicationContext

        generationJob = viewModelScope.launch {
            try {
                val url = settingsManager.comfyUiVideoUrl.first()
                comfyUIClient.setServerUrl(url)
                Log.d("VideoGen", "使用视频服务器: $url")
                val wf = workflowDao.getWorkflowById(workflowId)?.workflowJson ?: run { onError("工作流不存在"); _isGenerating.value = false; return@launch }

                val dir = File(ctx.filesDir, "video_images"); if (!dir.exists()) dir.mkdirs()
                ctx.contentResolver.openInputStream(imageUri)?.use { inp -> File(dir, "input_${System.currentTimeMillis()}.png").outputStream().use { inp.copyTo(it) } }
                _progress.value = "正在上传图片..."
                val up = withContext(Dispatchers.IO) { comfyUIClient.uploadImage(imageUri) }
                if (up.isFailure) { onError("上传失败"); _isGenerating.value = false; return@launch }
                val imgFn = up.getOrThrow()

                val videos = mutableListOf<String>()
                val clientId = "video_${System.currentTimeMillis()}"
                comfyUIClient.connectWebSocket(clientId)
                try {
                    for (i in 1..batchCount) {
                        if (!isActive) break
                        _progress.value = "提交 $i/$batchCount..."
                        val injected = injectVideoWorkflow(wf, imgFn, if (i == 1) prompt else "$prompt #$i", fps, duration, width, height)
                        val pid = withContext(Dispatchers.IO) { comfyUIClient.submitRawWorkflow(injected, clientId) }
                        if (pid.isFailure) continue
                        val targetPid = pid.getOrThrow()

                        val startTime = System.currentTimeMillis()
                        var lastProgressTime = startTime; var lastProgressValue = 0; var stepAvgMs = 0L; var found = false

                        var wsJob: kotlinx.coroutines.Job? = null
                        wsJob = launch {
                            comfyUIClient.wsEvents.collect { (type, data) ->
                                val p = data.getAsJsonObject("data")?.get("prompt_id")?.asString ?: ""
                                if (p != targetPid) return@collect
                                when (type) {
                                    "progress" -> {
                                        val v = data.getAsJsonObject("data")?.get("value")?.asInt ?: 0
                                        val m = data.getAsJsonObject("data")?.get("max")?.asInt ?: 1
                                        val node = data.getAsJsonObject("data")?.get("node")?.asString ?: ""
                                        val now = System.currentTimeMillis()
                                        val deltaV = v - lastProgressValue; val deltaT = now - lastProgressTime
                                        if (deltaV > 0 && deltaT > 0 && lastProgressValue > 0) {
                                            val curMs = deltaT / deltaV
                                            stepAvgMs = if (stepAvgMs == 0L) curMs else (stepAvgMs * 3 + curMs) / 4
                                        }
                                        val newPct = v.toFloat() / m
                                        // 进度百分比只增不减（不同节点的 max 不同，取最优估计）
                                        if (newPct > _progressPercent.value) _progressPercent.value = newPct
                                        _progress.value = "生成 $i/$batchCount: ${node.replaceFirst("320:", "")} $v/$m"
                                        val remain = if (stepAvgMs > 0) ((m - v) * stepAvgMs) / 1000 else 0
                                        if (remain > 0) _estimatedTime.value = "预计剩余 ${remain}s"
                                        Log.d("VideoGen", "Progress更新: node=$node $v/$m stepAvg=${stepAvgMs}ms remain=${remain}s")
                                        lastProgressTime = now; lastProgressValue = v
                                    }
                                    "execution_success" -> {
                                        Log.d("VideoGen", "WS execution_success!"); found = true; wsJob?.cancel()
                                    }
                                }
                            }
                        }

                        while (isActive && !found) {
                            if (!isActive) { Log.d("VideoGen", "while内 isActive=false break"); break }
                            kotlinx.coroutines.delay(1000)
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000
                            // WS 没数据时才显示运行时间，不覆盖 WS 的真实 ETA 和进度
                            if (_progressPercent.value <= 0f) _estimatedTime.value = "${elapsed}s"
                            if (_progress.value == "提交 $i/$batchCount..." || _progress.value.startsWith("正在生成...")) _progress.value = "正在生成...${elapsed}s"
                            if (elapsed % 5 == 0L) Log.d("VideoGen", "等待中: elapsed=${elapsed}s isActive=$isActive")
                            if (elapsed > 10) {
                                val hist = withContext(Dispatchers.IO) { comfyUIClient.getHistoryById(targetPid) }
                                if (hist.isSuccess && hist.getOrNull() != null) {
                                    val obj = hist.getOrNull()!!
                                    val outs = obj.getAsJsonObject("outputs")
                                    if (outs != null) for ((_, nv) in outs.entrySet()) {
                                        val arr = nv.asJsonObject.getAsJsonArray("images") ?: nv.asJsonObject.getAsJsonArray("gif") ?: nv.asJsonObject.getAsJsonArray("video") ?: continue
                                        if (arr.size() > 0) {
                                            val info = arr[0].asJsonObject
                                            _progress.value = "下载视频 $i/$batchCount..."
                                            val dl = withContext(Dispatchers.IO) { comfyUIClient.downloadFile(info.get("filename").asString, info.get("subfolder")?.asString ?: "", info.get("type")?.asString ?: "output", "video") }
                                            if (dl.isSuccess) {
                                                videos.add(dl.getOrThrow())
                                                _generatedVideos.value = videos.toList()
                                                found = true
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        wsJob?.cancel()

                        // 退出等待循环后，如果还没下载成功该视频，做一次最后的拉取和下载重试
                        if (isActive && videos.size < i) {
                            _progress.value = "获取生成结果 $i/$batchCount..."
                            var dlSuccess = false
                            for (retry in 1..3) {
                                if (!isActive) break
                                val hist = withContext(Dispatchers.IO) { comfyUIClient.getHistoryById(targetPid) }
                                if (hist.isSuccess && hist.getOrNull() != null) {
                                    val obj = hist.getOrNull()!!
                                    val outs = obj.getAsJsonObject("outputs")
                                    if (outs != null) {
                                        for ((_, nv) in outs.entrySet()) {
                                            val arr = nv.asJsonObject.getAsJsonArray("images") ?: nv.asJsonObject.getAsJsonArray("gif") ?: nv.asJsonObject.getAsJsonArray("video") ?: continue
                                            if (arr.size() > 0) {
                                                val info = arr[0].asJsonObject
                                                _progress.value = "下载视频 $i/$batchCount..."
                                                val dl = withContext(Dispatchers.IO) { comfyUIClient.downloadFile(info.get("filename").asString, info.get("subfolder")?.asString ?: "", info.get("type")?.asString ?: "output", "video") }
                                                if (dl.isSuccess) {
                                                    videos.add(dl.getOrThrow())
                                                    _generatedVideos.value = videos.toList()
                                                    dlSuccess = true
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                                if (dlSuccess) break
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                    }
                } finally { comfyUIClient.disconnectWebSocket() }

                _generatedVideos.value = videos; _progress.value = ""; _isGenerating.value = false
            } catch (e: kotlinx.coroutines.CancellationException) { _isGenerating.value = false; _progress.value = "" }
            catch (e: Exception) { _isGenerating.value = false; onError("生成失败: ${e.message}") }
        }
    }

    private fun injectVideoWorkflow(json: String, imageFilename: String, prompt: String, fps: Int, duration: Int, width: Int, height: Int): String {
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        for ((_, nodeEl) in root.entrySet()) {
            val node = nodeEl.asJsonObject
            val classType = node.get("class_type")?.asString ?: continue
            if (!node.has("inputs") || !node.get("inputs").isJsonObject) continue
            val inputs = node.getAsJsonObject("inputs")
            val title = node.getAsJsonObject("_meta")?.get("title")?.asString ?: ""
            when {
                classType == "LoadImage" && inputs.has("image") -> inputs.addProperty("image", imageFilename)
                classType == "Simple String" && title == "Simple String" && inputs.has("string") && prompt.isNotBlank() -> inputs.addProperty("string", prompt)
                classType == "PrimitiveInt" && inputs.get("value")?.isJsonPrimitive == true -> when (title) { "Width" -> inputs.addProperty("value", width); "Height" -> inputs.addProperty("value", height); "Frame Rate" -> inputs.addProperty("value", fps); "Duration" -> inputs.addProperty("value", duration) }
            }
        }
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
    }
}
