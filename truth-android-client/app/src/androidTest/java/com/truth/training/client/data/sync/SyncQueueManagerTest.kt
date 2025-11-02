package com.truth.training.client.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.SyncQueueEntity
import com.truth.training.client.data.network.dto.CreateEventRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for SyncQueueManager.
 * 
 * Coverage targets:
 * - queueOperation() - CREATE, UPDATE, DELETE operations
 * - getPendingOperations() - filtering, ordering
 * - markSyncing() - state transitions
 * - markCompleted() - success handling, queue cleanup
 * - markFailed() - retry logic, max retry handling
 * - cleanupFailedOperations() - failed operation removal
 * - Conflict resolution: local-wins strategy
 * 
 * Test scenarios:
 * - Multiple operations for same entity
 * - Retry count limits (0, 1, 2, 3+)
 * - Concurrent operation queuing
 * 
 * Target coverage: ≥95%
 */
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
    fun `queueOperation creates new CREATE operation`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1,
            formaId = 2,
            causeId = 3,
            developId = 4,
            effectId = 5,
            startDate = "2024-01-01T00:00:00Z",
            endDate = "2024-01-02T00:00:00Z"
        )

        val result = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )

        assertTrue(result.isSuccess)
        val operationId = result.getOrNull()!!
        assertTrue(operationId > 0)

        // Verify operation saved
        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("CREATE", pending[0].operationType)
        assertEquals("EVENT", pending[0].entityType)
        assertEquals("event_1", pending[0].entityId)
        assertEquals("PENDING", pending[0].status)
        assertEquals(0, pending[0].retryCount)
    }

    @Test
    fun `queueOperation creates UPDATE operation`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Updated Event",
            description = "Updated",
            categoryId = 1
        )

        val result = syncManager.queueOperation(
            operationType = "UPDATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )

        assertTrue(result.isSuccess)
        
        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending[0].operationType)
    }

    @Test
    fun `queueOperation creates DELETE operation`() = runBlocking {
        val payload = mapOf<String, Any>() // Empty payload for delete

        val result = syncManager.queueOperation(
            operationType = "DELETE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )

        assertTrue(result.isSuccess)
        
        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("DELETE", pending[0].operationType)
    }

    @Test
    fun `queueOperation updates existing operation for same entity (local-wins)`() = runBlocking {
        val payload1 = CreateEventRequest(
            title = "Original",
            description = "Original",
            categoryId = 1
        )
        
        // Create first operation
        val result1 = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload1
        )
        assertTrue(result1.isSuccess)
        val operationId1 = result1.getOrNull()!!

        val payload2 = CreateEventRequest(
            title = "Updated",
            description = "Updated",
            categoryId = 1
        )

        // Queue another operation for same entity (should update existing)
        val result2 = syncManager.queueOperation(
            operationType = "UPDATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload2
        )
        assertTrue(result2.isSuccess)
        val operationId2 = result2.getOrNull()!!

        // Should be same operation ID (local-wins strategy)
        assertEquals(operationId1, operationId2)

        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending[0].operationType) // Latest operation type
        assertEquals("PENDING", pending[0].status)
        assertEquals(0, pending[0].retryCount) // Reset retry count
    }

    @Test
    fun `getPendingOperations returns only PENDING operations`() = runBlocking {
        // Create multiple operations
        repeat(3) { i ->
            val payload = CreateEventRequest(
                title = "Event $i",
                description = "Event $i",
                categoryId = 1
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
    fun `getPendingCount returns correct count`() = runBlocking {
        repeat(5) { i ->
            val payload = CreateEventRequest(
                title = "Event $i",
                description = "Event $i",
                categoryId = 1
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
    fun `markSyncing transitions status from PENDING to SYNCING`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!

        val result = syncManager.markSyncing(operationId)
        
        assertTrue(result.isSuccess)
        
        // Operation should no longer be in pending list
        val pending = syncManager.getPendingOperations()
        assertEquals(0, pending.size)
    }

    @Test
    fun `markSyncing handles error when operation not found`() = runBlocking {
        val result = syncManager.markSyncing(9999L)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `markCompleted removes operation from queue`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!
        
        // Mark as syncing first
        syncManager.markSyncing(operationId)

        val result = syncManager.markCompleted(operationId)
        
        assertTrue(result.isSuccess)
        
        // Operation should be removed from queue
        val pending = syncManager.getPendingOperations()
        assertEquals(0, pending.size)
    }

    @Test
    fun `markCompleted handles error when operation not found`() = runBlocking {
        val result = syncManager.markCompleted(9999L)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `markFailed increments retry count and keeps PENDING if under max`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!
        
        // Mark as syncing first
        syncManager.markSyncing(operationId)

        // Mark as failed (retry 1)
        val result1 = syncManager.markFailed(operationId, "Network error")
        assertTrue(result1.isSuccess)
        
        // Should still be in pending (retry count < 3)
        var pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("PENDING", pending[0].status)
        assertEquals(1, pending[0].retryCount)
        assertEquals("Network error", pending[0].errorMessage)

        // Mark as syncing and failed again (retry 2)
        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Timeout")
        
        pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals(2, pending[0].retryCount)
    }

    @Test
    fun `markFailed marks as FAILED when retry count reaches max`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!

        // Retry 1
        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Error 1")
        
        // Retry 2
        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Error 2")
        
        // Retry 3 (max reached)
        syncManager.markSyncing(operationId)
        val result = syncManager.markFailed(operationId, "Error 3")
        assertTrue(result.isSuccess)
        
        // Should be marked as FAILED and removed from pending
        val pending = syncManager.getPendingOperations()
        assertEquals(0, pending.size)
    }

    @Test
    fun `cleanupFailedOperations removes FAILED operations`() = runBlocking {
        // Create operation and fail it 3 times
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!

        // Fail 3 times to reach FAILED status
        repeat(3) {
            syncManager.markSyncing(operationId)
            syncManager.markFailed(operationId, "Error")
        }

        val result = syncManager.cleanupFailedOperations()
        
        assertTrue(result.isSuccess)
        
        // Failed operations should be cleaned up
        val pending = syncManager.getPendingOperations()
        assertEquals(0, pending.size)
    }

    @Test
    fun `multiple operations for same entity are merged (local-wins)`() = runBlocking {
        val payload1 = CreateEventRequest(title = "First", description = "First", categoryId = 1)
        syncManager.queueOperation("CREATE", "EVENT", "event_1", payload1)
        
        val payload2 = CreateEventRequest(title = "Second", description = "Second", categoryId = 1)
        syncManager.queueOperation("UPDATE", "EVENT", "event_1", payload2)
        
        val payload3 = CreateEventRequest(title = "Third", description = "Third", categoryId = 1)
        syncManager.queueOperation("UPDATE", "EVENT", "event_1", payload3)

        // Should only have one operation (latest)
        val pending = syncManager.getPendingOperations()
        assertEquals(1, pending.size)
        assertEquals("UPDATE", pending[0].operationType)
    }

    @Test
    fun `concurrent operation queuing handles different entities independently`() = runBlocking {
        // Queue operations for different entities
        repeat(5) { i ->
            val payload = CreateEventRequest(
                title = "Event $i",
                description = "Event $i",
                categoryId = 1
            )
            syncManager.queueOperation(
                operationType = "CREATE",
                entityType = "EVENT",
                entityId = "event_$i",
                payload = payload
            )
        }

        val pending = syncManager.getPendingOperations()
        assertEquals(5, pending.size)
        
        // Each should have unique entity IDs
        val entityIds = pending.map { it.entityId }.toSet()
        assertEquals(5, entityIds.size)
    }

    @Test
    fun `retry count limits are enforced correctly`() = runBlocking {
        val payload = CreateEventRequest(
            title = "Test Event",
            description = "Test",
            categoryId = 1
        )
        
        val queueResult = syncManager.queueOperation(
            operationType = "CREATE",
            entityType = "EVENT",
            entityId = "event_1",
            payload = payload
        )
        val operationId = queueResult.getOrNull()!!

        // Verify initial retry count is 0
        var pending = syncManager.getPendingOperations()
        assertEquals(0, pending[0].retryCount)

        // Fail and verify retry count increments
        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Error 1")
        pending = syncManager.getPendingOperations()
        assertEquals(1, pending[0].retryCount)

        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Error 2")
        pending = syncManager.getPendingOperations()
        assertEquals(2, pending[0].retryCount)

        // Third failure should mark as FAILED
        syncManager.markSyncing(operationId)
        syncManager.markFailed(operationId, "Error 3")
        pending = syncManager.getPendingOperations()
        assertEquals(0, pending.size) // Removed from pending
    }
}

