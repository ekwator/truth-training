// Contract tests for POST /events with embedded context fields
// These tests MUST FAIL until implementation is complete (TDD)

use actix_web::{test, App};

#[actix_web::test]
async fn post_events_accepts_embedded_fields() {
    // in-memory DB
    let conn = core_lib::storage::open_db(":memory:").unwrap();
    let conn_data = std::sync::Arc::new(tokio::sync::Mutex::new(conn));
    {
        let mut c = conn_data.lock().await;
        core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
    }

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(crate::api::routes)
    ).await;

    // Contract: POST /events accepts embedded fields (category_id, forma_id, etc.)
    let body = serde_json::json!({
        "description": "contract: event with embedded context",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "develop_id": 3,
        "effect_id": 2,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("id").and_then(|v| v.as_i64()).is_some(), "response must contain id");
}

#[actix_web::test]
async fn post_events_rejects_context_id() {
    // Contract: POST /events MUST reject context_id field (breaking change)
    let conn = core_lib::storage::open_db(":memory:").unwrap();
    let conn_data = std::sync::Arc::new(tokio::sync::Mutex::new(conn));
    {
        let mut c = conn_data.lock().await;
        core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
    }

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(crate::api::routes)
    ).await;

    let body = serde_json::json!({
        "description": "contract: invalid request with context_id",
        "context_id": 1,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp = test::call_service(&app, req).await;
    // Should reject context_id - either 400 Bad Request or deserialization error
    assert!(!resp.status().is_success(), "context_id should be rejected");
}

#[actix_web::test]
async fn post_events_validates_foreign_keys() {
    // Contract: POST /events validates FK references, returns 400 for invalid FKs
    let conn = core_lib::storage::open_db(":memory:").unwrap();
    let conn_data = std::sync::Arc::new(tokio::sync::Mutex::new(conn));
    {
        let mut c = conn_data.lock().await;
        core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
    }

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(crate::api::routes)
    ).await;

    // Invalid category_id (non-existent)
    let body = serde_json::json!({
        "description": "contract: invalid FK reference",
        "category_id": 99999,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status().as_u16(), 400, "invalid FK should return 400");
}

#[actix_web::test]
async fn post_events_accepts_nullable_fields() {
    // Contract: POST /events accepts NULL for optional context fields
    let conn = core_lib::storage::open_db(":memory:").unwrap();
    let conn_data = std::sync::Arc::new(tokio::sync::Mutex::new(conn));
    {
        let mut c = conn_data.lock().await;
        core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
    }

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(crate::api::routes)
    ).await;

    // All context fields NULL (valid)
    let body = serde_json::json!({
        "description": "contract: event with null context fields",
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("id").and_then(|v| v.as_i64()).is_some(), "should accept all NULL fields");
}

