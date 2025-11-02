package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.EventDetailsResponse
import com.truth.training.client.data.network.dto.Impact
import com.truth.training.client.data.network.dto.Judgment
import com.truth.training.client.data.network.dto.Summary
import com.truth.training.client.data.network.dto.Consensus
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
 * Contract test for GET /api/v1/events/{id} endpoint.
 */
class ContractEventGetTest {
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
    fun `GET event by id returns EventDetailsResponse with impacts judgments summary`() = runBlocking {
        val expectedResponse = EventDetailsResponse(
            id = "event_123",
            title = "Test Event",
            description = "Description",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null,
            status = "active",
            impacts = listOf(Impact("impact_1", "event_123", 3, "Notes", "2024-01-01T00:00:00Z")),
            judgments = listOf(Judgment("judg_1", "event_123", "true", 0.8, "Reasoning", "2024-01-01T00:00:00Z")),
            summary = Summary("summ_1", "event_123", "Summary text", "Recommendations", "2024-01-01T00:00:00Z"),
            consensus = Consensus("true", 0.75, 10, 0.8)
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.getEvent("event_123")
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("event_123", body!!.id)
        assertEquals(1, body.impacts.size)
        assertEquals(1, body.judgments.size)
        assertNotNull(body.summary)
        assertNotNull(body.consensus)
        
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/events/event_123", request.path)
    }

    @Test
    fun `GET non-existent event returns 404`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val response = api.getEvent("event_nonexistent")
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

