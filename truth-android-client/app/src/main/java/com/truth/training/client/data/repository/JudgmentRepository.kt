package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.JudgmentDao
import com.truth.training.client.data.database.entities.JudgmentEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateJudgmentRequest
import com.truth.training.client.data.network.dto.Judgment
import com.truth.training.client.data.network.dto.JudgmentListResponse
import com.truth.training.client.data.network.dto.JudgmentStatsResponse
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for Judgments with offline-first architecture.
 */
class JudgmentRepository(
    private val database: TruthDatabase,
    private val api: TruthApi?
) {
    private val judgmentDao: JudgmentDao = database.judgmentDao()

    /**
     * Get judgments for an event as Flow.
     */
    fun getJudgmentsForEventFlow(eventId: Long): Flow<List<JudgmentEntity>> =
        judgmentDao.listJudgmentsForEventFlow(eventId)

    /**
     * List judgments for an event with pagination.
     */
    suspend fun listJudgmentsForEvent(eventId: Long, limit: Int = 35, offset: Int = 0): List<JudgmentEntity> =
        judgmentDao.listJudgmentsForEvent(eventId, limit, offset)

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
            
            val entity = JudgmentEntity(
                id = "judg_${UUID.randomUUID()}",
                eventId = request.eventId,
                assessment = request.assessment,
                confidenceLevel = request.confidenceLevel,
                reasoning = request.reasoning,
                submittedAt = java.time.Instant.now().toString()
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
    suspend fun getJudgmentStats(eventId: Long): Result<JudgmentStatsResponse> {
        return try {
            val trueCount = judgmentDao.countJudgmentsByAssessment(eventId, "true")
            val falseCount = judgmentDao.countJudgmentsByAssessment(eventId, "false")
            val uncertainCount = judgmentDao.countJudgmentsByAssessment(eventId, "uncertain")
            val avgConfidence = judgmentDao.averageConfidence(eventId) ?: 0.0
            
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
    suspend fun syncJudgmentsForEvent(eventId: Long): Result<Int> {
        return try {
            val response = api?.listJudgments(eventId, limit = 200, offset = 0)
                ?: return Result.failure(Exception("API not available"))
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to sync judgments: ${response.code()}"))
            }
            val body: JudgmentListResponse = response.body()
                ?: return Result.failure(Exception("Empty judgment list"))

            var synced = 0
            body.data.forEach { dto: Judgment ->
                val entity = JudgmentEntity(
                    id = dto.id,
                    eventId = dto.eventId,
                    assessment = dto.assessment,
                    confidenceLevel = dto.confidenceLevel,
                    reasoning = dto.reasoning,
                    submittedAt = dto.submittedAt
                )
                judgmentDao.insertJudgment(entity)
                synced++
            }
            Result.success(synced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

