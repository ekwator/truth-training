package com.truth.training.client.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a recorded impact for a truth event.
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
    val id: Long = 0L,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "type_id")
    val typeId: Int,

    @ColumnInfo(name = "value")
    val value: Boolean,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

