package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.JudgmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JudgmentDao {
    @Query("SELECT * FROM judgments WHERE event_id = :eventId ORDER BY submitted_at DESC LIMIT :limit OFFSET :offset")
    suspend fun listJudgmentsForEvent(eventId: String, limit: Int, offset: Int): List<JudgmentEntity>
    
    @Query("SELECT * FROM judgments WHERE event_id = :eventId ORDER BY submitted_at DESC")
    fun listJudgmentsForEventFlow(eventId: String): Flow<List<JudgmentEntity>>
    
    @Query("SELECT COUNT(*) FROM judgments WHERE event_id = :eventId")
    suspend fun getJudgmentCountForEvent(eventId: String): Int
    
    @Query("SELECT * FROM judgments WHERE id = :id")
    suspend fun getJudgmentById(id: String): JudgmentEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJudgment(judgment: JudgmentEntity)
    
    @Update
    suspend fun updateJudgment(judgment: JudgmentEntity)
    
    @Delete
    suspend fun deleteJudgment(judgment: JudgmentEntity)
    
    // Judgment statistics for consensus calculation
    @Query("SELECT COUNT(*) FROM judgments WHERE event_id = :eventId AND assessment = :assessment")
    suspend fun countJudgmentsByAssessment(eventId: String, assessment: String): Int
    
    @Query("SELECT AVG(confidence_level) FROM judgments WHERE event_id = :eventId")
    suspend fun getAverageConfidence(eventId: String): Double?
}

