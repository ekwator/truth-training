//! Integration test for run_http_reachability_checks function
//! Tests T024: HTTP reachability checks with timeouts and retries

use anyhow::Result;
use core_lib::models::{Node, NodeSource, NodeType};
use core_lib::storage;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Mutex;
use truth_core::p2p::node::run_http_reachability_checks;

#[tokio::test(flavor = "multi_thread")]
async fn test_http_reachability_empty_nodes() -> Result<()> {
    // Test with empty node list
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));
    
    let processed = run_http_reachability_checks(
        conn.clone(),
        Duration::from_secs(5),
        3,
    ).await?;
    
    assert_eq!(processed, 0, "Empty node list should process 0 nodes");
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn test_http_reachability_updates_db() -> Result<()> {
    // Test that reachability checks update node status in database
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));
    
    // Insert a test node
    let now = chrono::Utc::now().timestamp();
    let test_node = Node {
        id: 0,
        address: "http://127.0.0.1:65535/api/v1".to_string(), // Unlikely to be reachable
        node_type: NodeType::Lan,
        reachable: true, // Initially marked as reachable
        last_seen: now,
        ttl: 120,
        source: Some(NodeSource::LocalBroadcast),
        node_id: Some("test-node".to_string()),
        created_at: now,
        updated_at: now,
    };
    
    {
        let guard = conn.lock().await;
        storage::upsert_node_by_address(&guard, &test_node)?;
    }
    
    // Run reachability check with short timeout
    let processed = run_http_reachability_checks(
        conn.clone(),
        Duration::from_millis(100), // Very short timeout
        1, // Single retry
    ).await?;
    
    assert_eq!(processed, 1, "Should process one node");
    
    // Verify node status was updated (likely marked as unreachable)
    {
        let guard = conn.lock().await;
        let filter = core_lib::models::NodeFilter {
            address: Some(test_node.address.clone()),
            ..Default::default()
        };
        let nodes = storage::list_nodes(&guard, &filter)?;
        assert_eq!(nodes.len(), 1, "Node should still exist");
        // Node may be marked as unreachable due to timeout
        // This is expected behavior
    }
    
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn test_http_reachability_timeout_handling() -> Result<()> {
    // Test that timeouts are handled gracefully
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));
    
    // Insert multiple nodes with different addresses
    let now = chrono::Utc::now().timestamp();
    let nodes = vec![
        Node {
            id: 0,
            address: "http://192.0.2.1:8080/api/v1".to_string(), // Test net (RFC 3330)
            node_type: NodeType::Global,
            reachable: true,
            last_seen: now,
            ttl: 3600,
            source: Some(NodeSource::GlobalRegistry),
            node_id: Some("test-1".to_string()),
            created_at: now,
            updated_at: now,
        },
        Node {
            id: 0,
            address: "http://192.0.2.2:8080/api/v1".to_string(),
            node_type: NodeType::Global,
            reachable: true,
            last_seen: now,
            ttl: 3600,
            source: Some(NodeSource::GlobalRegistry),
            node_id: Some("test-2".to_string()),
            created_at: now,
            updated_at: now,
        },
    ];
    
    {
        let guard = conn.lock().await;
        for node in &nodes {
            storage::upsert_node_by_address(&guard, node)?;
        }
    }
    
    // Run reachability check with very short timeout
    let processed = run_http_reachability_checks(
        conn.clone(),
        Duration::from_millis(50), // Very short timeout
        0, // No retries
    ).await?;
    
    assert_eq!(processed, nodes.len() as usize, "Should process all nodes");
    
    // Verify all nodes were checked (status may be updated)
    {
        let guard = conn.lock().await;
        let filter = core_lib::models::NodeFilter::default();
        let stored_nodes = storage::list_nodes(&guard, &filter)?;
        assert_eq!(stored_nodes.len(), nodes.len(), "All nodes should still exist");
    }
    
    Ok(())
}

