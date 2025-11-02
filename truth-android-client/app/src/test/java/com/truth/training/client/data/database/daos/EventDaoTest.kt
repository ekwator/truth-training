package com.truth.training.client.data.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for EventDao.
 */
@RunWith(AndroidJUnit4::class)
class EventDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventDao: EventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()
        eventDao = database.eventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get event by id`() = runBlocking {
        val event = createTestEvent("event_1", "Test Event")
        eventDao.insertEvent(event)
        
        val retrieved = eventDao.getEventById("event_1")
        assertNotNull(retrieved)
        assertEquals("Test Event", retrieved!!.title)
        assertEquals("event_1", retrieved.id)
    }

    @Test
    fun `get event by id flow`() = runBlocking {
        val event = createTestEvent("event_2", "Flow Test")
        eventDao.insertEvent(event)
        
        val flow = eventDao.getEventByIdFlow("event_2")
        val retrieved = flow.first()
        assertNotNull(retrieved)
        assertEquals("Flow Test", retrieved!!.title)
    }

    @Test
    fun `list events with pagination`() = runBlocking {
        // Insert multiple events
        repeat(10) { i ->
            eventDao.insertEvent(createTestEvent("event_$i", "Event $i"))
        }
        
        val events = eventDao.listEvents(limit = 5, offset = 0)
        assertEquals(5, events.size)
        
        val nextPage = eventDao.listEvents(limit = 5, offset = 5)
        assertEquals(5, nextPage.size)
    }

    @Test
    fun `list events by status`() = runBlocking {
        eventDao.insertEvent(createTestEvent("event_active", "Active", "active"))
        eventDao.insertEvent(createTestEvent("event_inactive", "Inactive", "inactive"))
        
        val activeEvents = eventDao.listEventsByStatus("active", limit = 10, offset = 0)
        assertEquals(1, activeEvents.size)
        assertEquals("Active", activeEvents[0].title)
    }

    @Test
    fun `update event`() = runBlocking {
        val event = createTestEvent("event_3", "Original")
        eventDao.insertEvent(event)
        
        val updated = event.copy(title = "Updated", status = "archived")
        eventDao.updateEvent(updated)
        
        val retrieved = eventDao.getEventById("event_3")
        assertEquals("Updated", retrieved!!.title)
        assertEquals("archived", retrieved.status)
    }

    @Test
    fun `delete event`() = runBlocking {
        val event = createTestEvent("event_4", "To Delete")
        eventDao.insertEvent(event)
        
        eventDao.deleteEvent(event)
        
        val retrieved = eventDao.getEventById("event_4")
        assertNull(retrieved)
    }

    @Test
    fun `get event count`() = runBlocking {
        repeat(5) { i ->
            eventDao.insertEvent(createTestEvent("event_$i", "Event $i"))
        }
        
        val count = eventDao.getEventCount()
        assertEquals(5, count)
    }

    @Test
    fun `get all events flow`() = runBlocking {
        repeat(3) { i ->
            eventDao.insertEvent(createTestEvent("event_$i", "Event $i"))
        }
        
        val flow = eventDao.getAllEventsFlow()
        val events = flow.first()
        assertEquals(3, events.size)
    }

    private fun createTestEvent(
        id: String = "event_test",
        title: String = "Test Event",
        status: String = "active"
    ): EventEntity {
        return EventEntity(
            id = id,
            title = title,
            description = "Test description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null,
            status = status
        )
    }
}

