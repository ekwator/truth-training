package com.truth.training.client.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Truth events table aligned with core/desktop schema.
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
    val id: Long = 0L,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,

    @ColumnInfo(name = "forma_id")
    val formaId: Int? = null,

    @ColumnInfo(name = "cause_id")
    val causeId: Int? = null,

    @ColumnInfo(name = "develop_id")
    val developId: Int? = null,

    @ColumnInfo(name = "effect_id")
    val effectId: Int? = null,

    @ColumnInfo(name = "vector")
    val vector: Boolean = true,

    @ColumnInfo(name = "detected")
    val detected: Boolean? = null,

    @ColumnInfo(name = "corrected")
    val corrected: Boolean = false,

    @ColumnInfo(name = "timestamp_start")
    val timestampStart: Long,

    @ColumnInfo(name = "timestamp_end")
    val timestampEnd: Long? = null,

    @ColumnInfo(name = "code")
    val code: Int = 1,

    @ColumnInfo(name = "collective_score")
    val collectiveScore: Double? = null
)

