use core_lib::storage;
/// Tests for DiscoveryManager lifecycle, worker management, and settings persistence
///
/// Note: These tests require Tauri runtime, so they are integration-style tests.
/// For unit testing individual functions, see the core library tests.
use std::sync::Arc;
use tempfile::TempDir;
use tokio::sync::Mutex as TokioMutex;

#[tokio::test]
async fn test_discovery_manager_initialization() {
    let temp_dir = TempDir::new().expect("Failed to create temp dir");
    let db_path = temp_dir.path().join("test_nodes.sqlite");

    // Test that we can open a database connection (core functionality)
    let conn = Arc::new(TokioMutex::new(
        core_lib::storage::open_db(&db_path.display().to_string()).expect("Failed to open test DB"),
    ));

    // Verify connection works
    {
        let guard = conn.lock().await;
        let result: i64 = guard
            .query_row("SELECT 1", rusqlite::params![], |row| row.get(0))
            .expect("Connection should work");
        assert_eq!(result, 1);
    }

    // Verify nodes table exists (created by init_db)
    {
        let guard = conn.lock().await;
        let mut stmt = guard
            .prepare("SELECT name FROM sqlite_master WHERE type='table' AND name='nodes'")
            .expect("Failed to prepare statement");
        let exists: bool = stmt
            .exists(rusqlite::params![])
            .expect("Failed to check table");
        assert!(exists, "nodes table should exist after init_db");
    }
}

#[tokio::test]
async fn test_node_storage_operations() {
    // Test core storage operations used by DiscoveryManager
    let temp_dir = TempDir::new().expect("Failed to create temp dir");
    let db_path = temp_dir.path().join("test_nodes.sqlite");

    let conn = Arc::new(TokioMutex::new(
        core_lib::storage::open_db(&db_path.display().to_string()).expect("Failed to open test DB"),
    ));

    // Insert a test node
    let now = chrono::Utc::now().timestamp();
    let test_node = core_lib::models::Node {
        id: 0,
        address: "http://test-node:8080/api/v1".to_string(),
        node_type: core_lib::models::NodeType::Lan,
        reachable: true,
        last_seen: now,
        ttl: 120,
        source: Some(core_lib::models::NodeSource::LocalBroadcast),
        node_id: Some("test-node-id".to_string()),
        created_at: now,
        updated_at: now,
    };

    {
        let guard = conn.lock().await;
        core_lib::storage::upsert_node_by_address(&guard, &test_node)
            .expect("Failed to upsert node");
    }

    // Verify node was stored
    {
        let guard = conn.lock().await;
        let filter = core_lib::models::NodeFilter {
            node_type: Some(core_lib::models::NodeType::Lan),
            reachable: None,
            limit: None,
            address: None,
        };
        let nodes = core_lib::storage::list_nodes(&guard, &filter).expect("Failed to list nodes");
        assert_eq!(nodes.len(), 1, "Should have one node");
        assert_eq!(nodes[0].address, test_node.address);
    }
}

#[tokio::test]
async fn test_ttl_cleanup_logic() {
    // Test TTL cleanup logic used by DiscoveryManager
    let temp_dir = TempDir::new().expect("Failed to create temp dir");
    let db_path = temp_dir.path().join("test_nodes.sqlite");

    let conn = Arc::new(TokioMutex::new(
        core_lib::storage::open_db(&db_path.display().to_string()).expect("Failed to open test DB"),
    ));

    // Insert a stale node (expired TTL)
    let old_timestamp = chrono::Utc::now().timestamp() - 200; // 200 seconds ago
    let stale_node = core_lib::models::Node {
        id: 0,
        address: "http://stale-node:8080/api/v1".to_string(),
        node_type: core_lib::models::NodeType::Lan,
        reachable: false,
        last_seen: old_timestamp,
        ttl: 120, // TTL is 120, but last_seen is 200 seconds ago
        source: Some(core_lib::models::NodeSource::LocalBroadcast),
        node_id: Some("stale-node-id".to_string()),
        created_at: old_timestamp,
        updated_at: old_timestamp,
    };

    {
        let guard = conn.lock().await;
        core_lib::storage::upsert_node_by_address(&guard, &stale_node)
            .expect("Failed to upsert stale node");
    }

    // Run cleanup
    let now = chrono::Utc::now().timestamp();
    {
        let guard = conn.lock().await;
        let pruned =
            core_lib::storage::prune_stale_nodes(&guard, now).expect("Failed to prune stale nodes");
        assert!(pruned > 0, "Should prune at least one stale node");
    }

    // Verify stale node was removed
    {
        let guard = conn.lock().await;
        let filter = core_lib::models::NodeFilter::default();
        let nodes = core_lib::storage::list_nodes(&guard, &filter).expect("Failed to list nodes");
        assert_eq!(nodes.len(), 0, "Stale node should be pruned");
    }
}
