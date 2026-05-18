package com.aiphoto.manager.ui.screen.history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.aiphoto.manager.App
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GeneratedHistoryViewModel(
    private val generatedImageDao: com.aiphoto.manager.data.local.dao.GeneratedImageDao
) : ViewModel() {
    val images = generatedImageDao.getAllGeneratedImages()

    fun deleteImage(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            generatedImageDao.deleteGeneratedImage(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GeneratedHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GeneratedHistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = context.applicationContext as App
                GeneratedHistoryViewModel(app.database.generatedImageDao())
            }
        }
    )

    val images by viewModel.images.collectAsState(initial = emptyList())

    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    // 保存图片到相册的函数
    fun saveImageToGallery(imagePath: String) {
        try {
            val file = File(imagePath)
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
                            model = File(previewImagePath!!),
                            contentDescription = "预览图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生成历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有生成记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.id }) { image ->
                    GeneratedImageCard(
                        image = image,
                        onDelete = { viewModel.deleteImage(image.id) },
                        onClick = {
                            // 点击打开全屏预览
                            previewImagePath = image.imagePath
                            showImagePreview = true
                        },
                        onLongClick = {
                            // 长按显示保存确认对话框
                            selectedImagePath = image.imagePath
                            showSaveDialog = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GeneratedImageCard(
    image: GeneratedImageEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = File(image.imagePath),
                contentDescription = "生成的图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    dateFormat.format(Date(image.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
