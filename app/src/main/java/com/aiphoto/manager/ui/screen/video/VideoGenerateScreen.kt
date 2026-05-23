package com.aiphoto.manager.ui.screen.video

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import com.aiphoto.manager.ui.component.AuraParticlesBackground
import com.aiphoto.manager.ui.component.MediaPreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGenerateScreen(
    imagePath: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: VideoGenerateViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val context = LocalContext.current
    val workflows by viewModel.workflows.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressPercent by viewModel.progressPercent.collectAsState()
    val generatedVideos by viewModel.generatedVideos.collectAsState()
    val estimatedTime by viewModel.estimatedTime.collectAsState()

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(if (imagePath != null) listOf(Uri.parse("file://$imagePath")) else emptyList()) }
    var prompt by remember { mutableStateOf(viewModel.lastPrompt) }
    var selectedWorkflowId by remember { mutableStateOf<String?>(null) }
    var fps by remember { mutableStateOf(viewModel.lastFps.toString()) }
    var duration by remember { mutableStateOf(viewModel.lastDuration.toString()) }
    var vidWidth by remember { mutableStateOf(viewModel.lastWidth.toString()) }
    var vidHeight by remember { mutableStateOf(viewModel.lastHeight.toString()) }
    var batchCount by remember { mutableStateOf("1") }
    var previewVideoUri by remember { mutableStateOf<Uri?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUris = listOf(it)
            detectImageSize(context, it) { w, h ->
                vidWidth = w.toString()
                vidHeight = h.toString()
            }
        }
    }

    LaunchedEffect(selectedImageUris) {
        if (selectedImageUris.isNotEmpty()) {
            detectImageSize(context, selectedImageUris.first()) { w, h ->
                vidWidth = w.toString()
                vidHeight = h.toString()
            }
        }
    }

    LaunchedEffect(workflows) {
        if (selectedWorkflowId == null && workflows.isNotEmpty()) {
            selectedWorkflowId = workflows.first().id
        }
    }

    AuraParticlesBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("图生视频") },
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
                // 图片预览
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                    if (selectedImageUris.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUris.first(),
                                contentDescription = "输入图片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            FilledTonalButton(
                                onClick = { imageLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("更换图片")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledTonalButton(onClick = { imageLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("选择图片")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 提示词
                Text("画面描述", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("描述视频画面的内容...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 参数设置
                Text("视频参数", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ParamField("宽度", vidWidth) { vidWidth = it }
                            ParamField("高度", vidHeight) { vidHeight = it }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ParamField("FPS", fps) { fps = it }
                            ParamField("时长(秒)", duration) { duration = it }
                            ParamField("数量", batchCount) { batchCount = it }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 工作流选择
                Text("工作流", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                if (workflows.isEmpty()) {
                    Text("暂无图生视频工作流，请先在设置中添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        FilledTonalButton(onClick = { showMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(workflows.find { it.id == selectedWorkflowId }?.name ?: "选择工作流")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            workflows.forEach { w ->
                                DropdownMenuItem(text = { Text(w.name) }, onClick = { selectedWorkflowId = w.id; showMenu = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 进度
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
                            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) // 无数据时旋转
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 生成按钮
                Button(
                    onClick = {
                        if (isGenerating) {
                            viewModel.stopGeneration()
                        } else {
                            if (selectedImageUris.isEmpty()) {
                                Toast.makeText(context, "请选择图片", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val wid = selectedWorkflowId
                            if (wid == null) {
                                Toast.makeText(context, "请选择工作流", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.startGeneration(
                                imageUri = selectedImageUris.first(),
                                prompt = prompt,
                                workflowId = wid,
                                fps = fps.toIntOrNull() ?: 25,
                                duration = duration.toIntOrNull() ?: 5,
                                width = vidWidth.toIntOrNull() ?: 960,
                                height = vidHeight.toIntOrNull() ?: 544,
                                batchCount = batchCount.toIntOrNull() ?: 1,
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isGenerating || (selectedImageUris.isNotEmpty() && selectedWorkflowId != null),
                    colors = if (isGenerating) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                             else ButtonDefaults.buttonColors()
                ) {
                    Icon(if (isGenerating) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGenerating) "停止生成" else "生成视频")
                }

                // 生成结果网格
                if (generatedVideos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("生成结果 (${generatedVideos.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(min = 150.dp, max = 400.dp)
                    ) {
                        items(generatedVideos, key = { it }) { videoPath ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column {
                                    val thumb = remember(videoPath) {
                                        ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND)
                                    }
                                    if (thumb != null) {
                                        Image(bitmap = thumb.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(videoPath))
                                        IconButton(onClick = {
                                            previewVideoUri = Uri.fromFile(File(videoPath))
                                        }) { Icon(Icons.Default.PlayArrow, "播放", modifier = Modifier.size(20.dp)) }
                                        IconButton(onClick = {
                                            val file = File(videoPath)
                                            val destDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "AnimaForge")
                                            if (!destDir.exists()) destDir.mkdirs()
                                            val ext = file.extension.ifEmpty { "mp4" }
                                            val uniqueName = "AnimaForge_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}_${java.util.UUID.randomUUID().toString().take(8)}.$ext"
                                            val dest = File(destDir, uniqueName)
                                            file.copyTo(dest, overwrite = false)
                                            android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
                                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                                        }) { Icon(Icons.Default.Save, "保存", modifier = Modifier.size(20.dp)) }
                                        IconButton(onClick = {
                                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "video/*"; putExtra(Intent.EXTRA_STREAM, fileUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享"))
                                        }) { Icon(Icons.Default.Share, "分享", modifier = Modifier.size(20.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    previewVideoUri?.let { uri ->
        MediaPreviewDialog(
            mediaUri = uri,
            isVideo = true,
            onDismiss = { previewVideoUri = null }
        )
    }
}

private fun detectImageSize(context: android.content.Context, uri: Uri, onResult: (Int, Int) -> Unit) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            onResult(options.outWidth, options.outHeight)
        }
    } catch (_: Exception) {}
}

@Composable
private fun RowScope.ParamField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
