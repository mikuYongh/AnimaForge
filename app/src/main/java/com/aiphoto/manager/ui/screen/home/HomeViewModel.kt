package com.aiphoto.manager.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.data.local.entity.TagEntity
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.data.repository.PromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PromptRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    init {
        val db = (application as App).database
        repository = PromptRepository(db.promptDao(), db.tagDao())
    }

    // 只显示被提示词使用的标签
    val allTags: StateFlow<List<TagEntity>> = combine(
        repository.getAllTags(),
        repository.getAllPrompts()
    ) { allTags, allPrompts ->
        // 获取所有提示词使用的标签ID
        val usedTagIds = allPrompts.flatMap { it.tags.map { tag -> tag.id } }.toSet()
        // 只返回被使用的标签
        allTags.filter { it.id in usedTagIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prompts: StateFlow<List<PromptWithTags>> = combine(
        repository.getAllPrompts(),
        _searchQuery,
        _selectedTagIds,
        _showFavoritesOnly
    ) { allPrompts, query, tagIds, favoritesOnly ->
        var filtered = allPrompts

        // 筛选喜欢的提示词
        if (favoritesOnly) {
            filtered = filtered.filter { it.prompt.isFavorite }
        }

        // 搜索和标签筛选
        if (query.isNotBlank() || tagIds.isNotEmpty()) {
            filtered = filtered.filter { pwt ->
                val matchesQuery = query.isBlank() ||
                    pwt.prompt.title.contains(query, true) ||
                    pwt.prompt.description.contains(query, true) ||
                    pwt.prompt.positivePrompt.contains(query, true)
                val matchesTags = tagIds.isEmpty() ||
                    pwt.tags.any { it.id in tagIds }
                matchesQuery && matchesTags
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterToggle(tagId: String) {
        _selectedTagIds.value = if (tagId in _selectedTagIds.value) {
            _selectedTagIds.value - tagId
        } else {
            _selectedTagIds.value + tagId
        }
    }

    fun clearTagFilter() {
        _selectedTagIds.value = emptySet()
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(promptId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(promptId, isFavorite)
        }
    }

    fun deletePrompt(promptId: String) {
        viewModelScope.launch {
            repository.deletePrompt(promptId)
        }
    }
}
