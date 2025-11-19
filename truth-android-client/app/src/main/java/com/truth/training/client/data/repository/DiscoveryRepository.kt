package com.truth.training.client.data.repository

import android.util.Log
import com.truth.training.client.data.database.TruthDatabase
import com.truth.training.client.data.database.daos.NodeDao
import com.truth.training.client.data.database.entities.NodeEntity
import com.truth.training.client.network.LanDiscoveryClient
import com.truth.training.client.network.LanAnnouncement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repository for node discovery operations.
 * 
 * Implements T043: DiscoveryRepository with:
 * - NodeDao integration
 * - Global registry polling (HTTP)
 * - UDP/LAN discovery integration
 * - TTL logic and cleanup
 * - Deduplication by canonical address + node_id
 * 
 * Matches Rust implementation in src/p2p/node.rs:
 * - poll_global_registries()
 * - run_http_reachability_checks()
 * - TTL cleanup logic
 * 
 * Reference:
 * - docs/cross_platform_discovery_compatibility.md
 */
class DiscoveryRepository(
    private val database: TruthDatabase,
    private val lanDiscoveryClient: LanDiscoveryClient? = null
) {
    private val nodeDao: NodeDao = database.nodeDao()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private val _syncStatus = MutableStateFlow<DiscoverySyncStatus>(DiscoverySyncStatus.Idle)
    val syncStatus: Flow<DiscoverySyncStatus> = _syncStatus.asStateFlow()
    
    /**
     * List all nodes with optional filters.
     * Matches core/src/storage.rs::list_nodes()
     */
    fun listNodes(
        nodeType: String? = null,
        reachable: Int? = null,
        address: String? = null,
        limit: Int = 0
    ): Flow<List<NodeEntity>> = nodeDao.listNodes(nodeType, reachable, address, limit)
    
    /**
     * Get node by address.
     */
    suspend fun getNodeByAddress(address: String): NodeEntity? = 
        nodeDao.getNodeByAddress(address)
    
    /**
     * Upsert node by address (canonical deduplication).
     * Matches core/src/storage.rs::upsert_node_by_address()
     * 
     * Deduplication rules:
     * - Same address → update existing
     * - Same node_id + different address → update address (prefer newer)
     */
    suspend fun upsertNode(node: NodeEntity): Result<NodeEntity> = withContext(Dispatchers.IO) {
        try {
            // Check for existing node by address
            val existing = nodeDao.getNodeByAddress(node.address)
            
            val finalNode = if (existing != null) {
                // Update existing node (preserve ID)
                val updated = existing.copy(
                    type = node.type,
                    reachable = node.reachable,
                    lastSeen = node.lastSeen,
                    ttl = node.ttl,
                    source = node.source,
                    nodeId = node.nodeId ?: existing.nodeId,
                    updatedAt = System.currentTimeMillis() / 1000
                )
                nodeDao.upsertNode(updated)
                updated
            } else {
                // Check for existing node by node_id (if provided)
                if (node.nodeId != null) {
                    val existingById = nodeDao.listNodesSync(limit = 1000)
                        .find { it.nodeId == node.nodeId }
                    
                    if (existingById != null) {
                        // Same node_id, different address → update address
                        val updated = existingById.copy(
                            address = node.address,
                            lastSeen = node.lastSeen,
                            ttl = node.ttl,
                            source = node.source,
                            updatedAt = System.currentTimeMillis() / 1000
                        )
                        nodeDao.upsertNode(updated)
                        updated
                    } else {
                        // New node
                        val now = System.currentTimeMillis() / 1000
                        val newNode = node.copy(
                            createdAt = now,
                            updatedAt = now
                        )
                        nodeDao.upsertNode(newNode)
                        nodeDao.getNodeByAddress(node.address) ?: newNode
                    }
                } else {
                    // New node without node_id
                    val now = System.currentTimeMillis() / 1000
                    val newNode = node.copy(
                        createdAt = now,
                        updatedAt = now
                    )
                    nodeDao.upsertNode(newNode)
                    nodeDao.getNodeByAddress(node.address) ?: newNode
                }
            }
            
            Result.success(finalNode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert node ${node.address}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Poll global registries for nodes.
     * Matches src/p2p/node.rs::poll_global_registries()
     * 
     * @param registryUrls List of registry URLs to poll
     * @return Number of nodes discovered and stored
     */
    suspend fun pollGlobalRegistries(registryUrls: List<String>): Result<Int> = 
        withContext(Dispatchers.IO) {
            if (registryUrls.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            _syncStatus.value = DiscoverySyncStatus.PollingRegistries(registryUrls.size)
            
            var totalDiscovered = 0
            val errors = mutableListOf<String>()
            
            for (url in registryUrls) {
                try {
                    val nodes = fetchRegistryNodes(url)
                    for (node in nodes) {
                        upsertNode(node).fold(
                            onSuccess = { totalDiscovered++ },
                            onFailure = { e ->
                                Log.w(TAG, "Failed to store node from $url: ${e.message}")
                            }
                        )
                    }
                } catch (e: Exception) {
                    val error = "Registry $url failed: ${e.message}"
                    Log.w(TAG, error, e)
                    errors.add(error)
                }
            }
            
            _syncStatus.value = DiscoverySyncStatus.Idle
            
            if (errors.isNotEmpty() && totalDiscovered == 0) {
                Result.failure(Exception("All registries failed: ${errors.joinToString("; ")}"))
            } else {
                Result.success(totalDiscovered)
            }
        }
    
    /**
     * Run HTTP reachability checks for all nodes.
     * Matches src/p2p/node.rs::run_http_reachability_checks()
     * 
     * @param timeoutSeconds HTTP request timeout
     * @param retries Number of retry attempts per node
     * @return Number of nodes checked
     */
    suspend fun runReachabilityChecks(
        timeoutSeconds: Long = 5,
        retries: Int = 2
    ): Result<Int> = withContext(Dispatchers.IO) {
        val nodes = nodeDao.listNodesSync()
        if (nodes.isEmpty()) {
            return@withContext Result.success(0)
        }
        
        _syncStatus.value = DiscoverySyncStatus.CheckingReachability(nodes.size)
        
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
        
        var checked = 0
        for (node in nodes) {
            try {
                val reachable = checkNodeReachability(httpClient, node.address, retries)
                val now = System.currentTimeMillis() / 1000
                
                if (reachable) {
                    nodeDao.updateReachability(node.id, 1, now)
                    nodeDao.updateLastSeen(node.id, now, now)
                } else {
                    nodeDao.updateReachability(node.id, 0, now)
                }
                checked++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check reachability for ${node.address}", e)
            }
        }
        
        _syncStatus.value = DiscoverySyncStatus.Idle
        return@withContext Result.success(checked)
    }
    
    /**
     * Prune stale nodes (TTL expired or unreachable for > ttl/2).
     * Matches core/src/storage.rs::prune_stale_nodes()
     * 
     * @return Number of nodes pruned
     */
    suspend fun pruneStaleNodes(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis() / 1000
            val pruned = nodeDao.pruneStaleNodes(now)
            Result.success(pruned)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune stale nodes", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process LAN announcement from UDP multicast.
     * Called by LanDiscoveryClient when a node is discovered.
     */
    suspend fun processLanAnnouncement(announcement: LanAnnouncement): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Verify signature (TODO: implement ed25519 verification in T045)
                // For now, accept all announcements
                
                val now = System.currentTimeMillis() / 1000
                val ttl = maxOf(announcement.ttl, getMinTtlForType(announcement.node_type))
                
                val node = NodeEntity(
                    id = 0,
                    address = announcement.address,
                    type = announcement.node_type,
                    reachable = 1,
                    lastSeen = now,
                    ttl = ttl,
                    source = "local_broadcast",
                    nodeId = announcement.node_id,
                    createdAt = now,
                    updatedAt = now
                )
                
                upsertNode(node).fold(
                    onSuccess = {
                        Log.d(TAG, "Discovered LAN node ${announcement.node_id} at ${announcement.address}")
                        Result.success(Unit)
                    },
                    onFailure = { e ->
                        Log.w(TAG, "Failed to store LAN node ${announcement.node_id}", e)
                        Result.failure(e)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process LAN announcement", e)
                Result.failure(e)
            }
        }
    
    // Private helper methods
    
    private suspend fun fetchRegistryNodes(url: String): List<NodeEntity> {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Registry $url returned ${response.code}")
        }
        
        val body = response.body?.string() ?: throw Exception("Empty registry response")
        return parseRegistryPayload(body)
    }
    
    private fun parseRegistryPayload(body: String): List<NodeEntity> {
        return try {
            // Try envelope format: { "nodes": [...] }
            val json = JSONObject(body)
            if (json.has("nodes")) {
                parseNodeArray(json.getJSONArray("nodes"))
            } else {
                // Try direct array format: [...]
                parseNodeArray(JSONArray(body))
            }
        } catch (e: Exception) {
            // Fallback: try as array directly
            try {
                parseNodeArray(JSONArray(body))
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to parse registry payload", e2)
                emptyList()
            }
        }
    }
    
    private fun parseNodeArray(array: JSONArray): List<NodeEntity> {
        val nodes = mutableListOf<NodeEntity>()
        val now = System.currentTimeMillis() / 1000
        
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val address = obj.getString("address").trim()
                if (address.isEmpty()) continue
                
                val nodeType = obj.optString("node_type", "GLOBAL")
                val ttl = obj.optLong("ttl", getMinTtlForType(nodeType))
                val nodeId = obj.optString("node_id", null).takeIf { it.isNotEmpty() }
                
                val node = NodeEntity(
                    id = 0,
                    address = address,
                    type = nodeType,
                    reachable = 0, // Initially unreachable until health check
                    lastSeen = now,
                    ttl = maxOf(ttl, getMinTtlForType(nodeType)),
                    source = "global_registry",
                    nodeId = nodeId,
                    createdAt = now,
                    updatedAt = now
                )
                nodes.add(node)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse registry node at index $i", e)
            }
        }
        
        return nodes
    }
    
    private suspend fun checkNodeReachability(
        client: OkHttpClient,
        address: String,
        retries: Int
    ): Boolean {
        val healthUrl = buildHealthUrl(address)
        var attempt = 0
        
        while (attempt <= retries) {
            try {
                val request = Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return true
                }
            } catch (e: Exception) {
                // Continue to retry
            }
            
            attempt++
            if (attempt <= retries) {
                kotlinx.coroutines.delay(250 * attempt.toLong())
            }
        }
        
        return false
    }
    
    private fun buildHealthUrl(address: String): String {
        return if (address.endsWith("/health")) {
            address
        } else if (address.endsWith("/")) {
            "${address}health"
        } else {
            "$address/health"
        }
    }
    
    private fun getMinTtlForType(nodeType: String): Long {
        return when (nodeType.uppercase()) {
            "LAN", "WIFI" -> 120L
            "GLOBAL" -> 3600L
            "RELAY" -> 1800L
            "CLIENT" -> 300L
            else -> 300L
        }
    }
    
    companion object {
        private const val TAG = "DiscoveryRepository"
    }
}

/**
 * Status of discovery sync operations.
 */
sealed class DiscoverySyncStatus {
    object Idle : DiscoverySyncStatus()
    data class PollingRegistries(val total: Int) : DiscoverySyncStatus()
    data class CheckingReachability(val total: Int) : DiscoverySyncStatus()
}

