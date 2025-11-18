mod common;

use actix_web::{http::StatusCode, test, App};
use common::setup_nodes_db;
use serde_json::json;

#[actix_web::test]
async fn post_nodes_creates_node_with_valid_payload() {
    let conn = setup_nodes_db(&[]);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "address": "http://new-node:8080",
        "type": "LAN",
        "reachable": true,
        "last_seen": 1_700_000_000,
        "ttl": 180,
        "source": "local_broadcast",
        "node_id": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    });
    let req = test::TestRequest::post()
        .uri("/nodes")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::CREATED,
        "POST /nodes should return 201"
    );
    let json: serde_json::Value =
        serde_json::from_slice(&test::read_body(resp).await).expect("json response");
    assert!(
        json.get("id").and_then(|v| v.as_i64()).is_some(),
        "response should include id"
    );
}

#[actix_web::test]
async fn post_nodes_rejects_ttl_below_minimum() {
    let conn = setup_nodes_db(&[]);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "address": "http://too-short-ttl:8080",
        "type": "GLOBAL",
        "reachable": true,
        "last_seen": 1_700_000_100,
        "ttl": 30, // must be >= 3600 for global per FR-012
        "source": "global_registry",
        "node_id": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    });
    let req = test::TestRequest::post()
        .uri("/nodes")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::BAD_REQUEST,
        "TTL smaller than minimum must be rejected"
    );
}

#[actix_web::test]
async fn post_nodes_conflict_when_address_exists() {
    let seed = [common::insert_node_sql(
        "http://dup-node:8080",
        "LAN",
        true,
        120,
        1_000,
        "local_broadcast",
        "dup-node",
    )];
    let conn = setup_nodes_db(&seed);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let body = json!({
        "address": "http://dup-node:8080",
        "type": "LAN",
        "reachable": true,
        "last_seen": 1_700_000_100,
        "ttl": 180,
        "source": "local_broadcast",
        "node_id": "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
    });
    let req = test::TestRequest::post()
        .uri("/nodes")
        .set_json(&body)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::CONFLICT,
        "Duplicate address must return 409 conflict"
    );
}
