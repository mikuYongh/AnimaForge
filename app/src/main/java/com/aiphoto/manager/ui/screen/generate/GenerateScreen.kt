package com.aiphoto.manager.ui.screen.generate

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.aiphoto.manager.ui.component.AuraParticlesBackground
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.ui.component.ImagePicker
import com.aiphoto.manager.ui.component.ModelConfigSection
import com.aiphoto.manager.ui.component.ModelConfigSectionLoraItem
import com.aiphoto.manager.ui.theme.LocalAppColorSet
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenerateScreen(
    promptData: PromptWithTags?,
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit = {},
    viewModel: GenerateViewModel = viewModel()
) {
    val context = LocalContext.current
    val workflows by viewModel.workflows.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressPercent by viewModel.progressPercent.collectAsState()
    val generatedImages by viewModel.generatedImages.collectAsState()
    val selectedSampler by viewModel.selectedSampler.collectAsState()
    val selectedScheduler by viewModel.selectedScheduler.collectAsState()
    val selectedKSampler by viewModel.selectedKSampler.collectAsState()
    val selectedKScheduler by viewModel.selectedKScheduler.collectAsState()
    val estimatedTime by viewModel.estimatedTime.collectAsState()
    val queuePosition by viewModel.queuePosition.collectAsState()
    val favoritePositivePrompts by viewModel.favoritePositivePrompts.collectAsState()
    val favoriteNegativePrompts by viewModel.favoriteNegativePrompts.collectAsState()
    val workflowType by viewModel.workflowType.collectAsState()

    var selectedWorkflowId by remember { mutableStateOf<String?>(null) }
    var showWorkflowMenu by remember { mutableStateOf(false) }
    var batchSize by remember { mutableStateOf("1") }
    var seed by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var showFavoritePositiveMenu by remember { mutableStateOf(false) }
    var showFavoriteNegativeMenu by remember { mutableStateOf(false) }
    var selectedInputImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var denoiseStrength by remember { mutableStateOf(0.75f) }
    var useWorkflowDimensions by remember { mutableStateOf(false) }
    var genWidth by remember { mutableStateOf("896") }
    var genHeight by remember { mutableStateOf("1088") }
    var genSteps by remember { mutableStateOf("30") }
    var genCfgScale by remember { mutableStateOf("5.5") }
    var showAdvanced by remember { mutableStateOf(false) }

    // 在生成过程中定期检查队列状态
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (true) {
                viewModel.checkQueueStatus()
                kotlinx.coroutines.delay(2000) // 每2秒检查一次
            }
        }
    }

    // 保存图片到相册的函数
    fun saveImageToGallery(imagePath: String) {
        try {
            val file = java.io.File(imagePath)
            if (file.exists()) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "AI_${System.currentTimeMillis()}.png")
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/AI_PhotoManager")
                }

                val contentResolver = context.contentResolver
                val imageUri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                imageUri?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 保存确认对话框
    if (showSaveDialog && selectedImagePath != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存图片") },
            text = { Text("确定要将图片保存到相册吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        selectedImagePath?.let { saveImageToGallery(it) }
                        selectedImagePath = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    selectedImagePath = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 图片预览对话框（全屏）
    if (showImagePreview && previewImagePath != null) {
        Dialog(
            onDismissRequest = {
                showImagePreview = false
                previewImagePath = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 顶部操作栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            showImagePreview = false
                            previewImagePath = null
                        }) {
                            Text("关闭")
                        }
                        FilledTonalButton(
                            onClick = {
                                showImagePreview = false
                                selectedImagePath = previewImagePath
                                showSaveDialog = true
                            }
                        ) {
                            Text("保存到相册")
                        }
                        FilledTonalButton(
                            onClick = {
                                val path = previewImagePath
                                showImagePreview = false
                                previewImagePath = null
                                if (path != null) onNavigateToVideo(path)
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("转视频")
                        }
                    }

                    // 图片显示区域
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = java.io.File(previewImagePath!!),
                            contentDescription = "预览图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }

    // 加载上次使用的工作流（按提示词项目绑定）
    LaunchedEffect(workflows, promptData) {
        val pid = promptData?.prompt?.id ?: return@LaunchedEffect
        val lastUsedId = viewModel.getLastUsedWorkflowId(pid)
        if (lastUsedId != null && workflows.any { it.id == lastUsedId }) {
            selectedWorkflowId = lastUsedId
        }
    }

    // 检测工作流类型
    LaunchedEffect(selectedWorkflowId, workflows) {
        val workflow = if (selectedWorkflowId != null) {
            workflows.find { it.id == selectedWorkflowId }
        } else {
            null
        }

        val type = if (workflow != null) {
            viewModel.detectWorkflowType(workflow.workflowJson)
        } else {
            "text2img"
        }
        viewModel.setWorkflowType(type)
        viewModel.detectHasSizePicker(workflow?.workflowJson)
    }

    LaunchedEffect(promptData) {
        promptData?.let {
            viewModel.loadPrompt(it)
            positivePrompt = it.prompt.positivePrompt
            negativePrompt = it.prompt.negativePrompt
            genWidth = it.prompt.width.toString()
            genHeight = it.prompt.height.toString()
            genSteps = it.prompt.steps.toString()
            genCfgScale = it.prompt.cfgScale.toString()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFavoritePrompts()
    }

    // 解析工作流 LoRA 配置并加载可用模型列表
    LaunchedEffect(selectedWorkflowId, workflows) {
        val workflow = if (selectedWorkflowId != null) {
            workflows.find { it.id == selectedWorkflowId }
        } else {
            null
        }
        if (workflow != null) {
            viewModel.parseLoraFromWorkflow(workflow.workflowJson)
        }
        viewModel.loadAvailableModelsAndLoras()
    }

    // 从提示词项目恢复已保存的模型/LoRA 配置
    LaunchedEffect(promptData) {
        promptData?.let {
            viewModel.importLoraConfigsFromPrompt(it.prompt)
        }
    }

    // 收集 ViewModel 的模型/LoRA 状态
    val selectedBaseModel by viewModel.selectedBaseModel.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val modelsLoading by viewModel.availableModelsLoading.collectAsState()
    val workflowLoraConfigs by viewModel.workflowLoraConfigs.collectAsState()
    val availableLoras by viewModel.availableLoras.collectAsState()
    val lorasLoading by viewModel.availableLorasLoading.collectAsState()
    val hasSizePicker by viewModel.hasSizePicker.collectAsState()
    val selectedResolution by viewModel.selectedResolution.collectAsState()

    AuraParticlesBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("生成图片") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 提示词信息
            promptData?.let { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            data.prompt.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (data.prompt.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                data.prompt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "正向提示词：",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            data.prompt.positivePrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (data.prompt.negativePrompt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "负向提示词：",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                data.prompt.negativePrompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 工作流选择
            Text(
                "工作流",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { showWorkflowMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        workflows.find { it.id == selectedWorkflowId }?.name
                            ?: "默认工作流"
                    )
                }

                DropdownMenu(
                    expanded = showWorkflowMenu,
                    onDismissRequest = { showWorkflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("默认工作流") },
                        onClick = {
                            selectedWorkflowId = null
                            showWorkflowMenu = false
                        }
                    )
                    workflows.forEach { workflow ->
                        DropdownMenuItem(
                            text = { Text(workflow.name) },
                            onClick = {
                                selectedWorkflowId = workflow.id
                                showWorkflowMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 模型/LoRA 配置（仅文生图模式显示）
            val appColorSet = LocalAppColorSet.current
            if (workflowType == "text2img") {
                ModelConfigSection(
                    baseModel = selectedBaseModel,
                    onBaseModelChange = { viewModel.setBaseModel(it) },
                    availableModels = availableModels,
                    modelsLoading = modelsLoading,
                    loraConfigs = workflowLoraConfigs.map { lora ->
                        ModelConfigSectionLoraItem(lora.name, lora.strength, lora.enabled)
                    },
                    availableLoras = availableLoras,
                    lorasLoading = lorasLoading,
                    onLoraToggle = { index, enabled ->
                        val item = workflowLoraConfigs[index]
                        viewModel.updateLoraConfig(index, item.copy(enabled = enabled))
                    },
                    onLoraStrengthChange = { index, strength ->
                        val item = workflowLoraConfigs[index]
                        viewModel.updateLoraConfig(index, item.copy(strength = strength))
                    },
                    onLoraAdd = { name -> viewModel.addLora(name) },
                    onLoraRemove = { index -> viewModel.removeLora(index) },
                    appColorSet = appColorSet
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 如果检测到图生图工作流，显示图片选择
            if (workflowType == "img2img") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "输入图片",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 图片选择器
                        ImagePicker(
                            imageUris = selectedInputImages,
                            onImagesSelected = { uris -> selectedInputImages = uris },
                            onImageRemoved = { index ->
                                selectedInputImages = selectedInputImages.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 去噪强度滑块
                        Text(
                            "去噪强度: ${String.format("%.2f", denoiseStrength)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = denoiseStrength,
                            onValueChange = { denoiseStrength = it },
                            valueRange = 0f..1f,
                            steps = 100
                        )
                        Text(
                            "值越大，AI自由度越高；值越小，越接近原图",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 当工作流需要图片但未选择图片时
            if (workflowType == "img2img" && selectedInputImages.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "图生图工作流需要选择输入图片",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 种子值
            OutlinedTextField(
                value = seed,
                onValueChange = { seed = it },
                label = { Text("种子值（留空随机）") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生成数量
            OutlinedTextField(
                value = batchSize,
                onValueChange = { batchSize = it.filter { c -> c.isDigit() } },
                label = { Text("生成数量") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 根据工作流中的采样器类型动态选择选项列表
            // 检测工作流中是否有 ClownsharKSampler
            val hasClownsharKSampler = remember(selectedWorkflowId, workflows) {
                val workflow = if (selectedWorkflowId != null) {
                    workflows.find { it.id == selectedWorkflowId }
                } else null
                if (workflow != null) {
                    try {
                        val json = com.google.gson.JsonParser.parseString(workflow.workflowJson).asJsonObject
                        json.entrySet().any { entry ->
                            entry.value.isJsonObject &&
                                entry.value.asJsonObject.get("class_type")?.asString == "ClownsharKSampler_Beta"
                        }
                    } catch (_: Exception) { false }
                } else false
            }

            // 动态采样器选项
            val currentSamplerOptions = if (hasClownsharKSampler) viewModel.samplerOptions else viewModel.ksamplerOptions
            val currentSchedulerOptions = if (hasClownsharKSampler) viewModel.schedulerOptions else viewModel.kschedulerOptions
            val currentSampler = if (hasClownsharKSampler) selectedSampler else selectedKSampler
            val currentScheduler = if (hasClownsharKSampler) selectedScheduler else selectedKScheduler
            val onSamplerSelected = if (hasClownsharKSampler) { v: String -> viewModel.setSelectedSampler(v) } else { v: String -> viewModel.setSelectedKSampler(v) }
            val onSchedulerSelected = if (hasClownsharKSampler) { v: String -> viewModel.setSelectedScheduler(v) } else { v: String -> viewModel.setSelectedKScheduler(v) }

            // 采样器选择
            var showSamplerMenu by remember { mutableStateOf(false) }
            Text(
                if (hasClownsharKSampler) "采样器 (ClownsharKSampler)" else "采样器 (KSampler)",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { showSamplerMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        currentSamplerOptions.find { it.first == currentSampler }?.second
                            ?: currentSampler
                    )
                }

                DropdownMenu(
                    expanded = showSamplerMenu,
                    onDismissRequest = { showSamplerMenu = false }
                ) {
                    currentSamplerOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSamplerSelected(value)
                                showSamplerMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 调度器选择
            var showSchedulerMenu by remember { mutableStateOf(false) }
            Text(
                "调度器 (Scheduler)",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { showSchedulerMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        currentSchedulerOptions.find { it.first == currentScheduler }?.second
                            ?: currentScheduler
                    )
                }

                DropdownMenu(
                    expanded = showSchedulerMenu,
                    onDismissRequest = { showSchedulerMenu = false }
                ) {
                    currentSchedulerOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSchedulerSelected(value)
                                showSchedulerMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 高级参数设置（折叠）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "高级参数",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showAdvanced) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (hasSizePicker) {
                            // SDXLEmptyLatentSizePicker+ 工作流：分辨率下拉框
                            Text(
                                "分辨率",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            var showResMenu by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { showResMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        selectedResolution,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showResMenu,
                                    onDismissRequest = { showResMenu = false }
                                ) {
                                    viewModel.resolutionOptions.forEach { res ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    res,
                                                    fontWeight = if (res == selectedResolution) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSelectedResolution(res)
                                                val (w, h) = viewModel.parseResolution(res)
                                                genWidth = w.toString()
                                                genHeight = h.toString()
                                                showResMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // 无 SDXLEmptyLatentSizePicker+：手动输入宽高
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NumberField("宽度", genWidth, "896") { genWidth = it }
                                NumberField("高度", genHeight, "1088") { genHeight = it }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NumberField("步数", genSteps, "30") { genSteps = it }
                            NumberField("CFG", genCfgScale, "5.5") { genCfgScale = it }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 使用工作流宽高选项（所有工作流都显示）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useWorkflowDimensions,
                        onCheckedChange = { useWorkflowDimensions = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "使用工作流本身的宽高",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "勾选后不替换工作流中的宽高参数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 进度卡片
            if (isGenerating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(progress.ifBlank { "处理中..." }, style = MaterialTheme.typography.bodyMedium)
                            if (progressPercent > 0f) Text("${(progressPercent * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        estimatedTime?.let { Spacer(modifier = Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (progressPercent > 0f) LinearProgressIndicator(progress = { progressPercent }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 生成/停止按钮
            Button(
                onClick = {
                    if (isGenerating) {
                        viewModel.stopGeneration {
                            Toast.makeText(context, "已停止生成", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (promptData == null) {
                            Toast.makeText(context, "没有提示词数据", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (workflowType == "img2img" && selectedInputImages.isEmpty()) {
                            Toast.makeText(context, "请选择输入图片", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.generateImage(
                            promptId = promptData.prompt.id,
                            positivePrompt = promptData.prompt.positivePrompt,
                            negativePrompt = promptData.prompt.negativePrompt,
                            seed = seed.toLongOrNull() ?: -1L,
                            workflowId = selectedWorkflowId,
                            batchSize = batchSize.toIntOrNull() ?: 1,
                            samplerName = selectedSampler,
                            scheduler = selectedScheduler,
                            width = genWidth.toIntOrNull() ?: 896,
                            height = genHeight.toIntOrNull() ?: 1088,
                            steps = genSteps.toIntOrNull() ?: 30,
                            cfgScale = genCfgScale.toDoubleOrNull() ?: 5.5,
                            ksamplerName = selectedKSampler,
                            kscheduler = selectedKScheduler,
                            inputImageUris = selectedInputImages,
                            denoise = denoiseStrength.toDouble(),
                            useWorkflowDimensions = useWorkflowDimensions,
                            artistPrompt = promptData.prompt.artistPrompt,
                            baseModel = selectedBaseModel,
                            loraConfigs = viewModel.exportLoraConfigsToJson(),
                            resolution = if (hasSizePicker) selectedResolution else null,
                            onSuccess = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = isGenerating || promptData != null,
                colors = if (isGenerating) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                         else ButtonDefaults.buttonColors()
            ) {
                if (isGenerating) {
                    Text("停止生成")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始生成")
                }
            }

            // 显示生成的图片
            if (generatedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "生成的图片",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = generatedImages, key = { it }) { imagePath ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        // 点击打开预览
                                        previewImagePath = imagePath
                                        showImagePreview = true
                                    },
                                    onLongClick = {
                                        // 长按显示保存确认对话框
                                        selectedImagePath = imagePath
                                        showSaveDialog = true
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            AsyncImage(
                                model = java.io.File(imagePath),
                                contentDescription = "生成的图片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun RowScope.NumberField(
    label: String,
    value: String,
    default: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (value != default) {
                TextButton(
                    onClick = { onValueChange(default) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("默认", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { v ->
                val filtered = v.filter { it.isDigit() || it == '.' }
                onValueChange(filtered)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
