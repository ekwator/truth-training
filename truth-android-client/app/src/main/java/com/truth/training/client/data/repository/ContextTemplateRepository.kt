package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.ContextTemplateDao
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Context Templates with offline-first architecture.
 */
class ContextTemplateRepository(
    private val database: TruthDatabase,
    private val api: TruthApi
) {
    private val templateDao: ContextTemplateDao = database.contextTemplateDao()

    /**
     * Get all templates as Flow.
     */
    fun getAllTemplatesFlow(): Flow<List<ContextTemplateEntity>> = templateDao.listTemplatesFlow()

    /**
     * Get all templates from local database.
     */
    suspend fun listTemplates(): List<ContextTemplateEntity> = templateDao.listTemplates()

    /**
     * Get template by ID.
     */
    suspend fun getTemplateById(id: Int): ContextTemplateEntity? = templateDao.getTemplateById(id)

    /**
     * Get template by name.
     */
    suspend fun getTemplateByName(name: String): ContextTemplateEntity? = templateDao.getTemplateByName(name)

    /**
     * Create template locally, then queue for sync.
     */
    suspend fun createTemplate(request: CreateContextRequest): Result<ContextTemplateEntity> {
        return try {
            // Check for duplicates locally first
            val duplicateCount = templateDao.countDuplicateTemplates(
                request.categoryId,
                request.formaId,
                request.causeId,
                request.developId,
                request.effectId,
                excludeId = 0
            )
            
            if (duplicateCount > 0) {
                return Result.failure(Exception("Template with identical fields already exists (409 Conflict)"))
            }
            
            val entity = ContextTemplateEntity(
                id = 0, // Will be auto-generated
                name = request.name,
                categoryId = request.categoryId,
                formaId = request.formaId,
                causeId = request.causeId,
                developId = request.developId,
                effectId = request.effectId,
                description = request.description
            )
            
            val newId = templateDao.insertTemplate(entity)
            val created = entity.copy(id = newId.toInt())
            
            // TODO: Add to sync queue
            
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update template locally, then queue for sync.
     */
    suspend fun updateTemplate(id: Int, request: CreateContextRequest): Result<ContextTemplateEntity> {
        return try {
            val existing = templateDao.getTemplateById(id) ?: return Result.failure(
                IllegalArgumentException("Template not found: $id")
            )
            
            // Check for duplicates (excluding current template)
            val duplicateCount = templateDao.countDuplicateTemplates(
                request.categoryId,
                request.formaId,
                request.causeId,
                request.developId,
                request.effectId,
                excludeId = id
            )
            
            if (duplicateCount > 0) {
                return Result.failure(Exception("Template with identical fields already exists (409 Conflict)"))
            }
            
            val updated = existing.copy(
                name = request.name,
                categoryId = request.categoryId,
                formaId = request.formaId,
                causeId = request.causeId,
                developId = request.developId,
                effectId = request.effectId,
                description = request.description
            )
            
            templateDao.updateTemplate(updated)
            // TODO: Add to sync queue
            
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete template locally, then queue for sync.
     */
    suspend fun deleteTemplate(id: Int): Result<Unit> {
        return try {
            val entity = templateDao.getTemplateById(id) ?: return Result.failure(
                IllegalArgumentException("Template not found: $id")
            )
            
            templateDao.deleteTemplate(entity)
            // TODO: Add to sync queue
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Match event fields to a template.
     */
    suspend fun matchTemplate(
        categoryId: Int?,
        formaId: Int?,
        causeId: Int?,
        developId: Int?,
        effectId: Int?
    ): ContextTemplateEntity? {
        return templateDao.matchTemplate(categoryId, formaId, causeId, developId, effectId)
    }

    /**
     * Sync templates from server.
     */
    suspend fun syncFromServer(): Result<Int> {
        return try {
            val response = api.listContexts()
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to sync templates: ${response.code()}"))
            }
            
            val templates = response.body()!!.data
            var syncedCount = 0
            
            templates.forEach { templateDto ->
                val entity = ContextTemplateEntity(
                    id = templateDto.id,
                    name = templateDto.name,
                    categoryId = templateDto.categoryId,
                    formaId = templateDto.formaId,
                    causeId = templateDto.causeId,
                    developId = templateDto.developId,
                    effectId = templateDto.effectId,
                    description = templateDto.description
                )
                
                templateDao.insertTemplate(entity) // REPLACE strategy handles updates
                syncedCount++
            }
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

