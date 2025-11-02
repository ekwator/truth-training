package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.CreateContextRequest
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
 * Contract test for POST /api/v1/contexts endpoint.
 */
class ContractContextCreateTest {
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
    fun `POST contexts creates template with embedded fields`() = runBlocking {
        val request = CreateContextRequest(
            name = "Test Template",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            description = "Template description"
        )
        
        val expectedResponse = ContextTemplate(
            id = 1,
            name = request.name,
            categoryId = request.categoryId,
            formaId = request.formaId,
            causeId = request.causeId,
            developId = request.developId,
            effectId = request.effectId,
            description = request.description
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        val response = api.createContext(request)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("Test Template", body!!.name)
        assertEquals(1, body.categoryId)
        
        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals("/api/v1/contexts", httpRequest.path)
    }

    @Test
    fun `POST contexts with duplicate fields returns 409`() = runBlocking {
        val request = CreateContextRequest("Duplicate", 1, 2, 3, 4, 5, null)
        
        mockWebServer.enqueue(MockResponse().setResponseCode(409))
        
        val response = api.createContext(request)
        
        assertFalse(response.isSuccessful)
        assertEquals(409, response.code())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

