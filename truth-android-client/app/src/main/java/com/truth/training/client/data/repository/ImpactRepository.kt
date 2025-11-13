package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.ImpactDao
import com.truth.training.client.data.database.entities.ImpactEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateImpactRequest
import com.truth.training.client.data.network.dto.Impact
import kotlinx.coroutines.flow.Flow

class ImpactRepository(
    private val database: TruthDatabase,
    private val api: TruthApi
) {
    private val impactDao: ImpactDao = database.impactDao()

    fun getImpactsForEventFlow(eventId: Long): Flow<List<ImpactEntity>> =
        impactDao.listImpactsForEventFlow(eventId)

    suspend fun listImpactsForEvent(eventId: Long): List<ImpactEntity> =
        impactDao.listImpactsForEvent(eventId)

    suspend fun addImpact(request: CreateImpactRequest): Result<ImpactEntity> {
        return try {
            val entity = ImpactEntity(
                eventId = request.eventId,
                typeId = 1, // default type until expanded API support
                value = request.value,
                notes = request.notes,
                createdAt = System.currentTimeMillis()
            )

            val id = impactDao.insertImpact(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncImpactsForEvent(eventId: Long, impacts: List<Impact>): Result<Int> {
        return try {
            var synced = 0
            impacts.forEach { dto ->
                val entity = ImpactEntity(
                    id = dto.id,
                    eventId = dto.eventId,
                    typeId = 1,
                    value = dto.value,
                    notes = dto.notes,
                    createdAt = dto.createdAt
                )
                impactDao.insertImpact(entity)
                synced++
            }
            Result.success(synced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

