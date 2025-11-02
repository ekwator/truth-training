package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a user's assessment of an event.
 */
@Entity(
    tableName = "judgments",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["submitted_at"])
    ]
)
data class JudgmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // Format: "judg_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events
    
    @ColumnInfo(name = "assessment")
    val assessment: String,  // "true" | "false" | "uncertain"
    
    @ColumnInfo(name = "confidence_level")
    val confidenceLevel: Double,  // Range: 0.0-1.0
    
    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null,
    
    @ColumnInfo(name = "submitted_at")
    val submittedAt: String  // ISO 8601 format, required
)

