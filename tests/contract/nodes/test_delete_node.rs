mod common;

use actix_web::{http::StatusCode, test, App};
use common::{insert_node_sql, setup_nodes_db};

#[actix_web::test]
async fn delete_nodes_removes_record() {
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

    let req = test::TestRequest::delete().uri("/nodes/1").to_request();
    let resp = test::call_service(&app, req).await;
    assert_eq!(resp.status(), StatusCode::NO_CONTENT);

    let req_get = test::TestRequest::get().uri("/nodes/1").to_request();
    let resp_get = test::call_service(&app, req_get).await;
    assert_eq!(
        resp_get.status(),
        StatusCode::NOT_FOUND,
        "deleted node must not be retrievable"
    );
}
