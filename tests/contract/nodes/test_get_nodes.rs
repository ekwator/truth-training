mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};
use serde_json::Value;

#[actix_web::test]
async fn get_nodes_supports_type_and_reachable_filters() {
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
            "http://global-1",
            "GLOBAL",
            true,
            3600,
            2_000,
            "global_registry",
            "global-1",
        ),
        insert_node_sql(
            "http://lan-2",
            "LAN",
            false,
            120,
            3_000,
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
        .uri("/nodes?type=LAN&reachable=true&limit=5")
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(
        resp.status(),
        StatusCode::OK,
        "GET /nodes should return 200"
    );
    let body = test::read_body(resp).await;
    let json: Value = serde_json::from_slice(&body).expect("json response");
    let nodes = json
        .get("nodes")
        .and_then(|v| v.as_array())
        .expect("nodes array");
    assert_eq!(nodes.len(), 1, "only one LAN & reachable node should match");
    assert_eq!(
        json.get("total")
            .and_then(|v| v.as_u64())
            .unwrap_or_default(),
        1,
        "total must reflect filtered count"
    );
}

#[actix_web::test]
async fn get_nodes_honors_limit_parameter() {
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
        insert_node_sql(
            "http://lan-3",
            "LAN",
            true,
            120,
            3_000,
            "local_broadcast",
            "lan-3",
        ),
    ];
    let conn = setup_nodes_db(&seed);

    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let req = test::TestRequest::get().uri("/nodes?limit=2").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
    let body = test::read_body(resp).await;
    let json: Value = serde_json::from_slice(&body).expect("json response");
    let nodes = json.get("nodes").and_then(|v| v.as_array()).unwrap();
    assert_eq!(nodes.len(), 2, "limit must cap number of results");
}
