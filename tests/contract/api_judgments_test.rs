
// contract test: POST /api/v1/judgments requires Bearer and endpoint exists
use actix_web::{test, App};

#[actix_web::test]
async fn post_judgments_requires_bearer() {
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

    let payload = serde_json::json!({
        "event_id": "00000000-0000-0000-0000-000000000000",
        "assessment": "confirm",
        "confidence_level": 0.5,
        "reasoning": null,
        "signature": "",
        "public_key": ""
    });
    let req = test::TestRequest::post().uri("/api/v1/judgments").set_json(&payload).to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), actix_web::http::StatusCode::UNAUTHORIZED);
}


