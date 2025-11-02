package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents pending synchronization operations for offline-first architecture.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["operation_type"]),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    
    @ColumnInfo(name = "operation_type")
    val operationType: String,  // "CREATE" | "UPDATE" | "DELETE"
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,  // "EVENT" | "CONTEXT_TEMPLATE" | "JUDGMENT" | "IMPACT" | "SUMMARY"
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,  // ID of the entity
    
    @ColumnInfo(name = "payload")
    val payload: String,  // JSON serialized entity data
    
    @ColumnInfo(name = "status")
    val status: String,  // "PENDING" | "SYNCING" | "COMPLETED" | "FAILED"
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,  // Default: 0, max: 3
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,  // ISO 8601 format
    
    @ColumnInfo(name = "synced_at")
    val syncedAt: String? = null  // ISO 8601 format, set when sync completes
)

