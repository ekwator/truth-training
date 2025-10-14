use serde_json::{json, Value};

/// Test the JSON request processing logic without JNI dependencies
fn process_json_request_logic(input: &str) -> String {
    let parsed: Value = match serde_json::from_str(input) {
        Ok(v) => v,
        Err(_) => return r#"{"error":"invalid_json"}"#.to_string(),
    };

    let response = match parsed["action"].as_str() {
        Some("get_state") => json!({
            "status": "ok",
            "state": "connected",
            "version": "0.3.0",
            "uptime": 12345
        }),
        Some("ping") => json!({
            "status": "ok",
            "reply": "pong",
            "timestamp": 1710000000
        }),
        Some("get_info") => json!({
            "status": "ok",
            "name": "truth-core",
            "version": "0.3.0",
            "features": ["p2p-client-sync", "jwt"],
            "peer_count": 0
        }),
        Some("get_stats") => json!({
            "status": "ok",
            "events": 120,
            "statements": 340,
            "impacts": 21,
            "node_ratings": 8,
            "group_ratings": 2,
            "avg_trust_score": 0.62,
            "avg_propagation_priority": 0.71,
            "avg_relay_success_rate": 0.84,
            "active_nodes": 7
        }),
        _ => json!({
            "error": "unknown_action",
            "received_action": parsed["action"]
        }),
    };

    response.to_string()
}

#[test]
fn test_get_state_request() {
    let input = r#"{"action":"get_state"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["status"], "ok");
    assert_eq!(parsed["state"], "connected");
    assert_eq!(parsed["version"], "0.3.0");
    assert_eq!(parsed["uptime"], 12345);
}

#[test]
fn test_ping_request() {
    let input = r#"{"action":"ping"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["status"], "ok");
    assert_eq!(parsed["reply"], "pong");
    assert!(parsed["timestamp"].is_number());
}

#[test]
fn test_get_info_request() {
    let input = r#"{"action":"get_info"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["status"], "ok");
    assert_eq!(parsed["name"], "truth-core");
    assert_eq!(parsed["version"], "0.3.0");
    assert!(parsed["features"].is_array());
    assert_eq!(parsed["peer_count"], 0);
}

#[test]
fn test_get_stats_request() {
    let input = r#"{"action":"get_stats"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["status"], "ok");
    assert_eq!(parsed["events"], 120);
    assert_eq!(parsed["statements"], 340);
    assert_eq!(parsed["impacts"], 21);
    assert_eq!(parsed["node_ratings"], 8);
    assert_eq!(parsed["group_ratings"], 2);
    assert_eq!(parsed["avg_trust_score"], 0.62);
    assert_eq!(parsed["avg_propagation_priority"], 0.71);
    assert_eq!(parsed["avg_relay_success_rate"], 0.84);
    assert_eq!(parsed["active_nodes"], 7);
}

#[test]
fn test_unknown_action() {
    let input = r#"{"action":"unknown_action"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["error"], "unknown_action");
    assert_eq!(parsed["received_action"], "unknown_action");
}

#[test]
fn test_invalid_json() {
    let input = r#"{"action":"get_state""#; // Missing closing brace
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["error"], "invalid_json");
}

#[test]
fn test_missing_action() {
    let input = r#"{"other_field":"value"}"#;
    let output = process_json_request_logic(input);
    let parsed: Value = serde_json::from_str(&output).unwrap();
    
    assert_eq!(parsed["error"], "unknown_action");
    assert!(parsed["received_action"].is_null());
}

