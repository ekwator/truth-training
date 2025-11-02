package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.ContextListResponse
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
 * Contract test for GET /api/v1/contexts endpoint.
 */
class ContractContextsListTest {
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
    fun `GET contexts returns ContextListResponse`() = runBlocking {
        val templates = listOf(
            ContextTemplate(1, "Template 1", 1, 2, 3, 4, 5, "Description 1"),
            ContextTemplate(2, "Template 2", null, null, null, null, null, null)
        )
        val expectedResponse = ContextListResponse(data = templates, total = 2)
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.listContexts()
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(2, body!!.data.size)
        assertEquals(2, body.total)
        assertEquals("Template 1", body.data[0].name)
        assertEquals(1, body.data[0].categoryId)
        
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/contexts", request.path)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

