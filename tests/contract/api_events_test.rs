
// contract test: POST /events exists and accepts minimal payload
use actix_web::{test, App};

#[actix_web::test]
async fn post_events_accepts_minimal_payload() {
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

    let body = serde_json::json!({
        "description": "contract: minimal event",
        "context_id": 1,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(resp.get("id").and_then(|v| v.as_i64()).is_some(), "response must contain id");
}


