package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a training event with embedded context fields (v1.0.0).
 * Matches truth_events table schema from Data_Schema.md.
 * Replaces legacy context_id with embedded fields: category_id, forma_id, cause_id, develop_id, effect_id.
 */
@Entity(
    tableName = "truth_events",
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["forma_id"]),
        Index(value = ["cause_id"]),
        Index(value = ["develop_id"]),
        Index(value = ["effect_id"]),
        Index(value = ["timestamp_start"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FormaEntity::class,
            parentColumns = ["id"],
            childColumns = ["forma_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CauseEntity::class,
            parentColumns = ["id"],
            childColumns = ["cause_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DevelopEntity::class,
            parentColumns = ["id"],
            childColumns = ["develop_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EffectEntity::class,
            parentColumns = ["id"],
            childColumns = ["effect_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,  // INTEGER PRIMARY KEY AUTOINCREMENT
    
    @ColumnInfo(name = "description")
    val description: String,  // TEXT NOT NULL
    
    // Embedded context fields (v1.0.0 - replaces context_id)
    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // FK → category.id, nullable
    
    @ColumnInfo(name = "forma_id")
    val formaId: Int? = null,  // FK → forma.id, nullable
    
    @ColumnInfo(name = "cause_id")
    val causeId: Int? = null,  // FK → cause.id, nullable
    
    @ColumnInfo(name = "develop_id")
    val developId: Int? = null,  // FK → develop.id, nullable
    
    @ColumnInfo(name = "effect_id")
    val effectId: Int? = null,  // FK → effect.id, nullable
    
    @ColumnInfo(name = "vector")
    val vector: Boolean,  // BOOLEAN (0/1) - true = outgoing, false = incoming
    
    @ColumnInfo(name = "detected")
    val detected: Boolean? = null,  // BOOLEAN nullable - whether event was identified as truth or lie
    
    @ColumnInfo(name = "corrected")
    val corrected: Boolean = false,  // BOOLEAN - event correction indicator
    
    @ColumnInfo(name = "timestamp_start")
    val timestampStart: Long,  // INTEGER (UNIX timestamp)
    
    @ColumnInfo(name = "timestamp_end")
    val timestampEnd: Long? = null,  // INTEGER nullable (UNIX timestamp)
    
    @ColumnInfo(name = "code")
    val code: Int = 1,  // INTEGER - event classification code (default: 1)
    
    @ColumnInfo(name = "collective_score")
    val collectiveScore: Double? = null  // REAL nullable - Collective truth score (0–1)
)

