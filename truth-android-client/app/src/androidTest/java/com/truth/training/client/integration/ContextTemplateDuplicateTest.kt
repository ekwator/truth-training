package com.truth.training.client.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.ContextTemplateRepository
import com.truth.training.client.data.network.dto.CreateContextRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration test: Scenario 2 - Context template creation and duplicate detection.
 * Validates that duplicate templates (identical non-NULL fields) are rejected.
 */
@RunWith(AndroidJUnit4::class)
class ContextTemplateDuplicateTest {
    private lateinit var database: TruthDatabase
    private lateinit var templateRepository: ContextTemplateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        templateRepository = ContextTemplateRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun creatingDuplicateTemplateReturnsConflictError() = runBlocking {
        // Step 1: Create first template
        val request1 = CreateContextRequest(
            name = "Template 1",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "First template"
        )
        
        val result1 = templateRepository.createTemplate(request1)
        assertTrue(result1.isSuccess)
        assertNotNull(result1.getOrNull())

        // Step 2: Attempt to create duplicate template (same fields, different name)
        val request2 = CreateContextRequest(
            name = "Template 2", // Different name, but same context fields
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "Duplicate template"
        )
        
        val result2 = templateRepository.createTemplate(request2)
        assertTrue(result2.isFailure)
        val error = result2.exceptionOrNull()
        assertNotNull(error)
        val message = error!!.message
        assertNotNull(message)
        assertTrue(message!!.contains("409") || message.contains("duplicate"))
    }

    @Test
    fun templatesWithDifferentFieldsAreAllowed() = runBlocking {
        val request1 = CreateContextRequest("Template A", 1, 2, 3, 4, 5, null)
        val request2 = CreateContextRequest("Template B", 10, 20, 30, 40, 50, null)
        
        val result1 = templateRepository.createTemplate(request1)
        val result2 = templateRepository.createTemplate(request2)
        
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
    }

    @Test
    fun templatesWithNullFieldsAreMatchedCorrectly() = runBlocking {
        val request1 = CreateContextRequest("Template C", null, null, null, null, null, null)
        val request2 = CreateContextRequest("Template D", null, null, null, null, null, null)
        
        val result1 = templateRepository.createTemplate(request1)
        val result2 = templateRepository.createTemplate(request2)
        
        // Both should succeed (NULL fields don't count as duplicates)
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
    }
}

