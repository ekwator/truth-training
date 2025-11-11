package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a develop (manifestation) in the knowledge base.
 */
@Entity(tableName = "develop")
data class DevelopEntity(
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

