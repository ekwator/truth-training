package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.CreateImpactRequest
import com.truth.training.client.data.network.dto.Impact
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
 * Contract test for POST /api/v1/impacts endpoint.
 */
class ContractImpactAddTest {
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
    fun `POST impacts adds impact with level 1-5`() = runBlocking {
        val request = CreateImpactRequest(
            eventId = "event_123",
            impactLevel = 3,
            notes = "Moderate impact observed"
        )
        
        val expectedResponse = Impact(
            id = "impact_123",
            eventId = request.eventId,
            impactLevel = request.impactLevel,
            notes = request.notes,
            createdAt = "2024-01-01T00:00:00Z"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.addImpact(request)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("event_123", body!!.eventId)
        assertEquals(3, body.impactLevel)
        assertEquals("Moderate impact observed", body.notes)
        
        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/impacts", httpRequest.path)
    }

    @Test
    fun `POST impacts with invalid level returns 400`() = runBlocking {
        val request = CreateImpactRequest("event_123", 10, null) // Invalid: > 5
        
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        
        val response = api.addImpact(request)
        
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

