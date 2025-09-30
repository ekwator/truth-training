use actix_web::{get, post, web, HttpResponse, Responder};
use serde::Deserialize;
use std::sync::{Arc, Mutex};

use crate::db;
use crate::models::{TruthEvent, Impact};

type DbPool = Arc<Mutex<rusqlite::Connection>>;

/// Health
#[get("/health")]
async fn health() -> impl Responder {
    HttpResponse::Ok().body("OK")
}

/// GET /statements
#[get("/statements")]
async fn get_statements(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    // web::block возвращает Result<T, BlockingError>.
    // Внутри closure возвращаем rusqlite::Result<T>, поэтому итог — Result<Result<T,rusqlite::Error>, BlockingError>
    let result = web::block(move || {
        let conn = pool.lock().unwrap();
        db::get_all(&conn)
    })
    .await;

    match result {
        Ok(Ok(list)) => HttpResponse::Ok().json(list),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[derive(Deserialize)]
struct NewStatement {
    text: String,
    context: Option<String>,
    truth_score: Option<f32>,
}

/// POST /statements
#[post("/statements")]
async fn add_statement(
    pool: web::Data<DbPool>,
    payload: web::Json<NewStatement>,
) -> impl Responder {
    let pool = pool.clone();
    let body = payload.into_inner();

    let result = web::block(move || {
        let conn = pool.lock().unwrap();
        db::insert_new_statement(&conn, &body.text, body.context, body.truth_score)
    })
    .await;

    match result {
        Ok(Ok(s)) => HttpResponse::Ok().json(s),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /events
#[get("/events")]
async fn get_events(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let conn = pool.lock().unwrap();
        db::get_all_events(&conn)
    })
    .await;

    match result {
        Ok(Ok(list)) => HttpResponse::Ok().json(list),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// POST /events
#[post("/events")]
async fn add_event(pool: web::Data<DbPool>, payload: web::Json<TruthEvent>) -> impl Responder {
    let pool = pool.clone();
    // клонируем событие, чтобы можно было вернуть его клиенту после вставки
    let ev = payload.into_inner();
    let ev_copy = ev.clone();

    let result = web::block(move || {
        let conn = pool.lock().unwrap();
        db::insert_event(&conn, &ev_copy)
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(ev),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// POST /impacts
#[post("/impacts")]
async fn add_impact(pool: web::Data<DbPool>, payload: web::Json<Impact>) -> impl Responder {
    let pool = pool.clone();
    let im = payload.into_inner();
    let im_copy = im.clone();

    let result = web::block(move || {
        let conn = pool.lock().unwrap();
        db::insert_impact(&conn, &im_copy)
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(im),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// helper: зарегистрировать все маршруты
pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.service(health)
        .service(get_statements)
        .service(add_statement)
        .service(get_events)
        .service(add_event)
        .service(add_impact);
}
