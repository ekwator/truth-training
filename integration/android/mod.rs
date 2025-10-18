#![allow(non_snake_case, clippy::not_unsafe_ptr_arg_deref)]

pub mod verify_json;

use std::ffi::CString;
use std::os::raw::c_char;

#[cfg(target_os = "android")]
use jni::objects::{JClass, JString};
#[cfg(target_os = "android")]
use jni::sys::jstring;
#[cfg(target_os = "android")]
use jni::JNIEnv;

/// Initialize the Truth Core runtime.
#[no_mangle]
pub extern "C" fn Java_com_truth_training_client_TruthCore_initNode() {
    // Initialize the Truth Core runtime here
    // truth_core::init_runtime();
}

/// Get node info as a JSON string.
#[no_mangle]
pub extern "C" fn Java_com_truth_training_client_TruthCore_getInfo() -> *mut c_char {
    let info = r#"{"name":"truth-core","version":"0.3.0","uptime_sec":0,"started_at":0,"features":["p2p-client-sync","jwt"],"peer_count":0}"#;
    CString::new(info).unwrap().into_raw()
}

/// Free a C string returned by the library.
#[no_mangle]
pub extern "C" fn Java_com_truth_training_client_TruthCore_freeString(s: *mut c_char) {
    if !s.is_null() {
        unsafe {
            let _ = CString::from_raw(s);
        }
    }
}

/// Process JSON requests from Android client. Available only on Android builds.
#[cfg(target_os = "android")]
#[no_mangle]
pub extern "system" fn Java_com_truth_training_client_TruthCore_processJsonRequest(
    mut env: JNIEnv,
    _class: JClass,
    request: JString,
) -> jstring {
    let input: String = match env.get_string(&request) {
        Ok(jstr) => jstr.into(),
        Err(_) => return env.new_string(r#"{"error":"invalid_input"}"#).unwrap().into_raw(),
    };

    // If Android envelope contains signed payload, verify first.
    let parsed: serde_json::Value = match serde_json::from_str(&input) {
        Ok(v) => v,
        Err(_) => return env.new_string(r#"{"error":"invalid_json"}"#).unwrap().into_raw(),
    };

    // Try verification path if envelope fields present
    let verified_payload_opt = if parsed.get("signature").is_some()
        && parsed.get("public_key").is_some()
        && parsed.get("payload").is_some()
    {
        match verify_json::verify_json_message(&input) {
            Ok(trusted) => {
                // replace parsed with verified payload
                Some(trusted.payload)
            }
            Err(_) => {
                let err = json!({"status":"error","reason":"invalid_signature"});
                return env.new_string(err.to_string()).unwrap().into_raw();
            }
        }
    } else {
        None
    };

    let parsed = verified_payload_opt.unwrap_or(parsed);

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
            "timestamp": chrono::Utc::now().timestamp()
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
        // P2P: trigger peer discovery/sync
        Some("sync_peers") => {
            // TODO: integrate with truth_core::p2p::sync to perform discovery/sync and fetch peers
            json!({
                "status": "ok",
                "peers": ["node1.local", "node2.local"]
            })
        }
        // Semantic: register a new claim
        Some("submit_claim") => {
            let claim = parsed["claim"].as_str().unwrap_or("").to_string();
            // TODO: integrate with storage or semantic engine to persist claim
            json!({
                "status": "received",
                "claim": claim
            })
        }
        // Semantic: get existing claims (summary)
        Some("get_claims") => {
            // TODO: load claims from storage
            json!({
                "status": "ok",
                "claims": [
                    "Earth is round",
                    "Truth is distributed"
                ]
            })
        }
        // Semantic: analyze a text snippet
        Some("analyze_text") => {
            let text = parsed["text"].as_str().unwrap_or("");
            // TODO: call truth_core semantic analyzer when available
            let keywords = if text.is_empty() { vec![] } else { vec!["truth", "context"] };
            json!({
                "status": "ok",
                "sentiment": "neutral",
                "keywords": keywords
            })
        }
        _ => json!({
            "error": "unknown_action",
            "received_action": parsed["action"]
        }),
    };

    match env.new_string(response.to_string()) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => env
            .new_string(r#"{"error":"response_creation_failed"}"#)
            .unwrap()
            .into_raw(),
    }
}
