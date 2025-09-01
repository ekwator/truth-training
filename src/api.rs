use actix_web::{get, post, web, HttpResponse, Responder};
use serde::Deserialize;
use crate::db::{get_all, insert_new_statement, upsert_statement};
use crate::models::Statement;
use rusqlite::Connection;

#[get("/health")]
pub async fn health() -> impl Responder {
    HttpResponse::Ok().body("OK")
}

#[post("/init")]
pub async fn init() -> impl Responder {
    // TODO: Реализация команды init
    HttpResponse::Ok().json("DB initialized")
}

#[post("/seed")]
pub async fn seed() -> impl Responder {
    // TODO: seed --locale
    HttpResponse::Ok().json("Database seeded")
}

#[post("/add-event")]
pub async fn add_event() -> impl Responder {
    HttpResponse::Ok().json("Event added")
}

#[post("/detect")]
pub async fn detect() -> impl Responder {
    HttpResponse::Ok().json("Detection updated")
}

#[post("/impact")]
pub async fn impact() -> impl Responder {
    HttpResponse::Ok().json("Impact added")
}

#[post("/recalc")]
pub async fn recalc() -> impl Responder {
    HttpResponse::Ok().json("Metrics recalculated")
}

#[get("/data")]
pub async fn get_data(conn: web::Data<Connection>) -> impl Responder {
    match get_all(&conn) {
        Ok(v) => HttpResponse::Ok().json(v),
        Err(e) => HttpResponse::InternalServerError().body(format!("db error: {e}")),
    }
}

#[derive(Deserialize)]
pub struct NewStatementReq {
    pub text: String,
    pub context: Option<String>,
    pub truth_score: Option<f32>,
}

#[post("/add")]
pub async fn add_statement(
    conn: web::Data<Connection>,
    body: web::Json<NewStatementReq>,
) -> impl Responder {
    match insert_new_statement(&conn, &body.text, body.context.clone(), body.truth_score) {
        Ok(s) => HttpResponse::Ok().json(s),
        Err(e) => HttpResponse::InternalServerError().body(format!("db error: {e}")),
    }
}

#[derive(Deserialize)]
pub struct SyncPayload {
    pub statements: Vec<Statement>,
}

#[post("/sync")]
pub async fn sync_data(
    conn: web::Data<Connection>,
    body: web::Json<SyncPayload>,
) -> impl Responder {
    for s in &body.statements {
        if let Err(e) = upsert_statement(&conn, s) {
            return HttpResponse::InternalServerError().body(format!("db error: {e}"));
        }
    }
    HttpResponse::Ok().json(serde_json::json!({ "status": "ok", "received": body.statements.len() }))
}