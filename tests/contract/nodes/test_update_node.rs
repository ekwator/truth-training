mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};
use serde_json::json;

#[actix_web::test]
async fn put_nodes_updates_reachability_and_ttl() {
    let seed = [insert_node_sql(
        "http://lan-1",
        "LAN",
        true,
        120,
        1_000,
        "local_broadcast",
        "lan-1",
    )];
    let conn = setup_nodes_db(&seed);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "reachable": false,
        "ttl": 200
    });
    let req = test::TestRequest::put()
        .uri("/nodes/1")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
    let json: serde_json::Value =
        serde_json::from_slice(&test::read_body(resp).await).expect("json");
    assert_eq!(json.get("ttl").and_then(|v| v.as_i64()), Some(200));
    assert_eq!(json.get("reachable").and_then(|v| v.as_bool()), Some(false));
}

#[actix_web::test]
async fn put_nodes_rejects_ttl_below_minimum() {
    let seed = [insert_node_sql(
        "http://global-1",
        "GLOBAL",
        true,
        3600,
        1_000,
        "global_registry",
        "global-1",
    )];
    let conn = setup_nodes_db(&seed);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "ttl": 100 // must be >= 3600 for global
    });
    let req = test::TestRequest::put()
        .uri("/nodes/1")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::BAD_REQUEST);
}
