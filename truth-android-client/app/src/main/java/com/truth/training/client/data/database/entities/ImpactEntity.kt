package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents an impact assessment for an event (v1.0.0).
 * Matches impact table schema from Data_Schema.md.
 */
@Entity(
    tableName = "impact",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ImpactTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["type_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["type_id"])
    ]
)
data class ImpactEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,  // INTEGER PRIMARY KEY AUTOINCREMENT
    
    @ColumnInfo(name = "event_id")
    val eventId: Long,  // INTEGER FK → truth_events.id
    
    @ColumnInfo(name = "type_id")
    val typeId: Int,  // INTEGER FK → impact_type.id
    
    @ColumnInfo(name = "value")
    val value: Boolean,  // BOOLEAN (0/1) - true = positive, false = negative
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,  // TEXT nullable
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long  // INTEGER (UNIX timestamp) - not in schema but useful for tracking
)

