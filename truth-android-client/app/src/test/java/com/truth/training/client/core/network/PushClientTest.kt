package com.truth.training.client.core.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PushClientTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TruthPushApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TruthPushApi::class.java)
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun send_event_ok() {
        server.enqueue(MockResponse().setResponseCode(200))
        val body = PushEnvelope(mapOf("event" to "truth_claim"), "sig", "pub")
        val resp = kotlinx.coroutines.runBlocking { api.sendEvent("Bearer token", body) }
        assertEquals(true, resp.isSuccessful)
    }
}


