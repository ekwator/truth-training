package com.truth.training.client.data.network.contract

import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.AuthResponse
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
 * Contract test for POST /api/v1/auth endpoint.
 * Validates request/response schema matches OpenAPI spec.
 */
class ContractAuthTest {
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
    fun `POST auth with valid credentials returns AuthResponse`() = runBlocking {
        // Arrange
        val expectedResponse = AuthResponse(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            tokenType = "Bearer"
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(expectedResponse))
        )

        // Act
        val response = api.authenticate(AuthRequest("user@example.com", "password123"))
        
        // Assert
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("test_access_token", body!!.accessToken)
        assertEquals("test_refresh_token", body.refreshToken)
        assertEquals("Bearer", body.tokenType)
        
        // Validate request schema
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth", request.path)
    }

    @Test
    fun `POST auth with invalid credentials returns 401`() = runBlocking {
        // Arrange
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
        )

        // Act
        val response = api.authenticate(AuthRequest("user@example.com", "wrong_password"))
        
        // Assert
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }
}

// Helper extension for coroutines in tests
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

