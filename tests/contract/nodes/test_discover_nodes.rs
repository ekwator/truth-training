mod common;

use actix_web::{http::StatusCode, test, App};
use common::setup_nodes_db;
use serde_json::json;

#[actix_web::test]
async fn post_nodes_discover_returns_counts() {
    let conn = setup_nodes_db(&[]);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "types": ["LAN", "GLOBAL"]
    });
    let req = test::TestRequest::post()
        .uri("/nodes/discover")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::OK,
        "POST /nodes/discover should respond with 200"
    );
    let json: serde_json::Value =
        serde_json::from_slice(&test::read_body(resp).await).expect("json response");
    for field in &["discovered", "updated", "duration_ms"] {
        assert!(
            json.get(*field).and_then(|v| v.as_u64()).is_some(),
            "response must include numeric {field}"
        );
    }
}

#[actix_web::test]
async fn post_nodes_discover_defaults_to_all_types() {
    let conn = setup_nodes_db(&[]);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let req = test::TestRequest::post()
        .uri("/nodes/discover")
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
}
