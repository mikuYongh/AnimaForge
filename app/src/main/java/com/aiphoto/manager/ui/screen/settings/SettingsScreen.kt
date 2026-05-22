package com.aiphoto.manager.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiphoto.manager.ui.component.AuraParticlesBackground
import com.aiphoto.manager.ui.theme.ThemeMode
import com.aiphoto.manager.ui.theme.MikuSakuraPrimary
import com.aiphoto.manager.ui.theme.MikuSakuraSecondary
import com.aiphoto.manager.ui.theme.MikuBluePrimary
import com.aiphoto.manager.ui.theme.MikuBlueSecondary
import com.aiphoto.manager.ui.theme.MikuGreenPrimary
import com.aiphoto.manager.ui.theme.MikuGreenSecondary
import com.aiphoto.manager.ui.theme.MikuDarkPrimary
import com.aiphoto.manager.ui.theme.MikuDarkSecondary
import com.aiphoto.manager.ui.theme.CyberNeonPrimary
import com.aiphoto.manager.ui.theme.CyberNeonSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val backupFiles by viewModel.backupFiles.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val comfyUIUrl by viewModel.comfyUIUrl.collectAsState()
    val comfyUIVideoUrl by viewModel.comfyUIVideoUrl.collectAsState()
    var showComfyUrlDialog by remember { mutableStateOf(false) }
    var showComfyVideoUrlDialog by remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf<File?>(null) }
    val showRestoreDialog = remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initSettings(context)
        viewModel.loadBackupFiles(context)
    }

    AuraParticlesBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Transparent background to show particles
            topBar = {
                TopAppBar(
                    title = { Text("设置", fontWeight = FontWeight.Bold) },
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
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                item { SectionTitle("外观") }
                item {
                    ThemePickerCard(currentTheme, onThemeSelected = { viewModel.setTheme(context, it) })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }

                item { SectionTitle("服务") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showComfyUrlDialog = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("文生图服务地址", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(comfyUIUrl.ifBlank { "未配置" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showComfyVideoUrlDialog = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("图生视频服务地址", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(comfyUIVideoUrl.ifBlank { "未配置（使用文生图地址）" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }

                item { SectionTitle("数据管理") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("自动备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (lastBackupTime != null) "上次备份: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastBackupTime!!))}" else "暂无备份数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.createBackup(context) { Toast.makeText(context, if (it) "备份创建成功" else "失败", Toast.LENGTH_SHORT).show() } },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("立即备份", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                if (backupFiles.isEmpty()) {
                    item { Text("暂无备份文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
                } else {
                    item { Text("备份历史", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                    items(backupFiles) { file -> BackupFileCard(file, onRestore = { showRestoreDialog.value = file }, onDelete = { showDeleteDialog.value = file }) }
                }
            }
        }
    }

    if (showDeleteDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = null },
            title = { Text("删除备份", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除此备份吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog.value?.delete()
                    showDeleteDialog.value = null
                    viewModel.loadBackupFiles(context)
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = null }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    if (showRestoreDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog.value = null },
            title = { Text("恢复备份", fontWeight = FontWeight.Bold) },
            text = { Text("将覆盖当前所有数据") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreBackup(context, showRestoreDialog.value!!) {
                        Toast.makeText(context, if (it) "恢复成功" else "失败", Toast.LENGTH_LONG).show()
                        showRestoreDialog.value = null
                    }
                }) {
                    Text("恢复", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog.value = null }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    if (showComfyUrlDialog) {
        var editUrl by remember { mutableStateOf(comfyUIUrl) }
        AlertDialog(
            onDismissRequest = { showComfyUrlDialog = false },
            title = { Text("文生图 ComfyUI", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("服务器地址", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, placeholder = { Text("http://192.168.123.178:8188") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveComfyUIUrl(editUrl)
                    showComfyUrlDialog = false
                }) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showComfyUrlDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    if (showComfyVideoUrlDialog) {
        var editUrl by remember { mutableStateOf(comfyUIVideoUrl) }
        AlertDialog(
            onDismissRequest = { showComfyVideoUrlDialog = false },
            title = { Text("图生视频 ComfyUI", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("服务器地址", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text("留空则使用文生图地址", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, placeholder = { Text("http://192.168.123.178:8188") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveComfyUIVideoUrl(editUrl)
                    showComfyVideoUrlDialog = false
                }) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showComfyVideoUrlDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePickerCard(currentTheme: ThemeMode, onThemeSelected: (ThemeMode) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("主题风格", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.entries.forEach { mode -> ThemeChoiceCard(mode, mode == currentTheme, onClick = { onThemeSelected(mode) }) }
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(mode: ThemeMode, isSelected: Boolean, onClick: () -> Unit) {
    val (start, end) = when (mode) {
        ThemeMode.MIKU_SAKURA -> MikuSakuraPrimary to MikuSakuraSecondary
        ThemeMode.MIKU_BLUE -> MikuBluePrimary to MikuBlueSecondary
        ThemeMode.MIKU_GREEN -> MikuGreenPrimary to MikuGreenSecondary
        ThemeMode.DARK_PURPLE -> MikuDarkPrimary to MikuDarkSecondary
        ThemeMode.CYBER_NEON -> CyberNeonPrimary to CyberNeonSecondary
    }
    Surface(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Translucent theme choice card
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(start, end))),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                mode.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackupFileCard(file: File, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row {
                        Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified())), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(String.format("%.1f KB", file.length() / 1024.0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Restore, "恢复", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}
