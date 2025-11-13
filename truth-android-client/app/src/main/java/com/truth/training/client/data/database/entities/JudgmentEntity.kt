package com.truth.training.client.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val id: String,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "assessment")
    val assessment: String,

    @ColumnInfo(name = "confidence_level")
    val confidenceLevel: Double,

    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null,

    @ColumnInfo(name = "submitted_at")
    val submittedAt: String
)

