package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.JudgmentStatsResponse
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
 * Contract test for GET /api/v1/judgments/stats/{event_id} endpoint.
 */
class ContractJudgmentStatsTest {
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
    fun `GET judgment stats returns consensus statistics`() = runBlocking {
        val expectedResponse = JudgmentStatsResponse(
            trueCount = 5,
            falseCount = 2,
            uncertainCount = 1,
            avgConfidence = 0.75,
            lastSubmittedAt = "2024-01-01T12:00:00Z"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.getJudgmentStats("event_123")
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(5, body!!.trueCount)
        assertEquals(2, body.falseCount)
        assertEquals(1, body.uncertainCount)
        assertEquals(0.75, body.avgConfidence, 0.01)
        
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/judgments/stats/event_123", request.path)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

