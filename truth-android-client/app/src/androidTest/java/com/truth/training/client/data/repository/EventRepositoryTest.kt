package com.truth.training.client.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.EventEntity
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
 * Unit tests for EventRepository.
 * 
 * Coverage targets:
 * - createEvent() - local save, sync queue addition
 * - updateEvent() - local update, conflict handling
 * - deleteEvent() - local delete, queue management
 * - syncFromServer() - server sync, local merge
 * - getAllEventsFlow() - Flow emission, reactive updates
 * - getEventById() - local retrieval, null handling
 * - Offline-first behavior validation
 * 
 * Target coverage: ≥95%
 */
@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {
    private lateinit var database: TruthDatabase
    private lateinit var repository: EventRepository
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi
    private val gson = GsonBuilder().create()

    @Before
    fun setup() {
        // Setup in-memory Room database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()

        // Setup MockWebServer for API mocking
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
        repository = EventRepository(database, api)
    }

    @After
    fun tearDown() {
        database.close()
        mockWebServer.shutdown()
    }

    @Test
    fun `createEvent saves locally immediately`() = runBlocking {
        val request = CreateEventRequest(
            title = "Test Event",
            description = "Test description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )

        val result = repository.createEvent(request)
        
        assertTrue(result.isSuccess)
        val entity = result.getOrNull()!!
        assertNotNull(entity.id)
        assertEquals("Test Event", entity.title)
        
        // Verify saved in local database (offline-first)
        val retrieved = repository.getEventById(entity.id)
        assertNotNull(retrieved)
        assertEquals("Test Event", retrieved!!.title)
    }

    @Test
    fun `updateEvent updates locally immediately`() = runBlocking {
        // Create event first
        val createRequest = CreateEventRequest(
            title = "Original",
            description = "Original description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        val created = repository.createEvent(createRequest).getOrNull()!!
        val eventId = created.id

        // Update event
        val updateRequest = UpdateEventRequest(
            title = "Updated",
            description = "Updated description",
            categoryId = 1
        )
        val result = repository.updateEvent(eventId, updateRequest)
        
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("Updated", updated.title)
        assertEquals("Updated description", updated.description)
        
        // Verify local update
        val retrieved = repository.getEventById(eventId)
        assertEquals("Updated", retrieved!!.title)
    }

    @Test
    fun `updateEvent handles conflict when event not found`() = runBlocking {
        val updateRequest = UpdateEventRequest(
            title = "Updated",
            description = "Updated description"
        )
        
        val result = repository.updateEvent("non_existent_id", updateRequest)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `deleteEvent deletes locally immediately`() = runBlocking {
        // Create event first
        val createRequest = CreateEventRequest(
            title = "To Delete",
            description = "Will be deleted",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        val created = repository.createEvent(createRequest).getOrNull()!!
        val eventId = created.id

        // Delete event
        val result = repository.deleteEvent(eventId)
        
        assertTrue(result.isSuccess)
        
        // Verify deleted from local database
        val retrieved = repository.getEventById(eventId)
        assertNull(retrieved)
    }

    @Test
    fun `deleteEvent handles error when event not found`() = runBlocking {
        val result = repository.deleteEvent("non_existent_id")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `syncFromServer syncs events from API to local database`() = runBlocking {
        // Mock API response
        val eventDto = EventResponse(
            id = "server_event_1",
            title = "Server Event",
            description = "From server",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null,
            status = "active"
        )
        
        val listResponse = EventListResponse(
            data = listOf(eventDto),
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
        
        // Verify event synced to local database
        val synced = repository.getEventById("server_event_1")
        assertNotNull(synced)
        assertEquals("Server Event", synced!!.title)
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
    fun `getAllEventsFlow emits events reactively`() = runBlocking {
        // Create multiple events
        repeat(3) { i ->
            val request = CreateEventRequest(
                title = "Event $i",
                description = "Description $i",
                categoryId = 1,
                formaId = 2,
                causeId = 3,
                developId = 4,
                effectId = 5,
                startDate = "2024-01-01T00:00:00Z",
                endDate = "2024-01-02T00:00:00Z"
            )
            repository.createEvent(request)
        }
        
        // Test Flow emission
        val flow = repository.getAllEventsFlow()
        val events = flow.first()
        
        assertEquals(3, events.size)
        assertTrue(events.any { it.title == "Event 0" })
        assertTrue(events.any { it.title == "Event 1" })
        assertTrue(events.any { it.title == "Event 2" })
    }

    @Test
    fun `getEventById returns null for non-existent event`() = runBlocking {
        val result = repository.getEventById("non_existent")
        assertNull(result)
    }

    @Test
    fun `getEventById returns event from local database`() = runBlocking {
        val request = CreateEventRequest(
            title = "Test Event",
            description = "Test description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        val created = repository.createEvent(request).getOrNull()!!
        
        val retrieved = repository.getEventById(created.id)
        assertNotNull(retrieved)
        assertEquals(created.id, retrieved!!.id)
        assertEquals("Test Event", retrieved.title)
    }

    @Test
    fun `listEvents returns paginated events`() = runBlocking {
        // Create 10 events
        repeat(10) { i ->
            val request = CreateEventRequest(
                title = "Event $i",
                description = "Description $i",
                categoryId = 1,
                formaId = 2,
                causeId = 3,
                developId = 4,
                effectId = 5,
                startDate = "2024-01-01T00:00:00Z",
                endDate = "2024-01-02T00:00:00Z"
            )
            repository.createEvent(request)
        }
        
        val firstPage = repository.listEvents(limit = 5, offset = 0)
        assertEquals(5, firstPage.size)
        
        val secondPage = repository.listEvents(limit = 5, offset = 5)
        assertEquals(5, secondPage.size)
    }

    @Test
    fun `listEvents filters by status`() = runBlocking {
        // Create events with different statuses
        val activeRequest = CreateEventRequest(
            title = "Active Event",
            description = "Active",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        repository.createEvent(activeRequest)
        
        // Update one to archived (simulating status change)
        val allEvents = repository.listEvents()
        val activeEvent = allEvents.first { it.title == "Active Event" }
        repository.updateEvent(activeEvent.id, UpdateEventRequest(status = "archived"))
        
        val archivedEvents = repository.listEvents(status = "archived")
        assertEquals(1, archivedEvents.size)
        assertEquals("Active Event", archivedEvents[0].title)
    }

    @Test
    fun `offline-first behavior - create works without network`() = runBlocking {
        // Simulate network failure by shutting down server
        mockWebServer.shutdown()
        
        val request = CreateEventRequest(
            title = "Offline Event",
            description = "Created offline",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        
        // Should still work (offline-first)
        val result = repository.createEvent(request)
        assertTrue(result.isSuccess)
        
        // Verify saved locally
        val created = result.getOrNull()!!
        val retrieved = repository.getEventById(created.id)
        assertNotNull(retrieved)
        assertEquals("Offline Event", retrieved!!.title)
    }
}

