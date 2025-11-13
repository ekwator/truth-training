package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.ProgressMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressMetricsDao {
    @Query("SELECT * FROM progress_metrics ORDER BY timestamp DESC")
    suspend fun listProgressMetrics(): List<ProgressMetricsEntity>
    
    @Query("SELECT * FROM progress_metrics ORDER BY timestamp DESC")
    fun listProgressMetricsFlow(): Flow<List<ProgressMetricsEntity>>
    
    @Query("SELECT * FROM progress_metrics WHERE id = :id")
    suspend fun getProgressMetricsById(id: Int): ProgressMetricsEntity?
    
    @Query("SELECT * FROM progress_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestProgressMetrics(): ProgressMetricsEntity?
    
    @Query("SELECT * FROM progress_metrics ORDER BY timestamp DESC LIMIT 1")
    fun getLatestProgressMetricsFlow(): Flow<ProgressMetricsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressMetrics(progressMetrics: ProgressMetricsEntity)
    
    @Update
    suspend fun updateProgressMetrics(progressMetrics: ProgressMetricsEntity)
    
    @Delete
    suspend fun deleteProgressMetrics(progressMetrics: ProgressMetricsEntity)
    
    @Query("SELECT COUNT(*) FROM progress_metrics")
    suspend fun getProgressMetricsCount(): Int
}

