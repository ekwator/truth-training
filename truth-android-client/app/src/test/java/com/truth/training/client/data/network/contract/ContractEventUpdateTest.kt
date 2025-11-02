package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.UpdateEventRequest
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
 * Contract test for PUT /api/v1/events/{id} endpoint.
 */
class ContractEventUpdateTest {
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
    fun `PUT event updates event with embedded context fields`() = runBlocking {
        val request = UpdateEventRequest(
            title = "Updated Title",
            description = "Updated Description",
            categoryId = 10,
            status = "archived"
        )
        
        val expectedResponse = EventResponse(
            id = "event_123",
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null,
            startDate = null,
            endDate = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-02T00:00:00Z",
            status = request.status!!
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.updateEvent("event_123", request)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("Updated Title", body!!.title)
        assertEquals(10, body.categoryId)
        assertEquals("archived", body.status)
        
        val httpRequest = mockWebServer.takeRequest()
        assertEquals("PUT", httpRequest.method)
        assertEquals("/api/v1/events/event_123", httpRequest.path)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

