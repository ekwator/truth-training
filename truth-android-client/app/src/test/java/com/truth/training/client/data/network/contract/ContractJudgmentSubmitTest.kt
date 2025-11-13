package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateJudgmentRequest
import com.truth.training.client.data.network.dto.Judgment
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

/**
 * Contract test for POST /api/v1/judgments endpoint.
 */
class ContractJudgmentSubmitTest {
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
    fun `POST judgments submits judgment with assessment and confidence`() = runBlocking {
        val request = CreateJudgmentRequest(
            eventId = 123L,
            assessment = "true",
            confidenceLevel = 0.85,
            reasoning = "Strong evidence supports this"
        )

        val expectedResponse = Judgment(
            id = "judg_123",
            eventId = request.eventId,
            assessment = request.assessment,
            confidenceLevel = request.confidenceLevel,
            reasoning = request.reasoning,
            submittedAt = "2024-01-01T00:00:00Z"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.submitJudgment(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(123L, body!!.eventId)
        assertEquals("true", body.assessment)
        assertEquals(0.85, body.confidenceLevel, 0.01)

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/judgments", httpRequest.path)
    }

    @Test
    fun `POST judgments with invalid assessment returns 400`() = runBlocking {
        val request = CreateJudgmentRequest(123L, "invalid", 0.5, null)

        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val response = api.submitJudgment(request)

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}

