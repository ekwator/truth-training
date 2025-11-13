package com.truth.training.client.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.network.TruthApi
import com.truth.training.client.data.network.dto.CreateEventRequest
import com.truth.training.client.data.repository.EventRepository
import com.truth.training.client.data.sync.SyncWorker
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class OfflineFirstTest {
    private lateinit var database: TruthDatabase
    private lateinit var eventRepository: EventRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val testConfig = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, testConfig)

        database = TruthDatabase.getInstance(context)
        eventRepository = EventRepository(database, null)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun offlineEventCreationAndSync() = runBlocking {
        val event = eventRepository.createEvent(
            CreateEventRequest(
                description = "Offline Event",
                timestampStart = 1_000L
            )
        ).getOrThrow()

        val savedEvent = eventRepository.getEventById(event.id)
        assertEquals("Offline Event", savedEvent!!.description)

        val worker = TestListenableWorkerBuilder<SyncWorker>(
            context = ApplicationProvider.getApplicationContext()
        ).build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val beforeRestart = eventRepository.getEventById(event.id)
        assertEquals("Offline Event", beforeRestart!!.description)

        database.close()

        database = TruthDatabase.getInstance(ApplicationProvider.getApplicationContext())
        eventRepository = EventRepository(database, null)

        val afterRestart = eventRepository.getEventById(event.id)
        assertNotNull(afterRestart)
        assertEquals("Offline Event", afterRestart!!.description)
    }
}

