package com.aiphoto.manager.ui.screen.workflow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class WorkflowViewModel(application: Application) : AndroidViewModel(application) {

    private val workflowDao = (application as App).database.workflowDao()

    val workflows: StateFlow<List<WorkflowEntity>> = workflowDao.getAllWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteWorkflow(workflowId: String) {
        viewModelScope.launch {
            workflowDao.deleteWorkflowById(workflowId)
        }
    }

    fun setDefaultWorkflow(workflowId: String) {
        viewModelScope.launch {
            workflowDao.clearDefaultWorkflow()
            workflowDao.setDefaultWorkflow(workflowId)
        }
    }
}
