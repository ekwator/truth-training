package com.truth.training.client.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.Configuration
import androidx.work.WorkManager
import com.truth.training.client.TruthTrainingApplication
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Android instrumentation tests for NodeSyncWorker.
 * 
 * Tests T044: NodeSyncWorker periodic sync, TTL decrement, cleanup, reachability checks
 * 
 * Uses TestWorkManager for isolated testing.
 * 
 * Reference:
 * - Desktop tests: ui/desktop/src-tauri/tests/discovery_manager_test.rs
 * - Core tests: tests/test_global_registry_poll.rs, tests/test_http_reachability.rs
 */
@RunWith(AndroidJUnit4::class)
class NodeSyncWorkerTest {
    private lateinit var context: Context
    private lateinit var database: TruthDatabase
    private lateinit var repository: DiscoveryRepository
    private lateinit var workManager: WorkManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize test WorkManager
        val testConfig = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, testConfig)
        workManager = WorkManager.getInstance(context)
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            TruthDatabase::class.java
        ).build()
        
        repository = DiscoveryRepository(database, null)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testNodeSyncWorkerExecutesSuccessfully() = runBlocking {
        // Create worker with test configuration
        val worker = TestListenableWorkerBuilder<NodeSyncWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putStringArray("registry_urls", emptyArray())
                    .putLong("reachability_timeout_seconds", 5L)
                    .putInt("reachability_retries", 2)
                    .build()
            )
            .build()
        
        // Execute worker
        val result = worker.doWork()
        
        // Should succeed even with no nodes (no-op)
        assert(result == ListenableWorker.Result.success())
    }
    
    @Test
    fun testNodeSyncWorkerPrunesStaleNodes() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        
        // Insert fresh node
        repository.upsertNode(NodeEntity(
            id = 0,
            address = "http://fresh:8080/api/v1",
            type = "LAN",
            reachable = 1,
            lastSeen = now,
            ttl = 120,
            source = "local_broadcast",
            nodeId = "fresh-node",
            createdAt = now,
            updatedAt = now
        ))
        
        // Insert expired node
        repository.upsertNode(NodeEntity(
            id = 0,
            address = "http://expired:8080/api/v1",
            type = "LAN",
            reachable = 1,
            lastSeen = now - 200, // Expired (ttl = 120)
            ttl = 120,
            source = "local_broadcast",
            nodeId = "expired-node",
            createdAt = now - 200,
            updatedAt = now - 200
        ))
        
        // Insert unreachable node that should be pruned
        repository.upsertNode(NodeEntity(
            id = 0,
            address = "http://unreachable:8080/api/v1",
            type = "LAN",
            reachable = 0,
            lastSeen = now - 100, // > ttl/2 (60)
            ttl = 120,
            source = "local_broadcast",
            nodeId = "unreachable-node",
            createdAt = now - 100,
            updatedAt = now - 100
        ))
        
        val beforeCount = repository.listNodes().first().size
        
        // Execute worker
        val worker = TestListenableWorkerBuilder<NodeSyncWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putStringArray("registry_urls", emptyArray())
                    .putLong("reachability_timeout_seconds", 5L)
                    .putInt("reachability_retries", 2)
                    .build()
            )
            .build()
        
        val result = worker.doWork()
        assert(result == ListenableWorker.Result.success())
        
        // Verify stale nodes were pruned
        val afterCount = repository.listNodes().first().size
        assert(afterCount < beforeCount)
        assert(afterCount == 1) // Only fresh node should remain
    }
    
    @Test
    fun testNodeSyncWorkerWithEmptyRegistryUrls() = runBlocking {
        // Worker should succeed even with no registry URLs configured
        val worker = TestListenableWorkerBuilder<NodeSyncWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putStringArray("registry_urls", emptyArray())
                    .putLong("reachability_timeout_seconds", 5L)
                    .putInt("reachability_retries", 2)
                    .build()
            )
            .build()
        
        val result = worker.doWork()
        assert(result == ListenableWorker.Result.success())
    }
    
    @Test
    fun testNodeSyncWorkerCreatesPeriodicWorkRequest() {
        val workRequest = NodeSyncWorker.createPeriodicWorkRequest(
            registryUrls = listOf("https://registry.example.com/nodes"),
            reachabilityTimeout = 10L,
            reachabilityRetries = 3
        )
        
        assert(workRequest.workSpec.intervalDuration == TimeUnit.MINUTES.toMillis(15))
        assert(workRequest.workSpec.flexDuration == TimeUnit.MINUTES.toMillis(5))
        assert(workRequest.tags.contains("node_sync"))
        assert(workRequest.tags.contains("discovery"))
        
        val inputData = workRequest.workSpec.input
        assert(inputData.getStringArray("registry_urls")?.size == 1)
        assert(inputData.getLong("reachability_timeout_seconds", 0) == 10L)
        assert(inputData.getInt("reachability_retries", 0) == 3)
    }
    
    @Test
    fun testNodeSyncWorkerCreatesOneTimeWorkRequest() {
        val workRequest = NodeSyncWorker.createOneTimeWorkRequest(
            registryUrls = listOf("https://registry.example.com/nodes"),
            reachabilityTimeout = 5L,
            reachabilityRetries = 2
        )
        
        assert(workRequest.tags.contains("node_sync"))
        assert(workRequest.tags.contains("discovery"))
        assert(workRequest.tags.contains("one_time"))
        
        val inputData = workRequest.workSpec.input
        assert(inputData.getStringArray("registry_urls")?.size == 1)
    }
}

