package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.EventListResponse
import com.truth.training.client.data.network.dto.EventResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.truth.training.client.data.network.TruthApi
import com.google.gson.Gson
import org.junit.Assert.*

/**
 * Contract test for GET /api/v1/events endpoint (v1.0.0 with embedded fields).
 */
class ContractEventsListTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi
    private lateinit var gson: Gson

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        gson = Gson()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        api = retrofit.create(TruthApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `GET events returns EventListResponse with embedded context fields`() = runBlocking {
        // Arrange
        val events = listOf(
            EventResponse(
                id = "event_123",
                title = "Test Event",
                description = "Test Description",
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
        )
        val expectedResponse = EventListResponse(data = events, total = 1)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        // Act
        val response = api.listEvents(limit = 35, offset = 0, status = null)
        
        // Assert
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(1, body!!.data.size)
        assertEquals(1, body.total)
        
        val event = body.data[0]
        assertEquals("event_123", event.id)
        assertEquals(1, event.categoryId)
        assertEquals(2, event.formaId)
        assertEquals(3, event.causeId)
        assertEquals(4, event.developId)
        assertEquals(5, event.effectId)
        
        // Validate request
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/api/v1/events"))
        assertTrue(request.path!!.contains("limit=35"))
        assertTrue(request.path!!.contains("offset=0"))
    }

    @Test
    fun `GET events with status filter returns filtered results`() = runBlocking {
        // Arrange
        val expectedResponse = EventListResponse(data = emptyList(), total = 0)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        // Act
        val response = api.listEvents(limit = 35, offset = 0, status = "active")
        
        // Assert
        assertTrue(response.isSuccessful)
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("status=active"))
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

