package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents an effect (consequence) in the knowledge base.
 */
@Entity(tableName = "effect")
data class EffectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quality")
    val quality: Boolean,  // true = positive, false = negative
    
    @ColumnInfo(name = "description")
    val description: String? = null
)

