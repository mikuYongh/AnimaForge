package com.aiphoto.manager.ui.screen.edit

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.api.ComfyUIClient
import com.aiphoto.manager.api.ArtistApiClient
import com.aiphoto.manager.data.SettingsManager
import com.aiphoto.manager.data.local.entity.FavoritePromptEntity
import com.aiphoto.manager.data.local.entity.GeneratedImageEntity
import com.aiphoto.manager.data.local.entity.PromptEntity
import com.aiphoto.manager.data.local.entity.TagEntity
import com.aiphoto.manager.data.local.entity.WorkflowEntity
import com.aiphoto.manager.data.model.PromptWithTags
import com.aiphoto.manager.data.repository.PromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as App).database
    private val repository: PromptRepository
    private val settingsManager: SettingsManager
    private val comfyUIClient: ComfyUIClient
    private val context = application.applicationContext

    init {
        repository = PromptRepository(db.promptDao(), db.tagDao())
        settingsManager = SettingsManager(context)
        comfyUIClient = ComfyUIClient(context)
    }

    val allTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _positivePrompt = MutableStateFlow("")
    val positivePrompt: StateFlow<String> = _positivePrompt

    private val _negativePrompt = MutableStateFlow("")
    val negativePrompt: StateFlow<String> = _negativePrompt

    private val _seed = MutableStateFlow("")
    val seed: StateFlow<String> = _seed

    private val _parameters = MutableStateFlow("")
    val parameters: StateFlow<String> = _parameters

    // 生成参数
    private val _width = MutableStateFlow(896)
    val width: StateFlow<Int> = _width

    private val _height = MutableStateFlow(1088)
    val height: StateFlow<Int> = _height

    private val _steps = MutableStateFlow(30)
    val steps: StateFlow<Int> = _steps

    private val _cfgScale = MutableStateFlow(5.5)
    val cfgScale: StateFlow<Double> = _cfgScale

    private val _selectedTags = MutableStateFlow<List<TagEntity>>(emptyList())
    val selectedTags: StateFlow<List<TagEntity>> = _selectedTags

    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    // 收藏的提示词
    private val _favoritePositivePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoritePositivePrompts: StateFlow<List<String>> = _favoritePositivePrompts

    private val _favoriteNegativePrompts = MutableStateFlow<List<String>>(emptyList())
    val favoriteNegativePrompts: StateFlow<List<String>> = _favoriteNegativePrompts

    private val _favoriteArtistPrompts = MutableStateFlow<List<String>>(emptyList())
    val favoriteArtistPrompts: StateFlow<List<String>> = _favoriteArtistPrompts

    private val artistApiClient = ArtistApiClient()

    private val _artistList = MutableStateFlow<List<com.aiphoto.manager.data.model.Artist>>(emptyList())
    val artistList: StateFlow<List<com.aiphoto.manager.data.model.Artist>> = _artistList

    private val _artistListLoading = MutableStateFlow(false)
    val artistListLoading: StateFlow<Boolean> = _artistListLoading

    // 画师选择器持久化状态
    var artistSearchQuery by mutableStateOf("")
    var artistSortByPostCount by mutableStateOf(true)
    var artistVisibleCount by mutableStateOf(200)

    private val _artistPrompt = MutableStateFlow("")
    val artistPrompt: StateFlow<String> = _artistPrompt

    val comfyUiUrl: StateFlow<String> = settingsManager.comfyUiUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://192.168.123.178:8188")

    private var editPromptId: String? = null
    private var isNew = true
    private var originalCreatedAt: Long = 0L
    private var originalIsFavorite: Boolean = false
    private var originalIsPinned: Boolean = false

    fun loadForEdit(promptWithTags: PromptWithTags) {
        isNew = false
        editPromptId = promptWithTags.prompt.id
        originalCreatedAt = promptWithTags.prompt.createdAt
        originalIsFavorite = promptWithTags.prompt.isFavorite
        originalIsPinned = promptWithTags.prompt.isPinned
        _title.value = promptWithTags.prompt.title
        _description.value = promptWithTags.prompt.description
        _positivePrompt.value = promptWithTags.prompt.positivePrompt
        _negativePrompt.value = promptWithTags.prompt.negativePrompt
        _seed.value = promptWithTags.prompt.seed
        _parameters.value = promptWithTags.prompt.parameters
        _width.value = promptWithTags.prompt.width
        _height.value = promptWithTags.prompt.height
        _steps.value = promptWithTags.prompt.steps
        _cfgScale.value = promptWithTags.prompt.cfgScale
        _selectedTags.value = promptWithTags.tags
        _imagePaths.value = promptWithTags.images.map { it.imagePath }
        _artistPrompt.value = promptWithTags.prompt.artistPrompt
    }

    fun onTitleChange(v: String) { _title.value = v }
    fun onDescriptionChange(v: String) { _description.value = v }
    fun onPositivePromptChange(v: String) { _positivePrompt.value = v }
    fun onNegativePromptChange(v: String) { _negativePrompt.value = v }
    fun onSeedChange(v: String) { _seed.value = v }
    fun onParametersChange(v: String) { _parameters.value = v }
    fun onWidthChange(v: Int) { _width.value = v }
    fun onHeightChange(v: Int) { _height.value = v }
    fun onStepsChange(v: Int) { _steps.value = v }
    fun onCfgScaleChange(v: Double) { _cfgScale.value = v }

    fun addTag(tagName: String) {
        viewModelScope.launch {
            try {
                val currentTags = _selectedTags.value
                if (currentTags.none { it.name.equals(tagName, ignoreCase = true) }) {
                    val realTag = repository.getOrCreateTag(tagName)
                    _selectedTags.value = currentTags + realTag
                }
            } catch (e: Exception) {
                Log.e("EditViewModel", "添加标签失败", e)
            }
        }
    }

    fun removeTag(tagId: String) {
        _selectedTags.value = _selectedTags.value.filter { it.id != tagId }
    }

    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            val newPaths = uris.mapNotNull { uri ->
                copyImageToLocal(uri)
            }
            _imagePaths.value = _imagePaths.value + newPaths
        }
    }

    fun removeImage(index: Int) {
        _imagePaths.value = _imagePaths.value.toMutableList().apply { removeAt(index) }
    }

    fun loadFavoritePrompts() {
        viewModelScope.launch {
            db.favoritePromptDao().getAll().collect { favorites ->
                _favoritePositivePrompts.value = favorites.filter { it.type == "positive" }.map { it.content }
                _favoriteNegativePrompts.value = favorites.filter { it.type == "negative" }.map { it.content }
                _favoriteArtistPrompts.value = favorites.filter { it.type == "artist" }.map { it.content }
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
                        id = UUID.randomUUID().toString(),
                        content = content,
                        type = type,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun onArtistPromptChange(v: String) { _artistPrompt.value = v }

    fun loadArtistList() {
        viewModelScope.launch {
            _artistListLoading.value = true
            val result = artistApiClient.fetchArtists()
            _artistListLoading.value = false
            if (result.isSuccess) {
                _artistList.value = result.getOrDefault(emptyList())
            }
        }
    }

    private fun copyImageToLocal(uri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "prompt_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun saveComfyUiUrl(url: String) {
        viewModelScope.launch {
            settingsManager.saveComfyUiUrl(url)
            comfyUIClient.setServerUrl(url)
        }
    }

    fun savePrompt(onSaved: () -> Unit) {
        viewModelScope.launch {
            val id = editPromptId ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val prompt = PromptEntity(
                id = id,
                title = _title.value,
                description = _description.value,
                positivePrompt = _positivePrompt.value,
                negativePrompt = _negativePrompt.value,
                seed = _seed.value,
                parameters = _parameters.value,
                width = _width.value,
                height = _height.value,
                steps = _steps.value,
                cfgScale = _cfgScale.value,
                isFavorite = if (isNew) false else originalIsFavorite,
                isPinned = if (isNew) false else originalIsPinned,
                artistPrompt = _artistPrompt.value,
                createdAt = if (isNew) now else originalCreatedAt,
                updatedAt = now
            )
            val imagePaths = _imagePaths.value

            // 确保所有标签都已持久化到 DB，避免 FOREIGN KEY 约束失败
            val resolvedTags = _selectedTags.value.map { tag ->
                repository.getOrCreateTag(tag.name, tag.color)
            }
            val tagIds = resolvedTags.map { it.id }
            _selectedTags.value = resolvedTags

            if (isNew) {
                repository.savePrompt(prompt, tagIds, imagePaths)
            } else {
                repository.updatePrompt(prompt, tagIds, imagePaths)
            }
            onSaved()
        }
    }
}
