package com.aiphoto.manager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.aiphoto.manager.api.ArtistApiClient
import com.aiphoto.manager.data.model.Artist
import com.aiphoto.manager.ui.theme.LocalAppColorSet
import kotlinx.coroutines.launch

enum class ArtistFilterCategory {
    ALL, SELECTED, FAVORITE, WITH_IMAGE, HIGH_POPULARITY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtistPickerDialog(
    artists: List<Artist>,
    isLoading: Boolean,
    selectedArtists: MutableList<String>,
    favoriteArtists: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    sortByPostCount: Boolean = true,
    onSortChange: (Boolean) -> Unit = {},
    visibleCount: Int = 200,
    onVisibleCountChange: (Int) -> Unit = {}
) {
    var previewArtistTag by remember { mutableStateOf<String?>(null) }
    var activeCategory by remember { mutableStateOf(ArtistFilterCategory.ALL) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val appColorSet = LocalAppColorSet.current

    // 计算过滤和分类后的画师列表
    val filteredArtists = remember(artists, searchQuery, sortByPostCount, activeCategory, selectedArtists, favoriteArtists) {
        var list = if (searchQuery.isBlank()) artists
        else artists.filter {
            it.tag.contains(searchQuery, ignoreCase = true) ||
                it.slug.contains(searchQuery, ignoreCase = true)
        }

        list = when (activeCategory) {
            ArtistFilterCategory.ALL -> list
            ArtistFilterCategory.SELECTED -> list.filter { it.tag in selectedArtists }
            ArtistFilterCategory.FAVORITE -> list.filter { it.tag in favoriteArtists }
            ArtistFilterCategory.WITH_IMAGE -> list.filter { it.hasImage }
            ArtistFilterCategory.HIGH_POPULARITY -> list.filter { it.postCount > 1000 }
        }

        if (sortByPostCount) list.sortedByDescending { it.postCount }
        else list.sortedBy { it.slug.lowercase() }
    }

    val displayArtists = remember(filteredArtists, visibleCount) {
        filteredArtists.take(visibleCount)
    }

    val showBackToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 6 } }

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
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景粒子及模糊图层
            AuraParticlesBackground {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    topBar = {
                        // 玻璃拟态顶部栏
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "取消",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "选择画师",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (selectedArtists.isNotEmpty()) {
                                        Text(
                                            text = " (已选 ${selectedArtists.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // 完成按钮 (改用和取消一样的圆形按钮)
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
                                            contentDescription = "完成",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 搜索框
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        onSearchChange(it)
                                        if (searchQuery != it) onVisibleCountChange(200)
                                    },
                                    placeholder = { Text("输入画师名或首字母搜索...", fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearchChange("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "清除")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
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
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 头部配置面板：分类选择 + 排序选择
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    // 分类 Chip 行
                                    Text(
                                        text = "筛选分类",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ArtistFilterCategory.entries.forEach { category ->
                                            val label = when (category) {
                                                ArtistFilterCategory.ALL -> "全部"
                                                ArtistFilterCategory.SELECTED -> "已选 (${selectedArtists.size})"
                                                ArtistFilterCategory.FAVORITE -> "已收藏 (${favoriteArtists.size})"
                                                ArtistFilterCategory.WITH_IMAGE -> "带头像"
                                                ArtistFilterCategory.HIGH_POPULARITY -> "高人气 (🔥>1K)"
                                            }
                                            val isSelected = activeCategory == category
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    activeCategory = category
                                                    onVisibleCountChange(200)
                                                },
                                                label = { Text(label, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 排序与统计信息
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = sortByPostCount,
                                                onClick = {
                                                    if (!sortByPostCount) onVisibleCountChange(200)
                                                    onSortChange(true)
                                                },
                                                label = { Text("按热度排序", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = !sortByPostCount,
                                                onClick = {
                                                    if (sortByPostCount) onVisibleCountChange(200)
                                                    onSortChange(false)
                                                },
                                                label = { Text("按名字字母", fontSize = 11.sp) }
                                            )
                                        }

                                        Text(
                                            text = "共找到 ${filteredArtists.size} 人",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 画师列表主体
                            if (isLoading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else if (filteredArtists.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "无匹配的画师",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            } else {
                                items(displayArtists, key = { it.tag }) { artist ->
                                    val isSelected = artist.tag in selectedArtists
                                    val isFavorite = artist.tag in favoriteArtists
                                    ArtistGridCard(
                                        artist = artist,
                                        isSelected = isSelected,
                                        isFavorite = isFavorite,
                                        highlightColor = if (appColorSet.promptChipPositive.startsWith("#")) {
                                            Color(android.graphics.Color.parseColor(appColorSet.promptChipPositive))
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        onClick = {
                                            if (isSelected) {
                                                selectedArtists.remove(artist.tag)
                                            } else {
                                                selectedArtists.add(artist.tag)
                                            }
                                        },
                                        onPreview = {
                                            previewArtistTag = artist.tag
                                        },
                                        onToggleFavorite = {
                                            onToggleFavorite(artist.tag)
                                        }
                                    )
                                }

                                // 加载更多按钮
                                if (visibleCount < filteredArtists.size) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        TextButton(
                                            onClick = { onVisibleCountChange(visibleCount + 200) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Text("加载更多 (${filteredArtists.size - visibleCount} 个画师)")
                                        }
                                    }
                                }
                            }
                        }

                        // 回到顶部按钮
                        if (showBackToTop) {
                            SmallFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        gridState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 80.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "回到顶部")
                            }
                        }

                        // 玻璃拟态底部快捷悬浮操作条
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 一键清空
                                TextButton(
                                    onClick = { selectedArtists.clear() },
                                    enabled = selectedArtists.isNotEmpty(),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("清空选择", fontWeight = FontWeight.Bold)
                                }

                                // 全选当前过滤结果
                                TextButton(
                                    onClick = {
                                        filteredArtists.forEach { artist ->
                                            if (artist.tag !in selectedArtists) {
                                                selectedArtists.add(artist.tag)
                                            }
                                        }
                                    },
                                    enabled = filteredArtists.isNotEmpty() && filteredArtists.size <= 200,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("全选结果", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistGridCard(
    artist: Artist,
    isSelected: Boolean,
    isFavorite: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 呼吸/按压缩放与elevation过渡动效
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isSelected) 1.02f else 1f,
        label = "scale"
    )
    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else if (isSelected) 6f else 2f,
        label = "elevation"
    )

    // 初音绿/粉色渐变呼吸边框
    val selectedBorderBrush = Brush.linearGradient(
        colors = listOf(
            highlightColor,
            highlightColor.copy(alpha = 0.5f)
        )
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isSelected) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = highlightColor,
                spotColor = highlightColor
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) selectedBorderBrush else Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onPreview,
                onDoubleClick = onPreview
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头像区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.Transparent),
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // 收藏按钮 (左上角)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        modifier = Modifier.size(16.dp),
                        tint = if (isFavorite) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.9f)
                    )
                }

                // 选中时的右上角指示器
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(highlightColor, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // 文字详情区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = artist.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 人气指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(
                            color = if (artist.postCount > 1000) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "热度",
                        modifier = Modifier.size(11.dp),
                        tint = if (artist.postCount > 1000) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${artist.postCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (artist.postCount > 1000) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
