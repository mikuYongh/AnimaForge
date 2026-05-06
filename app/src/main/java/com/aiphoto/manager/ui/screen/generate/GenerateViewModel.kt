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

    private val _generatedImages = MutableStateFlow<List<String>>(emptyList())
    val generatedImages: StateFlow<List<String>> = _generatedImages

    private val _estimatedTime = MutableStateFlow<String?>(null)
    val estimatedTime: StateFlow<String?> = _estimatedTime

    private val _queuePosition = MutableStateFlow<Int?>(null)
    val queuePosition: StateFlow<Int?> = _queuePosition

    private var generationStartTime: Long = 0
    private var lastProgressValue: Int = 0

    // 收藏的提示词
    private val _favoritePositivePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoritePositivePrompts: StateFlow<List<String>> = _favoritePositivePrompts

    private val _favoriteNegativePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoriteNegativePrompts: StateFlow<List<String>> = _favoriteNegativePrompts

    // 基于历史数据动态调整估算时间
    private val generationTimeHistory = mutableListOf<Long>() // 存储最近10次生成时间
    private var estimatedTotalTime: Int = 35 // 初始估算35秒（基于日志中的32秒实际时间）

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
        "linear/euler" to  "linear/euler"
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
                    Log.d("GenerateViewModel", "【服务状态变化】服务isGenerating: $generating, 当前ViewModel状态: ${_isGenerating.value}")
                    _isGenerating.value = generating
                    Log.d("GenerateViewModel", "【服务状态变化】已更新ViewModel状态: ${_isGenerating.value}")

                    // 当开始生成时，初始化开始时间
                    if (generating && generationStartTime == 0L) {
                        generationStartTime = System.currentTimeMillis()
                        lastProgressValue = 0
                        Log.d("GenerateViewModel", "【服务状态变化】初始化开始时间: ${java.util.Date(generationStartTime)}")
                    }

                    // 当生成结束时，重置开始时间
                    if (!generating) {
                        generationStartTime = 0
                        lastProgressValue = 0
                        _estimatedTime.value = null
                        Log.d("GenerateViewModel", "【服务状态变化】重置开始时间和估算时间")
                    }
                }
            }

            // 立即同步当前状态
            val currentServiceState = generationService?.isGenerating?.value ?: false
            _isGenerating.value = currentServiceState
            Log.d("GenerateViewModel", "【初始状态同步】服务状态: $currentServiceState, ViewModel状态: ${_isGenerating.value}")

            viewModelScope.launch {
                generationService?.progress?.collect { progressText ->
                    _progress.value = progressText

                    // 尝试从进度文本中提取百分比（例如 "生成中... 45%"）
                    val progressMatch = Regex("(\\d+)%").find(progressText)
                    if (progressMatch != null) {
                        lastProgressValue = progressMatch.groupValues[1].toInt()
                    }
                }
            }

            viewModelScope.launch {
                generationService?.generatedImages?.collect { images ->
                    _generatedImages.value = images

                    // 当有新图片生成完成时，记录生成时间并更新估算
                    if (images.isNotEmpty() && generationStartTime > 0) {
                        val generationTime = (System.currentTimeMillis() - generationStartTime) / 1000
                        Log.d("GenerateViewModel", "图片生成完成，用时: ${generationTime}秒")

                        // 记录到历史
                        generationTimeHistory.add(generationTime)
                        if (generationTimeHistory.size > 10) {
                            generationTimeHistory.removeAt(0)
                        }

                        // 更新估算时间为历史平均值
                        if (generationTimeHistory.isNotEmpty()) {
                            estimatedTotalTime = (generationTimeHistory.sum() / generationTimeHistory.size).toInt()
                            Log.d("GenerateViewModel", "更新估算时间: ${estimatedTotalTime}秒 (基于${generationTimeHistory.size}次历史数据)")
                        }

                        // 重置开始时间，为下一张图片准备
                        generationStartTime = System.currentTimeMillis()
                        lastProgressValue = 0
                    }
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

    // 获取上次使用的工作流ID
    fun getLastUsedWorkflowId(): String? {
        return sharedPreferences.getString("last_workflow_id", null)
    }

    // 保存工作流ID
    private fun saveLastUsedWorkflowId(workflowId: String?) {
        sharedPreferences.edit().putString("last_workflow_id", workflowId ?: "").apply()
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

                Log.d("GenerateViewModel", "========== 队列状态检查 ==========")
                Log.d("GenerateViewModel", "队列JSON: $queueStatus")

                // 解析队列状态
                val queueRunning = queueStatus.getAsJsonArray("queue_running")?.size() ?: 0
                val queuePending = queueStatus.getAsJsonArray("queue_pending")?.size() ?: 0

                Log.d("GenerateViewModel", "运行中任务数: $queueRunning, 等待中任务数: $queuePending")

                _queuePosition.value = queueRunning + queuePending

                // 根据进度计算剩余时间
                if (generationStartTime > 0 && lastProgressValue > 0) {
                    // 有进度信息，基于进度百分比计算
                    val elapsedTime = (System.currentTimeMillis() - generationStartTime) / 1000
                    val estimatedTotalTime = (elapsedTime * 100) / lastProgressValue
                    val remainingTime = estimatedTotalTime - elapsedTime

                    val minutes = remainingTime / 60
                    val seconds = remainingTime % 60

                    _estimatedTime.value = when {
                        remainingTime <= 0 -> "即将完成"
                        minutes > 0 -> "预计剩余 ${minutes}分${seconds}秒"
                        else -> "预计剩余 ${seconds}秒"
                    }

                    Log.d("GenerateViewModel", "【有进度】进度: $lastProgressValue%, 已用: ${elapsedTime}秒, 预计总时间: ${estimatedTotalTime}秒, 剩余: ${remainingTime}秒")
                    Log.d("GenerateViewModel", "【有进度】显示文本: ${_estimatedTime.value}")
                } else if (generationStartTime > 0) {
                    // 没有进度信息，但有开始时间，基于已用时间估算
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = (currentTime - generationStartTime) / 1000

                    Log.d("GenerateViewModel", "【无进度】开始时间: $generationStartTime (${java.util.Date(generationStartTime)})")
                    Log.d("GenerateViewModel", "【无进度】当前时间: $currentTime (${java.util.Date(currentTime)})")
                    Log.d("GenerateViewModel", "【无进度】已用时间: ${elapsedTime}秒")
                    Log.d("GenerateViewModel", "【无进度】lastProgressValue: $lastProgressValue")

                    // 基于已用时间动态调整估算（估算值随时间递减）
                    val estimatedRemaining = maxOf(5, estimatedTotalTime - elapsedTime.toInt())

                    val minutes = estimatedRemaining / 60
                    val seconds = estimatedRemaining % 60

                    _estimatedTime.value = when {
                        estimatedRemaining <= 5 -> "即将完成"
                        minutes > 0 -> "预计剩余 ${minutes}分${seconds}秒"
                        else -> "预计剩余 ${seconds}秒"
                    }

                    Log.d("GenerateViewModel", "【无进度】预计总时间: ${estimatedTotalTime}秒, 预计剩余: ${estimatedRemaining}秒")
                    Log.d("GenerateViewModel", "【无进度】显示文本: ${_estimatedTime.value}")
                } else if (queueRunning > 0 || queuePending > 0) {
                    // 还没开始生成，显示队列信息
                    val waitTime = if (queueRunning > 0) queueRunning * 30 else 0
                    val queueTime = queuePending * 30
                    val estimatedSeconds = waitTime + queueTime

                    val minutes = estimatedSeconds / 60
                    val seconds = estimatedSeconds % 60
                    _estimatedTime.value = when {
                        minutes > 0 -> "队列等待 ${minutes}分${seconds}秒"
                        seconds > 0 -> "队列等待 ${seconds}秒"
                        else -> "即将开始"
                    }

                    Log.d("GenerateViewModel", "【队列等待】显示文本: ${_estimatedTime.value}")
                } else {
                    _estimatedTime.value = "正在生成中..."
                    Log.d("GenerateViewModel", "【其他】显示文本: ${_estimatedTime.value}")
                }

                Log.d("GenerateViewModel", "===================================")
            } catch (e: Exception) {
                Log.e("GenerateViewModel", "获取队列状态失败", e)
                _estimatedTime.value = "生成中..."
            }
        }
    }

    fun stopGeneration(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // 先中断 ComfyUI 服务器端的生成
                val url = settingsManager.comfyUiUrl.first()
                comfyUIClient.setServerUrl(url)
                val success = comfyUIClient.interrupt()

                // 发送停止意图给服务，取消批处理循环
                val intent = Intent(getApplication(), ImageGenerationService::class.java).apply {
                    action = ImageGenerationService.ACTION_STOP_GENERATION
                }
                getApplication<Application>().startService(intent)

                if (success) {
                    _progress.value = "已停止生成"
                    generationStartTime = 0
                    lastProgressValue = 0
                    _estimatedTime.value = null
                    Log.d("GenerateViewModel", "【停止生成】已重置所有状态")
                }
                onComplete()
            } catch (e: Exception) {
                Log.e("GenerateViewModel", "停止生成失败", e)
                onComplete()
            }
        }
    }

    fun generateImage(
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

        // 初始化生成时间追踪
        generationStartTime = System.currentTimeMillis()
        lastProgressValue = 0

        // 保存使用的工作流和采样器设置
        saveLastUsedWorkflowId(workflowId)
        saveLastUsedSampler(samplerName)
        saveLastUsedScheduler(scheduler)
        saveLastUsedKSampler(ksamplerName)
        saveLastUsedKScheduler(kscheduler)

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
