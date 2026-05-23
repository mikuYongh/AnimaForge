package com.aiphoto.manager.ui.screen.edit

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.ui.component.ArtistPickerDialog
import com.aiphoto.manager.ui.component.ArtistPreviewDialog
import com.aiphoto.manager.ui.component.ImagePicker
import com.aiphoto.manager.ui.component.TagChip
import kotlinx.coroutines.launch
import com.aiphoto.manager.ui.component.TagInput
import com.aiphoto.manager.ui.theme.LocalAppColorSet
import com.aiphoto.manager.ui.component.AuraParticlesBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import com.aiphoto.manager.api.ArtistApiClient
import com.aiphoto.manager.data.model.Artist
import androidx.compose.ui.window.DialogProperties

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
    val colorSet = LocalAppColorSet.current
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
    val favoriteArtistPrompts by viewModel.favoriteArtistPrompts.collectAsState()
    val artistList by viewModel.artistList.collectAsState()
    val artistListLoading by viewModel.artistListLoading.collectAsState()

    var positiveSingleInput by remember { mutableStateOf("") }
    var positiveBatchInput by remember { mutableStateOf("") }
    var negativeSingleInput by remember { mutableStateOf("") }
    var negativeBatchInput by remember { mutableStateOf("") }
    val positivePromptList = remember { mutableStateListOf<String>() }
    val negativePromptList = remember { mutableStateListOf<String>() }
    val artistPromptList = remember { mutableStateListOf<String>() }
    var artistSingleInput by remember { mutableStateOf("") }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFavoritePositiveDialog by remember { mutableStateOf(false) }
    var showFavoriteNegativeDialog by remember { mutableStateOf(false) }
    var showFavoriteArtistDialog by remember { mutableStateOf(false) }
    var showArtistPickerDialog by remember { mutableStateOf(false) }
    val selectedFavoritePositive = remember { mutableStateListOf<String>() }
    val selectedFavoriteNegative = remember { mutableStateListOf<String>() }
    val selectedFavoriteArtist = remember { mutableStateListOf<String>() }

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
            if (existingPrompt.prompt.artistPrompt.isNotBlank()) {
                artistPromptList.clear()
                artistPromptList.addAll(parsePrompts(existingPrompt.prompt.artistPrompt))
            }
        } else if (initialPositivePrompt.isNotBlank()) {
            positivePromptList.addAll(parsePrompts(initialPositivePrompt))
            if (initialNegativePrompt.isNotBlank()) {
                negativePromptList.addAll(parsePrompts(initialNegativePrompt))
            }
            initialTags.forEach { viewModel.addTag(it) }
        } else {
            val defaultNegativePrompt = "lazyneg, lazyhand, censored, mosaic censoring, photorealistic, realistic, artist name, signature, lowres, bad anatomy, bad hands, text, error, missing fingers, extra fingers, fewer digits, cropped, worst quality, low quality, jpeg artifacts, watermark, username, sketch, jpeg Closed eyes, artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, shiny clothes, shiny skin, gold skin, white hair, halo,three hands"
            negativePromptList.addAll(parsePrompts(defaultNegativePrompt))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFavoritePrompts()
    }

    // 对话框
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
    if (showFavoritePositiveDialog) {
        FavoritePromptDialog(
            title = "选择正向提示词",
            favoritePrompts = favoritePositivePrompts,
            selectedPrompts = selectedFavoritePositive,
            onDismiss = { showFavoritePositiveDialog = false; selectedFavoritePositive.clear() },
            onConfirm = {
                selectedFavoritePositive.forEach { prompt ->
                    if (positivePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                        positivePromptList.add(prompt)
                    }
                }
                showFavoritePositiveDialog = false; selectedFavoritePositive.clear()
            }
        )
    }
    if (showFavoriteNegativeDialog) {
        FavoritePromptDialog(
            title = "选择负向提示词",
            favoritePrompts = favoriteNegativePrompts,
            selectedPrompts = selectedFavoriteNegative,
            onDismiss = { showFavoriteNegativeDialog = false; selectedFavoriteNegative.clear() },
            onConfirm = {
                selectedFavoriteNegative.forEach { prompt ->
                    if (negativePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                        negativePromptList.add(prompt)
                    }
                }
                showFavoriteNegativeDialog = false; selectedFavoriteNegative.clear()
            }
        )
    }
    if (showFavoriteArtistDialog) {
        FavoriteArtistDialog(
            artists = artistList,
            favoriteArtistPrompts = favoriteArtistPrompts,
            selectedArtists = selectedFavoriteArtist,
            onToggleFavorite = { artistTag -> viewModel.toggleFavoritePrompt(artistTag, "artist") },
            onDismiss = { showFavoriteArtistDialog = false; selectedFavoriteArtist.clear() },
            onConfirm = {
                selectedFavoriteArtist.forEach { prompt ->
                    if (artistPromptList.none { it.equals(prompt, ignoreCase = true) }) {
                        artistPromptList.add(prompt)
                    }
                }
                showFavoriteArtistDialog = false; selectedFavoriteArtist.clear()
            }
        )
    }
    if (showArtistPickerDialog) {
        ArtistPickerDialog(
            artists = artistList,
            isLoading = artistListLoading,
            selectedArtists = artistPromptList,
            favoriteArtists = favoriteArtistPrompts.toSet(),
            onToggleFavorite = { artistTag -> viewModel.toggleFavoritePrompt(artistTag, "artist") },
            onDismiss = { showArtistPickerDialog = false },
            onConfirm = { showArtistPickerDialog = false },
            searchQuery = viewModel.artistSearchQuery,
            onSearchChange = { viewModel.artistSearchQuery = it },
            sortByPostCount = viewModel.artistSortByPostCount,
            onSortChange = { viewModel.artistSortByPostCount = it },
            visibleCount = viewModel.artistVisibleCount,
            onVisibleCountChange = { viewModel.artistVisibleCount = it }
        )
    }

    AuraParticlesBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (existingPrompt != null) "编辑提示词" else "新建提示词", fontWeight = FontWeight.SemiBold) },
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
                                viewModel.onArtistPromptChange(artistPromptList.joinToString(", "))
                                viewModel.savePrompt(onNavigateBack)
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "保存",
                                tint = if (title.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
            // 基本信息卡片
            SectionHeader(icon = Icons.Default.Info, title = "基本信息")
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("标题 *") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("描述") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标签
            SectionHeader(icon = Icons.AutoMirrored.Filled.Label, title = "标签")
            TagInput(
                tags = selectedTags,
                allTags = allTags,
                onTagAdded = viewModel::addTag,
                onTagRemoved = viewModel::removeTag
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 参考图片
            SectionHeader(icon = Icons.Default.AddPhotoAlternate, title = "参考图片")
            Spacer(modifier = Modifier.height(4.dp))
            ImagePicker(
                imageUris = imagePaths.map { Uri.parse("file://$it") },
                onImagesSelected = { uris -> viewModel.addImages(uris) },
                onImageRemoved = viewModel::removeImage
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 正向提示词
            PromptSectionCard(
                icon = Icons.Default.AutoAwesome,
                title = "正向提示词",
                gradientColors = listOf("#4A90D9", "#6EC6F8"),
                chipColor = colorSet.promptChipPositive,
                promptList = positivePromptList,
                singleInput = positiveSingleInput,
                batchInput = positiveBatchInput,
                favoriteCount = favoritePositivePrompts.size,
                onSingleInputChange = { positiveSingleInput = it },
                onBatchInputChange = { positiveBatchInput = it },
                onAddSingle = {
                    if (positiveSingleInput.isNotBlank()) {
                        val trimmed = positiveSingleInput.trim()
                        if (positivePromptList.none { it.equals(trimmed, ignoreCase = true) }) {
                            positivePromptList.add(trimmed)
                        }
                        positiveSingleInput = ""
                    }
                },
                onParseBatch = {
                    if (positiveBatchInput.isNotBlank()) {
                        parsePrompts(positiveBatchInput).forEach { prompt ->
                            if (positivePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                                positivePromptList.add(prompt)
                            }
                        }
                        positiveBatchInput = ""
                    }
                },
                onClear = { positivePromptList.clear() },
                onShowFavorites = { showFavoritePositiveDialog = true },
                onRemoveItem = { positivePromptList.removeAt(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 负向提示词
            PromptSectionCard(
                icon = Icons.Default.BookmarkAdd,
                title = "负向提示词",
                gradientColors = listOf("#B0A0D0", "#C084FC"),
                chipColor = colorSet.promptChipNegative,
                promptList = negativePromptList,
                singleInput = negativeSingleInput,
                batchInput = negativeBatchInput,
                favoriteCount = favoriteNegativePrompts.size,
                onSingleInputChange = { negativeSingleInput = it },
                onBatchInputChange = { negativeBatchInput = it },
                onAddSingle = {
                    if (negativeSingleInput.isNotBlank()) {
                        val trimmed = negativeSingleInput.trim()
                        if (negativePromptList.none { it.equals(trimmed, ignoreCase = true) }) {
                            negativePromptList.add(trimmed)
                        }
                        negativeSingleInput = ""
                    }
                },
                onParseBatch = {
                    if (negativeBatchInput.isNotBlank()) {
                        parsePrompts(negativeBatchInput).forEach { prompt ->
                            if (negativePromptList.none { it.equals(prompt, ignoreCase = true) }) {
                                negativePromptList.add(prompt)
                            }
                        }
                        negativeBatchInput = ""
                    }
                },
                onClear = { negativePromptList.clear() },
                onShowFavorites = { showFavoriteNegativeDialog = true },
                onRemoveItem = { negativePromptList.removeAt(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 画师
            PromptSectionCard(
                icon = Icons.Default.Person,
                title = "画师",
                gradientColors = listOf("#6EC6F8", "#22D3EE"),
                chipColor = colorSet.promptChipArtist,
                promptList = artistPromptList,
                singleInput = artistSingleInput,
                batchInput = "",
                favoriteCount = favoriteArtistPrompts.size,
                onSingleInputChange = { artistSingleInput = it },
                onBatchInputChange = { },
                onAddSingle = {
                    if (artistSingleInput.isNotBlank()) {
                        val trimmed = artistSingleInput.trim()
                        if (artistPromptList.none { it.equals(trimmed, ignoreCase = true) }) {
                            artistPromptList.add(trimmed)
                        }
                        artistSingleInput = ""
                    }
                },
                onParseBatch = { },
                onClear = { artistPromptList.clear() },
                onShowFavorites = {
                    viewModel.loadArtistList()
                    showFavoriteArtistDialog = true
                },
                onRemoveItem = { artistPromptList.removeAt(it) },
                extraActions = {
                    TextButton(onClick = {
                        viewModel.loadArtistList()
                        showArtistPickerDialog = true
                    }) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("画师库")
                    }
                },
                chipLabelSingular = "画师",
                chipLabelPlural = "个画师"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 保存按钮
            Button(
                onClick = {
                    viewModel.onPositivePromptChange(positivePromptList.joinToString(", "))
                    viewModel.onNegativePromptChange(negativePromptList.joinToString(", "))
                    viewModel.onArtistPromptChange(artistPromptList.joinToString(", "))
                    viewModel.savePrompt(onNavigateBack)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存提示词", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

// ==================== 辅助组件 ====================

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    gradientColors: List<String>,
    chipColor: String,
    promptList: List<String>,
    singleInput: String,
    batchInput: String,
    favoriteCount: Int,
    onSingleInputChange: (String) -> Unit,
    onBatchInputChange: (String) -> Unit,
    onAddSingle: () -> Unit,
    onParseBatch: () -> Unit,
    onClear: () -> Unit,
    onShowFavorites: () -> Unit,
    onRemoveItem: (Int) -> Unit = {},
    extraActions: (@Composable () -> Unit)? = null,
    chipLabelSingular: String = "提示词",
    chipLabelPlural: String = "个提示词"
) {
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    if (extraActions != null) {
                        extraActions()
                    }
                    TextButton(onClick = onShowFavorites) {
                        Text("收藏 ($favoriteCount)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 单个添加
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = singleInput,
                    onValueChange = onSingleInputChange,
                    placeholder = { Text("输入后添加") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalButton(
                    onClick = onAddSingle,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("添加")
                }
            }

            // 批量粘贴（仅非画师section显示）
            if (batchInput.isNotEmpty() || onParseBatch.toString() != "{}") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = batchInput,
                    onValueChange = onBatchInputChange,
                    label = { Text("或批量粘贴（逗号分隔）") },
                    placeholder = { Text("粘贴多个提示词...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        TextButton(onClick = onParseBatch) {
                            Text("解析", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }

            // 提示词芯片
            if (promptList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${promptList.size} $chipLabelPlural",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onClear,
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
                    promptList.forEachIndexed { index, prompt ->
                        TagChip(
                            text = prompt,
                            color = chipColor,
                            isSelected = false,
                            onRemove = { onRemoveItem(index) }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 对话框 ====================

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
    val isPositive = title.contains("正向")
    val isArtist = title.contains("画师")
    val appColorSet = LocalAppColorSet.current
    val selectedColor = if (isPositive) appColorSet.promptChipPositive else appColorSet.promptChipNegative
    val unselectedColor = if (isPositive) "#B3D8F7" else "#DDD6FE"
    var previewArtistTag by remember { mutableStateOf<String?>(null) }

    if (previewArtistTag != null) {
        ArtistPreviewDialog(
            artistTag = previewArtistTag!!,
            onDismiss = { previewArtistTag = null }
        )
    }

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
                                },
                                onLongClick = if (isArtist) {{ previewArtistTag = prompt }} else null
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun FavoriteArtistDialog(
    artists: List<Artist>,
    favoriteArtistPrompts: List<String>,
    selectedArtists: MutableList<String>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val appColorSet = LocalAppColorSet.current
    val highlightColor = if (appColorSet.promptChipPositive.startsWith("#")) {
        Color(android.graphics.Color.parseColor(appColorSet.promptChipPositive))
    } else {
        MaterialTheme.colorScheme.primary
    }

    var searchQuery by remember { mutableStateOf("") }
    var previewArtistTag by remember { mutableStateOf<String?>(null) }

    // Look up the Artist details for each favorite tag and apply filter
    val favoriteArtistObjects = remember(favoriteArtistPrompts, artists, searchQuery) {
        val list = favoriteArtistPrompts.map { tag ->
            artists.find { it.tag.equals(tag, ignoreCase = true) } ?: Artist(
                tag = tag,
                slug = tag,
                imageId = "",
                hasImage = false,
                postCount = 0,
                shard = ""
            )
        }
        if (searchQuery.isBlank()) list
        else list.filter {
            it.tag.contains(searchQuery, ignoreCase = true) ||
                it.slug.contains(searchQuery, ignoreCase = true)
        }
    }

    // 唤起画师详细作品预览弹窗
    previewArtistTag?.let { artistTag ->
        ArtistPreviewDialog(
            artistTag = artistTag,
            onDismiss = { previewArtistTag = null }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            highlightColor.copy(alpha = 0.5f),
                            highlightColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
        ) {
            // 背景粒子及模糊图层
            AuraParticlesBackground {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 取消按钮 (圆形)
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "画师收藏夹",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "共收藏 ${favoriteArtistPrompts.size} 位画师" +
                                            if (selectedArtists.isNotEmpty()) " · 已选 ${selectedArtists.size} 个" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 确认/添加按钮 (圆形带渐变)
                                IconButton(
                                    onClick = onConfirm,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    appColorSet.btnGradientStart,
                                                    appColorSet.btnGradientEnd
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "添加",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 搜索栏 (只有在有收藏时才显示搜索栏)
                            if (favoriteArtistPrompts.isNotEmpty()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("搜索已收藏画师...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 48.dp)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (favoriteArtistPrompts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = highlightColor.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "暂无收藏的画师",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "可以在画师选择列表中点击爱心收藏",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (favoriteArtistObjects.isEmpty()) {
                            // 搜索无结果
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有找到符合搜索条件的画师",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(favoriteArtistObjects, key = { it.tag }) { artist ->
                                    val isSelected = artist.tag in selectedArtists

                                    // 收藏卡片设计
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            } else {
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) highlightColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelected) {
                                                        selectedArtists.remove(artist.tag)
                                                    } else {
                                                        selectedArtists.add(artist.tag)
                                                    }
                                                },
                                                onLongClick = {
                                                    previewArtistTag = artist.tag
                                                },
                                                onDoubleClick = {
                                                    previewArtistTag = artist.tag
                                                }
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // 头像与取消收藏按钮组合
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (artist.hasImage) {
                                                    AsyncImage(
                                                        model = ArtistApiClient.getImageUrl(artist.imageId),
                                                        contentDescription = artist.slug,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = artist.tag,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) highlightColor else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 人气
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Whatshot,
                                                        contentDescription = "热度",
                                                        modifier = Modifier.size(10.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "${artist.postCount}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 9.sp
                                                    )
                                                }

                                                // 取消收藏红心按钮 (点击垃圾桶或红心取消收藏)
                                                IconButton(
                                                    onClick = { onToggleFavorite(artist.tag) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "取消收藏",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color(0xFFFF2D55)
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
            }
        }
    }
}

private fun parsePrompts(text: String): List<String> {
    return text.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}


