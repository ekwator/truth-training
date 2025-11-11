package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Represents progress metrics calculated from truth_events and impact tables.
 */
@Entity(tableName = "progress_metrics")
data class ProgressMetricsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // UNIX timestamp
    
    @ColumnInfo(name = "total_events")
    val totalEvents: Int,
    
    @ColumnInfo(name = "total_events_group")
    val totalEventsGroup: Int,
    
    @ColumnInfo(name = "total_positive_impact")
    val totalPositiveImpact: Double,
    
    @ColumnInfo(name = "total_positive_impact_group")
    val totalPositiveImpactGroup: Double,
    
    @ColumnInfo(name = "total_negative_impact")
    val totalNegativeImpact: Double,
    
    @ColumnInfo(name = "total_negative_impact_group")
    val totalNegativeImpactGroup: Double,
    
    @ColumnInfo(name = "trend")
    val trend: Double,
    
    @ColumnInfo(name = "trend_group")
    val trendGroup: Double
)

