package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents an impact assessment for an event.
 */
@Entity(
    tableName = "impacts",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"])]
)
data class ImpactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // Format: "impact_{uuid}"
    
    @ColumnInfo(name = "event_id")
    val eventId: String,  // FK to events
    
    @ColumnInfo(name = "impact_level")
    val impactLevel: Int,  // Range: 1-5
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String  // ISO 8601 format, required
)

