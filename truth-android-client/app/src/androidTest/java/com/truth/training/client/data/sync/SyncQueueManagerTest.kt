package com.truth.training.client.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.network.dto.CreateEventRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncQueueManagerTest {
    private lateinit var database: TruthDatabase
    private lateinit var syncManager: SyncQueueManager

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).allowMainThreadQueries().build()
        syncManager = SyncQueueManager(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun queueoperationCreatesNewCreateOperation() = runBlocking {
        val payload = CreateEventRequest(
            description = "Test Event",
            categoryId = 1,
            timestampStart = 1_000L,
            vector = true,
            timestampEnd = 2_000L
        )

        val result = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "1",
            payload = payload
        )

        assertTrue(result.isSuccess)
        val operationId = result.getOrThrow()
        assertTrue(operationId > 0)

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("CREATE", pending[0].operationType)
        assertEquals("EVENT", pending[0].entityType)
        assertEquals("1", pending[0].entityId)
        assertEquals("PENDING", pending[0].status)
        assertEquals(0, pending[0].retryCount)
    }

    @Test
    fun queueoperationCreatesUpdateOperation() = runBlocking {
        val payload = CreateEventRequest(
            description = "Updated Event",
            categoryId = 1,
            timestampStart = 2_000L
        )

        val result = syncManager.queueOperation(
            operationType = "UPDATE",
            entityType = "EVENT",
            entityId = "1",
            payload = payload
        )

        assertTrue(result.isSuccess)

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending[0].operationType)
    }

    @Test
    fun queueoperationCreatesDeleteOperation() = runBlocking {
        val payload = emptyMap<String, Any>()

        val result = syncManager.queueOperation(
            operationType = "DELETE",
            entityType = "EVENT",
            entityId = "1",
            payload = payload
        )

        assertTrue(result.isSuccess)

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("DELETE", pending[0].operationType)
    }

    @Test
    fun queueoperationUpdatesExistingOperationForSameEntityLocalWins() = runBlocking {
        val payload1 = CreateEventRequest(
            description = "Original",
            categoryId = 1,
            timestampStart = 1_000L
        )

        val result1 = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "1",
            payload = payload1
        )
        assertTrue(result1.isSuccess)
        val operationId1 = result1.getOrThrow()

        val payload2 = CreateEventRequest(
            description = "Updated",
            categoryId = 1,
            timestampStart = 2_000L
        )

        val result2 = syncManager.queueOperation(
            operationType = "UPDATE",
            entityType = "EVENT",
            entityId = "1",
            payload = payload2
        )
        assertTrue(result2.isSuccess)
        val operationId2 = result2.getOrThrow()

        assertEquals(operationId1, operationId2)

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending[0].operationType)
        assertEquals("PENDING", pending[0].status)
        assertEquals(0, pending[0].retryCount)
    }

    @Test
    fun getpendingoperationsReturnsOnlyPendingOperations() = runBlocking {
        repeat(3) { i ->
            val payload = CreateEventRequest(
                description = "Event $i",
                categoryId = 1,
                timestampStart = 1_000L + i
            )
            syncManager.queueOperation(
                operationType = "CREATE",
                entityType = "EVENT",
                entityId = "event_$i",
                payload = payload
            )
        }

        val pending = syncManager.getPendingOperations()
        assertEquals(3, pending.size)
        assertTrue(pending.all { it.status == "PENDING" })
    }

    @Test
    fun getpendingcountReturnsCorrectCount() = runBlocking {
        repeat(5) { i ->
            val payload = CreateEventRequest(
                description = "Event $i",
                categoryId = 1,
                timestampStart = 2_000L + i
            )
            syncManager.queueOperation(
                operationType = "CREATE",
                entityType = "EVENT",
                entityId = "event_$i",
                payload = payload
            )
        }

        val count = syncManager.getPendingCount()
        assertEquals(5, count)
    }

    @Test
    fun markSyncingMovesOperationToSyncingQueue() = runBlocking {
        val payload = CreateEventRequest(
            description = "Event",
            categoryId = 1,
            timestampStart = 1_000L
        )
        val opId = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload).getOrThrow()

        syncManager.markSyncing(opId).getOrThrow()

        val pending = syncManager.getPendingOperations()
        assertTrue(pending.isEmpty())

        val syncing = syncManager.getPendingOperations(status = "SYNCING")
        assertEquals(1, syncing.size)
        assertEquals("SYNCING", syncing[0].status)
    }

    @Test
    fun completeoperationMarksAsCompleted() = runBlocking {
        val payload = CreateEventRequest(
            description = "Event",
            categoryId = 1,
            timestampStart = 1_000L
        )
        val opId = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload).getOrThrow()

        syncManager.markCompleted(opId).getOrThrow()

        val pending = syncManager.getPendingOperations()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun retryoperationIncrementsRetryCount() = runBlocking {
        val payload = CreateEventRequest(
            description = "Event",
            categoryId = 1,
            timestampStart = 1_000L
        )
        val opId = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload).getOrThrow()

        syncManager.markFailed(opId, "Network error").getOrThrow()

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].retryCount)
    }

    @Test
    fun markoperationFailedResetsStatusAfterRetries() = runBlocking {
        val payload = CreateEventRequest(
            description = "Event",
            categoryId = 1,
            timestampStart = 1_000L
        )
        val opId = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload).getOrThrow()

        repeat(3) {
            syncManager.markFailed(opId, "Network error").getOrThrow()
        }

        val failed = syncManager.getPendingOperations(status = "FAILED")
        assertEquals(1, failed.size)
        assertEquals("FAILED", failed[0].status)
    }

    @Test
    fun cleanupFailedoperationsRemovesOldFailedOperations() = runBlocking {
        val payload = CreateEventRequest(
            description = "Event",
            categoryId = 1,
            timestampStart = 1_000L
        )
        val opId = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload).getOrThrow()

        repeat(3) { syncManager.markFailed(opId, "Network error").getOrThrow() }

        val deletedCount = syncManager.cleanupFailedOperations().getOrThrow()
        assertEquals(1, deletedCount)

        val failed = syncManager.getPendingOperations(status = "FAILED")
        assertTrue(failed.isEmpty())
    }

    @Test
    fun getpendingoperationsRespectsLimitAndOffset() = runBlocking {
        repeat(5) { i ->
            val payload = CreateEventRequest(
                description = "Event $i",
                categoryId = 1,
                timestampStart = 1_000L + i
            )
            syncManager.queueOperation(
                operationType = "CREATE",
                entityType = "EVENT",
                entityId = "event_$i",
                payload = payload
            )
        }

        val paged = syncManager.getPendingOperations(limit = 2, offset = 2)
        assertEquals(2, paged.size)
    }

    @Test
    fun markoperationCompletedClearsPendingOperationsForEntity() = runBlocking {
        val payload1 = CreateEventRequest(description = "First", categoryId = 1, timestampStart = 1_000L)
        val payload2 = CreateEventRequest(description = "Second", categoryId = 1, timestampStart = 2_000L)
        val payload3 = CreateEventRequest(description = "Third", categoryId = 1, timestampStart = 3_000L)

        val opId1 = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload1).getOrThrow()
        val opId2 = syncManager.queueOperation("CREATE", "EVENT", "event_1", payload2).getOrThrow()
        syncManager.queueOperation("CREATE", "EVENT", "event_2", payload3).getOrThrow()

        syncManager.markCompleted(opId2).getOrThrow()

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.count { it.entityId == "event_2" })
        assertEquals(0, pending.count { it.entityId == "event_1" })

        syncManager.markCompleted(opId1).getOrThrow()
    }

    @Test
    fun enqueueMultipleOperationsHandlesQueueGracefully() = runBlocking {
        repeat(10) { i ->
            val payload = CreateEventRequest(
                description = "Event $i",
                categoryId = 1,
                timestampStart = 1_000L + i
            )
            syncManager.queueOperation(
                operationType = "CREATE",
                entityType = "EVENT",
                entityId = "event_$i",
                payload = payload
            )
        }

        val pending = syncManager.getPendingOperations()
        assertEquals(10, pending.size)
    }

    @Test
    fun cleanupFailedoperationsNoopsWhenQueueEmpty() = runBlocking {
        val result = syncManager.cleanupFailedOperations()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }
}

