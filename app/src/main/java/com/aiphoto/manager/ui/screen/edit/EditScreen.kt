package com.aiphoto.manager.ui.screen.edit

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.ui.component.ImagePicker
import com.aiphoto.manager.ui.component.TagChip
import com.aiphoto.manager.ui.component.TagInput

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    existingPrompt: PromptWithTags? = null,
    initialPositivePrompt: String = "",
    initialNegativePrompt: String = "",
    initialTags: List<String> = emptyList(),
    onNavigateBack: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val context = LocalContext.current
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val imagePaths by viewModel.imagePaths.collectAsState()
    val comfyUiUrl by viewModel.comfyUiUrl.collectAsState()
    val width by viewModel.width.collectAsState()
    val height by viewModel.height.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val cfgScale by viewModel.cfgScale.collectAsState()
    val favoritePositivePrompts by viewModel.favoritePositivePrompts.collectAsState()
    val favoriteNegativePrompts by viewModel.favoriteNegativePrompts.collectAsState()

    // 提示词列表状态
    var positiveSingleInput by remember { mutableStateOf("") }
    var positiveBatchInput by remember { mutableStateOf("") }
    var negativeSingleInput by remember { mutableStateOf("") }
    var negativeBatchInput by remember { mutableStateOf("") }
    val positivePromptList = remember { mutableStateListOf<String>() }
    val negativePromptList = remember { mutableStateListOf<String>() }

    // 设置对话框
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showFavoritePositiveDialog by remember { mutableStateOf(false) }
    var showFavoriteNegativeDialog by remember { mutableStateOf(false) }
    val selectedFavoritePositive = remember { mutableStateListOf<String>() }
    val selectedFavoriteNegative = remember { mutableStateListOf<String>() }

    // 加载现有数据
    LaunchedEffect(existingPrompt) {
        if (existingPrompt != null) {
            viewModel.loadForEdit(existingPrompt)
            if (existingPrompt.prompt.positivePrompt.isNotBlank()) {
                positivePromptList.clear()
                positivePromptList.addAll(parsePrompts(existingPrompt.prompt.positivePrompt))
            }
            if (existingPrompt.prompt.negativePrompt.isNotBlank()) {
                negativePromptList.clear()
                negativePromptList.addAll(parsePrompts(existingPrompt.prompt.negativePrompt))
            }
        } else if (initialPositivePrompt.isNotBlank()) {
            positivePromptList.addAll(parsePrompts(initialPositivePrompt))
            if (initialNegativePrompt.isNotBlank()) {
                negativePromptList.addAll(parsePrompts(initialNegativePrompt))
            }
            initialTags.forEach { viewModel.addTag(it) }
        } else {
            // 新建提示词时，添加默认的负向提示词
            val defaultNegativePrompt = "lazyneg, lazyhand, censored, mosaic censoring, photorealistic, realistic, artist name, signature, lowres, bad anatomy, bad hands, text, error, missing fingers, extra fingers, fewer digits, cropped, worst quality, low quality, jpeg artifacts, watermark, username, sketch, jpeg Closed eyes, artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, shiny clothes, shiny skin, gold skin, white hair, halo,three hands"
            negativePromptList.addAll(parsePrompts(defaultNegativePrompt))
        }
    }

    // 加载收藏的提示词
    LaunchedEffect(Unit) {
        viewModel.loadFavoritePrompts()
    }

    // 设置对话框
    if (showSettingsDialog) {
        SettingsDialog(
            currentUrl = comfyUiUrl,
            onDismiss = { showSettingsDialog = false },
            onSave = { url ->
                viewModel.saveComfyUiUrl(url)
                showSettingsDialog = false
            }
        )
    }

    // 收藏正向提示词对话框
    if (showFavoritePositiveDialog) {
        FavoritePromptDialog(
            title = "选择正向提示词",
            favoritePrompts = favoritePositivePrompts,
            selectedPrompts = selectedFavoritePositive,
            onDismiss = {
                showFavoritePositiveDialog = false
                selectedFavoritePositive.clear()
            },
            onConfirm = {
                selectedFavoritePositive.forEach { prompt ->
                    if (positivePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                        positivePromptList.add(prompt)
                    }
                }
                showFavoritePositiveDialog = false
                selectedFavoritePositive.clear()
            }
        )
    }

    // 收藏负向提示词对话框
    if (showFavoriteNegativeDialog) {
        FavoritePromptDialog(
            title = "选择负向提示词",
            favoritePrompts = favoriteNegativePrompts,
            selectedPrompts = selectedFavoriteNegative,
            onDismiss = {
                showFavoriteNegativeDialog = false
                selectedFavoriteNegative.clear()
            },
            onConfirm = {
                selectedFavoriteNegative.forEach { prompt ->
                    if (negativePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                        negativePromptList.add(prompt)
                    }
                }
                showFavoriteNegativeDialog = false
                selectedFavoriteNegative.clear()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingPrompt != null) "编辑提示词" else "新建提示词") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(
                        onClick = {
                            viewModel.onPositivePromptChange(positivePromptList.joinToString(", "))
                            viewModel.onNegativePromptChange(negativePromptList.joinToString(", "))
                            viewModel.savePrompt(onNavigateBack)
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("标题 *") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 描述
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("描述") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 标签
            TagInput(
                tags = selectedTags,
                allTags = allTags,
                onTagAdded = viewModel::addTag,
                onTagRemoved = viewModel::removeTag
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 参考图片
            Text(
                "参考图片",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ImagePicker(
                imageUris = imagePaths.map { Uri.parse("file://$it") },
                onImagesSelected = { uris -> viewModel.addImages(uris) },
                onImageRemoved = viewModel::removeImage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 正向提示词输入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "正向提示词",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showFavoritePositiveDialog = true }) {
                    Text("收藏 (${favoritePositivePrompts.size})")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 单个添加输入框
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = positiveSingleInput,
                    onValueChange = { positiveSingleInput = it },
                    placeholder = { Text("输入单个提示词后添加") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalButton(
                    onClick = {
                        if (positiveSingleInput.isNotBlank()) {
                            val trimmed = positiveSingleInput.trim()
                            if (positivePromptList.none { it.equals(trimmed, ignoreCase = true) }) {
                                positivePromptList.add(trimmed)
                            }
                            positiveSingleInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 批量解析输入框
            OutlinedTextField(
                value = positiveBatchInput,
                onValueChange = { positiveBatchInput = it },
                label = { Text("或批量粘贴（逗号分隔）") },
                placeholder = { Text("粘贴多个提示词，用逗号分隔...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            if (positiveBatchInput.isNotBlank()) {
                                val parsed = parsePrompts(positiveBatchInput)
                                parsed.forEach { prompt ->
                                    if (positivePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                                        positivePromptList.add(prompt)
                                    }
                                }
                                positiveBatchInput = ""
                            }
                        }
                    ) {
                        Text("解析", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            // 正向提示词芯片显示
            if (positivePromptList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${positivePromptList.size} 个提示词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { positivePromptList.clear() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    positivePromptList.forEachIndexed { index, prompt ->
                        TagChip(
                            text = prompt,
                            color = "#FFC0D0",
                            isSelected = false,
                            onRemove = { positivePromptList.removeAt(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 负向提示词输入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "负向提示词",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showFavoriteNegativeDialog = true }) {
                    Text("收藏 (${favoriteNegativePrompts.size})")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 单个添加输入框
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = negativeSingleInput,
                    onValueChange = { negativeSingleInput = it },
                    placeholder = { Text("输入单个提示词后添加") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalButton(
                    onClick = {
                        if (negativeSingleInput.isNotBlank()) {
                            val trimmed = negativeSingleInput.trim()
                            if (negativePromptList.none { it.equals(trimmed, ignoreCase = true) }) {
                                negativePromptList.add(trimmed)
                            }
                            negativeSingleInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 批量解析输入框
            OutlinedTextField(
                value = negativeBatchInput,
                onValueChange = { negativeBatchInput = it },
                label = { Text("或批量粘贴（逗号分隔）") },
                placeholder = { Text("粘贴多个提示词，用逗号分隔...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            if (negativeBatchInput.isNotBlank()) {
                                val parsed = parsePrompts(negativeBatchInput)
                                parsed.forEach { prompt ->
                                    if (negativePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                                        negativePromptList.add(prompt)
                                    }
                                }
                                negativeBatchInput = ""
                            }
                        }
                    ) {
                        Text("解析", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            // 负向提示词芯片显示
            if (negativePromptList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${negativePromptList.size} 个提示词",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { negativePromptList.clear() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    negativePromptList.forEachIndexed { index, prompt ->
                        TagChip(
                            text = prompt,
                            color = "#D8B4FE",
                            isSelected = false,
                            onRemove = { negativePromptList.removeAt(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 标题栏（可点击展开/收起）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "更多设置",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { showAdvancedSettings = !showAdvancedSettings },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAdvancedSettings) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 展开的内容
                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // 宽度
                        OutlinedTextField(
                            value = width.toString(),
                            onValueChange = {
                                val value = it.toIntOrNull()
                                if (value != null && value > 0) {
                                    viewModel.onWidthChange(value)
                                }
                            },
                            label = { Text("宽度 (Width)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 高度
                        OutlinedTextField(
                            value = height.toString(),
                            onValueChange = {
                                val value = it.toIntOrNull()
                                if (value != null && value > 0) {
                                    viewModel.onHeightChange(value)
                                }
                            },
                            label = { Text("高度 (Height)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 步数
                        OutlinedTextField(
                            value = steps.toString(),
                            onValueChange = {
                                val value = it.toIntOrNull()
                                if (value != null && value > 0) {
                                    viewModel.onStepsChange(value)
                                }
                            },
                            label = { Text("步数 (Steps)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CFG Scale
                        OutlinedTextField(
                            value = cfgScale.toString(),
                            onValueChange = {
                                val value = it.toDoubleOrNull()
                                if (value != null && value > 0) {
                                    viewModel.onCfgScaleChange(value)
                                }
                            },
                            label = { Text("CFG Scale") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    viewModel.onPositivePromptChange(positivePromptList.joinToString(", "))
                    viewModel.onNegativePromptChange(negativePromptList.joinToString(", "))
                    viewModel.savePrompt(onNavigateBack)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("保存提示词")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ComfyUI 设置") },
        text = {
            Column {
                Text(
                    "服务器地址",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("http://192.168.123.178:8188") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请确保 ComfyUI 正在运行，且可以通过此地址访问",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritePromptDialog(
    title: String,
    favoritePrompts: List<String>,
    selectedPrompts: MutableList<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // 根据标题判断是正向还是负向提示词
    val isPositive = title.contains("正向")
    val selectedColor = if (isPositive) "#FF6B9D" else "#C084FC"
    val unselectedColor = if (isPositive) "#FFC0D0" else "#D8B4FE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title)
                if (selectedPrompts.isNotEmpty()) {
                    Text(
                        "已选 ${selectedPrompts.size} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            if (favoritePrompts.isEmpty()) {
                Text(
                    "暂无收藏的提示词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "点击选择要添加的提示词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        favoritePrompts.forEach { prompt ->
                            val isSelected = prompt in selectedPrompts
                            TagChip(
                                text = prompt,
                                color = if (isSelected) selectedColor else unselectedColor,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedPrompts.remove(prompt)
                                    } else {
                                        selectedPrompts.add(prompt)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedPrompts.isNotEmpty()
            ) {
                Text(
                    if (selectedPrompts.isEmpty()) "确认"
                    else "添加 ${selectedPrompts.size} 个"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun parsePrompts(text: String): List<String> {
    return text.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}
