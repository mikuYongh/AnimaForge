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

    private val _generatedImages = MutableStateFlow<List<String>>(emptyList())
    val generatedImages: StateFlow<List<String>> = _generatedImages

    private var currentGenerationJob: Job? = null

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
                    artistPrompt = artistPrompt
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
                NotificationManager.IMPORTANCE_LOW
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
        artistPrompt: String = ""
    ) {
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
                    if (!isActive) break

                    val progressText = "正在生成第 $i/$batchSize 张..."
                    _progress.value = progressText

                    // 更新通知
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createNotification("图片生成中", progressText)
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
                            artistPrompt = artistPrompt
                        )

                        if (promptResult.isFailure) {
                            failCount++
                            continue
                        }

                        val promptIdResult = promptResult.getOrThrow()

                        val imageResult = comfyUIClient.waitForImage(promptIdResult)

                        if (imageResult.isFailure) {
                            failCount++
                            continue
                        }

                        val images = imageResult.getOrThrow()
                        if (images.isNotEmpty()) {
                            images.forEach { imageFile ->
                                val generatedImage = GeneratedImageEntity(
                                    id = UUID.randomUUID().toString(),
                                    promptId = promptId,
                                    imagePath = imageFile.absolutePath,
                                    positivePrompt = positivePrompt,
                                    negativePrompt = negativePrompt,
                                    seed = currentSeed,
                                    workflowId = workflowId
                                )
                                generatedImageDao.insertGeneratedImage(generatedImage)

                                _generatedImages.value = _generatedImages.value + imageFile.absolutePath
                            }
                            successCount += images.size
                        }
                    } catch (e: Exception) {
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
                _isGenerating.value = false

                // 停止前台服务
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

            } catch (e: Exception) {
                Log.e("ImageGenerationService", "生成图片失败", e)
                _progress.value = ""
                _isGenerating.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
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

    private fun stopGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _isGenerating.value = false
        _progress.value = ""
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
