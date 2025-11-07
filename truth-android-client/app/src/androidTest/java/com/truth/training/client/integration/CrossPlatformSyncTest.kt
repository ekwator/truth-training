package com.truth.training.client.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.network.dto.EventResponse
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration test: Scenario 6 - Cross-platform data consistency.
 * Validates that Android events match Desktop data model with embedded fields.
 */
@RunWith(AndroidJUnit4::class)
class CrossPlatformSyncTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventRepository: EventRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepository = EventRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun eventsCreatedInAndroidMatchDesktopV100Schema() = runTest {
        // Step 1: Create event with all embedded fields (v1.0.0 schema)
        val request = CreateEventRequest(
            title = "Cross-Platform Event",
            description = "Created on Android",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        
        val result = eventRepository.createEvent(request)
        assertTrue(result.isSuccess)
        val event = result.getOrNull()!!
        
        // Step 2: Verify embedded fields (not context_id)
        assertNotNull(event.categoryId)
        assertNotNull(event.formaId)
        assertNotNull(event.causeId)
        assertNotNull(event.developId)
        assertNotNull(event.effectId)
        // Note: EventEntity doesn't have context_id field (v1.0.0 schema)

        // Step 3: Verify data format matches Desktop
        assertEquals("Cross-Platform Event", event.title)
        assertEquals(1, event.categoryId)
        assertEquals(2, event.formaId)
        assertEquals(3, event.causeId)
        assertEquals(4, event.developId)
        assertEquals(5, event.effectId)
        
        // Step 4: Verify ISO 8601 date format
        assertNotNull(event.createdAt)
        assertTrue("CreatedAt should be ISO 8601 format", 
            event.createdAt.contains("T") || event.createdAt.contains("Z"))

        // Step 5: Verify event can be converted to API DTO format
        val eventResponse = EventResponse(
            id = event.id,
            title = event.title,
            description = event.description,
            categoryId = event.categoryId,
            formaId = event.formaId,
            causeId = event.causeId,
            developId = event.developId,
            effectId = event.effectId,
            startDate = event.startDate,
            endDate = event.endDate,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt,
            status = event.status
        )
        
        // Verify DTO matches Desktop format
        assertEquals(event.id, eventResponse.id)
        assertEquals(event.categoryId, eventResponse.categoryId)
    }

    @Test
    fun eventsWithPartialContextFieldsAreValid() = runTest {
        // Event with only some context fields
        val request = CreateEventRequest(
            title = "Partial Context Event",
            description = null,
            categoryId = 1,
            formaId = null,
            causeId = 2,
            developId = null,
            effectId = null,
            startDate = null,
            endDate = null
        )
        
        val result = eventRepository.createEvent(request)
        assertTrue(result.isSuccess)
        val event = result.getOrNull()!!
        
        // Verify partial fields are valid (all optional in v1.0.0)
        assertEquals(1, event.categoryId)
        assertNull(event.formaId)
        assertEquals(2, event.causeId)
        assertNull(event.developId)
        assertNull(event.effectId)
    }
}

