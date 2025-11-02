package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.MatchContextRequest
import com.truth.training.client.data.network.dto.MatchContextResponse
import com.truth.training.client.data.network.dto.ContextTemplate
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
 * Contract test for POST /api/v1/contexts/match endpoint.
 */
class ContractContextMatchTest {
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
    fun `POST contexts match returns matched template`() = runBlocking {
        val request = MatchContextRequest(1, 2, 3, 4, 5)
        
        val matchedTemplate = ContextTemplate(1, "Matched Template", 1, 2, 3, 4, 5, null)
        val expectedResponse = MatchContextResponse(matched = true, template = matchedTemplate)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.matchContext(request)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertTrue(body!!.matched)
        assertNotNull(body.template)
        assertEquals("Matched Template", body.template!!.name)
        
        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/contexts/match", httpRequest.path)
    }

    @Test
    fun `POST contexts match with no match returns unmatched`() = runBlocking {
        val request = MatchContextRequest(99, 99, 99, 99, 99)
        val expectedResponse = MatchContextResponse(matched = false, template = null)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.matchContext(request)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertFalse(body!!.matched)
        assertNull(body.template)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

