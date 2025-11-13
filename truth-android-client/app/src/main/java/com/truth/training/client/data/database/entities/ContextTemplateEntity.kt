package com.truth.training.client.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a reusable context template for event creation (knowledge base helper table).
 */
@Entity(
    tableName = "context",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["category_id", "forma_id", "cause_id", "develop_id", "effect_id"])
    ]
)
data class ContextTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

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

    @ColumnInfo(name = "description")
    val description: String? = null
)

