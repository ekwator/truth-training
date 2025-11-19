mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};

#[actix_web::test]
async fn get_nodes_health_returns_health_snapshot() {
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

    let req = test::TestRequest::get().uri("/nodes/health").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
    let body = test::read_body(resp).await;
    let json: serde_json::Value = serde_json::from_slice(&body).expect("json");
    let nodes = json
        .get("nodes")
        .and_then(|v| v.as_array())
        .expect("nodes array");
    assert!(
        !nodes.is_empty(),
        "health endpoint should report at least one node"
    );
    assert!(
        json.get("checked_at").and_then(|v| v.as_i64()).is_some(),
        "checked_at timestamp is required"
    );
}

#[actix_web::test]
async fn get_nodes_health_supports_single_node_filter() {
    let seed = [
        insert_node_sql(
            "http://lan-1",
            "LAN",
            true,
            120,
            1_000,
            "local_broadcast",
            "lan-1",
        ),
        insert_node_sql(
            "http://lan-2",
            "LAN",
            true,
            120,
            2_000,
            "local_broadcast",
            "lan-2",
        ),
    ];
    let conn = setup_nodes_db(&seed);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let req = test::TestRequest::get()
        .uri("/nodes/health?node_id=2")
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
    let json: serde_json::Value =
        serde_json::from_slice(&test::read_body(resp).await).expect("json");
    let nodes = json.get("nodes").and_then(|v| v.as_array()).unwrap();
    assert_eq!(
        nodes.len(),
        1,
        "filtering by node_id must reduce result set"
    );
}
