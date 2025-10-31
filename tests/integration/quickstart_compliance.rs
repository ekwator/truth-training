
use actix_web::{test, App};

#[actix_web::test]
async fn quickstart_confess_and_judge_flow() {
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

    // Confession: POST /events without author metadata
    let body = serde_json::json!({
        "description": "confession: sample",
        "context_id": 1,
        "vector": true
    });
    let req = test::TestRequest::post().uri("/events").set_json(&body).to_request();
    let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    let ev_id = resp.get("id").and_then(|v| v.as_i64()).expect("event id");

    // Ternary judgments: confirm/reject/abstain (simulate via impacts + abstain as no-op)
    {
        let c = conn_data.lock().await;
        let _ = core_lib::storage::add_impact(&c, ev_id, 1, true, Some("confirm".into())).unwrap();
        let _ = core_lib::storage::add_impact(&c, ev_id, 1, false, Some("reject".into())).unwrap();
        // abstain: no DB writes
    }

    // Recalc and verify event present
    let req = test::TestRequest::post().uri("/recalc").to_request();
    let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

    let req = test::TestRequest::get().uri("/get_data").to_request();
    let all: serde_json::Value = test::call_and_read_body_json(&app, req).await;
    assert!(all.get("events").unwrap().as_array().unwrap().len() >= 1);
}


