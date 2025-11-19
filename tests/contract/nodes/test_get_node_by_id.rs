mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};

#[actix_web::test]
async fn get_node_by_id_returns_record() {
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

    let req = test::TestRequest::get().uri("/nodes/1").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::OK);
    let json: serde_json::Value =
        serde_json::from_slice(&test::read_body(resp).await).expect("json");
    assert_eq!(
        json.get("address").and_then(|v| v.as_str()),
        Some("http://lan-1")
    );
}

#[actix_web::test]
async fn get_node_by_id_returns_404_for_missing_node() {
    let conn = setup_nodes_db(&[]);
    let app = test::init_service(
        App::new()
            .app_data(actix_web::web::Data::new(conn.clone()))
            .configure(crate::api::routes),
    )
    .await;

    let req = test::TestRequest::get().uri("/nodes/999").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::NOT_FOUND);
}
