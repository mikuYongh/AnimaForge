package com.aiphoto.manager.ui.screen.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.data.local.entity.FavoritePromptEntity
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.data.repository.PromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PromptRepository
    private val db = (application as App).database

    init {
        repository = PromptRepository(db.promptDao(), db.tagDao())
    }

    private val _promptData = MutableStateFlow<PromptWithTags?>(null)
    val promptData: StateFlow<PromptWithTags?> = _promptData

    private val _favoritePrompts = MutableStateFlow<Set<String>>(emptySet())
    val favoritePrompts: StateFlow<Set<String>> = _favoritePrompts

    fun loadPrompt(id: String) {
        viewModelScope.launch {
            repository.getPromptById(id).collect { data ->
                _promptData.value = data
            }
        }
    }

    fun loadFavoritePrompts() {
        viewModelScope.launch {
            db.favoritePromptDao().getAll().collect { favorites ->
                _favoritePrompts.value = favorites.map { it.content }.toSet()
            }
        }
    }

    fun toggleFavoritePrompt(content: String, type: String) {
        viewModelScope.launch {
            val existing = db.favoritePromptDao().findByContentAndType(content, type)
            if (existing != null) {
                db.favoritePromptDao().delete(existing)
            } else {
                db.favoritePromptDao().insert(
                    FavoritePromptEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        content = content,
                        type = type,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            loadFavoritePrompts()
        }
    }

    fun toggleFavorite() {
        _promptData.value?.let { data ->
            viewModelScope.launch {
                repository.toggleFavorite(data.prompt.id, data.prompt.isFavorite)
            }
        }
    }

    fun togglePinned() {
        _promptData.value?.let { data ->
            viewModelScope.launch {
                repository.togglePinned(data.prompt.id, data.prompt.isPinned)
            }
        }
    }

    fun deletePrompt(onDeleted: () -> Unit) {
        _promptData.value?.let { data ->
            viewModelScope.launch {
                repository.deletePrompt(data.prompt.id)
                onDeleted()
            }
        }
    }

    fun clonePrompt(onCloned: (String) -> Unit) {
        _promptData.value?.let { data ->
            viewModelScope.launch {
                val clonedId = repository.clonePrompt(data)
                onCloned(clonedId)
            }
        }
    }
}
