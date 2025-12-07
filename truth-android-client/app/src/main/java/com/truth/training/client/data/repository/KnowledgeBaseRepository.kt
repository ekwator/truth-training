package com.truth.training.client.data.repository

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Knowledge Base tables (category, forma, cause, develop, effect).
 * Provides access to lookup data for context fields in events and templates.
 */
class KnowledgeBaseRepository(
    private val database: TruthDatabase
) {
    private val categoryDao = database.categoryDao()
    private val formaDao = database.formaDao()
    private val causeDao = database.causeDao()
    private val developDao = database.developDao()
    private val effectDao = database.effectDao()

    /**
     * Get all categories as Flow.
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.listCategoriesFlow()

    /**
     * Get all formas as Flow.
     */
    fun getAllFormasFlow(): Flow<List<FormaEntity>> = formaDao.listFormasFlow()

    /**
     * Get all causes as Flow.
     */
    fun getAllCausesFlow(): Flow<List<CauseEntity>> = causeDao.listCausesFlow()

    /**
     * Get all develops as Flow.
     */
    fun getAllDevelopsFlow(): Flow<List<DevelopEntity>> = developDao.listDevelopsFlow()

    /**
     * Get all effects as Flow.
     */
    fun getAllEffectsFlow(): Flow<List<EffectEntity>> = effectDao.listEffectsFlow()

    /**
     * Get all categories from local database.
     */
    suspend fun listCategories(): List<CategoryEntity> = categoryDao.listCategories()

    /**
     * Get all formas from local database.
     */
    suspend fun listFormas(): List<FormaEntity> = formaDao.listFormas()

    /**
     * Get all causes from local database.
     */
    suspend fun listCauses(): List<CauseEntity> = causeDao.listCauses()

    /**
     * Get all develops from local database.
     */
    suspend fun listDevelops(): List<DevelopEntity> = developDao.listDevelops()

    /**
     * Get all effects from local database.
     */
    suspend fun listEffects(): List<EffectEntity> = effectDao.listEffects()

    /**
     * Get category by ID.
     */
    suspend fun getCategoryById(id: Int): CategoryEntity? = categoryDao.getCategoryById(id)

    /**
     * Get forma by ID.
     */
    suspend fun getFormaById(id: Int): FormaEntity? = formaDao.getFormaById(id)

    /**
     * Get cause by ID.
     */
    suspend fun getCauseById(id: Int): CauseEntity? = causeDao.getCauseById(id)

    /**
     * Get develop by ID.
     */
    suspend fun getDevelopById(id: Int): DevelopEntity? = developDao.getDevelopById(id)

    /**
     * Get effect by ID.
     */
    suspend fun getEffectById(id: Int): EffectEntity? = effectDao.getEffectById(id)
}

