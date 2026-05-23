package com.aiphoto.manager.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiphoto.manager.ui.theme.AppColorSet
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigSection(
    baseModel: String?,
    onBaseModelChange: (String) -> Unit,
    availableModels: List<String>,
    modelsLoading: Boolean,
    loraConfigs: List<ModelConfigSectionLoraItem>,
    availableLoras: List<String>,
    lorasLoading: Boolean,
    onLoraToggle: (Int, Boolean) -> Unit,
    onLoraStrengthChange: (Int, Double) -> Unit,
    onLoraAdd: (String) -> Unit,
    onLoraRemove: (Int) -> Unit,
    appColorSet: AppColorSet
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddLoraDialog by remember { mutableStateOf(false) }

    val highlightColor = if (appColorSet.promptChipPositive.startsWith("#")) {
        Color(android.graphics.Color.parseColor(appColorSet.promptChipPositive))
    } else {
        MaterialTheme.colorScheme.primary
    }

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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = highlightColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "模型 / LoRA 配置",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                val activeCount = loraConfigs.count { it.enabled }
                if (activeCount > 0 || baseModel != null) {
                    Text(
                        text = buildString {
                            if (baseModel != null) append(baseModel!!.substringBefore("."))
                            if (activeCount > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("$activeCount LoRA")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 基础模型选择
                    Text(
                        "基础模型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var showModelMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showModelMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = highlightColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = baseModel ?: if (modelsLoading) "加载中..." else "选择基础模型",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = if (baseModel != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            model,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (model == baseModel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onBaseModelChange(model)
                                        showModelMenu = false
                                    },
                                    leadingIcon = if (model == baseModel) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = highlightColor) }
                                    } else null
                                )
                            }
                            if (availableModels.isEmpty() && !modelsLoading) {
                                DropdownMenuItem(
                                    text = { Text("无可用模型", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { showModelMenu = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LoRA 列表
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LoRA 列表",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { showAddLoraDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = highlightColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加", color = highlightColor, fontSize = 12.sp)
                        }
                    }

                    if (loraConfigs.isEmpty()) {
                        Text(
                            "暂无 LoRA 配置，点击「添加」从可用列表中选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        loraConfigs.forEachIndexed { index, lora ->
                            LoraConfigCard(
                                lora = lora,
                                onToggle = { onLoraToggle(index, it) },
                                onStrengthChange = { onLoraStrengthChange(index, it) },
                                onRemove = { onLoraRemove(index) },
                                highlightColor = highlightColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // 添加 LoRA 对话框
    if (showAddLoraDialog) {
        AddLoraDialog(
            availableLoras = availableLoras,
            existingLoraNames = loraConfigs.map { it.name }.toSet(),
            lorasLoading = lorasLoading,
            onDismiss = { showAddLoraDialog = false },
            onAdd = {
                onLoraAdd(it)
                showAddLoraDialog = false
            },
            highlightColor = highlightColor
        )
    }
}

data class ModelConfigSectionLoraItem(
    val name: String,
    val strength: Double = 1.0,
    val enabled: Boolean = false
)

@Composable
private fun LoraConfigCard(
    lora: ModelConfigSectionLoraItem,
    onToggle: (Boolean) -> Unit,
    onStrengthChange: (Double) -> Unit,
    onRemove: () -> Unit,
    highlightColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (lora.enabled) 1.5.dp else 0.5.dp,
                color = if (lora.enabled) highlightColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lora.enabled) highlightColor.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lora.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (lora.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = lora.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = highlightColor
                    )
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "移除",
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = lora.strength.toFloat(),
                    onValueChange = { onStrengthChange(it.toDouble()) },
                    valueRange = 0f..2f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = highlightColor,
                        activeTrackColor = highlightColor
                    )
                )
                Text(
                    text = String.format("%.2f", lora.strength),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoraDialog(
    availableLoras: List<String>,
    existingLoraNames: Set<String>,
    lorasLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    highlightColor: Color
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLoras = remember(availableLoras, searchQuery, existingLoraNames) {
        if (searchQuery.isBlank()) {
            availableLoras.filter { it !in existingLoraNames }
        } else {
            availableLoras.filter {
                it !in existingLoraNames && it.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 LoRA") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索 LoRA...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (lorasLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (filteredLoras.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("无可用 LoRA", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredLoras) { lora ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAdd(lora) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Extension,
                                        null,
                                        Modifier.size(16.dp),
                                        tint = highlightColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        lora,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
