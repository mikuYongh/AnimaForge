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
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.service.ImageGenerationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

        // 保存高级参数到提示词
        kotlinx.coroutines.runBlocking {
            promptDao.updatePromptDimensions(
                promptId, width, height, steps, cfgScale,
                System.currentTimeMillis()
            )
        }

        // 启动前台服务进行生成
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
