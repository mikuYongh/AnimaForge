package com.aiphoto.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aiphoto.manager.App
import com.aiphoto.manager.MainActivity
import com.aiphoto.manager.R
import com.aiphoto.manager.api.ComfyUIClient
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class ImageGenerationService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var comfyUIClient: ComfyUIClient
    private lateinit var notificationManager: NotificationManager

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress

    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent

    private val _estimatedTime = MutableStateFlow("")
    val estimatedTime: StateFlow<String> = _estimatedTime

    private val _generatedImages = MutableStateFlow<List<String>>(emptyList())
    val generatedImages: StateFlow<List<String>> = _generatedImages

    private var currentGenerationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "image_generation_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_GENERATION = "com.aiphoto.manager.START_GENERATION"
        const val ACTION_STOP_GENERATION = "com.aiphoto.manager.STOP_GENERATION"

        const val EXTRA_POSITIVE_PROMPT = "positive_prompt"
        const val EXTRA_NEGATIVE_PROMPT = "negative_prompt"
        const val EXTRA_SEED = "seed"
        const val EXTRA_WORKFLOW_ID = "workflow_id"
        const val EXTRA_BATCH_SIZE = "batch_size"
        const val EXTRA_PROMPT_ID = "prompt_id"
        const val EXTRA_SAMPLER_NAME = "sampler_name"
        const val EXTRA_SCHEDULER = "scheduler"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_STEPS = "steps"
        const val EXTRA_CFG_SCALE = "cfg_scale"
        const val EXTRA_DENOISE = "denoise"
        const val EXTRA_INPUT_IMAGE_URIS = "input_image_uris"
        const val EXTRA_USE_WORKFLOW_DIMENSIONS = "use_workflow_dimensions"
        const val EXTRA_KSAMPLER_NAME = "ksampler_name"
        const val EXTRA_KSCHEDULER = "kscheduler"
        const val EXTRA_ARTIST_PROMPT = "artist_prompt"
        const val EXTRA_COMFYUI_URL = "comfyui_url"
        const val EXTRA_BASE_MODEL = "base_model"
        const val EXTRA_LORA_CONFIGS = "lora_configs"
        const val EXTRA_RESOLUTION = "resolution"
    }

    inner class LocalBinder : Binder() {
        fun getService(): ImageGenerationService = this@ImageGenerationService
    }

    override fun onCreate() {
        super.onCreate()
        comfyUIClient = ComfyUIClient(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ImageGen", "=== onStartCommand: action=${intent?.action} ===")
        when (intent?.action) {
            ACTION_START_GENERATION -> {
                val positivePrompt = intent.getStringExtra(EXTRA_POSITIVE_PROMPT) ?: ""
                val negativePrompt = intent.getStringExtra(EXTRA_NEGATIVE_PROMPT) ?: ""
                val seed = intent.getLongExtra(EXTRA_SEED, -1L)
                val workflowId = intent.getStringExtra(EXTRA_WORKFLOW_ID)
                val batchSize = intent.getIntExtra(EXTRA_BATCH_SIZE, 1)
                val promptId = intent.getStringExtra(EXTRA_PROMPT_ID)
                val samplerName = intent.getStringExtra(EXTRA_SAMPLER_NAME) ?: "multistep"
                val scheduler = intent.getStringExtra(EXTRA_SCHEDULER) ?: "beta57"
                val width = intent.getIntExtra(EXTRA_WIDTH, 896)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1088)
                val steps = intent.getIntExtra(EXTRA_STEPS, 30)
                val cfgScale = intent.getDoubleExtra(EXTRA_CFG_SCALE, 5.5)
                val denoise = intent.getDoubleExtra(EXTRA_DENOISE, 0.75)
                val inputImageUris = intent.getStringArrayListExtra(EXTRA_INPUT_IMAGE_URIS) ?: emptyList()
                val useWorkflowDimensions = intent.getBooleanExtra(EXTRA_USE_WORKFLOW_DIMENSIONS, false)
                val ksamplerName = intent.getStringExtra(EXTRA_KSAMPLER_NAME) ?: "euler_ancestral"
                val kscheduler = intent.getStringExtra(EXTRA_KSCHEDULER) ?: "normal"
                val artistPrompt = intent.getStringExtra(EXTRA_ARTIST_PROMPT) ?: ""
                val serverUrl = intent.getStringExtra(EXTRA_COMFYUI_URL)
                val baseModel = intent.getStringExtra(EXTRA_BASE_MODEL)
                val loraConfigs = intent.getStringExtra(EXTRA_LORA_CONFIGS)
                val resolution = intent.getStringExtra(EXTRA_RESOLUTION)
                if (serverUrl != null) {
                    comfyUIClient.setServerUrl(serverUrl)
                }

                startGeneration(
                    positivePrompt = positivePrompt,
                    negativePrompt = negativePrompt,
                    seed = seed,
                    workflowId = workflowId,
                    batchSize = batchSize,
                    promptId = promptId,
                    samplerName = samplerName,
                    scheduler = scheduler,
                    width = width,
                    height = height,
                    steps = steps,
                    cfgScale = cfgScale,
                    denoise = denoise,
                    inputImageUris = inputImageUris,
                    useWorkflowDimensions = useWorkflowDimensions,
                    ksamplerName = ksamplerName,
                    kscheduler = kscheduler,
                    artistPrompt = artistPrompt,
                    baseModel = baseModel,
                    loraConfigs = loraConfigs,
                    resolution = resolution
                )
            }
            ACTION_STOP_GENERATION -> {
                stopGeneration()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "图片生成服务",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "显示图片生成进度"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startGeneration(
        positivePrompt: String,
        negativePrompt: String,
        seed: Long,
        workflowId: String?,
        batchSize: Int,
        promptId: String?,
        samplerName: String = "multistep",
        scheduler: String = "beta57",
        width: Int = 896,
        height: Int = 1088,
        steps: Int = 30,
        cfgScale: Double = 5.5,
        denoise: Double = 1.0,
        inputImageUris: List<String> = emptyList(),
        useWorkflowDimensions: Boolean = false,
        ksamplerName: String = "euler_ancestral",
        kscheduler: String = "normal",
        artistPrompt: String = "",
        baseModel: String? = null,
        loraConfigs: String? = null,
        resolution: String? = null
    ) {
        // 获取唤醒锁，防止锁屏后 CPU 休眠
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AnimaForge:ImageGeneration"
        ).apply { acquire() }

        // 启动前台服务
        val notification = createNotification("图片生成中", "准备中...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isGenerating.value = true
        _generatedImages.value = emptyList()
        _progressPercent.value = 0f

        // 连接 WebSocket
        val clientId = "img_${System.currentTimeMillis()}"
        comfyUIClient.connectWebSocket(clientId)

        currentGenerationJob = scope.launch {
            try {
                val database = (application as App).database
                val workflowDao = database.workflowDao()
                val generatedImageDao = database.generatedImageDao()

                // 获取工作流
                val customWorkflow = if (workflowId != null) {
                    workflowDao.getWorkflowById(workflowId)?.workflowJson
                } else {
                    workflowDao.getDefaultWorkflow()?.workflowJson
                }

                // 检测工作流类型
                val workflowType = if (customWorkflow != null) {
                    comfyUIClient.detectWorkflowType(customWorkflow)
                } else {
                    "text2img"
                }

                // 如果是图生图且需要上传图片
                var inputImageFilename: String? = null
                if (workflowType == "img2img" && inputImageUris.isNotEmpty()) {
                    _progress.value = "正在上传图片..."
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createNotification("图片生成中", "正在上传图片...")
                    )

                    // 上传第一张图片
                    val uploadResult = comfyUIClient.uploadImage(Uri.parse(inputImageUris[0]))
                    if (uploadResult.isSuccess) {
                        inputImageFilename = uploadResult.getOrThrow()
                        Log.d("ImageGenerationService", "图片上传成功: $inputImageFilename")
                    } else {
                        Log.e("ImageGenerationService", "图片上传失败", uploadResult.exceptionOrNull())
                        _progress.value = "图片上传失败"
                        showCompletionNotification(0, "图片上传失败，无法生成")
                        _isGenerating.value = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                }

                var successCount = 0
                var failCount = 0

                for (i in 1..batchSize) {
                    Log.d("ImageGen", "循环迭代 i=$i isActive=$isActive")
                    if (!isActive) { Log.d("ImageGen", "isActive=false break"); break }

                    _progress.value = "提交 $i/$batchSize..."
                    _progressPercent.value = 0f
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createNotification("图片生成中", _progress.value)
                    )

                    try {
                        val currentSeed = if (seed == -1L) {
                            System.currentTimeMillis() + i
                        } else {
                            seed + i - 1
                        }

                        val promptResult = comfyUIClient.submitPrompt(
                            positivePrompt = positivePrompt,
                            negativePrompt = negativePrompt,
                            seed = currentSeed,
                            width = width,
                            height = height,
                            steps = steps,
                            cfgScale = cfgScale,
                            samplerName = samplerName,
                            scheduler = scheduler,
                            customWorkflow = customWorkflow,
                            denoise = denoise,
                            inputImageFilename = inputImageFilename,
                            useWorkflowDimensions = useWorkflowDimensions,
                            ksamplerName = ksamplerName,
                            kscheduler = kscheduler,
                            artistPrompt = artistPrompt,
                            baseModel = baseModel,
                            loraConfigs = loraConfigs,
                            resolution = resolution,
                            clientId = clientId
                        )

                        if (promptResult.isFailure) {
                            failCount++
                            continue
                        }

                        val promptIdResult = promptResult.getOrThrow()

                        // WebSocket 驱动: 进度 + 完成检测
                        val startTime = System.currentTimeMillis()
                        var lastProgressTime = startTime; var lastProgressValue = 0; var stepAvgMs = 0L; var found = false

                        var wsJob: kotlinx.coroutines.Job? = null
                        wsJob = scope.launch {
                            comfyUIClient.wsEvents.collect { (type, data) ->
                                val p = data.getAsJsonObject("data")?.get("prompt_id")?.asString ?: return@collect
                                if (p != promptIdResult) return@collect
                                when (type) {
                                    "progress" -> {
                                        val v = data.getAsJsonObject("data")?.get("value")?.asInt ?: 0
                                        val m = data.getAsJsonObject("data")?.get("max")?.asInt ?: 1
                                        val now = System.currentTimeMillis()
                                        val deltaV = v - lastProgressValue; val deltaT = now - lastProgressTime
                                        if (deltaV > 0 && deltaT > 0 && lastProgressValue > 0) {
                                            val curMs = deltaT / deltaV
                                            stepAvgMs = if (stepAvgMs == 0L) curMs else (stepAvgMs * 3 + curMs) / 4
                                        }
                                        val newPct = v.toFloat() / m
                                        if (newPct > _progressPercent.value) _progressPercent.value = newPct
                                        _progress.value = "生成 $i/$batchSize: $v/$m"
                                        val remain = if (stepAvgMs > 0) ((m - v) * stepAvgMs) / 1000 else 0
                                        if (remain > 0) _estimatedTime.value = "预计剩余 ${remain}s"
                                        notificationManager.notify(NOTIFICATION_ID, createNotification("图片生成中", _progress.value))
                                        Log.d("ImageGen", "Progress: $v/$m stepAvg=${stepAvgMs}ms remain=${remain}s")
                                        lastProgressTime = now; lastProgressValue = v
                                    }
                                    "execution_success" -> {
                                        Log.d("ImageGen", "WS execution_success!"); found = true; wsJob?.cancel()
                                    }
                                    "execution_error" -> {
                                        Log.e("ImageGen", "WS execution_error!"); found = true; wsJob?.cancel()
                                    }
                                }
                            }
                        }

                        // HTTP 轮询 fallback: 等 WS 完成或超时后切换
                        while (isActive && !found) {
                            if (!isActive) { Log.d("ImageGen", "while内 isActive=false break"); break }
                            kotlinx.coroutines.delay(1000)
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000
                            // WS 没数据时才显示运行时间，不覆盖 WS 的真实 ETA
                            if (_progressPercent.value <= 0f) _estimatedTime.value = "${elapsed}s"
                            if (elapsed % 5 == 0L) Log.d("ImageGen", "等待中: elapsed=${elapsed}s isActive=$isActive")
                            if (elapsed > 10) {
                                val hist = withContext(Dispatchers.IO) { comfyUIClient.getHistoryById(promptIdResult) }
                                if (hist.isSuccess && hist.getOrNull() != null) {
                                    val obj = hist.getOrNull()!!
                                    val outs = obj.getAsJsonObject("outputs")
                                    if (outs != null) {
                                        for ((_, nv) in outs.entrySet()) {
                                            val arr = nv.asJsonObject.getAsJsonArray("images") ?: continue
                                            if (arr.size() > 0) {
                                                val info = arr[0].asJsonObject
                                                _progress.value = "下载图片 $i/$batchSize..."
                                                val dl = withContext(Dispatchers.IO) { comfyUIClient.downloadFile(info.get("filename").asString, info.get("subfolder")?.asString ?: "", info.get("type")?.asString ?: "output", "images") }
                                                if (dl.isSuccess) {
                                                    val file = File(dl.getOrThrow())
                                                    val generatedImage = GeneratedImageEntity(
                                                        id = UUID.randomUUID().toString(),
                                                        promptId = promptId,
                                                        imagePath = file.absolutePath,
                                                        positivePrompt = positivePrompt,
                                                        negativePrompt = negativePrompt,
                                                        seed = currentSeed,
                                                        workflowId = workflowId
                                                    )
                                                    generatedImageDao.insertGeneratedImage(generatedImage)
                                                    _generatedImages.value = _generatedImages.value + file.absolutePath
                                                    successCount++
                                                    found = true
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        wsJob?.cancel()

            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("ImageGenerationService", "生成被取消")
                    } catch (e: Exception) {
                        Log.d("ImageGen", "循环内异常: isActive=$isActive msg=${e.message}")
                        if (!isActive) break  // 被取消
                        Log.e("ImageGenerationService", "第 $i 次生成失败", e)
                        failCount++
                    }
                }

                // 生成完成，发送完成通知
                val resultText = if (successCount > 0 && failCount == 0) {
                    "生成成功！共生成 $successCount 张图片"
                } else if (successCount > 0 && failCount > 0) {
                    "生成完成！成功 $successCount 张，失败 $failCount 张"
                } else {
                    "生成失败：所有生成都失败了"
                }

                showCompletionNotification(successCount, resultText)

                _progress.value = ""
                _progressPercent.value = 0f
                _estimatedTime.value = ""
                _isGenerating.value = false
                releaseWakeLock()
                comfyUIClient.disconnectWebSocket()
                stopForeground(STOP_FOREGROUND_DETACH)

            } catch (e: Exception) {
                Log.e("ImageGenerationService", "生成图片失败", e)
                _progress.value = ""
                _progressPercent.value = 0f
                _estimatedTime.value = ""
                _isGenerating.value = false
                releaseWakeLock()
                comfyUIClient.disconnectWebSocket()
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun showCompletionNotification(successCount: Int, message: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (successCount > 0) "图片生成完成" else "图片生成失败")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    fun stopGeneration() {
        Log.d("ImageGen", "=== stopGeneration 被调用 ===")
        Log.d("ImageGen", "job 状态: isActive=${currentGenerationJob?.isActive}, isCancelled=${currentGenerationJob?.isCancelled}")
        currentGenerationJob?.cancel()
        Log.d("ImageGen", "cancel 后 job 状态: isActive=${currentGenerationJob?.isActive}, isCancelled=${currentGenerationJob?.isCancelled}")
        currentGenerationJob = null
        _isGenerating.value = false
        _progress.value = ""
        scope.launch { comfyUIClient.interrupt(); comfyUIClient.disconnectWebSocket() }
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        scope.cancel()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}
