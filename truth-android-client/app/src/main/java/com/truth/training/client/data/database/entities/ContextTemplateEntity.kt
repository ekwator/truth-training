package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a reusable context template for event creation.
 */
@Entity(
    tableName = "context_templates",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["category_id", "forma_id", "cause_id", "develop_id", "effect_id"])
    ]
)
data class ContextTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,
    
    @ColumnInfo(name = "name")
    val name: String,  // Required, unique
    
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

