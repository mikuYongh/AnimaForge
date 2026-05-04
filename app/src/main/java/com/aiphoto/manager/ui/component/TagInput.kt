package com.aiphoto.manager.ui.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aiphoto.manager.data.local.entity.TagEntity

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
    var showDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                            Log.d("TagInput", "添加标签: $tagText")
                            onTagAdded(tagText.trim())
                            tagText = ""
                        }
                    }
                ),
                trailingIcon = {
                    if (allTags.isNotEmpty()) {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择已有标签")
                        }
                    }
                }
            )

            // 已有标签下拉菜单
            if (allTags.isNotEmpty()) {
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        "选择已有标签",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    allTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag.name) },
                            onClick = {
                                onTagAdded(tag.name)
                                showDropdown = false
                            },
                            enabled = tags.none { it.id == tag.id }
                        )
                    }
                }
            }
        }

        // 添加按钮
        Button(
            onClick = {
                if (tagText.isNotBlank()) {
                    Log.d("TagInput", "按钮点击添加标签: $tagText")
                    onTagAdded(tagText.trim())
                    tagText = ""
                } else {
                    Log.d("TagInput", "标签文本为空")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("添加标签")
        }

        // 显示已添加的标签
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        color = tag.color,
                        isSelected = true,
                        onRemove = { onTagRemoved(tag.id) }
                    )
                }
            }
        }
    }
}
