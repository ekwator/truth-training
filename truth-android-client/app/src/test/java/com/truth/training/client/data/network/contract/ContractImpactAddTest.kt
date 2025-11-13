package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateImpactRequest
import com.truth.training.client.data.network.dto.Impact
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

class ContractImpactAddTest {
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
    fun `POST impacts adds boolean impact value`() = runBlocking {
        val request = CreateImpactRequest(
            eventId = 123L,
            value = true,
            notes = "Moderate positive impact"
        )

        val expectedResponse = Impact(
            id = 1L,
            eventId = request.eventId,
            value = request.value,
            notes = request.notes,
            createdAt = 1_500L
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
        assertEquals(123L, body!!.eventId)
        assertTrue(body.value)
        assertEquals("Moderate positive impact", body.notes)

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/impacts", httpRequest.path)
    }

    @Test
    fun `POST impacts with missing event returns 400`() = runBlocking {
        val request = CreateImpactRequest(0L, value = true, notes = null)

        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val response = api.addImpact(request)

        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}

