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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {
    private lateinit var database: TruthDatabase
    private lateinit var repository: EventRepository
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi
    private val gson = GsonBuilder().create()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(TruthApi::class.java)
        repository = EventRepository(database, api)
    }

    @After
    fun tearDown() {
        try {
            database.close()
        } catch (_: Exception) {
        }
        try {
            if (::mockWebServer.isInitialized) {
                mockWebServer.shutdown()
            }
        } catch (_: Exception) {
        }
    }

    @Test
    fun createEventSavesLocallyImmediately() = runTest {
        val request = CreateEventRequest(
            description = "Test Event",
            categoryId = 1,
            timestampStart = 1_000L
        )

        val result = repository.createEvent(request)

        assertTrue(result.isSuccess)
        val entity = result.getOrNull()!!
        assertTrue(entity.id > 0)
        assertEquals("Test Event", entity.description)

        val retrieved = repository.getEventById(entity.id)
        assertNotNull(retrieved)
        assertEquals("Test Event", retrieved!!.description)
    }

    @Test
    fun updateEventUpdatesLocallyImmediately() = runTest {
        val created = repository.createEvent(
            CreateEventRequest(
                description = "Original",
                timestampStart = 1_000L
            )
        ).getOrNull()!!

        val result = repository.updateEvent(
            created.id,
            UpdateEventRequest(
                description = "Updated",
                detected = true
            )
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("Updated", updated.description)
        assertTrue(updated.detected!!)

        val reloaded = repository.getEventById(created.id)
        assertEquals("Updated", reloaded!!.description)
    }

    @Test
    fun updateEventHandlesMissingEntity() = runTest {
        val result = repository.updateEvent(999L, UpdateEventRequest(description = "Updated"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun deleteEventDeletesLocallyImmediately() = runTest {
        val created = repository.createEvent(
            CreateEventRequest(
                description = "To delete",
                timestampStart = 1_000L
            )
        ).getOrNull()!!

        val result = repository.deleteEvent(created.id)
        assertTrue(result.isSuccess)
        assertNull(repository.getEventById(created.id))
    }

    @Test
    fun deleteEventHandlesMissingEntity() = runTest {
        val result = repository.deleteEvent(1234L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun syncFromServerPersistsEvents() = runTest {
        val eventDto = EventResponse(
            id = 1L,
            description = "Server Event",
            categoryId = 2,
            formaId = 3,
            causeId = 4,
            developId = 5,
            effectId = 6,
            vector = true,
            detected = null,
            corrected = false,
            timestampStart = 42L,
            timestampEnd = null,
            code = 1,
            collectiveScore = null
        )

        val response = EventListResponse(data = listOf(eventDto), total = 1)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(response))
        )

        val result = repository.syncFromServer()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        val saved = repository.getEventById(1L)
        assertNotNull(saved)
        assertEquals("Server Event", saved!!.description)
    }

    @Test
    fun getAllEventsFlowEmitsUpdates() = runTest {
        repository.createEvent(
            CreateEventRequest(
                description = "Flow Event",
                timestampStart = 100L
            )
        )

        val events = repository.getAllEventsFlow().first()
        assertEquals(1, events.size)
        assertEquals("Flow Event", events.first().description)
    }

    private suspend fun enqueueEventDetailsResponse(eventId: Long) {
        val details = EventDetailsResponse(
            id = eventId,
            description = "Detailed",
            categoryId = null,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null,
            vector = true,
            detected = null,
            corrected = false,
            timestampStart = 1L,
            timestampEnd = null,
            code = 1,
            collectiveScore = null
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(details))
        )
    }
}

