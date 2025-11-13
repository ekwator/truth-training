package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.TruthApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContractEventDeleteTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TruthApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `DELETE event returns 204`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        val response = api.deleteEvent(123L)

        assertTrue(response.isSuccessful)
        assertEquals(204, response.code())

        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/events/123", request.path)
    }

    @Test
    fun `DELETE non-existent event returns 404`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val response = api.deleteEvent(9999L)

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}

