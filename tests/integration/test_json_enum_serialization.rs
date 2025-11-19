//! Integration test for JSON enum serialization consistency.
//! 
//! Tests T060: Verify JSON enum serialization matches across Rust/Kotlin/JS.
//! 
//! This test verifies that:
//! - NodeType serializes to UPPERCASE strings ("LAN", "WIFI", etc.)
//! - NodeSource serializes to snake_case strings ("local_broadcast", etc.)
//! - Serialization matches Android Kotlin and JavaScript implementations

use core_lib::models::{NodeType, NodeSource};
use serde_json;

#[test]
fn test_node_type_serialization_uppercase() {
    // Verify NodeType serializes to UPPERCASE (matches Android and JS)
    assert_eq!(serde_json::to_string(&NodeType::Lan).unwrap(), "\"LAN\"");
    assert_eq!(serde_json::to_string(&NodeType::Wifi).unwrap(), "\"WIFI\"");
    assert_eq!(serde_json::to_string(&NodeType::Global).unwrap(), "\"GLOBAL\"");
    assert_eq!(serde_json::to_string(&NodeType::Relay).unwrap(), "\"RELAY\"");
    assert_eq!(serde_json::to_string(&NodeType::Client).unwrap(), "\"CLIENT\"");
}

#[test]
fn test_node_type_deserialization_case_insensitive() {
    // Verify NodeType can deserialize from various case formats
    assert_eq!(serde_json::from_str::<NodeType>("\"LAN\"").unwrap(), NodeType::Lan);
    assert_eq!(serde_json::from_str::<NodeType>("\"lan\"").unwrap(), NodeType::Lan);
    assert_eq!(serde_json::from_str::<NodeType>("\"WIFI\"").unwrap(), NodeType::Wifi);
    assert_eq!(serde_json::from_str::<NodeType>("\"wifi\"").unwrap(), NodeType::Wifi);
    assert_eq!(serde_json::from_str::<NodeType>("\"WI-FI\"").unwrap(), NodeType::Wifi);
    assert_eq!(serde_json::from_str::<NodeType>("\"GLOBAL\"").unwrap(), NodeType::Global);
    assert_eq!(serde_json::from_str::<NodeType>("\"RELAY\"").unwrap(), NodeType::Relay);
    assert_eq!(serde_json::from_str::<NodeType>("\"SERVER\"").unwrap(), NodeType::Relay);
    assert_eq!(serde_json::from_str::<NodeType>("\"CLIENT\"").unwrap(), NodeType::Client);
}

#[test]
fn test_node_source_serialization_snake_case() {
    // Verify NodeSource serializes to snake_case (matches Android and JS)
    assert_eq!(
        serde_json::to_string(&NodeSource::LocalBroadcast).unwrap(),
        "\"local_broadcast\""
    );
    assert_eq!(
        serde_json::to_string(&NodeSource::WifiScan).unwrap(),
        "\"wifi_scan\""
    );
    assert_eq!(
        serde_json::to_string(&NodeSource::GlobalRegistry).unwrap(),
        "\"global_registry\""
    );
    assert_eq!(
        serde_json::to_string(&NodeSource::Manual).unwrap(),
        "\"manual\""
    );
    assert_eq!(
        serde_json::to_string(&NodeSource::PeerSync).unwrap(),
        "\"peer_sync\""
    );
}

#[test]
fn test_node_source_deserialization_case_insensitive() {
    // Verify NodeSource can deserialize from various case formats
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"local_broadcast\"").unwrap(),
        NodeSource::LocalBroadcast
    );
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"LOCAL_BROADCAST\"").unwrap(),
        NodeSource::LocalBroadcast
    );
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"wifi_scan\"").unwrap(),
        NodeSource::WifiScan
    );
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"global_registry\"").unwrap(),
        NodeSource::GlobalRegistry
    );
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"manual\"").unwrap(),
        NodeSource::Manual
    );
    assert_eq!(
        serde_json::from_str::<NodeSource>("\"peer_sync\"").unwrap(),
        NodeSource::PeerSync
    );
}

#[test]
fn test_lan_announcement_json_format() {
    // Verify LanAnnouncement JSON format matches Android/Kotlin expectations
    use truth_core::p2p::node::LanAnnouncement;
    
    let ann = LanAnnouncement {
        node_id: "abc123".to_string(),
        address: "http://127.0.0.1:8080/api/v1".to_string(),
        node_type: NodeType::Lan,
        ttl: 120,
        timestamp: 1_700_000_000,
        signature: "sig".to_string(),
    };
    
    let json = serde_json::to_string(&ann).unwrap();
    let parsed: serde_json::Value = serde_json::from_str(&json).unwrap();
    
    // Verify field names and types match Android expectations
    assert_eq!(parsed["node_id"], "abc123");
    assert_eq!(parsed["address"], "http://127.0.0.1:8080/api/v1");
    assert_eq!(parsed["node_type"], "LAN"); // UPPERCASE
    assert_eq!(parsed["ttl"], 120);
    assert_eq!(parsed["timestamp"], 1_700_000_000);
    assert_eq!(parsed["signature"], "sig");
}

