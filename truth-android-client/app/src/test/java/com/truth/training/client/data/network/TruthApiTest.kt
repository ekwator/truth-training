package com.truth.training.client.data.network

import com.google.gson.Gson
import com.truth.training.client.data.network.dto.AuthRequest
import com.truth.training.client.data.network.dto.AuthResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TruthApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TruthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TruthApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun auth_parses_response() {
        val body = AuthResponse(accessToken = "a", refreshToken = "r")
        server.enqueue(MockResponse().setResponseCode(200).setBody(Gson().toJson(body)))

        val resp = kotlinx.coroutines.runBlocking {
            api.authenticate(AuthRequest("e","p"))
        }

        assertEquals(true, resp.isSuccessful)
        assertEquals("a", resp.body()!!.accessToken)
    }
}


