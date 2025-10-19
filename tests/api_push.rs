#[cfg(feature = "desktop")]
use actix_web::{test, App};
#[cfg(feature = "desktop")]
use base64::{engine::general_purpose, Engine as _};
#[cfg(feature = "desktop")]
use truth_core::api;
#[cfg(feature = "desktop")]
use truth_core::p2p::encryption::CryptoIdentity;

#[cfg(feature = "desktop")]
#[actix_web::test]
async fn push_valid_and_invalid() {
    // Init in-memory DB & app
    let conn = core_lib::storage::open_db(":memory:").unwrap();
    let conn_data = std::sync::Arc::new(tokio::sync::Mutex::new(conn));

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(crate::api::routes)
    ).await;

    // Create identity and sign payload
    let id = CryptoIdentity::new();
    let payload = serde_json::json!({ "event": "truth_claim", "value": 1 });
    let payload_bytes = serde_json::to_vec(&payload).unwrap();
    let sig = id.sign(&payload_bytes);

    let sig_b64 = general_purpose::STANDARD.encode(sig.to_bytes());
    let pk_b64 = general_purpose::STANDARD.encode(id.verifying_key.to_bytes());

    // Issue JWT via existing auth flow
    // Note: /api/v1/auth expects headers with pk/sig/timestamp; we shortcut by issuing directly with helper when available.
    // For test simplicity, just build a Bearer from api::issue_jwt_pair_with through DB.
    let token = {
        let conn = conn_data.lock().await;
        let (access, _refresh, _exp) = api::issue_jwt_pair_with(&conn, &id.public_key_hex()).unwrap();
        access
    };

    // Valid request
    let req_body = serde_json::json!({
        "node_id": "android-abc123",
        "payload": payload,
        "signature": sig_b64,
        "public_key": pk_b64
    });
    let req = test::TestRequest::post()
        .uri("/api/v1/push")
        .insert_header(("Authorization", format!("Bearer {}", token)))
        .set_json(&req_body)
        .to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert_eq!(resp.get("status").unwrap(), "ok");

    // Invalid signature
    let bad_sig_b64 = general_purpose::STANDARD.encode([0u8; 64]);
    let bad_req = test::TestRequest::post()
        .uri("/api/v1/push")
        .insert_header(("Authorization", format!("Bearer {}", token)))
        .set_json(serde_json::json!({
            "node_id": "android-abc123",
            "payload": { "event": "truth_claim", "value": 1 },
            "signature": bad_sig_b64,
            "public_key": pk_b64
        }))
        .to_request();
    let bad_resp: serde_json::Value = test::call_and_read_body_json(&app, bad_req).await;
    assert_eq!(bad_resp.get("status").unwrap(), "error");
    assert_eq!(bad_resp.get("reason").unwrap(), "invalid_signature");
}
