package com.aiphoto.manager.ui.screen.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.util.ComfyUILogAnalyzer
import com.aiphoto.manager.util.WorkflowInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ComfyUIHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = (application as App).settingsManager

    private val _history = MutableStateFlow<Map<String, WorkflowInfo>>(emptyMap())
    val history: StateFlow<Map<String, WorkflowInfo>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val comfyUiUrl = settingsManager.comfyUiUrl.first()
                val history = ComfyUILogAnalyzer.getHistory(
                    context = getApplication(),
                    comfyUiUrl = comfyUiUrl,
                    maxItems = 50
                )
                _history.value = history
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
