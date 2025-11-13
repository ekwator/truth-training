package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContractEventGetTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi
    private lateinit var gson: Gson

    @Before
    fun setup() {
        mockWebServer = MockWebServer().apply { start() }
        gson = Gson()
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TruthApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `GET event by id returns canonical details`() = runBlocking {
        val expectedResponse = EventDetailsResponse(
            id = 321L,
            description = "Detailed Event",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            vector = true,
            detected = true,
            corrected = false,
            timestampStart = 1_000L,
            timestampEnd = 2_000L,
            code = 1,
            collectiveScore = 0.75,
            impacts = listOf(Impact(id = 1L, eventId = 321L, value = true, notes = "note", createdAt = 1_500L)),
            judgments = listOf(Judgment(id = "judg_1", eventId = 321L, assessment = "true", confidenceLevel = 0.8, reasoning = "reason", submittedAt = "2024-01-01T00:00:00Z")),
            summary = Summary(id = "sum_1", eventId = 321L, summaryText = "Summary", recommendations = "Rec", updatedAt = "2024-01-01T00:00:00Z"),
            consensus = Consensus(dominantAssessment = "true", confidence = 0.7, totalJudgments = 5, weightedConfidence = 0.72)
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.getEvent(321L)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(321L, body!!.id)
        assertEquals(1, body.impacts.size)
        assertEquals(1, body.judgments.size)
        assertNotNull(body.summary)
        assertNotNull(body.consensus)

        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/events/321", request.path)
    }

    @Test
    fun `GET non-existent event returns 404`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val response = api.getEvent(404L)
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}

