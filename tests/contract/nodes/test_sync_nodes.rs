mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};
use serde_json::json;

#[actix_web::test]
async fn post_nodes_sync_merges_payloads() {
    let seed = [insert_node_sql(
        "http://local-node:8080",
        "LAN",
        true,
        120,
        1_000,
        "local_broadcast",
        "local-node",
    )];
    let conn = setup_nodes_db(&seed);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let remote_nodes = vec![json!({
        "address": "http://remote-node:8080",
        "type": "GLOBAL",
        "reachable": true,
        "last_seen": 1_700_000_100,
        "ttl": 3600,
        "source": "global_registry",
        "node_id": "remote-node"
    })];

    let req = test::TestRequest::post()
        .uri("/nodes/sync")
        .set_json(&json!({ "nodes": remote_nodes }))
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::OK,
        "sync endpoint should return 200"
    );
    let body = test::read_body(resp).await;
    let json: serde_json::Value = serde_json::from_slice(&body).expect("json response");

    assert!(
        json.get("merged").and_then(|v| v.as_array()).is_some(),
        "response must include merged array"
    );
    assert!(
        json.get("local_added").and_then(|v| v.as_u64()).is_some(),
        "response must report local_added"
    );
    assert!(
        json.get("local_updated").and_then(|v| v.as_u64()).is_some(),
        "response must report local_updated"
    );
}
