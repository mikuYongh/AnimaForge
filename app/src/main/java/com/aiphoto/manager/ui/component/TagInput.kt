package com.aiphoto.manager.ui.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aiphoto.manager.data.local.entity.TagEntity
import com.aiphoto.manager.ui.theme.tagColorFor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInput(
    tags: List<TagEntity>,
    allTags: List<TagEntity>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tagText by remember { mutableStateOf("") }
    var showPickerDialog by remember { mutableStateOf(false) }

    // 联想过滤：已添加的不显示
    val suggestions = remember(tagText, tags, allTags) {
        if (tagText.isBlank()) emptyList()
        else allTags.filter {
            it.name.contains(tagText, ignoreCase = true) &&
                tags.none { t -> t.id == it.id }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 输入行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("添加标签...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (tagText.isNotBlank()) {
                                onTagAdded(tagText.trim())
                                tagText = ""
                            }
                        }
                    ),
                    singleLine = true
                )

                // 联想下拉
                if (suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            suggestions.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onTagAdded(tag.name)
                                            tagText = ""
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        tag.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 添加按钮
            Button(
                onClick = {
                    if (tagText.isNotBlank()) {
                        onTagAdded(tagText.trim())
                        tagText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }

        // 选择已有标签 + 搜索弹窗
        if (allTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FilledTonalButton(
                onClick = { showPickerDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("选择已有标签")
            }
        }

        // 已添加的标签
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        color = tagColorFor(tag.name),
                        isSelected = true,
                        onRemove = { onTagRemoved(tag.id) }
                    )
                }
            }
        }
    }

    // 标签选择弹窗
    if (showPickerDialog) {
        TagPickerDialog(
            allTags = allTags,
            selectedTagNames = tags.map { it.id }.toSet(),
            onTagSelected = { tagName ->
                onTagAdded(tagName)
                showPickerDialog = false
            },
            onDismiss = { showPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerDialog(
    allTags: List<TagEntity>,
    selectedTagNames: Set<String>,
    onTagSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(allTags, searchQuery, selectedTagNames) {
        val available = allTags.filter { it.id !in selectedTagNames }
        if (searchQuery.isBlank()) available
        else available.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索标签...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Text(
                        "没有可选的标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filtered.forEach { tag ->
                                TagChip(
                                    text = tag.name,
                                    color = tagColorFor(tag.name),
                                    isSelected = false,
                                    onClick = { onTagSelected(tag.name) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
