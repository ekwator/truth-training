package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.JudgmentDao
import com.truth.training.client.data.database.entities.JudgmentEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for Judgments with offline-first architecture.
 */
class JudgmentRepository(
    private val database: TruthDatabase,
    private val api: TruthApi
) {
    private val judgmentDao: JudgmentDao = database.judgmentDao()

    /**
     * Get judgments for an event as Flow.
     */
    fun getJudgmentsForEventFlow(eventId: String): Flow<List<JudgmentEntity>> =
        judgmentDao.listJudgmentsForEventFlow(eventId)

    /**
     * List judgments for an event with pagination.
     */
    suspend fun listJudgmentsForEvent(eventId: String, limit: Int = 35, offset: Int = 0): List<JudgmentEntity> {
        return judgmentDao.listJudgmentsForEvent(eventId, limit, offset)
    }

    /**
     * Submit a judgment locally, then queue for sync.
     */
    suspend fun submitJudgment(request: CreateJudgmentRequest): Result<JudgmentEntity> {
        return try {
            // Validate assessment
            if (request.assessment !in listOf("true", "false", "uncertain")) {
                return Result.failure(IllegalArgumentException("Invalid assessment: ${request.assessment}"))
            }
            
            // Validate confidence level
            if (request.confidenceLevel < 0.0 || request.confidenceLevel > 1.0) {
                return Result.failure(IllegalArgumentException("Confidence level must be between 0.0 and 1.0"))
            }
            
            val id = "judg_${UUID.randomUUID()}"
            val now = java.time.Instant.now().toString()
            
            val entity = JudgmentEntity(
                id = id,
                eventId = request.eventId,
                assessment = request.assessment,
                confidenceLevel = request.confidenceLevel,
                reasoning = request.reasoning,
                submittedAt = now
            )
            
            judgmentDao.insertJudgment(entity)
            // TODO: Add to sync queue
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get judgment statistics for consensus calculation.
     */
    suspend fun getJudgmentStats(eventId: String): Result<JudgmentStatsResponse> {
        return try {
            val trueCount = judgmentDao.countJudgmentsByAssessment(eventId, "true")
            val falseCount = judgmentDao.countJudgmentsByAssessment(eventId, "false")
            val uncertainCount = judgmentDao.countJudgmentsByAssessment(eventId, "uncertain")
            val avgConfidence = judgmentDao.getAverageConfidence(eventId) ?: 0.0
            
            val stats = JudgmentStatsResponse(
                trueCount = trueCount,
                falseCount = falseCount,
                uncertainCount = uncertainCount,
                avgConfidence = avgConfidence,
                lastSubmittedAt = null // TODO: Get from latest judgment
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync judgments from server.
     */
    suspend fun syncJudgmentsForEvent(eventId: String): Result<Int> {
        return try {
            val response = api.listJudgments(eventId, limit = 100, offset = 0)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to sync judgments: ${response.code()}"))
            }
            
            val judgments = response.body()!!.data
            var syncedCount = 0
            
            judgments.forEach { judgmentDto ->
                val entity = JudgmentEntity(
                    id = judgmentDto.id,
                    eventId = judgmentDto.eventId,
                    assessment = judgmentDto.assessment,
                    confidenceLevel = judgmentDto.confidenceLevel,
                    reasoning = judgmentDto.reasoning,
                    submittedAt = judgmentDto.submittedAt
                )
                
                judgmentDao.insertJudgment(entity) // REPLACE strategy handles updates
                syncedCount++
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

