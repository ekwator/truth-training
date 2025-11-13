package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.EventListResponse
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

class ContractEventsListTest {
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
    fun `GET events returns canonical payload`() = runBlocking {
        val events = listOf(
            EventResponse(
                id = 123L,
                description = "Test Event",
                categoryId = 1,
                formaId = 2,
                causeId = 3,
                developId = 4,
                effectId = 5,
                vector = true,
                detected = null,
                corrected = false,
                timestampStart = 1_000L,
                timestampEnd = 2_000L,
                code = 1,
                collectiveScore = null
            )
        )
        val expectedResponse = EventListResponse(data = events, total = events.size)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.listEvents(limit = 35, offset = 0)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(1, body!!.total)
        val event = body.data.first()
        assertEquals(123L, event.id)
        assertEquals(1, event.categoryId)
        assertTrue(event.vector)

        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/api/v1/events"))
        assertTrue(request.path!!.contains("limit=35"))
        assertTrue(request.path!!.contains("offset=0"))
    }

    @Test
    fun `GET events supports pagination parameters`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(gson.toJson(EventListResponse(emptyList(), 0))))

        api.listEvents(limit = 50, offset = 100)

        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("limit=50"))
        assertTrue(request.path!!.contains("offset=100"))
    }
}

