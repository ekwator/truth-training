package com.truth.training.client.data.database.daos

import androidx.room.*
import com.truth.training.client.data.database.entities.NodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for nodes table operations matching core/src/storage.rs functionality.
 * 
 * Implements canonical SQLite schema for cross-platform compatibility.
 * 
 * Reference:
 * - core/src/storage.rs::NodeRepository
 * - docs/cross_platform_discovery_compatibility.md
 */
@Dao
interface NodeDao {
    /**
     * Upsert node by address (INSERT OR REPLACE).
     * Matches core/src/storage.rs::upsert_node_by_address()
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNode(node: NodeEntity): Long
    
    /**
     * Get node by ID.
     * Matches core/src/storage.rs::get_node()
     */
    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getNode(id: Long): NodeEntity?
    
    /**
     * Get node by address.
     * Matches core/src/storage.rs::get_node_by_address()
     */
    @Query("SELECT * FROM nodes WHERE address = :address")
    suspend fun getNodeByAddress(address: String): NodeEntity?
    
    /**
     * List nodes with optional filters.
     * Matches core/src/storage.rs::list_nodes()
     * 
     * @param nodeType Optional filter by type ("LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT")
     * @param reachable Optional filter by reachability (0 or 1)
     * @param address Optional filter by exact address match
     * @param limit Optional limit on result count
     */
    @Query("""
        SELECT * FROM nodes 
        WHERE (:nodeType IS NULL OR type = :nodeType)
          AND (:reachable IS NULL OR reachable = :reachable)
          AND (:address IS NULL OR address = :address)
        ORDER BY last_seen DESC
        LIMIT CASE WHEN :limit > 0 THEN :limit ELSE 999999 END
    """)
    fun listNodes(
        nodeType: String? = null,
        reachable: Int? = null,
        address: String? = null,
        limit: Int = 0
    ): Flow<List<NodeEntity>>
    
    /**
     * List nodes synchronously (for testing and non-reactive contexts).
     */
    @Query("""
        SELECT * FROM nodes 
        WHERE (:nodeType IS NULL OR type = :nodeType)
          AND (:reachable IS NULL OR reachable = :reachable)
          AND (:address IS NULL OR address = :address)
        ORDER BY last_seen DESC
        LIMIT CASE WHEN :limit > 0 THEN :limit ELSE 999999 END
    """)
    suspend fun listNodesSync(
        nodeType: String? = null,
        reachable: Int? = null,
        address: String? = null,
        limit: Int = 0
    ): List<NodeEntity>
    
    /**
     * Update node reachability and last_seen.
     * Matches core/src/storage.rs::update_node() for reachability updates.
     */
    @Query("""
        UPDATE nodes 
        SET reachable = :reachable, 
            last_seen = CASE WHEN :reachable = 1 THEN :now ELSE last_seen END,
            updated_at = :now
        WHERE id = :nodeId
    """)
    suspend fun updateReachability(nodeId: Long, reachable: Int, now: Long): Int
    
    /**
     * Update node TTL.
     */
    @Query("""
        UPDATE nodes 
        SET ttl = :ttl, updated_at = :now
        WHERE id = :nodeId
    """)
    suspend fun updateTtl(nodeId: Long, ttl: Long, now: Long): Int
    
    /**
     * Update node last_seen timestamp.
     */
    @Query("""
        UPDATE nodes 
        SET last_seen = :lastSeen, updated_at = :now
        WHERE id = :nodeId
    """)
    suspend fun updateLastSeen(nodeId: Long, lastSeen: Long, now: Long): Int
    
    /**
     * Delete node by ID.
     * Matches core/src/storage.rs::delete_node()
     */
    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteNode(id: Long): Int
    
    /**
     * Prune stale nodes (TTL expired or unreachable for > ttl/2).
     * Matches core/src/storage.rs::prune_stale_nodes()
     * 
     * @param now Current Unix timestamp (seconds)
     * @return Number of nodes deleted
     */
    @Query("""
        DELETE FROM nodes 
        WHERE (:now - last_seen) > ttl
           OR (reachable = 0 AND (:now - last_seen) > (ttl / 2))
    """)
    suspend fun pruneStaleNodes(now: Long): Int
    
    /**
     * Count nodes matching filters.
     */
    @Query("""
        SELECT COUNT(*) FROM nodes 
        WHERE (:nodeType IS NULL OR type = :nodeType)
          AND (:reachable IS NULL OR reachable = :reachable)
    """)
    suspend fun countNodes(nodeType: String? = null, reachable: Int? = null): Int
}

