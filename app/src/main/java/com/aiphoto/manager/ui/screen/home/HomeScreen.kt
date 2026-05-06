package com.aiphoto.manager.ui.screen.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiphoto.manager.ui.component.EmptyState
import com.aiphoto.manager.ui.component.PromptCard
import com.aiphoto.manager.ui.component.SearchBar
import com.aiphoto.manager.ui.component.TagChip
import com.aiphoto.manager.ui.theme.tagColorFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToWorkflows: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToComfyUIHistory: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val prompts by viewModel.prompts.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                promptsCount = prompts.size,
                tagsCount = tags.size,
                showFavoritesOnly = showFavoritesOnly,
                onToggleFavorites = viewModel::toggleFavoritesFilter,
                onTemplates = onNavigateToTemplates,
                onWorkflows = onNavigateToWorkflows,
                onHistory = onNavigateToHistory,
                onComfyUIHistory = onNavigateToComfyUIHistory,
                onSettings = onNavigateToSettings,
                onImport = onImport,
                onExport = onExport,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "AnimaForge",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavoritesFilter() }) {
                            Icon(
                                if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "收藏筛选",
                                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAdd,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        TagChip(
                            text = "全部",
                            color = "#B0B8C4",
                            isSelected = selectedTagIds.isEmpty(),
                            onClick = viewModel::clearTagFilter
                        )
                        tags.forEach { tag ->
                            TagChip(
                                text = tag.name,
                                color = tagColorFor(tag.name),
                                isSelected = tag.id in selectedTagIds,
                                onClick = { viewModel.onTagFilterToggle(tag.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (prompts.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(prompts, key = { it.prompt.id }) { promptWithTags ->
                            PromptCard(
                                promptWithTags = promptWithTags,
                                onClick = { onNavigateToDetail(promptWithTags.prompt.id) },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(
                                        promptWithTags.prompt.id,
                                        promptWithTags.prompt.isFavorite
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(
    promptsCount: Int,
    tagsCount: Int,
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    onTemplates: () -> Unit,
    onWorkflows: () -> Unit,
    onHistory: () -> Unit,
    onComfyUIHistory: () -> Unit,
    onSettings: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Text(
                    "\u2728 AnimaForge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$promptsCount 提示词 / $tagsCount 标签",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            DrawerItem(text = "提示词收藏", icon = if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, selected = showFavoritesOnly) {
                onToggleFavorites(); onClose()
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            DrawerItem(text = "提示词模板", icon = Icons.Default.AutoAwesome) { onTemplates(); onClose() }
            DrawerItem(text = "工作流", icon = Icons.Default.Work) { onWorkflows(); onClose() }
            DrawerItem(text = "生成历史", icon = Icons.Default.Search) { onHistory(); onClose() }
            DrawerItem(text = "ComfyUI 历史", icon = Icons.Default.Search) { onComfyUIHistory(); onClose() }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            DrawerItem(text = "设置", icon = Icons.Default.Settings) { onSettings(); onClose() }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            DrawerItem(text = "导入 JSON", icon = Icons.Default.Upload) { onImport(); onClose() }
            DrawerItem(text = "导出 JSON", icon = Icons.Default.IosShare) { onExport(); onClose() }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun DrawerItem(
    text: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        label = {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(2.dp))
}
