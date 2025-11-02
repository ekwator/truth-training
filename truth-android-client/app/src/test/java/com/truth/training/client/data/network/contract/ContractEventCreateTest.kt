package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.CreateEventRequest
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
 * Contract test for POST /api/v1/events endpoint (v1.0.0 with embedded fields).
 */
class ContractEventCreateTest {
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
    fun `POST events creates event with embedded context fields`() = runBlocking {
        // Arrange
        val request = CreateEventRequest(
            title = "New Event",
            description = "Event Description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )
        
        val expectedResponse = EventResponse(
            id = "event_new",
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            formaId = request.formaId,
            causeId = request.causeId,
            developId = request.developId,
            effectId = request.effectId,
            startDate = request.startDate,
            endDate = request.endDate,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null,
            status = "active"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        // Act
        val response = api.createEvent(request)
        
        // Assert
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("New Event", body!!.title)
        assertEquals(1, body.categoryId)
        assertEquals(2, body.formaId)
        
        // Validate request schema
        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/events", httpRequest.path)
    }

    @Test
    fun `POST events with invalid title returns 400`() = runBlocking {
        // Arrange
        val request = CreateEventRequest(
            title = "", // Invalid: empty title
            description = null
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
        )

        // Act
        val response = api.createEvent(request)
        
        // Assert
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

