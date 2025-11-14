package com.truth.training.client.data.sync

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.SyncQueueDao
import com.truth.training.client.data.database.entities.SyncQueueEntity
import com.google.gson.Gson

/**
 * Manages offline operation queue for background synchronization.
 * Implements local-wins conflict resolution strategy.
 */
class SyncQueueManager(private val database: TruthDatabase) {
    private val syncQueueDao: SyncQueueDao = database.syncQueueDao()
    private val gson = Gson()

    /**
     * Add operation to sync queue.
     */
    suspend fun queueOperation(
        operationType: String, // "CREATE" | "UPDATE" | "DELETE"
        entityType: String, // "EVENT" | "CONTEXT_TEMPLATE" | "JUDGMENT" | "IMPACT" | "SUMMARY"
        entityId: String,
        payload: Any
    ): Result<Long> {
        return try {
            val jsonPayload = gson.toJson(payload)
            val now = java.time.Instant.now().toString()
            
            // Check if operation already exists for this entity
            val existing = syncQueueDao.findOperationByEntity(entityType, entityId, "PENDING")
            
            val entity = if (existing != null) {
                // Update existing operation
                existing.copy(
                    operationType = operationType,
                    payload = jsonPayload,
                    status = "PENDING",
                    retryCount = 0,
                    errorMessage = null
                )
            } else {
                // Create new operation
                SyncQueueEntity(
                    id = 0, // Auto-generated
                    operationType = operationType,
                    entityType = entityType,
                    entityId = entityId,
                    payload = jsonPayload,
                    status = "PENDING",
                    retryCount = 0,
                    errorMessage = null,
                    createdAt = now,
                    syncedAt = null
                )
            }
            
            val id = if (existing != null) {
                syncQueueDao.updateOperation(entity)
                existing.id
            } else {
                syncQueueDao.insertOperation(entity)
            }
            
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending operations for processing.
     */
    suspend fun getPendingOperations(status: String = "PENDING", limit: Int = Int.MAX_VALUE, offset: Int = 0): List<SyncQueueEntity> {
        return if (limit == Int.MAX_VALUE && offset == 0) {
            syncQueueDao.getPendingOperations(status)
        } else {
            val safeLimit = if (limit == Int.MAX_VALUE) Int.MAX_VALUE else limit
            syncQueueDao.getPendingOperationsPaged(status, safeLimit, offset)
        }
    }

    /**
     * Get count of pending operations.
     */
    suspend fun getPendingCount(): Int {
        return syncQueueDao.getPendingOperationCount("PENDING")
    }

    /**
     * Mark operation as syncing.
     */
    suspend fun markSyncing(operationId: Long): Result<Unit> {
        return try {
            val operation = syncQueueDao.getOperationById(operationId)
                ?: return Result.failure(IllegalArgumentException("Operation not found: $operationId"))
            
            val updated = operation.copy(status = "SYNCING")
            syncQueueDao.updateOperation(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark operation as completed.
     */
    suspend fun markCompleted(operationId: Long): Result<Unit> {
        return try {
            val operation = syncQueueDao.getOperationById(operationId)
                ?: return Result.failure(IllegalArgumentException("Operation not found: $operationId"))
            
            val updated = operation.copy(
                status = "COMPLETED",
                syncedAt = java.time.Instant.now().toString()
            )
            syncQueueDao.updateOperation(updated)
            syncQueueDao.deleteOperation(updated) // Remove from queue after successful sync
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark operation as failed and increment retry count.
     */
    suspend fun markFailed(operationId: Long, errorMessage: String): Result<Unit> {
        return try {
            val operation = syncQueueDao.getOperationById(operationId)
                ?: return Result.failure(IllegalArgumentException("Operation not found: $operationId"))
            
            val newRetryCount = operation.retryCount + 1
            val status = if (newRetryCount >= 3) "FAILED" else "PENDING"
            
            val updated = operation.copy(
                status = status,
                retryCount = newRetryCount,
                errorMessage = errorMessage
            )
            syncQueueDao.updateOperation(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean up failed operations (max retries exceeded).
     */
    suspend fun cleanupFailedOperations(): Result<Int> {
        return try {
            val failed = syncQueueDao.getPendingOperations("FAILED")
            syncQueueDao.deleteFailedOperations("FAILED")
            Result.success(failed.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

