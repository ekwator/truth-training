package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.ImpactDao
import com.truth.training.client.data.database.entities.ImpactEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for Impacts with offline-first architecture.
 */
class ImpactRepository(
    private val database: TruthDatabase,
    private val api: TruthApi
) {
    private val impactDao: ImpactDao = database.impactDao()

    /**
     * Get impacts for an event as Flow.
     */
    fun getImpactsForEventFlow(eventId: String): Flow<List<ImpactEntity>> =
        impactDao.listImpactsForEventFlow(eventId)

    /**
     * List impacts for an event.
     */
    suspend fun listImpactsForEvent(eventId: String): List<ImpactEntity> {
        return impactDao.listImpactsForEvent(eventId)
    }

    /**
     * Add impact locally, then queue for sync.
     */
    suspend fun addImpact(request: CreateImpactRequest): Result<ImpactEntity> {
        return try {
            // Validate impact level
            if (request.impactLevel < 1 || request.impactLevel > 5) {
                return Result.failure(IllegalArgumentException("Impact level must be between 1 and 5"))
            }
            
            val id = "impact_${UUID.randomUUID()}"
            val now = java.time.Instant.now().toString()
            
            val entity = ImpactEntity(
                id = id,
                eventId = request.eventId,
                impactLevel = request.impactLevel,
                notes = request.notes,
                createdAt = now
            )
            
            impactDao.insertImpact(entity)
            // TODO: Add to sync queue
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync impacts from server (via event details endpoint).
     */
    suspend fun syncImpactsForEvent(eventId: String, impacts: List<Impact>): Result<Int> {
        return try {
            var syncedCount = 0
            
            impacts.forEach { impactDto ->
                val entity = ImpactEntity(
                    id = impactDto.id,
                    eventId = impactDto.eventId,
                    impactLevel = impactDto.impactLevel,
                    notes = impactDto.notes,
                    createdAt = impactDto.createdAt
                )
                
                impactDao.insertImpact(entity) // REPLACE strategy handles updates
                syncedCount++
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

