package com.truth.training.client.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.data.repository.ContextTemplateRepository
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.network.dto.CreateContextRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration test: Scenario 1 - Event creation with context template.
 * Tests the full flow from template creation to event creation with template matching.
 */
@RunWith(AndroidJUnit4::class)
class EventCreationWithTemplateTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventRepository: EventRepository
    private lateinit var templateRepository: ContextTemplateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TruthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Create repositories with null API (offline mode)
        eventRepository = EventRepository(database, null)
        templateRepository = ContextTemplateRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create event with context template matches template and prefills fields`() = runBlocking {
        // Step 1: Create context template
        val templateRequest = CreateContextRequest(
            name = "Standard Training Event",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "Standard template for training events"
        )
        
        val templateResult = templateRepository.createTemplate(templateRequest)
        assertTrue(templateResult.isSuccess)
        val template = templateResult.getOrNull()
        assertNotNull(template)
        assertEquals("Standard Training Event", template!!.name)

        // Step 2: Match template based on event fields
        val matchedTemplate = templateRepository.matchTemplate(1, 2, 3, 4, 5)
        assertNotNull(matchedTemplate)
        assertEquals(template.id, matchedTemplate!!.id)

        // Step 3: Create event with template-matched fields
        val eventRequest = CreateEventRequest(
            title = "New Training Event",
            description = "Created using template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5
        )
        
        val eventResult = eventRepository.createEvent(eventRequest)
        assertTrue(eventResult.isSuccess)
        val event = eventResult.getOrNull()
        assertNotNull(event)
        assertEquals("New Training Event", event!!.title)
        assertEquals(1, event.categoryId)
        assertEquals(2, event.formaId)

        // Step 4: Verify event is saved in database
        val savedEvent = eventRepository.getEventById(event.id)
        assertNotNull(savedEvent)
        assertEquals(event.title, savedEvent!!.title)
    }
}

