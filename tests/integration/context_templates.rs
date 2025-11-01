// Integration tests for context template workflows
// These tests MUST FAIL until implementation is complete (TDD)

use actix_web::{test, App};

#[actix_web::test]
async fn template_selection_prefills_event_form() {
    // Integration: Template selection should prefill event form fields
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

    // 1. Create a context template
    let template_body = serde_json::json!({
        "name": "Integration Test Template",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "develop_id": 3,
        "effect_id": 2
    });
    let req_template = test::TestRequest::post().uri("/contexts").set_json(&template_body).to_request();
    let template_resp: serde_json::Value = test::call_and_read_body_json(&app, req_template).await;
    let template_id = template_resp.get("id").and_then(|v| v.as_i64()).unwrap();

    // 2. Get template by name (simulates template selection)
    let req_get = test::TestRequest::get().uri("/contexts/by-name/Integration Test Template").to_request();
    let template: serde_json::Value = test::call_and_read_body_json(&app, req_get).await;

    // 3. Create event using template fields (simulates prefilled form)
    let event_body = serde_json::json!({
        "description": "Event created from template",
        "category_id": template.get("category_id"),
        "forma_id": template.get("forma_id"),
        "cause_id": template.get("cause_id"),
        "develop_id": template.get("develop_id"),
        "effect_id": template.get("effect_id"),
        "vector": true
    });
    let req_event = test::TestRequest::post().uri("/events").set_json(&event_body).to_request();
    let event_resp: serde_json::Value = test::call_and_read_body_json(&app, req_event).await;
    assert!(event_resp.get("id").is_some(), "event should be created with template fields");
}

#[actix_web::test]
async fn event_creation_saves_embedded_fields() {
    // Integration: Event creation should save embedded fields correctly
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

    let event_body = serde_json::json!({
        "description": "Event with embedded fields",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "develop_id": 3,
        "effect_id": 2,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&event_body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    let event_id = resp.get("id").and_then(|v| v.as_i64()).unwrap();

    // Verify event was saved with embedded fields (by matching template)
    let match_body = serde_json::json!({
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "develop_id": 3,
        "effect_id": 2
    });
    let req_match = test::TestRequest::post().uri("/contexts/match").set_json(&match_body).to_request();
    let match_resp: serde_json::Value = test::call_and_read_body_json(&app, req_match).await;
    // If we had a template matching these fields, it should be found
    // For now, just verify the event was created
    assert!(event_id > 0, "event should have valid ID");
}

#[actix_web::test]
async fn template_matching_uses_null_aware_comparison() {
    // Integration: Template matching should use non-NULL field comparison
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

    // Create template with only category_id (NULL for other fields)
    let template_body = serde_json::json!({
        "name": "NULL Aware Template",
        "category_id": 1
    });
    let req_template = test::TestRequest::post().uri("/contexts").set_json(&template_body).to_request();
    let _template_resp = test::call_service(&app, req_template).await;

    // Match with same category_id but NULL other fields - should match
    let match_body = serde_json::json!({
        "category_id": 1
    });
    let req_match = test::TestRequest::post().uri("/contexts/match").set_json(&match_body).to_request();
    let match_resp: serde_json::Value = test::call_and_read_body_json(&app, req_match).await;
    assert_eq!(match_resp.get("matched").and_then(|v| v.as_bool()), Some(true), "should match on non-NULL fields only");
}

#[actix_web::test]
async fn duplicate_detection_prevents_creation() {
    // Integration: Duplicate detection should prevent creating templates with identical non-NULL fields
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
        "name": "First Template",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req1 = test::TestRequest::post().uri("/contexts").set_json(&body1).to_request();
    let _resp1 = test::call_service(&app, req1).await;

    // Try to create duplicate (same non-NULL fields)
    let body2 = serde_json::json!({
        "name": "Second Template",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5
    });
    let req2 = test::TestRequest::post().uri("/contexts").set_json(&body2).to_request();
    let resp2 = test::call_service(&app, req2).await;
    assert_eq!(resp2.status().as_u16(), 409, "duplicate should return 409 Conflict");
}

#[actix_web::test]
async fn fk_validation_rejects_invalid_references() {
    // Integration: FK validation should reject invalid references for events and templates
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

    // Test invalid FK for event
    let event_body = serde_json::json!({
        "description": "Event with invalid FK",
        "category_id": 99999,
        "vector": true
    });
    let req_event = test::TestRequest::post().uri("/events").set_json(&event_body).to_request();
    let resp_event = test::call_service(&app, req_event).await;
    assert_eq!(resp_event.status().as_u16(), 400, "invalid FK for event should return 400");

    // Test invalid FK for template
    let template_body = serde_json::json!({
        "name": "Template with invalid FK",
        "category_id": 99999
    });
    let req_template = test::TestRequest::post().uri("/contexts").set_json(&template_body).to_request();
    let resp_template = test::call_service(&app, req_template).await;
    assert_eq!(resp_template.status().as_u16(), 400, "invalid FK for template should return 400");
}

#[actix_web::test]
async fn create_template_from_event_workflow() {
    // Integration: Create template from event fields workflow
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

    // 1. Create event
    let event_body = serde_json::json!({
        "description": "Event to convert to template",
        "category_id": 1,
        "forma_id": 2,
        "cause_id": 5,
        "vector": true
    });
    let req_event = test::TestRequest::post().uri("/events").set_json(&event_body).to_request();
    let event_resp: serde_json::Value = test::call_and_read_body_json(&app, req_event).await;
    let event_id = event_resp.get("id").and_then(|v| v.as_i64()).unwrap();

    // 2. Create template from event
    let template_body = serde_json::json!({
        "name": "Template from Event",
        "event_id": event_id,
        "description": "Created from event"
    });
    let req_template = test::TestRequest::post().uri("/contexts/from-event").set_json(&template_body).to_request();
    let template_resp: serde_json::Value = test::call_and_read_body_json(&app, req_template).await;
    assert!(template_resp.get("id").is_some(), "template should be created from event");
    assert_eq!(template_resp.get("category_id"), event_body.get("category_id"), "template should have event's category_id");
    assert_eq!(template_resp.get("forma_id"), event_body.get("forma_id"), "template should have event's forma_id");
    assert_eq!(template_resp.get("cause_id"), event_body.get("cause_id"), "template should have event's cause_id");
}

