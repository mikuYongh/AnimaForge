package com.aiphoto.manager.ui.screen.detail

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiphoto.manager.ui.component.ArtistPreviewDialog
import com.aiphoto.manager.ui.component.AuraParticlesBackground
import com.aiphoto.manager.ui.component.MediaPreviewDialog
import com.aiphoto.manager.ui.component.TagChip
import com.aiphoto.manager.ui.theme.LocalAppColorSet
import com.aiphoto.manager.ui.theme.tagColorFor
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    promptId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToGenerate: (String) -> Unit,
    onNavigateToCloned: (String) -> Unit = {},
    viewModel: DetailViewModel = viewModel()
) {
    val promptData by viewModel.promptData.collectAsState()
    val favoritePrompts by viewModel.favoritePrompts.collectAsState()
    val colorSet = LocalAppColorSet.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var previewArtistTag by remember { mutableStateOf<String?>(null) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(promptId) {
        viewModel.loadPrompt(promptId)
        viewModel.loadFavoritePrompts()
    }

    val prompt = promptData?.prompt
    val tags = promptData?.tags ?: emptyList()
    val images = promptData?.images ?: emptyList()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除提示词", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这个提示词吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePrompt(onNavigateBack)
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 画师预览弹窗
    previewArtistTag?.let { artist ->
        ArtistPreviewDialog(
            artistTag = artist,
            onDismiss = { previewArtistTag = null }
        )
    }

    // 图片预览弹窗
    previewImageUri?.let { uri ->
        MediaPreviewDialog(
            mediaUri = uri,
            isVideo = false,
            onDismiss = { previewImageUri = null }
        )
    }

    AuraParticlesBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Transparent background to show particles
            topBar = {
                TopAppBar(
                    title = { Text(prompt?.title ?: "加载中...", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        prompt?.let { p ->
                            IconButton(onClick = { onNavigateToGenerate(p.id) }) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "生成",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.clonePrompt { clonedId ->
                                    onNavigateToCloned(clonedId)
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "克隆")
                            }
                            IconButton(onClick = { onNavigateToEdit(p.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            prompt?.let { p ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 图片轮播
                    if (images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(images.size) { index ->
                                AsyncImage(
                                    model = File(images[index].imagePath),
                                    contentDescription = "参考图片",
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            previewImageUri = Uri.fromFile(File(images[index].imagePath))
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 描述
                    if (p.description.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = p.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 标签
                    if (tags.isNotEmpty()) {
                        Text(
                            text = "标签",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                TagChip(
                                    text = tag.name,
                                    color = tagColorFor(tag.name),
                                    isSelected = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 正向提示词
                    if (p.positivePrompt.isNotBlank()) {
                        PromptChipsSection(
                            title = "正向提示词",
                            prompts = p.positivePrompt.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            color = colorSet.promptChipPositive,
                            promptType = "positive",
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(p.positivePrompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制！")
                                }
                            },
                            onCopySingle = { prompt ->
                                clipboardManager.setText(AnnotatedString(prompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制: $prompt")
                                }
                            },
                            onFavorite = { content, type ->
                                viewModel.toggleFavoritePrompt(content, type)
                            },
                            favoritePrompts = favoritePrompts
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 负向提示词
                    if (p.negativePrompt.isNotBlank()) {
                        PromptChipsSection(
                            title = "负向提示词",
                            prompts = p.negativePrompt.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            color = colorSet.promptChipNegative,
                            promptType = "negative",
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(p.negativePrompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制！")
                                }
                            },
                            onCopySingle = { prompt ->
                                clipboardManager.setText(AnnotatedString(prompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制: $prompt")
                                }
                            },
                            onFavorite = { content, type ->
                                viewModel.toggleFavoritePrompt(content, type)
                            },
                            favoritePrompts = favoritePrompts
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 画师 - 支持点击查看作品
                    if (p.artistPrompt.isNotBlank()) {
                        PromptChipsSection(
                            title = "画师",
                            prompts = p.artistPrompt.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            color = colorSet.promptChipArtist,
                            promptType = "artist",
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(p.artistPrompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制！")
                                }
                            },
                            onCopySingle = { prompt ->
                                clipboardManager.setText(AnnotatedString(prompt))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制: $prompt")
                                }
                            },
                            onFavorite = { content, type ->
                                viewModel.toggleFavoritePrompt(content, type)
                            },
                            onArtistClick = { artistTag ->
                                previewArtistTag = artistTag
                            },
                            favoritePrompts = favoritePrompts
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 种子值和参数
                    if (p.seed.isNotBlank() || p.parameters.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (p.seed.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "种子值: ",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            p.seed,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (p.parameters.isNotBlank()) {
                                    if (p.seed.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        "参数:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        p.parameters,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PromptChipsSection(
    title: String,
    prompts: List<String>,
    color: String,
    promptType: String,
    onCopy: () -> Unit,
    onCopySingle: (String) -> Unit,
    onFavorite: (String, String) -> Unit = { _, _ -> },
    onArtistClick: ((String) -> Unit)? = null,
    favoritePrompts: Set<String> = emptySet()
) {
    val isArtist = promptType == "artist"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onCopy
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isArtist) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "点击查看作品",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${prompts.size} 个",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prompts.forEach { prompt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        TagChip(
                            text = prompt,
                            color = color,
                            isSelected = false,
                            onClick = if (isArtist && onArtistClick != null) {
                                { onArtistClick(prompt) }
                            } else null,
                            onLongClick = {
                                onCopySingle(prompt)
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { onFavorite(prompt, promptType) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (prompt in favoritePrompts) Icons.Default.Favorite
                                else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (prompt in favoritePrompts) "取消收藏" else "收藏",
                                modifier = Modifier.size(16.dp),
                                tint = if (prompt in favoritePrompts)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
