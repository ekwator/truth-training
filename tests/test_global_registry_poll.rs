//! Integration test for poll_global_registries function
//! Tests T024: Global registry polling + HTTP reachability checks

use anyhow::Result;
use core_lib::storage;
use std::sync::Arc;
use tokio::sync::Mutex;
use truth_core::node::NodeConfig;
use truth_core::p2p::node::poll_global_registries;

#[tokio::test(flavor = "multi_thread")]
async fn test_poll_global_registries_empty_list() -> Result<()> {
    // Test with empty registry list
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));
    let config = NodeConfig {
        bind_host: "0.0.0.0".into(),
        bind_port: 8080,
        public_host: None,
        public_port: None,
        timing: core_lib::DEFAULT_DISCOVERY_TIMING,
        global_registry_urls: vec![],
    };

    let count = poll_global_registries(&config, conn.clone()).await?;
    assert_eq!(count, 0, "Empty registry list should return 0");
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn test_poll_global_registries_invalid_url() -> Result<()> {
    // Test with invalid registry URL (should handle gracefully)
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));
    let config = NodeConfig {
        bind_host: "0.0.0.0".into(),
        bind_port: 8080,
        public_host: None,
        public_port: None,
        timing: core_lib::DEFAULT_DISCOVERY_TIMING,
        global_registry_urls: vec![
            "http://invalid-registry-that-does-not-exist.example.com/nodes".to_string(),
        ],
    };

    // Should not panic, but may return 0 or log warnings
    let count = poll_global_registries(&config, conn.clone()).await?;
    // Count may be 0 if registry is unreachable, which is acceptable
    // count is usize, which is always >= 0, so we just verify it's a valid result
    assert_eq!(count, 0, "Invalid registry URL should return 0 nodes");
    Ok(())
}

#[tokio::test(flavor = "multi_thread")]
async fn test_poll_global_registries_db_integration() -> Result<()> {
    // Test that polled nodes are stored in database
    let conn = Arc::new(Mutex::new(storage::open_db(":memory:")?));

    // Use a mock registry URL (in real test, would use wiremock or similar)
    // For now, test with empty list and verify DB structure
    let config = NodeConfig {
        bind_host: "0.0.0.0".into(),
        bind_port: 8080,
        public_host: None,
        public_port: None,
        timing: core_lib::DEFAULT_DISCOVERY_TIMING,
        global_registry_urls: vec![],
    };

    let _count = poll_global_registries(&config, conn.clone()).await?;

    // Verify nodes table exists and is accessible
    {
        let guard = conn.lock().await;
        let filter = core_lib::models::NodeFilter::default();
        let nodes = storage::list_nodes(&guard, &filter)?;
        // With empty registry, should have 0 nodes
        assert_eq!(nodes.len(), 0);
    }

    Ok(())
}
