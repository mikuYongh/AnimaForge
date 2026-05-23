package com.aiphoto.manager.ui.screen.generate

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.api.ComfyUIClient
import com.aiphoto.manager.data.SettingsManager
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.service.ImageGenerationService
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class GenerateViewModel(application: Application) : AndroidViewModel(application) {

    private val workflowDao = (application as App).database.workflowDao()
    private val generatedImageDao = (application as App).database.generatedImageDao()
    private val promptDao = (application as App).database.promptDao()
    private val settingsManager = SettingsManager(application.applicationContext)
    private val comfyUIClient = ComfyUIClient(application.applicationContext)
    private val sharedPreferences = application.getSharedPreferences("generate_settings", Context.MODE_PRIVATE)

    val workflows: StateFlow<List<com.aiphoto.manager.data.local.entity.WorkflowEntity>> =
        workflowDao.getAllWorkflows()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress

    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent

    private val _generatedImages = MutableStateFlow<List<String>>(emptyList())
    val generatedImages: StateFlow<List<String>> = _generatedImages

    private val _estimatedTime = MutableStateFlow<String?>(null)
    val estimatedTime: StateFlow<String?> = _estimatedTime

    private val _queuePosition = MutableStateFlow<Int?>(null)
    val queuePosition: StateFlow<Int?> = _queuePosition

    // 收藏的提示词
    private val _favoritePositivePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoritePositivePrompts: StateFlow<List<String>> = _favoritePositivePrompts

    private val _favoriteNegativePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoriteNegativePrompts: StateFlow<List<String>> = _favoriteNegativePrompts

    // 基于历史数据动态调整估算时间
    private var currentPromptId: String? = null
    private var generationService: ImageGenerationService? = null
    private var isBound = false

    // Sampler 和 Scheduler 选项
    val samplerOptions = listOf(
        "multistep/res_3m" to "multistep/res_3m (推荐)",
        "multistep/res_2m" to "multistep/res_2m",
        "multistep/dpmpp_2m" to "multistep/dpmpp_2m",
        "multistep/dpmpp_3m" to "multistep/dpmpp_3m",
        "multistep/abnorsett_3m" to "multistep/abnorsett_3m",
        "multistep/abnorsett_4m" to "multistep/abnorsett_4m",
        "linear/euler" to  "linear/euler",
        )

    val schedulerOptions = listOf(
        "beta57" to "beta57 (推荐)",
        "normal" to "normal",
        "karras" to "karras",
        "exponential" to "exponential",
        "sgm_uniform" to "sgm_uniform",
        "simple" to "simple",
        "ddim_uniform" to "ddim_uniform",
        "kl_optimal" to "kl_optimal",
        "er_sde" to "er_sde",
        "euler_a" to "euler_a",
        "dpmpp_2m_sde_gpu" to "dpmpp_2m_sde_gpu",
    )

    // KSampler 专用采样器选项（标准采样器）
    val ksamplerOptions = listOf(
        "euler_ancestral" to "euler_ancestral (推荐)",
        "euler" to "euler",
        "dpmpp_2m" to "dpmpp_2m",
        "dpmpp_2m_sde" to "dpmpp_2m_sde",
        "dpmpp_3m_sde" to "dpmpp_3m_sde",
        "ddim" to "ddim",
        "uni_pc" to "uni_pc",
        "uni_pc_bh2" to "uni_pc_bh2",
        "lms" to "lms",
        "heun" to "heun",
        "dpm_2" to "dpm_2",
        "dpm_2_ancestral" to "dpm_2_ancestral",
        "er_sde" to "er_sde",
        "dpmpp_2m_sde_gpu" to "dpmpp_2m_sde_gpu",
    )

    // KSampler 专用调度器选项
    val kschedulerOptions = listOf(
        "normal" to "normal (推荐)",
        "karras" to "karras",
        "exponential" to "exponential",
        "sgm_uniform" to "sgm_uniform",
        "simple" to "simple",
        "ddim_uniform" to "ddim_uniform",
        "beta" to "beta",
        "beta57" to "beta57",
    )

    private val _selectedSampler = MutableStateFlow(getLastUsedSampler())
    val selectedSampler: StateFlow<String> = _selectedSampler

    private val _selectedScheduler = MutableStateFlow(getLastUsedScheduler())
    val selectedScheduler: StateFlow<String> = _selectedScheduler

    private val _selectedKSampler = MutableStateFlow(getLastUsedKSampler())
    val selectedKSampler: StateFlow<String> = _selectedKSampler

    private val _selectedKScheduler = MutableStateFlow(getLastUsedKScheduler())
    val selectedKScheduler: StateFlow<String> = _selectedKScheduler

    private val _workflowType = MutableStateFlow("text2img")
    val workflowType: StateFlow<String> = _workflowType

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ImageGenerationService.LocalBinder
            generationService = binder.getService()
            isBound = true

            // 监听服务的状态
            viewModelScope.launch {
                generationService?.isGenerating?.collect { generating ->
                    _isGenerating.value = generating
                    if (!generating) {
                        _estimatedTime.value = null
                    }
                }
            }

            // 立即同步当前状态
            val currentServiceState = generationService?.isGenerating?.value ?: false
            _isGenerating.value = currentServiceState
            _generatedImages.value = generationService?.generatedImages?.value ?: emptyList()
            Log.d("GenerateViewModel", "【初始状态同步】服务状态: $currentServiceState, 图片数: ${_generatedImages.value.size}")

            viewModelScope.launch {
                generationService?.progress?.collect { progressText ->
                    _progress.value = progressText
                }
            }

            viewModelScope.launch {
                generationService?.progressPercent?.collect { pct ->
                    _progressPercent.value = pct
                }
            }

            viewModelScope.launch {
                generationService?.estimatedTime?.collect { eta ->
                    _estimatedTime.value = eta.ifEmpty { null }
                }
            }

            viewModelScope.launch {
                generationService?.generatedImages?.collect { images ->
                    _generatedImages.value = images
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            generationService = null
            isBound = false
        }
    }

    init {
        // 绑定服务
        val intent = Intent(application, ImageGenerationService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // 获取上次使用的工作流ID (按提示词项目绑定)
    fun getLastUsedWorkflowId(promptId: String): String? {
        return sharedPreferences.getString("workflow_$promptId", null)
    }

    // 保存工作流ID (按提示词项目绑定)
    private fun saveLastUsedWorkflowId(promptId: String, workflowId: String?) {
        sharedPreferences.edit().putString("workflow_$promptId", workflowId ?: "").apply()
    }

    // 获取上次使用的 sampler
    private fun getLastUsedSampler(): String {
        return sharedPreferences.getString("last_sampler", "multistep") ?: "multistep"
    }

    // 保存 sampler
    private fun saveLastUsedSampler(sampler: String) {
        sharedPreferences.edit().putString("last_sampler", sampler).apply()
    }

    // 获取上次使用的 scheduler
    private fun getLastUsedScheduler(): String {
        return sharedPreferences.getString("last_scheduler", "beta57") ?: "beta57"
    }

    // 保存 scheduler
    private fun saveLastUsedScheduler(scheduler: String) {
        sharedPreferences.edit().putString("last_scheduler", scheduler).apply()
    }

    // 设置选中的 sampler
    fun setSelectedSampler(sampler: String) {
        _selectedSampler.value = sampler
        saveLastUsedSampler(sampler)
    }

    // 设置选中的 scheduler
    fun setSelectedScheduler(scheduler: String) {
        _selectedScheduler.value = scheduler
        saveLastUsedScheduler(scheduler)
    }

    // 获取上次使用的 KSampler
    private fun getLastUsedKSampler(): String {
        return sharedPreferences.getString("last_ksampler", "euler_ancestral") ?: "euler_ancestral"
    }

    // 保存 KSampler
    private fun saveLastUsedKSampler(sampler: String) {
        sharedPreferences.edit().putString("last_ksampler", sampler).apply()
    }

    // 获取上次使用的 KScheduler
    private fun getLastUsedKScheduler(): String {
        return sharedPreferences.getString("last_kscheduler", "normal") ?: "normal"
    }

    // 保存 KScheduler
    private fun saveLastUsedKScheduler(scheduler: String) {
        sharedPreferences.edit().putString("last_kscheduler", scheduler).apply()
    }

    // ============================================================
    // SDXLEmptyLatentSizePicker+ 分辨率选项
    // ============================================================

    val resolutionOptions = listOf(
        "704x1408 (0.5)", "704x1344 (0.52)", "768x1344 (0.57)", "768x1280 (0.6)",
        "832x1216 (0.68)", "832x1152 (0.72)", "896x1152 (0.78)", "896x1088 (0.82)",
        "960x1088 (0.88)", "960x1024 (0.94)", "1024x1024 (1.0)", "1024x960 (1.07)",
        "1088x960 (1.13)", "1088x896 (1.21)", "1152x896 (1.29)", "1152x832 (1.38)",
        "1216x832 (1.46)", "1280x768 (1.67)", "1344x768 (1.75)", "1344x704 (1.91)",
        "1408x704 (2.0)", "1472x704 (2.09)", "1536x640 (2.4)", "1600x640 (2.5)",
        "1664x576 (2.89)", "1728x576 (3.0)"
    )

    private val _selectedResolution = MutableStateFlow("896x1088 (0.82)")
    val selectedResolution: StateFlow<String> = _selectedResolution

    private val _hasSizePicker = MutableStateFlow(false)
    val hasSizePicker: StateFlow<Boolean> = _hasSizePicker

    /**
     * 检测工作流是否含有 SDXLEmptyLatentSizePicker+ 节点
     */
    fun detectHasSizePicker(workflowJson: String?) {
        if (workflowJson == null) {
            _hasSizePicker.value = false
            return
        }
        try {
            val obj = JsonParser.parseString(workflowJson).asJsonObject
            val has = obj.entrySet().any {
                it.value.isJsonObject && it.value.asJsonObject.get("class_type")?.asString == "SDXLEmptyLatentSizePicker+"
            }
            _hasSizePicker.value = has
            if (has) {
                // 从工作流解析当前分辨率
                for ((_, node) in obj.entrySet()) {
                    if (node.isJsonObject && node.asJsonObject.get("class_type")?.asString == "SDXLEmptyLatentSizePicker+") {
                        val widgets = node.asJsonObject.getAsJsonArray("widgets_values")
                        if (widgets != null && widgets.size() > 0) {
                            val res = widgets[0].asString
                            if (res.isNotBlank()) _selectedResolution.value = res
                        }
                    }
                }
            }
        } catch (_: Exception) {
            _hasSizePicker.value = false
        }
    }

    fun setSelectedResolution(resolution: String) {
        _selectedResolution.value = resolution
    }

    /**
     * 从分辨率字符串解析宽高 (如 "896x1088 (0.82)" → 896, 1088)
     */
    fun parseResolution(resolution: String): Pair<Int, Int> {
        val parts = resolution.split("x")
        val w = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 896
        val h = parts.getOrNull(1)?.substringBefore(" ")?.trim()?.toIntOrNull() ?: 1088
        return Pair(w, h)
    }

    // ============================================================
    // 模型 / LoRA 配置状态
    // ============================================================

    private val _selectedBaseModel = MutableStateFlow<String?>(null)
    val selectedBaseModel: StateFlow<String?> = _selectedBaseModel

    // 可用模型列表（从 ComfyUI API 获取）
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels

    private val _availableModelsLoading = MutableStateFlow(false)
    val availableModelsLoading: StateFlow<Boolean> = _availableModelsLoading

    // 可用 LoRA 列表
    private val _availableLoras = MutableStateFlow<List<String>>(emptyList())
    val availableLoras: StateFlow<List<String>> = _availableLoras

    private val _availableLorasLoading = MutableStateFlow(false)
    val availableLorasLoading: StateFlow<Boolean> = _availableLorasLoading

    // 当前工作流中的 LoRA 配置列表
    private val _workflowLoraConfigs = MutableStateFlow<List<LoraConfigItem>>(emptyList())
    val workflowLoraConfigs: StateFlow<List<LoraConfigItem>> = _workflowLoraConfigs

    // 正在从工作流解析的 LoRA 条目
    data class LoraConfigItem(
        val name: String,
        val strength: Double = 1.0,
        val enabled: Boolean = false
    )

    /**
     * 从 ComfyUI API 加载可用模型和 LoRA 列表
     */
    fun loadAvailableModelsAndLoras() {
        val serverUrl = runBlocking { settingsManager.comfyUiUrl.first() }
        if (serverUrl.isBlank()) return

        comfyUIClient.setServerUrl(serverUrl)

        viewModelScope.launch {
            _availableModelsLoading.value = true
            val modelResult = comfyUIClient.getAvailableModels()
            _availableModelsLoading.value = false
            if (modelResult.isSuccess) {
                _availableModels.value = modelResult.getOrDefault(emptyList())
            }
        }

        viewModelScope.launch {
            _availableLorasLoading.value = true
            val loraResult = comfyUIClient.getAvailableLoras()
            _availableLorasLoading.value = false
            if (loraResult.isSuccess) {
                _availableLoras.value = loraResult.getOrDefault(emptyList())
            }
        }
    }

    /**
     * 从工作流 JSON 解析当前的 LoRA 列表
     */
    fun parseLoraFromWorkflow(workflowJson: String) {
        try {
            val workflowObj = JsonParser.parseString(workflowJson).asJsonObject
            val loras = mutableListOf<LoraConfigItem>()

            for ((_, nodeElement) in workflowObj.entrySet()) {
                if (nodeElement.isJsonObject) {
                    val nodeObj = nodeElement.asJsonObject
                    val classType = nodeObj.get("class_type")?.asString ?: continue
                    val inputs = if (nodeObj.has("inputs") && nodeObj.get("inputs").isJsonObject) {
                        nodeObj.getAsJsonObject("inputs")
                    } else null

                    when (classType) {
                        "UNETLoader" -> {
                            // API 格式：inputs.unet_name
                            if (inputs != null && inputs.has("unet_name")) {
                                val model = inputs.get("unet_name").asString
                                if (model.isNotBlank()) {
                                    _selectedBaseModel.value = model
                                }
                            } else {
                                // UI 格式回退：widgets_values[0]
                                val widgets = nodeObj.getAsJsonArray("widgets_values")
                                if (widgets != null && widgets.size() > 0) {
                                    val model = widgets[0].asString
                                    if (model.isNotBlank()) {
                                        _selectedBaseModel.value = model
                                    }
                                }
                            }
                        }
                        "Power Lora Loader (rgthree)" -> {
                            // API 格式：inputs 中的 lora_1, lora_2, ... 键
                            var foundInApi = false
                            if (inputs != null) {
                                val sortedKeys = inputs.keySet()
                                    .filter { it.startsWith("lora_") }
                                    .sortedBy { it.removePrefix("lora_").toIntOrNull() ?: Int.MAX_VALUE }
                                for (key in sortedKeys) {
                                    val loraObj = inputs.getAsJsonObject(key)
                                    if (loraObj != null && loraObj.has("lora")) {
                                        loras.add(
                                            LoraConfigItem(
                                                name = loraObj.get("lora").asString,
                                                strength = loraObj.get("strength")?.asDouble ?: 1.0,
                                                enabled = loraObj.get("on")?.asBoolean ?: false
                                            )
                                        )
                                        foundInApi = true
                                    }
                                }
                            }
                            // UI 格式回退：widgets_values 数组
                            if (!foundInApi) {
                                val widgets = nodeObj.getAsJsonArray("widgets_values")
                                if (widgets != null) {
                                    for (i in 0 until widgets.size()) {
                                        val item = widgets[i]
                                        if (item.isJsonObject) {
                                            val obj = item.asJsonObject
                                            if (obj.has("lora")) {
                                                loras.add(
                                                    LoraConfigItem(
                                                        name = obj.get("lora").asString,
                                                        strength = obj.get("strength")?.asDouble ?: 1.0,
                                                        enabled = obj.get("on")?.asBoolean ?: false
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            _workflowLoraConfigs.value = loras
            Log.d("GenerateVM", "解析工作流: 模型=${_selectedBaseModel.value}, LoRA=${loras.size}个")
        } catch (e: Exception) {
            Log.e("GenerateVM", "解析工作流 LoRA 失败", e)
        }
    }

    fun setBaseModel(model: String) {
        _selectedBaseModel.value = model
    }

    fun updateLoraConfig(index: Int, config: LoraConfigItem) {
        val list = _workflowLoraConfigs.value.toMutableList()
        if (index < list.size) list[index] = config
        _workflowLoraConfigs.value = list
    }

    fun addLora(name: String) {
        val list = _workflowLoraConfigs.value.toMutableList()
        if (list.none { it.name == name }) {
            list.add(LoraConfigItem(name = name, strength = 1.0, enabled = true))
            _workflowLoraConfigs.value = list
        }
    }

    fun removeLora(index: Int) {
        val list = _workflowLoraConfigs.value.toMutableList()
        if (index < list.size) {
            list.removeAt(index)
            _workflowLoraConfigs.value = list
        }
    }

    fun importLoraConfigsFromPrompt(prompt: PromptEntity) {
        if (prompt.baseModel != null) {
            _selectedBaseModel.value = prompt.baseModel
        }
        if (!prompt.loraConfigs.isNullOrBlank()) {
            try {
                val arr = JsonParser.parseString(prompt.loraConfigs).asJsonArray
                val list = arr.map {
                    val obj = it.asJsonObject
                    LoraConfigItem(
                        name = obj.get("name").asString,
                        strength = obj.get("strength")?.asDouble ?: 1.0,
                        enabled = obj.get("enabled")?.asBoolean ?: false
                    )
                }
                _workflowLoraConfigs.value = list
            } catch (_: Exception) { }
        }
    }

    fun exportLoraConfigsToJson(): String {
        val arr = com.google.gson.JsonArray()
        _workflowLoraConfigs.value.forEach { item ->
            val obj = com.google.gson.JsonObject()
            obj.addProperty("name", item.name)
            obj.addProperty("strength", item.strength)
            obj.addProperty("enabled", item.enabled)
            arr.add(obj)
        }
        return com.google.gson.Gson().toJson(arr)
    }

    // 设置选中的 KSampler
    fun setSelectedKSampler(sampler: String) {
        _selectedKSampler.value = sampler
        saveLastUsedKSampler(sampler)
    }

    // 设置选中的 KScheduler
    fun setSelectedKScheduler(scheduler: String) {
        _selectedKScheduler.value = scheduler
        saveLastUsedKScheduler(scheduler)
    }

    fun savePromptDimensions(promptId: String, width: Int, height: Int, steps: Int, cfgScale: Double) {
        viewModelScope.launch {
            promptDao.updatePromptDimensions(
                promptId, width, height, steps, cfgScale,
                System.currentTimeMillis()
            )
        }
    }

    // 设置工作流类型
    fun setWorkflowType(type: String) {
        _workflowType.value = type
    }

    // 检测工作流类型
    fun detectWorkflowType(workflowJson: String): String {
        return comfyUIClient.detectWorkflowType(workflowJson)
    }

    fun loadPrompt(promptData: PromptWithTags) {
        currentPromptId = promptData.prompt.id
    }

    fun loadFavoritePrompts() {
        viewModelScope.launch {
            (getApplication<Application>() as App).database.favoritePromptDao().getAll().collect { favorites ->
                _favoritePositivePrompts.value = favorites.filter { it.type == "positive" }.map { it.content }
                _favoriteNegativePrompts.value = favorites.filter { it.type == "negative" }.map { it.content }
            }
        }
    }

    fun checkQueueStatus() {
        viewModelScope.launch {
            try {
                val url = settingsManager.comfyUiUrl.first()
                comfyUIClient.setServerUrl(url)
                val queueStatus = comfyUIClient.getQueueStatus()
                val queueRunning = queueStatus.getAsJsonArray("queue_running")?.size() ?: 0
                val queuePending = queueStatus.getAsJsonArray("queue_pending")?.size() ?: 0
                _queuePosition.value = queueRunning + queuePending
            } catch (e: Exception) {
                Log.e("GenerateViewModel", "检查队列状态失败", e)
            }
        }
    }

    fun stopGeneration(onComplete: () -> Unit) {
        Log.d("ImageGen", "=== GenerateViewModel.stopGeneration ===")
        generationService?.stopGeneration()
        viewModelScope.launch { comfyUIClient.interrupt() }
        _progress.value = "已停止生成"
        _estimatedTime.value = null
        onComplete()
    }

    fun generateImage(
        promptId: String,
        positivePrompt: String,
        negativePrompt: String,
        seed: Long,
        workflowId: String? = null,
        batchSize: Int = 1,
        samplerName: String = "multistep",
        scheduler: String = "beta57",
        width: Int = 896,
        height: Int = 1088,
        steps: Int = 30,
        cfgScale: Double = 5.5,
        inputImageUris: List<android.net.Uri> = emptyList(),
        denoise: Double = 1.0,
        useWorkflowDimensions: Boolean = false,
        ksamplerName: String = "euler_ancestral",
        kscheduler: String = "normal",
        artistPrompt: String = "",
        baseModel: String? = null,
        loraConfigs: String? = null,
        resolution: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val context = getApplication<Application>()

        // 保存使用的工作流和采样器设置
        saveLastUsedWorkflowId(promptId, workflowId)
        saveLastUsedSampler(samplerName)
        saveLastUsedScheduler(scheduler)
        saveLastUsedKSampler(ksamplerName)
        saveLastUsedKScheduler(kscheduler)

        // 保存高级参数 + 模型/LoRA 配置到提示词（同步确保写入）
        kotlinx.coroutines.runBlocking {
            promptDao.updatePromptDimensions(
                promptId, width, height, steps, cfgScale,
                System.currentTimeMillis()
            )
            try {
                val promptFlow = promptDao.getPromptById(promptId)
                val promptWithTags = promptFlow.first { it != null }
                val entity = promptWithTags?.prompt ?: return@runBlocking
                val updated = entity.copy(
                    baseModel = _selectedBaseModel.value,
                    loraConfigs = exportLoraConfigsToJson().ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
                promptDao.updatePrompt(updated)
                Log.d("GenerateVM", "模型/LoRA 配置已保存到 prompt: $promptId, model=${_selectedBaseModel.value}")
            } catch (e: Exception) {
                Log.e("GenerateVM", "保存模型配置失败", e)
            }
        }

        // 启动前台服务进行生成
        val comfyUrl = kotlinx.coroutines.runBlocking { settingsManager.comfyUiUrl.first() }
        val intent = Intent(context, ImageGenerationService::class.java).apply {
            action = ImageGenerationService.ACTION_START_GENERATION
            putExtra(ImageGenerationService.EXTRA_POSITIVE_PROMPT, positivePrompt)
            putExtra(ImageGenerationService.EXTRA_NEGATIVE_PROMPT, negativePrompt)
            putExtra(ImageGenerationService.EXTRA_SEED, seed)
            putExtra(ImageGenerationService.EXTRA_WORKFLOW_ID, workflowId)
            putExtra(ImageGenerationService.EXTRA_BATCH_SIZE, batchSize)
            putExtra(ImageGenerationService.EXTRA_PROMPT_ID, currentPromptId)
            putExtra(ImageGenerationService.EXTRA_SAMPLER_NAME, samplerName)
            putExtra(ImageGenerationService.EXTRA_SCHEDULER, scheduler)
            putExtra(ImageGenerationService.EXTRA_WIDTH, width)
            putExtra(ImageGenerationService.EXTRA_HEIGHT, height)
            putExtra(ImageGenerationService.EXTRA_STEPS, steps)
            putExtra(ImageGenerationService.EXTRA_CFG_SCALE, cfgScale)
            putExtra(ImageGenerationService.EXTRA_DENOISE, denoise)
            putExtra(ImageGenerationService.EXTRA_USE_WORKFLOW_DIMENSIONS, useWorkflowDimensions)
            putExtra(ImageGenerationService.EXTRA_KSAMPLER_NAME, ksamplerName)
            putExtra(ImageGenerationService.EXTRA_KSCHEDULER, kscheduler)
            putExtra(ImageGenerationService.EXTRA_ARTIST_PROMPT, artistPrompt)
            putExtra(ImageGenerationService.EXTRA_COMFYUI_URL, comfyUrl)
            // 模型/LoRA 配置
            baseModel?.let { putExtra(ImageGenerationService.EXTRA_BASE_MODEL, it) }
            loraConfigs?.let { putExtra(ImageGenerationService.EXTRA_LORA_CONFIGS, it) }
            resolution?.let { putExtra(ImageGenerationService.EXTRA_RESOLUTION, it) }
            putStringArrayListExtra(
                ImageGenerationService.EXTRA_INPUT_IMAGE_URIS,
                ArrayList(inputImageUris.map { it.toString() })
            )
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // 监听生成完成
        viewModelScope.launch {
            var lastImageCount = 0
            _generatedImages.collect { images ->
                if (images.size > lastImageCount && images.size == batchSize) {
                    // 所有图片生成完成
                    onSuccess("生成完成！共生成 ${images.size} 张图片")
                }
                lastImageCount = images.size
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
