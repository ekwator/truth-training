package com.truth.training.client.testing

import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.CategoryEntity
import com.truth.training.client.data.database.entities.CauseEntity
import com.truth.training.client.data.database.entities.DevelopEntity
import com.truth.training.client.data.database.entities.EffectEntity
import com.truth.training.client.data.database.entities.FormaEntity
import com.truth.training.client.data.database.entities.ImpactTypeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared helpers for androidTest targets to seed the canonical knowledge base tables.
 *
 * The Room schema enforces foreign keys from `truth_events` and `context`
 * to the knowledge base tables. Any tests that insert rows referencing those
 * IDs must pre-seed the parents to avoid SQLITE_CONSTRAINT errors.
 */
object TestDataSeeder {
    private val defaultIds = setOf(1, 2, 3, 4, 5, 6, 10, 20, 30, 40, 50)

    suspend fun seedKnowledgeBase(
        database: TruthDatabase,
        ids: Set<Int> = defaultIds
    ) = withContext(Dispatchers.IO) {
        val categoryDao = database.categoryDao()
        val formaDao = database.formaDao()
        val causeDao = database.causeDao()
        val developDao = database.developDao()
        val effectDao = database.effectDao()
        val impactTypeDao = database.impactTypeDao()

        ids.forEach { id ->
            categoryDao.insertCategory(
                CategoryEntity(
                    id = id,
                    name = "Category $id",
                    description = "Seeded category $id"
                )
            )
            formaDao.insertForma(
                FormaEntity(
                    id = id,
                    name = "Forma $id",
                    quality = id % 2 == 0,
                    description = "Seeded forma $id"
                )
            )
            causeDao.insertCause(
                CauseEntity(
                    id = id,
                    name = "Cause $id",
                    quality = id % 2 == 1,
                    description = "Seeded cause $id"
                )
            )
            developDao.insertDevelop(
                DevelopEntity(
                    id = id,
                    name = "Develop $id",
                    quality = true,
                    description = "Seeded develop $id"
                )
            )
            effectDao.insertEffect(
                EffectEntity(
                    id = id,
                    name = "Effect $id",
                    quality = id % 2 == 0,
                    description = "Seeded effect $id"
                )
            )
            impactTypeDao.insertImpactType(
                ImpactTypeEntity(
                    id = id,
                    name = "Impact $id",
                    description = "Seeded impact type $id"
                )
            )
        }
    }
}

