package com.truth.training.client.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truth.training.client.data.database.daos.NodeDao
import com.truth.training.client.data.database.entities.NodeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Android instrumentation tests for NodeDao.
 * 
 * Tests T042: NodeDao CRUD & TTL behavior
 * 
 * Verifies:
 * - Upsert by address
 * - List with filters
 * - TTL-based cleanup
 * - Reachability updates
 * 
 * Reference: core/src/storage.rs::NodeRepository tests
 */
@RunWith(AndroidJUnit4::class)
class NodeDaoTest {
    private lateinit var database: TruthDatabase
    private lateinit var nodeDao: NodeDao
    
    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TruthDatabase::class.java
        ).build()
        nodeDao = database.nodeDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun testUpsertNodeByAddress() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val node = NodeEntity(
            id = 0,
            address = "http://test-node:8080/api/v1",
            type = "LAN",
            reachable = 1,
            lastSeen = now,
            ttl = 120,
            source = "local_broadcast",
            nodeId = "test-node-id",
            createdAt = now,
            updatedAt = now
        )
        
        val id = nodeDao.upsertNode(node)
        assert(id > 0)
        
        val retrieved = nodeDao.getNodeByAddress("http://test-node:8080/api/v1")
        assert(retrieved != null)
        assert(retrieved!!.address == node.address)
        assert(retrieved.type == "LAN")
    }
    
    @Test
    fun testUpsertUpdatesExisting() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val node1 = NodeEntity(
            id = 0,
            address = "http://test-node:8080/api/v1",
            type = "LAN",
            reachable = 1,
            lastSeen = now,
            ttl = 120,
            source = "local_broadcast",
            nodeId = "test-node-id",
            createdAt = now,
            updatedAt = now
        )
        
        nodeDao.upsertNode(node1)
        val inserted = nodeDao.getNodeByAddress(node1.address)
        assertNotNull("Node should be inserted", inserted)
        val id1 = inserted!!.id
        assertTrue("Node should have a valid ID", id1 > 0)
        
        // Upsert with updated TTL - use the actual ID from the inserted node
        val node2 = inserted.copy(ttl = 200, updatedAt = now + 10)
        nodeDao.upsertNode(node2)
        
        val retrieved = nodeDao.getNodeByAddress(node1.address)
        assertNotNull("Node should still exist after upsert", retrieved)
        assertEquals("ID should remain the same after upsert", id1, retrieved!!.id)
        assertEquals("TTL should be updated to 200", 200L, retrieved.ttl)
    }
    
    @Test
    fun testListNodesWithFilters() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        
        // Insert multiple nodes
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://lan-1:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = now, ttl = 120,
            source = "local_broadcast", nodeId = "lan-1",
            createdAt = now, updatedAt = now
        ))
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://global-1:8080/api/v1", type = "GLOBAL",
            reachable = 1, lastSeen = now, ttl = 3600,
            source = "global_registry", nodeId = "global-1",
            createdAt = now, updatedAt = now
        ))
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://lan-2:8080/api/v1", type = "LAN",
            reachable = 0, lastSeen = now, ttl = 120,
            source = "local_broadcast", nodeId = "lan-2",
            createdAt = now, updatedAt = now
        ))
        
        // Filter by type
        val lanNodes = nodeDao.listNodesSync(nodeType = "LAN")
        assert(lanNodes.size == 2)
        assert(lanNodes.all { it.type == "LAN" })
        
        // Filter by reachable
        val reachableNodes = nodeDao.listNodesSync(reachable = 1)
        assert(reachableNodes.size == 2)
        assert(reachableNodes.all { it.reachable == 1 })
        
        // Filter by type + reachable
        val reachableLan = nodeDao.listNodesSync(nodeType = "LAN", reachable = 1)
        assert(reachableLan.size == 1)
        assert(reachableLan[0].address == "http://lan-1:8080/api/v1")
    }
    
    @Test
    fun testPruneStaleNodes() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        
        // Insert fresh node
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://fresh:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = now, ttl = 120,
            source = "local_broadcast", nodeId = "fresh",
            createdAt = now, updatedAt = now
        ))
        
        // Insert expired node (last_seen + ttl < now)
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://expired:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = now - 200, ttl = 120, // Expired
            source = "local_broadcast", nodeId = "expired",
            createdAt = now - 200, updatedAt = now - 200
        ))
        
        // Insert unreachable node that should be pruned (reachable=0, last_seen > ttl/2 ago)
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://unreachable:8080/api/v1", type = "LAN",
            reachable = 0, lastSeen = now - 100, ttl = 120, // last_seen > ttl/2 (60)
            source = "local_broadcast", nodeId = "unreachable",
            createdAt = now - 100, updatedAt = now - 100
        ))
        
        val beforeCount = nodeDao.countNodes()
        assert(beforeCount == 3)
        
        // Prune stale nodes
        val pruned = nodeDao.pruneStaleNodes(now)
        assert(pruned >= 2) // At least expired + unreachable
        
        val afterCount = nodeDao.countNodes()
        assert(afterCount == 1)
        
        val remaining = nodeDao.listNodesSync()
        assert(remaining.size == 1)
        assert(remaining[0].address == "http://fresh:8080/api/v1")
    }
    
    @Test
    fun testUpdateReachability() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val node = NodeEntity(
            id = 0, address = "http://test:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = now, ttl = 120,
            source = "local_broadcast", nodeId = "test",
            createdAt = now, updatedAt = now
        )
        
        val id = nodeDao.upsertNode(node)
        val nodeId = nodeDao.getNodeByAddress(node.address)!!.id
        
        // Update to unreachable
        val updated = nodeDao.updateReachability(nodeId, 0, now + 10)
        assert(updated == 1)
        
        val retrieved = nodeDao.getNode(nodeId)
        assert(retrieved != null)
        assert(retrieved!!.reachable == 0)
        assert(retrieved.updatedAt == now + 10)
        // last_seen should not change when marking unreachable
        assert(retrieved.lastSeen == now)
        
        // Update to reachable (should update last_seen)
        nodeDao.updateReachability(nodeId, 1, now + 20)
        val retrieved2 = nodeDao.getNode(nodeId)
        assert(retrieved2!!.reachable == 1)
        assert(retrieved2.lastSeen == now + 20) // Should be updated
    }
    
    @Test
    fun testDeleteNode() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val node = NodeEntity(
            id = 0, address = "http://test:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = now, ttl = 120,
            source = "local_broadcast", nodeId = "test",
            createdAt = now, updatedAt = now
        )
        
        val id = nodeDao.upsertNode(node)
        val nodeId = nodeDao.getNodeByAddress(node.address)!!.id
        
        val deleted = nodeDao.deleteNode(nodeId)
        assert(deleted == 1)
        
        val retrieved = nodeDao.getNode(nodeId)
        assert(retrieved == null)
    }
    
    @Test
    fun testListNodesOrderedByLastSeen() = runBlocking {
        val baseTime = System.currentTimeMillis() / 1000
        
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://old:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = baseTime, ttl = 120,
            source = "local_broadcast", nodeId = "old",
            createdAt = baseTime, updatedAt = baseTime
        ))
        
        nodeDao.upsertNode(NodeEntity(
            id = 0, address = "http://new:8080/api/v1", type = "LAN",
            reachable = 1, lastSeen = baseTime + 100, ttl = 120,
            source = "local_broadcast", nodeId = "new",
            createdAt = baseTime + 100, updatedAt = baseTime + 100
        ))
        
        val nodes = nodeDao.listNodesSync()
        assert(nodes.size == 2)
        // Should be ordered by last_seen DESC (newest first)
        assert(nodes[0].address == "http://new:8080/api/v1")
        assert(nodes[1].address == "http://old:8080/api/v1")
    }
}

