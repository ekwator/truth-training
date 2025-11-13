package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.SummaryDao
import com.truth.training.client.data.database.entities.SummaryEntity
import com.truth.training.client.data.network.dto.Summary
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for Summaries with offline-first architecture.
 */
class SummaryRepository(
    private val database: TruthDatabase
) {
    private val summaryDao: SummaryDao = database.summaryDao()

    /**
     * Get summary for an event as Flow.
     */
    fun getSummaryForEventFlow(eventId: Long): Flow<SummaryEntity?> =
        summaryDao.getSummaryForEventFlow(eventId)

    /**
     * Get summary for an event.
     */
    suspend fun getSummaryForEvent(eventId: Long): SummaryEntity? =
        summaryDao.getSummaryForEvent(eventId)

    /**
     * Save or update summary locally.
     */
    suspend fun saveSummary(eventId: Long, summaryText: String?, recommendations: String?): Result<SummaryEntity> {
        return try {
            val existing = summaryDao.getSummaryForEvent(eventId)
            
            val now = java.time.Instant.now().toString()
            val entity = if (existing != null) {
                existing.copy(
                    summaryText = summaryText,
                    recommendations = recommendations,
                    updatedAt = now
                )
            } else {
                SummaryEntity(
                    id = "summ_${UUID.randomUUID()}",
                    eventId = eventId,
                    summaryText = summaryText,
                    recommendations = recommendations,
                    updatedAt = now
                )
            }
            
            summaryDao.insertOrUpdateSummary(entity)
            // TODO: Add to sync queue if needed
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync summary from server (via event details endpoint).
     */
    suspend fun syncSummaryForEvent(eventId: Long, summary: Summary?): Result<Unit> {
        return try {
            if (summary != null) {
                val entity = SummaryEntity(
                    id = summary.id,
                    eventId = summary.eventId,
                    summaryText = summary.summaryText,
                    recommendations = summary.recommendations,
                    updatedAt = summary.updatedAt
                )
                
                summaryDao.insertOrUpdateSummary(entity)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

