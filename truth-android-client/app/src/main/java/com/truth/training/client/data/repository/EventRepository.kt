package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.EventDao
import com.truth.training.client.data.database.entities.EventEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.network.dto.EventDetailsResponse
import com.truth.training.client.data.network.dto.UpdateEventRequest
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val database: TruthDatabase,
    private val api: TruthApi?
) {
    private val eventDao: EventDao = database.eventDao()

    fun getAllEventsFlow(): Flow<List<EventEntity>> = eventDao.getAllEventsFlow()

    suspend fun getEventById(id: Long): EventEntity? = eventDao.getEventById(id)

    fun getEventByIdFlow(id: Long): Flow<EventEntity?> = eventDao.getEventByIdFlow(id)

    suspend fun listEvents(limit: Int = 35, offset: Int = 0): List<EventEntity> =
        eventDao.listEvents(limit, offset)

    suspend fun createEvent(request: CreateEventRequest): Result<EventEntity> {
        return try {
            val entity = EventEntity(
                description = request.description,
                categoryId = request.categoryId,
                formaId = request.formaId,
                causeId = request.causeId,
                developId = request.developId,
                effectId = request.effectId,
                vector = request.vector,
                detected = null,
                corrected = false,
                timestampStart = request.timestampStart,
                timestampEnd = request.timestampEnd,
                code = request.code,
                collectiveScore = request.collectiveScore
            )

            val id = eventDao.insertEvent(entity)
            val created = entity.copy(id = id)

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(id: Long, request: UpdateEventRequest): Result<EventEntity> {
        return try {
            val existing = eventDao.getEventById(id) ?: return Result.failure(
                IllegalArgumentException("Event not found: $id")
            )

            val updated = existing.copy(
                description = request.description ?: existing.description,
                categoryId = request.categoryId ?: existing.categoryId,
                formaId = request.formaId ?: existing.formaId,
                causeId = request.causeId ?: existing.causeId,
                developId = request.developId ?: existing.developId,
                effectId = request.effectId ?: existing.effectId,
                vector = request.vector ?: existing.vector,
                detected = request.detected ?: existing.detected,
                corrected = request.corrected ?: existing.corrected,
                timestampStart = request.timestampStart ?: existing.timestampStart,
                timestampEnd = request.timestampEnd ?: existing.timestampEnd,
                code = request.code ?: existing.code,
                collectiveScore = request.collectiveScore ?: existing.collectiveScore
            )

            eventDao.updateEvent(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(id: Long): Result<Unit> {
        return try {
            val entity = eventDao.getEventById(id) ?: return Result.failure(
                IllegalArgumentException("Event not found: $id")
            )

            eventDao.deleteEvent(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncFromServer(): Result<Int> {
        return try {
            val api = api ?: return Result.failure(Exception("API not available"))
            val response = api.listEvents(limit = 500, offset = 0)
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch events: ${response.code()}"))
            }

            val body = response.body() ?: return Result.failure(Exception("Empty event response"))
            var synced = 0
            body.data.forEach { dto ->
                val entity = dto.toEntity()
                eventDao.insertEvent(entity)
                synced++
            }
            Result.success(synced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncEventDetails(eventId: Long): Result<EventEntity> {
        return try {
            val api = api ?: return Result.failure(Exception("API not available"))
            val response = api.getEvent(eventId)
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch event: ${response.code()}"))
            }
            val dto = response.body() ?: return Result.failure(Exception("Empty event body"))
            val entity = dto.toEntity()
            eventDao.insertEvent(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun EventDetailsResponse.toEntity(): EventEntity = EventEntity(
        id = id,
        description = description,
        categoryId = categoryId,
        formaId = formaId,
        causeId = causeId,
        developId = developId,
        effectId = effectId,
        vector = vector,
        detected = detected,
        corrected = corrected,
        timestampStart = timestampStart,
        timestampEnd = timestampEnd,
        code = code,
        collectiveScore = collectiveScore
    )

    private fun com.truth.training.client.data.network.dto.EventResponse.toEntity(): EventEntity = EventEntity(
        id = id,
        description = description,
        categoryId = categoryId,
        formaId = formaId,
        causeId = causeId,
        developId = developId,
        effectId = effectId,
        vector = vector,
        detected = detected,
        corrected = corrected,
        timestampStart = timestampStart,
        timestampEnd = timestampEnd,
        code = code,
        collectiveScore = collectiveScore
    )
}

