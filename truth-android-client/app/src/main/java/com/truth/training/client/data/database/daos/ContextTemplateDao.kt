package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextTemplateDao {
    @Query("SELECT * FROM context ORDER BY name ASC")
    suspend fun listTemplates(): List<ContextTemplateEntity>

    @Query("SELECT * FROM context ORDER BY name ASC")
    fun listTemplatesFlow(): Flow<List<ContextTemplateEntity>>

    @Query("SELECT * FROM context WHERE id = :id")
    suspend fun getTemplateById(id: Int): ContextTemplateEntity?

    @Query("SELECT * FROM context WHERE name = :name")
    suspend fun getTemplateByName(name: String): ContextTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ContextTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: ContextTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: ContextTemplateEntity)

    // Template matching: find template with matching non-NULL fields
    @Query(
        """
        SELECT * FROM context
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:formaId IS NULL OR forma_id = :formaId)
          AND (:causeId IS NULL OR cause_id = :causeId)
          AND (:developId IS NULL OR develop_id = :developId)
          AND (:effectId IS NULL OR effect_id = :effectId)
        LIMIT 1
        """
    )
    suspend fun matchTemplate(
        categoryId: Int?,
        formaId: Int?,
        causeId: Int?,
        developId: Int?,
        effectId: Int?
    ): ContextTemplateEntity?

    // Duplicate detection: check if template with identical non-NULL fields exists
    @Query(
        """
        SELECT COUNT(*) FROM context
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:formaId IS NULL OR forma_id = :formaId)
          AND (:causeId IS NULL OR cause_id = :causeId)
          AND (:developId IS NULL OR develop_id = :developId)
          AND (:effectId IS NULL OR effect_id = :effectId)
          AND (:excludeId = 0 OR id != :excludeId)
        """
    )
    suspend fun countDuplicateTemplates(
        categoryId: Int?,
        formaId: Int?,
        causeId: Int?,
        developId: Int?,
        effectId: Int?,
        excludeId: Int = 0
    ): Int
}

