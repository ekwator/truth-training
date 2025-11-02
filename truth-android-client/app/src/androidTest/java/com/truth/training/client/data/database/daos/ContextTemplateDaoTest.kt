package com.truth.training.client.data.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for ContextTemplateDao.
 */
@RunWith(AndroidJUnit4::class)
class ContextTemplateDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var templateDao: ContextTemplateDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()
        templateDao = database.contextTemplateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetTemplateById() = runBlocking {
        val template = createTestTemplate(name = "Template 1")
        val id = templateDao.insertTemplate(template)
        
        val retrieved = templateDao.getTemplateById(id.toInt())
        assertNotNull(retrieved)
        assertEquals("Template 1", retrieved!!.name)
        assertEquals(1, retrieved.categoryId)
    }

    @Test
    fun getTemplateByName() = runBlocking {
        val template = createTestTemplate(name = "Unique Template")
        templateDao.insertTemplate(template)
        
        val retrieved = templateDao.getTemplateByName("Unique Template")
        assertNotNull(retrieved)
        assertEquals("Unique Template", retrieved!!.name)
    }

    @Test
    fun listTemplatesFlow() = runBlocking {
        repeat(5) { i ->
            templateDao.insertTemplate(createTestTemplate(name = "Template $i"))
        }
        
        val flow = templateDao.listTemplatesFlow()
        val templates = flow.first()
        assertEquals(5, templates.size)
        assertEquals("Template 0", templates[0].name) // Should be sorted by name
    }

    @Test
    fun matchTemplateWithExactFields() = runBlocking {
        templateDao.insertTemplate(createTestTemplate(name = "Matched", categoryId = 1, formaId = 2))
        
        val matched = templateDao.matchTemplate(
            categoryId = 1,
            formaId = 2,
            causeId = null,
            developId = null,
            effectId = null
        )
        assertNotNull(matched)
        assertEquals("Matched", matched!!.name)
    }

    @Test
    fun matchTemplateWithPartialFields() = runBlocking {
        templateDao.insertTemplate(createTestTemplate(name = "Partial", categoryId = 1, formaId = null))
        
        val matched = templateDao.matchTemplate(
            categoryId = 1,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null
        )
        assertNotNull(matched)
        assertEquals("Partial", matched!!.name)
    }

    @Test
    fun countDuplicateTemplates() = runBlocking {
        val template1 = createTestTemplate(name = "Template 1", categoryId = 1, formaId = 2)
        val id1 = templateDao.insertTemplate(template1).toInt()
        
        val duplicateCount = templateDao.countDuplicateTemplates(
            categoryId = 1,
            formaId = 2,
            causeId = null,
            developId = null,
            effectId = null,
            excludeId = 0
        )
        assertEquals(1, duplicateCount)
        
        // Excluding the same template should return 0
        val noDuplicate = templateDao.countDuplicateTemplates(
            categoryId = 1,
            formaId = 2,
            causeId = null,
            developId = null,
            effectId = null,
            excludeId = id1
        )
        assertEquals(0, noDuplicate)
    }

    @Test
    fun updateAndDeleteTemplate() = runBlocking {
        val template = createTestTemplate(name = "Original")
        val id = templateDao.insertTemplate(template).toInt()
        
        val updated = template.copy(id = id, name = "Updated", description = "New description")
        templateDao.updateTemplate(updated)
        
        val retrieved = templateDao.getTemplateById(id)
        assertEquals("Updated", retrieved!!.name)
        assertEquals("New description", retrieved.description)
        
        templateDao.deleteTemplate(retrieved)
        val deleted = templateDao.getTemplateById(id)
        assertNull(deleted)
    }

    private fun createTestTemplate(
        name: String = "Test Template",
        categoryId: Int? = 1,
        formaId: Int? = 2,
        causeId: Int? = 3,
        developId: Int? = 4,
        effectId: Int? = 5,
        description: String? = "Test description"
    ): ContextTemplateEntity {
        return ContextTemplateEntity(
            id = 0, // Will be auto-generated
            name = name,
            categoryId = categoryId,
            formaId = formaId,
            causeId = causeId,
            developId = developId,
            effectId = effectId,
            description = description
        )
    }
}

