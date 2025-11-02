package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.EventDao
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for Events with offline-first architecture.
 * All operations save to Room database first, then sync to server in background.
 */
class EventRepository(
    private val database: TruthDatabase,
    private val api: TruthApi?
) {
    private val eventDao: EventDao = database.eventDao()

    /**
     * Get all events as Flow for reactive UI updates.
     */
    fun getAllEventsFlow(): Flow<List<EventEntity>> = eventDao.getAllEventsFlow()

    /**
     * Get event by ID from local database.
     */
    suspend fun getEventById(id: String): EventEntity? = eventDao.getEventById(id)

    /**
     * Get event by ID as Flow.
     */
    fun getEventByIdFlow(id: String): Flow<EventEntity?> = eventDao.getEventByIdFlow(id)

    /**
     * List events with pagination from local database.
     */
    suspend fun listEvents(limit: Int = 35, offset: Int = 0, status: String? = null): List<EventEntity> {
        return if (status != null) {
            eventDao.listEventsByStatus(status, limit, offset)
        } else {
            eventDao.listEvents(limit, offset)
        }
    }

    /**
     * Create event locally first, then queue for sync.
     */
    suspend fun createEvent(request: CreateEventRequest): Result<EventEntity> {
        return try {
            val id = "event_${UUID.randomUUID()}"
            val now = java.time.Instant.now().toString()
            
            val entity = EventEntity(
                id = id,
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                formaId = request.formaId,
                causeId = request.causeId,
                developId = request.developId,
                effectId = request.effectId,
                startDate = request.startDate,
                endDate = request.endDate,
                createdAt = now,
                updatedAt = null,
                status = "active"
            )
            
            // Save to local database immediately (offline-first)
            eventDao.insertEvent(entity)
            
            // Queue for background sync
            // TODO: Add to sync queue (will be implemented in SyncQueueManager)
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update event locally first, then queue for sync.
     */
    suspend fun updateEvent(id: String, request: UpdateEventRequest): Result<EventEntity> {
        return try {
            val existing = eventDao.getEventById(id) ?: return Result.failure(
                IllegalArgumentException("Event not found: $id")
            )
            
            val updated = existing.copy(
                title = request.title ?: existing.title,
                description = request.description,
                categoryId = request.categoryId ?: existing.categoryId,
                formaId = request.formaId ?: existing.formaId,
                causeId = request.causeId ?: existing.causeId,
                developId = request.developId ?: existing.developId,
                effectId = request.effectId ?: existing.effectId,
                startDate = request.startDate ?: existing.startDate,
                endDate = request.endDate ?: existing.endDate,
                status = request.status ?: existing.status,
                updatedAt = java.time.Instant.now().toString()
            )
            
            eventDao.updateEvent(updated)
            // TODO: Add to sync queue
            
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete event locally, then queue for sync.
     */
    suspend fun deleteEvent(id: String): Result<Unit> {
        return try {
            val entity = eventDao.getEventById(id) ?: return Result.failure(
                IllegalArgumentException("Event not found: $id")
            )
            
            eventDao.deleteEvent(entity)
            // TODO: Add to sync queue
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync events from server to local database.
     * Called by background sync worker.
     */
    suspend fun syncFromServer(): Result<Int> {
        return try {
            val api = api ?: return Result.failure(Exception("API not available"))
            val response = api.listEvents(limit = 100, offset = 0)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to sync events: ${response.code()}"))
            }
            
            val events = response.body()!!.data
            var syncedCount = 0
            
            events.forEach { eventDto ->
                val entity = EventEntity(
                    id = eventDto.id,
                    title = eventDto.title,
                    description = eventDto.description,
                    categoryId = eventDto.categoryId,
                    formaId = eventDto.formaId,
                    causeId = eventDto.causeId,
                    developId = eventDto.developId,
                    effectId = eventDto.effectId,
                    startDate = eventDto.startDate,
                    endDate = eventDto.endDate,
                    createdAt = eventDto.createdAt,
                    updatedAt = eventDto.updatedAt,
                    status = eventDto.status
                )
                
                eventDao.insertEvent(entity) // REPLACE strategy handles updates
                syncedCount++
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

