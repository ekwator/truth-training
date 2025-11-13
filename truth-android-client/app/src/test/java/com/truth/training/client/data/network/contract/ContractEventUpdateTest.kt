package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.EventResponse
import com.truth.training.client.data.network.dto.UpdateEventRequest
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

class ContractEventUpdateTest {
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
    fun `PUT event updates canonical fields`() = runBlocking {
        val request = UpdateEventRequest(
            description = "Updated Description",
            categoryId = 10,
            vector = false,
            timestampEnd = 3_000L
        )

        val expectedResponse = EventResponse(
            id = 123L,
            description = request.description ?: "Updated Description",
            categoryId = request.categoryId,
            formaId = null,
            causeId = null,
            developId = null,
            effectId = null,
            vector = request.vector ?: true,
            detected = null,
            corrected = false,
            timestampStart = 1_000L,
            timestampEnd = request.timestampEnd,
            code = 1,
            collectiveScore = null
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.updateEvent(123L, request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(123L, body!!.id)
        assertEquals("Updated Description", body.description)
        assertEquals(10, body.categoryId)
        assertFalse(body.vector)

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("PUT", httpRequest.method)
        assertEquals("/api/v1/events/123", httpRequest.path)
    }
}

