package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.InfoResponse
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
 * Contract test for GET /api/v1/info endpoint.
 */
class ContractInfoTest {
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
    fun `GET info returns InfoResponse with version and uptime`() = runBlocking {
        // Arrange
        val expectedResponse = InfoResponse(
            version = "1.0.0",
            uptime = "3600s",
            nodeId = "node_123",
            network = "testnet"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        // Act
        val response = api.getInfo()
        
        // Assert
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("1.0.0", body!!.version)
        assertEquals("3600s", body.uptime)
        assertEquals("node_123", body.nodeId)
        assertEquals("testnet", body.network)
        
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/info", request.path)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

