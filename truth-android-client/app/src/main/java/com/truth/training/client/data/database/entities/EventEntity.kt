package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents a training event with embedded context fields (v1.0.0).
 * Replaces legacy context_id with embedded fields: category_id, forma_id, cause_id, develop_id, effect_id.
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["category_id", "forma_id", "cause_id", "develop_id", "effect_id"])
    ]
)
data class EventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // Format: "event_{uuid}"
    
    @ColumnInfo(name = "title")
    val title: String,  // Required, max 200 chars
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    // Embedded context fields (v1.0.0 - replaces context_id)
    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "forma_id")
    val formaId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "cause_id")
    val causeId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "develop_id")
    val developId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "effect_id")
    val effectId: Int? = null,  // FK to knowledge base
    
    @ColumnInfo(name = "start_date")
    val startDate: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "end_date")
    val endDate: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,  // ISO 8601 format, required
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,  // ISO 8601 format
    
    @ColumnInfo(name = "status")
    val status: String  // "active" | "inactive" | "archived" | "pending"
)

