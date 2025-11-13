package com.truth.training.client.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truth.training.client.core.crypto.Ed25519CryptoManager
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.p2p.P2PDiscoveryService
import com.truth.training.client.p2p.P2PMessageHandler
import com.truth.training.client.p2p.P2PSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrossPlatformSyncTest {
    private lateinit var context: android.content.Context
    private lateinit var database: TruthDatabase
    private lateinit var repository: EventRepository
    private lateinit var syncManager: P2PSyncManager
    private lateinit var discoveryService: P2PDiscoveryService
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = TruthDatabase.getInstance(context)
        repository = EventRepository(database, null)
        discoveryService = P2PDiscoveryService(context, testScope)
        val messageHandler = P2PMessageHandler(context, database)
        syncManager = P2PSyncManager(context, database, discoveryService, messageHandler)
        Ed25519CryptoManager.init(context)
    }

    @After
    fun tearDown() {
        discoveryService.stopDiscovery()
        testScope.cancel()
        database.close()
    }

    @Test
    fun crossPlatformSyncCreatesEvent() = runBlocking {
        val id = repository.createEvent(
            CreateEventRequest(
                description = "Cross-Platform Event",
                timestampStart = 1_000L,
                vector = true
            )
        ).getOrThrow().id

        val savedEvent = repository.getEventById(id)
        assertEquals("Cross-Platform Event", savedEvent!!.description)

        val result = syncManager.propagateEvent(savedEvent.id)
        assertTrue(result.isSuccess)
    }
}

