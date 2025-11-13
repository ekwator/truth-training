package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.network.dto.EventResponse
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

class ContractEventCreateTest {
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
    fun `POST events creates event with canonical fields`() = runBlocking {
        val request = CreateEventRequest(
            description = "New Event",
            categoryId = 1,
            timestampStart = 1_000L,
            vector = true,
            timestampEnd = 2_000L
        )

        val expectedResponse = EventResponse(
            id = 42L,
            description = request.description,
            categoryId = request.categoryId,
            formaId = request.formaId,
            causeId = request.causeId,
            developId = request.developId,
            effectId = request.effectId,
            vector = request.vector,
            detected = null,
            corrected = false,
            timestampStart = request.timestampStart,
            timestampEnd = request.timestampEnd,
            code = request.code,
            collectiveScore = request.collectiveScore
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.createEvent(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(42L, body!!.id)
        assertEquals("New Event", body.description)
        assertEquals(1, body.categoryId)

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/events", httpRequest.path)
    }

    @Test
    fun `POST events with missing description returns 400`() = runBlocking {
        val request = CreateEventRequest(
            description = "",
            timestampStart = 1_000L
        )

        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val response = api.createEvent(request)

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}

