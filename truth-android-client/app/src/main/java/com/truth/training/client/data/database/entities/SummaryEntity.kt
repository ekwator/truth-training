package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a summary and recommendations for an event (1:1 relationship with events).
 */
@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"], unique = true)]
)
data class SummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // Format: "summ_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events, unique
    
    @ColumnInfo(name = "summary_text")
    val summaryText: String? = null,
    
    @ColumnInfo(name = "recommendations")
    val recommendations: String? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String  // ISO 8601 format, required
)

