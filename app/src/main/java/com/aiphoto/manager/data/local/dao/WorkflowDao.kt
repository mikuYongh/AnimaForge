package com.aiphoto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiphoto.manager.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    @Query("SELECT * FROM workflows ORDER BY isDefault DESC, name")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE type = :type ORDER BY isDefault DESC, name")
    fun getWorkflowsByType(type: String): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :workflowId")
    suspend fun getWorkflowById(workflowId: String): WorkflowEntity?

    @Query("SELECT * FROM workflows WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWorkflow(): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Delete
    suspend fun deleteWorkflow(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    suspend fun deleteWorkflowById(workflowId: String)

    @Query("UPDATE workflows SET isDefault = 0")
    suspend fun clearDefaultWorkflow()

    @Query("UPDATE workflows SET isDefault = 1 WHERE id = :workflowId")
    suspend fun setDefaultWorkflow(workflowId: String)
}
