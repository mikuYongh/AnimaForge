package com.aiphoto.manager.ui.screen.workflow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiphoto.manager.App
import com.aiphoto.manager.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WorkflowEditViewModel(application: Application) : AndroidViewModel(application) {

    private val workflowDao = (application as App).database.workflowDao()

    private val _workflow = MutableStateFlow<WorkflowEntity?>(null)
    val workflow: StateFlow<WorkflowEntity?> = _workflow

    fun loadWorkflow(workflowId: String) {
        viewModelScope.launch {
            _workflow.value = workflowDao.getWorkflowById(workflowId)
        }
    }

    fun saveWorkflow(
        name: String,
        description: String,
        workflowJson: String,
        type: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = _workflow.value
            val workflow = WorkflowEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name,
                description = description,
                workflowJson = workflowJson,
                type = type,
                isDefault = existing?.isDefault ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            workflowDao.insertWorkflow(workflow)
            onSaved()
        }
    }
}
