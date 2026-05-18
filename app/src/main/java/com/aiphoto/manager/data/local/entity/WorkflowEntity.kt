package com.aiphoto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val workflowJson: String,
    val type: String = "text2img",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
