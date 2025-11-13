package com.truth.training.client.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val id: String,

    @ColumnInfo(name = "event_id")
    val eventId: Long,

    @ColumnInfo(name = "summary_text")
    val summaryText: String? = null,

    @ColumnInfo(name = "recommendations")
    val recommendations: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

