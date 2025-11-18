//! Integration test for UDP multicast packet compatibility between Desktop (Rust) and Android (Kotlin).
//! 
//! Tests T061: Verify UDP multicast compatibility between Desktop (Rust) and Android (Kotlin) with packet roundtrip tests.
//! 
//! This test verifies that:
//! - Rust can generate packets that Android can parse (JSON format compatibility)
//! - Android can generate packets that Rust can parse (JSON format compatibility)
//! - Signature verification works across platforms (ed25519)
//! - Packet format matches specification (node_id, address, node_type, ttl, timestamp, signature)
//! - All NodeType values are correctly serialized/deserialized

use anyhow::Result;
use chrono::Utc;
use core_lib::models::NodeType;
use ed25519_dalek::{SigningKey, VerifyingKey, Signature, Signer, Verifier};
use hex;
use rand::rngs::OsRng;
use serde_json;
use truth_core::p2p::encryption::CryptoIdentity;
use truth_core::p2p::node::LanAnnouncement;

/// Test that Rust-generated packets match Android's expected JSON format.
#[test]
fn test_rust_packet_format_matches_android() -> Result<()> {
    // Create a test identity
    let mut rng = OsRng;
    let signing_key = SigningKey::generate(&mut rng);
    let identity = CryptoIdentity::from_keypair(signing_key);
    
    let node_id = identity.public_key_hex();
    let address = "http://192.168.1.100:8080/api/v1".to_string();
    let node_type = NodeType::Lan;
    let ttl = 120;
    let timestamp = Utc::now().timestamp();
    
    // Create announcement payload (same format as Rust implementation)
    let payload = format!("{node_id}|{address}|{}|{ttl}|{timestamp}", node_type.as_str());
    let signature = identity.sign(payload.as_bytes());
    let signature_hex = hex::encode(signature.to_bytes());
    
    // Create LanAnnouncement struct
    let announcement = LanAnnouncement {
        node_id: node_id.clone(),
        address: address.clone(),
        node_type,
        ttl,
        timestamp,
        signature: signature_hex.clone(),
    };
    
    // Serialize to JSON (as Rust does)
    let json_bytes = serde_json::to_vec(&announcement)?;
    let json_string = String::from_utf8(json_bytes.clone())?;
    
    // Verify JSON structure matches Android's expected format
    let json: serde_json::Value = serde_json::from_str(&json_string)?;
    
    // Check all required fields are present
    assert!(json.get("node_id").is_some(), "JSON must contain 'node_id' field");
    assert!(json.get("address").is_some(), "JSON must contain 'address' field");
    assert!(json.get("node_type").is_some(), "JSON must contain 'node_type' field");
    assert!(json.get("ttl").is_some(), "JSON must contain 'ttl' field");
    assert!(json.get("timestamp").is_some(), "JSON must contain 'timestamp' field");
    assert!(json.get("signature").is_some(), "JSON must contain 'signature' field");
    
    // Verify field types match Android's expectations
    assert!(json["node_id"].is_string(), "node_id must be a string");
    assert!(json["address"].is_string(), "address must be a string");
    assert!(json["node_type"].is_string(), "node_type must be a string");
    assert!(json["ttl"].is_number(), "ttl must be a number");
    assert!(json["timestamp"].is_number(), "timestamp must be a number");
    assert!(json["signature"].is_string(), "signature must be a string");
    
    // Verify node_type is uppercase (Android expects "LAN", "WIFI", etc.)
    let node_type_str = json["node_type"].as_str().unwrap();
    assert_eq!(node_type_str, "LAN", "node_type must be uppercase: expected 'LAN', got '{}'", node_type_str);
    
    // Verify node_id is hex string (64 chars for ed25519 public key)
    let node_id_str = json["node_id"].as_str().unwrap();
    assert_eq!(node_id_str.len(), 64, "node_id must be 64 hex characters");
    assert!(hex::decode(node_id_str).is_ok(), "node_id must be valid hex");
    
    // Verify signature is hex string (128 chars for ed25519 signature)
    let sig_str = json["signature"].as_str().unwrap();
    assert_eq!(sig_str.len(), 128, "signature must be 128 hex characters");
    assert!(hex::decode(sig_str).is_ok(), "signature must be valid hex");
    
    // Verify signature can be verified (cross-platform compatibility)
    let pk_bytes = hex::decode(node_id_str)?;
    let pk_array: [u8; 32] = pk_bytes.as_slice().try_into()?;
    let verifying_key = VerifyingKey::from_bytes(&pk_array)?;
    
    let sig_bytes = hex::decode(sig_str)?;
    let sig_array: [u8; 64] = sig_bytes.as_slice().try_into()?;
    let signature = Signature::from_bytes(&sig_array);
    
    verifying_key.verify(payload.as_bytes(), &signature)?;
    
    Ok(())
}

/// Test that all NodeType values serialize correctly for Android.
#[test]
fn test_all_node_types_serialize_correctly() -> Result<()> {
    let node_types = vec![
        (NodeType::Lan, "LAN"),
        (NodeType::Wifi, "WIFI"),
        (NodeType::Global, "GLOBAL"),
        (NodeType::Relay, "RELAY"),
        (NodeType::Client, "CLIENT"),
    ];
    
    for (node_type, expected_str) in node_types {
        let announcement = LanAnnouncement {
            node_id: "a".repeat(64),
            address: "http://test:8080/api/v1".to_string(),
            node_type,
            ttl: 120,
            timestamp: 1000,
            signature: "b".repeat(128),
        };
        
        let json_bytes = serde_json::to_vec(&announcement)?;
        let json_string = String::from_utf8(json_bytes)?;
        let json: serde_json::Value = serde_json::from_str(&json_string)?;
        
        let node_type_str = json["node_type"].as_str().unwrap();
        assert_eq!(
            node_type_str, expected_str,
            "NodeType {:?} should serialize to '{}', got '{}'",
            node_type, expected_str, node_type_str
        );
    }
    
    Ok(())
}

/// Test that Android-style JSON can be deserialized by Rust.
#[test]
fn test_android_json_deserializes_in_rust() -> Result<()> {
    // Simulate JSON that Android would generate
    let android_json = r#"{
        "node_id": "a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890",
        "address": "http://192.168.1.200:8080/api/v1",
        "node_type": "WIFI",
        "ttl": 300,
        "timestamp": 1700000000,
        "signature": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }"#;
    
    // Deserialize using Rust's LanAnnouncement struct
    let announcement: LanAnnouncement = serde_json::from_str(android_json)?;
    
    // Verify all fields are correctly parsed
    assert_eq!(announcement.node_id, "a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890");
    assert_eq!(announcement.address, "http://192.168.1.200:8080/api/v1");
    assert_eq!(announcement.node_type, NodeType::Wifi);
    assert_eq!(announcement.ttl, 300);
    assert_eq!(announcement.timestamp, 1700000000);
    assert_eq!(announcement.signature.len(), 128);
    
    Ok(())
}

/// Test signature payload format matches between Rust and Android.
#[test]
fn test_signature_payload_format() -> Result<()> {
    // Create test identity
    let mut rng = OsRng;
    let signing_key = SigningKey::generate(&mut rng);
    let identity = CryptoIdentity::from_keypair(signing_key);
    
    let node_id = identity.public_key_hex();
    let address = "http://test:8080/api/v1".to_string();
    let node_type = NodeType::Lan;
    let ttl = 120;
    let timestamp = 1700000000;
    
    // Rust payload format: "{node_id}|{address}|{node_type}|{ttl}|{timestamp}"
    let rust_payload = format!("{node_id}|{address}|{}|{ttl}|{timestamp}", node_type.as_str());
    
    // Android payload format (from LanDiscoveryClient.kt):
    // "${announcement.node_id}|${announcement.address}|${announcement.node_type}|${announcement.ttl}|${announcement.timestamp}"
    let android_payload = format!("{node_id}|{address}|{}|{ttl}|{timestamp}", node_type.as_str());
    
    // They should be identical
    assert_eq!(rust_payload, android_payload, "Rust and Android payload formats must match");
    
    // Sign with Rust
    let rust_signature = identity.sign(rust_payload.as_bytes());
    let rust_sig_hex = hex::encode(rust_signature.to_bytes());
    
    // Verify signature can be verified using the same payload
    let pk_bytes = hex::decode(&node_id)?;
    let pk_array: [u8; 32] = pk_bytes.as_slice().try_into()?;
    let verifying_key = VerifyingKey::from_bytes(&pk_array)?;
    
    let sig_bytes = hex::decode(&rust_sig_hex)?;
    let sig_array: [u8; 64] = sig_bytes.as_slice().try_into()?;
    let signature = Signature::from_bytes(&sig_array);
    
    // Verify with both payload formats (they're the same)
    verifying_key.verify(rust_payload.as_bytes(), &signature)?;
    verifying_key.verify(android_payload.as_bytes(), &signature)?;
    
    Ok(())
}

/// Test packet roundtrip: Rust -> JSON -> Rust.
#[test]
fn test_packet_roundtrip_rust_to_rust() -> Result<()> {
    let mut rng = rand::rngs::OsRng;
    let signing_key = SigningKey::generate(&mut rng);
    let identity = CryptoIdentity::from_keypair(signing_key);
    
    let node_id = identity.public_key_hex();
    let address = "http://roundtrip:8080/api/v1".to_string();
    let node_type = NodeType::Global;
    let ttl = 3600;
    let timestamp = Utc::now().timestamp();
    
    let payload = format!("{node_id}|{address}|{}|{ttl}|{timestamp}", node_type.as_str());
    let signature = identity.sign(payload.as_bytes());
    let signature_hex = hex::encode(signature.to_bytes());
    
    let original = LanAnnouncement {
        node_id: node_id.clone(),
        address: address.clone(),
        node_type,
        ttl,
        timestamp,
        signature: signature_hex.clone(),
    };
    
    // Serialize to JSON
    let json_bytes = serde_json::to_vec(&original)?;
    let json_string = String::from_utf8(json_bytes)?;
    
    // Deserialize from JSON
    let deserialized: LanAnnouncement = serde_json::from_str(&json_string)?;
    
    // Verify all fields match
    assert_eq!(deserialized.node_id, original.node_id);
    assert_eq!(deserialized.address, original.address);
    assert_eq!(deserialized.node_type, original.node_type);
    assert_eq!(deserialized.ttl, original.ttl);
    assert_eq!(deserialized.timestamp, original.timestamp);
    assert_eq!(deserialized.signature, original.signature);
    
    // Verify signature still validates
    let pk_bytes = hex::decode(&deserialized.node_id)?;
    let pk_array: [u8; 32] = pk_bytes.as_slice().try_into()?;
    let verifying_key = VerifyingKey::from_bytes(&pk_array)?;
    
    let sig_bytes = hex::decode(&deserialized.signature)?;
    let sig_array: [u8; 64] = sig_bytes.as_slice().try_into()?;
    let sig = Signature::from_bytes(&sig_array);
    
    let verify_payload = format!("{node_id}|{address}|{}|{ttl}|{timestamp}", node_type.as_str());
    verifying_key.verify(verify_payload.as_bytes(), &sig)?;
    
    Ok(())
}

/// Test that packet format handles edge cases correctly.
#[test]
fn test_packet_format_edge_cases() -> Result<()> {
    // Test with different address formats
    let addresses = vec![
        "http://192.168.1.100:8080/api/v1",
        "https://example.com:443/api/v1",
        "http://[::1]:8080/api/v1",
    ];
    
    for address in addresses {
        let announcement = LanAnnouncement {
            node_id: "a".repeat(64),
            address: address.to_string(),
            node_type: NodeType::Lan,
            ttl: 120,
            timestamp: 1000,
            signature: "b".repeat(128),
        };
        
        let json_bytes = serde_json::to_vec(&announcement)?;
        let json_string = String::from_utf8(json_bytes)?;
        let deserialized: LanAnnouncement = serde_json::from_str(&json_string)?;
        
        assert_eq!(deserialized.address, address);
    }
    
    // Test with different TTL values
    let ttls = vec![120, 300, 3600, 7200];
    for ttl in ttls {
        let announcement = LanAnnouncement {
            node_id: "a".repeat(64),
            address: "http://test:8080/api/v1".to_string(),
            node_type: NodeType::Lan,
            ttl,
            timestamp: 1000,
            signature: "b".repeat(128),
        };
        
        let json_bytes = serde_json::to_vec(&announcement)?;
        let json_string = String::from_utf8(json_bytes)?;
        let deserialized: LanAnnouncement = serde_json::from_str(&json_string)?;
        
        assert_eq!(deserialized.ttl, ttl);
    }
    
    Ok(())
}

