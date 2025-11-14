package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY created_at ASC")
    suspend fun getPendingOperations(status: String = "PENDING"): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY created_at ASC")
    fun getPendingOperationsFlow(status: String = "PENDING"): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun getPendingOperationCount(status: String = "PENDING"): Int
    
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getOperationById(id: Long): SyncQueueEntity?

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    suspend fun getPendingOperationsPaged(status: String, limit: Int, offset: Int): List<SyncQueueEntity>
    
    @Insert
    suspend fun insertOperation(operation: SyncQueueEntity): Long
    
    @Update
    suspend fun updateOperation(operation: SyncQueueEntity)
    
    @Delete
    suspend fun deleteOperation(operation: SyncQueueEntity)
    
    @Query("DELETE FROM sync_queue WHERE status = :status AND retry_count >= 3")
    suspend fun deleteFailedOperations(status: String = "FAILED")
    
    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId AND status = :status LIMIT 1")
    suspend fun findOperationByEntity(
        entityType: String,
        entityId: String,
        status: String = "PENDING"
    ): SyncQueueEntity?
}

