package com.truth.training.client.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.ContextTemplateEntity
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit

/**
 * Unit tests for ContextTemplateRepository.
 * 
 * Coverage targets:
 * - createTemplate() - duplicate detection, local save
 * - updateTemplate() - duplicate validation, conflict resolution
 * - deleteTemplate() - cascade checks, sync queue
 * - matchTemplate() - non-NULL field matching
 * - countDuplicateTemplates() - duplicate logic validation
 * - syncFromServer() - server sync, template merge
 * 
 * Edge cases:
 * - Templates with all NULL fields
 * - Templates with partial fields
 * - Duplicate detection with exclude ID
 * 
 * Target coverage: ≥95%
 */
@RunWith(AndroidJUnit4::class)
class ContextTemplateRepositoryTest {
    private lateinit var database: TruthDatabase
    private lateinit var repository: ContextTemplateRepository
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi
    private val gson = GsonBuilder().create()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(TruthApi::class.java)
        repository = ContextTemplateRepository(database, api)
    }

    @After
    fun tearDown() {
        database.close()
        mockWebServer.shutdown()
    }

    @Test
    fun `createTemplate saves locally after duplicate check`() = runBlocking {
        val request = CreateContextRequest(
            name = "Test Template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "Test description"
        )

        val result = repository.createTemplate(request)
        
        assertTrue(result.isSuccess)
        val template = result.getOrNull()!!
        assertTrue(template.id > 0)
        assertEquals("Test Template", template.name)
        assertEquals(1, template.categoryId)
        
        // Verify saved in local database
        val retrieved = repository.getTemplateById(template.id)
        assertNotNull(retrieved)
        assertEquals("Test Template", retrieved!!.name)
    }

    @Test
    fun `createTemplate detects duplicate and fails`() = runBlocking {
        val request = CreateContextRequest(
            name = "Template 1",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )

        // Create first template
        val firstResult = repository.createTemplate(request)
        assertTrue(firstResult.isSuccess)

        // Try to create duplicate (same fields, different name)
        val duplicateRequest = CreateContextRequest(
            name = "Template 2", // Different name but same fields
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        val duplicateResult = repository.createTemplate(duplicateRequest)
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull()?.message?.contains("409") == true ||
                   duplicateResult.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun `createTemplate allows templates with all NULL fields`() = runBlocking {
        val request = CreateContextRequest(
            name = "Empty Template",
            categoryId = null,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null,
            description = "Template with all NULL fields"
        )

        val result = repository.createTemplate(request)
        
        assertTrue(result.isSuccess)
        val template = result.getOrNull()!!
        assertEquals("Empty Template", template.name)
        assertNull(template.categoryId)
        assertNull(template.formaId)
    }

    @Test
    fun `createTemplate allows templates with partial fields`() = runBlocking {
        val request = CreateContextRequest(
            name = "Partial Template",
            categoryId = 1,
            formaId = null, // Partial field
            causeId = 3,
            developId = null,
            effectId = 5
        )

        val result = repository.createTemplate(request)
        
        assertTrue(result.isSuccess)
        val template = result.getOrNull()!!
        assertEquals("Partial Template", template.name)
        assertEquals(1, template.categoryId)
        assertNull(template.formaId)
        assertEquals(3, template.causeId)
    }

    @Test
    fun `updateTemplate updates locally after duplicate validation`() = runBlocking {
        // Create template first
        val createRequest = CreateContextRequest(
            name = "Original",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        val created = repository.createTemplate(createRequest).getOrNull()!!
        val templateId = created.id

        // Update template
        val updateRequest = CreateContextRequest(
            name = "Updated",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "Updated description"
        )
        
        val result = repository.updateTemplate(templateId, updateRequest)
        
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("Updated", updated.name)
        assertEquals("Updated description", updated.description)
        
        // Verify local update
        val retrieved = repository.getTemplateById(templateId)
        assertEquals("Updated", retrieved!!.name)
    }

    @Test
    fun `updateTemplate detects duplicate when excluding current ID`() = runBlocking {
        // Create two different templates
        val template1Request = CreateContextRequest(
            name = "Template 1",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        val template1 = repository.createTemplate(template1Request).getOrNull()!!

        val template2Request = CreateContextRequest(
            name = "Template 2",
            categoryId = 10,
            formaId = 20,
            causeId = 30,
            developId = 40,
            effectId = 50
        )
        val template2 = repository.createTemplate(template2Request).getOrNull()!!

        // Try to update template2 with template1's fields (should detect duplicate)
        val updateRequest = CreateContextRequest(
            name = "Template 2 Updated",
            categoryId = 1, // Same as template1
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        val result = repository.updateTemplate(template2.id, updateRequest)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("409") == true ||
                   result.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun `updateTemplate handles error when template not found`() = runBlocking {
        val updateRequest = CreateContextRequest(
            name = "Updated",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        val result = repository.updateTemplate(9999, updateRequest)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `deleteTemplate deletes locally immediately`() = runBlocking {
        val createRequest = CreateContextRequest(
            name = "To Delete",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        val created = repository.createTemplate(createRequest).getOrNull()!!
        val templateId = created.id

        val result = repository.deleteTemplate(templateId)
        
        assertTrue(result.isSuccess)
        
        // Verify deleted from local database
        val retrieved = repository.getTemplateById(templateId)
        assertNull(retrieved)
    }

    @Test
    fun `deleteTemplate handles error when template not found`() = runBlocking {
        val result = repository.deleteTemplate(9999)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `matchTemplate matches by non-NULL fields`() = runBlocking {
        // Create template with specific fields
        val createRequest = CreateContextRequest(
            name = "Match Template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        repository.createTemplate(createRequest)

        // Match with exact fields
        val matched = repository.matchTemplate(
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        assertNotNull(matched)
        assertEquals("Match Template", matched!!.name)
    }

    @Test
    fun `matchTemplate returns null when no match found`() = runBlocking {
        // Create template
        val createRequest = CreateContextRequest(
            name = "Template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        repository.createTemplate(createRequest)

        // Try to match with different fields
        val matched = repository.matchTemplate(
            categoryId = 99, // Different
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        assertNull(matched)
    }

    @Test
    fun `matchTemplate handles partial field matching`() = runBlocking {
        // Create template with partial fields
        val createRequest = CreateContextRequest(
            name = "Partial Match",
            categoryId = 1,
            formaId = null,
            causeId = 3,
            developId = null,
            effectId = 5
        )
        repository.createTemplate(createRequest)

        // Match with matching non-NULL fields
        val matched = repository.matchTemplate(
            categoryId = 1,
            formaId = null,
            causeId = 3,
            developId = null,
            effectId = 5
        )
        
        assertNotNull(matched)
        assertEquals("Partial Match", matched!!.name)
    }

    @Test
    fun `syncFromServer syncs templates from API to local database`() = runBlocking {
        // Mock API response
        val templateDto = ContextTemplate(
            id = 100,
            name = "Server Template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "From server"
        )
        
        val listResponse = ContextListResponse(
            data = listOf(templateDto),
            total = 1
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(listResponse))
        )

        val result = repository.syncFromServer()
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        
        // Verify template synced to local database
        val synced = repository.getTemplateById(100)
        assertNotNull(synced)
        assertEquals("Server Template", synced!!.name)
    }

    @Test
    fun `syncFromServer handles API error`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val result = repository.syncFromServer()
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAllTemplatesFlow emits templates reactively`() = runBlocking {
        // Create multiple templates
        repeat(3) { i ->
            val request = CreateContextRequest(
                name = "Template $i",
                categoryId = 1,
                formaId = 2,
                causeId = 3,
                developId = 4,
                effectId = 5
            )
            repository.createTemplate(request)
        }
        
        // Test Flow emission
        val flow = repository.getAllTemplatesFlow()
        val templates = flow.first()
        
        assertEquals(3, templates.size)
        assertTrue(templates.any { it.name == "Template 0" })
        assertTrue(templates.any { it.name == "Template 1" })
        assertTrue(templates.any { it.name == "Template 2" })
    }

    @Test
    fun `getTemplateByName returns template by name`() = runBlocking {
        val request = CreateContextRequest(
            name = "Unique Name",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        repository.createTemplate(request)

        val retrieved = repository.getTemplateByName("Unique Name")
        assertNotNull(retrieved)
        assertEquals("Unique Name", retrieved!!.name)
    }

    @Test
    fun `getTemplateByName returns null for non-existent template`() = runBlocking {
        val retrieved = repository.getTemplateByName("Non Existent")
        assertNull(retrieved)
    }

    @Test
    fun `listTemplates returns all templates`() = runBlocking {
        repeat(5) { i ->
            val request = CreateContextRequest(
                name = "Template $i",
                categoryId = 1,
                formaId = 2,
                causeId = 3,
                developId = 4,
                effectId = 5
            )
            repository.createTemplate(request)
        }
        
        val templates = repository.listTemplates()
        assertEquals(5, templates.size)
    }
}

