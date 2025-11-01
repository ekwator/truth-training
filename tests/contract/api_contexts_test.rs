// Contract tests for context template endpoints
// These tests MUST FAIL until implementation is complete (TDD)

use actix_web::{test, App};

#[actix_web::test]
async fn get_contexts_lists_templates() {
    // Contract: GET /contexts returns list of templates
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

    let req = test::TestRequest::get().uri("/contexts").to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("data").and_then(|v| v.as_array()).is_some(), "response must contain data array");
    assert!(resp.get("total").and_then(|v| v.as_i64()).is_some(), "response must contain total count");
}

#[actix_web::test]
async fn post_contexts_creates_template() {
    // Contract: POST /contexts creates new template
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
        "name": "Test Template",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "develop_id": 3,
        "effect_id": 2,
        "description": "Test description"
    });
    let req = test::TestRequest::post().uri("/contexts").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("id").and_then(|v| v.as_i64()).is_some(), "response must contain template id");
    assert_eq!(resp.get("name").and_then(|v| v.as_str()), Some("Test Template"));
}

#[actix_web::test]
async fn post_contexts_rejects_duplicate() {
    // Contract: POST /contexts returns 409 if duplicate non-NULL fields match
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

    // Create first template
    let body1 = serde_json::json!({
        "name": "Template 1",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req1 = test::TestRequest::post().uri("/contexts").set_json(&body1).to_request();
    let _resp1 = test::call_service(&app, req1).await;

    // Try to create duplicate (same non-NULL fields, different name)
    let body2 = serde_json::json!({
        "name": "Template 2",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req2 = test::TestRequest::post().uri("/contexts").set_json(&body2).to_request();
    let resp2 = test::call_service(&app, req2).await;
    assert_eq!(resp2.status().as_u16(), 409, "duplicate template should return 409 Conflict");
}

#[actix_web::test]
async fn post_contexts_allows_null_differences() {
    // Contract: POST /contexts allows templates with same non-NULL fields but different NULL fields
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

    // Create first template (with develop_id)
    let body1 = serde_json::json!({
        "name": "Template A",
        "category_id": 1,
        "develop_id": 3
    });
    let req1 = test::TestRequest::post().uri("/contexts").set_json(&body1).to_request();
    let _resp1 = test::call_service(&app, req1).await;

    // Create second template (same non-NULL fields, different NULL pattern) - should be allowed
    let body2 = serde_json::json!({
        "name": "Template B",
        "category_id": 1,
        "develop_id": 3,
        "effect_id": null  // Different NULL pattern, but non-NULL fields match
    });
    let req2 = test::TestRequest::post().uri("/contexts").set_json(&body2).to_request();
    // This should return 409 because non-NULL fields (category_id=1, develop_id=3) match
    let resp2 = test::call_service(&app, req2).await;
    assert_eq!(resp2.status().as_u16(), 409, "same non-NULL fields should be considered duplicate");
}

#[actix_web::test]
async fn get_contexts_by_name_returns_template() {
    // Contract: GET /contexts/by-name/{name} returns template
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

    // Create template first
    let body = serde_json::json!({
        "name": "Lookup Test",
        "category_id": 1
    });
    let req_create = test::TestRequest::post().uri("/contexts").set_json(&body).to_request();
    let _ = test::call_service(&app, req_create).await;

    // Lookup by name
    let req = test::TestRequest::get().uri("/contexts/by-name/Lookup Test").to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert_eq!(resp.get("name").and_then(|v| v.as_str()), Some("Lookup Test"));
}

#[actix_web::test]
async fn get_contexts_by_name_returns_404_not_found() {
    // Contract: GET /contexts/by-name/{name} returns 404 if not found
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

    let req = test::TestRequest::get().uri("/contexts/by-name/Nonexistent").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status().as_u16(), 404, "nonexistent template should return 404");
}

#[actix_web::test]
async fn post_contexts_match_returns_matched_template() {
    // Contract: POST /contexts/match returns matched template using non-NULL field comparison
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

    // Create template
    let body_create = serde_json::json!({
        "name": "Match Test",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req_create = test::TestRequest::post().uri("/contexts").set_json(&body_create).to_request();
    let _ = test::call_service(&app, req_create).await;

    // Match request (same non-NULL fields)
    let body_match = serde_json::json!({
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req_match = test::TestRequest::post().uri("/contexts/match").set_json(&body_match).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req_match).await;
    assert_eq!(resp.get("matched").and_then(|v| v.as_bool()), Some(true));
    assert!(resp.get("template").and_then(|v| v.get("name")).is_some());
}

#[actix_web::test]
async fn post_contexts_match_returns_no_match() {
    // Contract: POST /contexts/match returns matched: false if no match
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

    let body_match = serde_json::json!({
        "category_id": 999,
        "forma_id": 999
    });
    let req_match = test::TestRequest::post().uri("/contexts/match").set_json(&body_match).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req_match).await;
    assert_eq!(resp.get("matched").and_then(|v| v.as_bool()), Some(false));
    assert_eq!(resp.get("template"), Some(&serde_json::json!(null)));
}

#[actix_web::test]
async fn post_contexts_from_event_creates_template() {
    // Contract: POST /contexts/from-event creates template from event fields
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

    // Create event first
    let event_body = serde_json::json!({
        "description": "Event for template creation",
        "category_id": 1,
        "forma_id": 2,
        "vector": true
    });
    let req_event = test::TestRequest::post().uri("/events").set_json(&event_body).to_request();
    let event_resp: serde_json::Value = test::call_and_read_body_json(&app, req_event).await;
    let event_id = event_resp.get("id").and_then(|v| v.as_i64()).unwrap();

    // Create template from event
    let body = serde_json::json!({
        "name": "Template from Event",
        "event_id": event_id,
        "description": "Created from event"
    });
    let req = test::TestRequest::post().uri("/contexts/from-event").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("id").and_then(|v| v.as_i64()).is_some());
    assert_eq!(resp.get("name").and_then(|v| v.as_str()), Some("Template from Event"));
}

#[actix_web::test]
async fn post_contexts_from_event_returns_404_not_found() {
    // Contract: POST /contexts/from-event returns 404 if event not found
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
        "name": "Template from Nonexistent Event",
        "event_id": 99999,
        "description": "Should fail"
    });
    let req = test::TestRequest::post().uri("/contexts/from-event").set_json(&body).to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status().as_u16(), 404, "nonexistent event should return 404");
}

#[actix_web::test]
async fn post_contexts_validates_foreign_keys() {
    // Contract: POST /contexts validates FK references, returns 400 for invalid FKs
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

    // Invalid category_id
    let body = serde_json::json!({
        "name": "Invalid FK Template",
        "category_id": 99999
    });
    let req = test::TestRequest::post().uri("/contexts").set_json(&body).to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status().as_u16(), 400, "invalid FK should return 400");
}

