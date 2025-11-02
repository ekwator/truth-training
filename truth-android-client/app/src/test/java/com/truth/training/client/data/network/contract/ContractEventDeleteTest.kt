package com.truth.training.client.data.network.contract

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.truth.training.client.data.network.TruthApi
import org.junit.Assert.*

/**
 * Contract test for DELETE /api/v1/events/{id} endpoint.
 */
class ContractEventDeleteTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: TruthApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TruthApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `DELETE event returns 204`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        
        val response = api.deleteEvent("event_123")
        
        assertTrue(response.isSuccessful)
        assertEquals(204, response.code())
        
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/events/event_123", request.path)
    }

    @Test
    fun `DELETE non-existent event returns 404`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        
        val response = api.deleteEvent("event_nonexistent")
        
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

