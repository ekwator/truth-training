package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a category in the knowledge base.
 */
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null
)

