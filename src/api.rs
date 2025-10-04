use actix_web::{get, post, web, HttpRequest, HttpResponse, Responder};
use serde::Deserialize;
use std::sync::Arc;
use tokio::sync::Mutex;

use core_lib::models::{Impact, NewTruthEvent};
use core_lib::storage;
use ed25519_dalek::{VerifyingKey, Signature, Verifier};
use hex;
use chrono::Utc;

type DbPool = Arc<Mutex<rusqlite::Connection>>;

/// Проверяет подпись сообщения, полученную от другого узла
pub fn verify_signature(
    public_key_hex: &str,
    signature_hex: &str,
    message: &str,
) -> bool {
    // Декодируем публичный ключ
    let public_key_bytes = match hex::decode(public_key_hex) {
        Ok(b) => b,
        Err(_) => return false,
    };

    // Проверяем длину ключа
    let public_key_array: [u8; 32] = match public_key_bytes.as_slice().try_into() {
        Ok(arr) => arr,
        Err(_) => return false,
    };

    let verifying_key = match VerifyingKey::from_bytes(&public_key_array) {
        Ok(k) => k,
        Err(_) => return false,
    };

    // Декодируем подпись
    let signature_bytes = match hex::decode(signature_hex) {
        Ok(b) => b,
        Err(_) => return false,
    };

    let signature = match Signature::try_from(signature_bytes.as_slice()) {
        Ok(sig) => sig,
        Err(_) => return false,
    };

    // Проверяем подпись
    verifying_key.verify(message.as_bytes(), &signature).is_ok()
}

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
        let _conn = pool.blocking_lock();
        // TODO: implement get_all_statements in core_lib
        Ok::<Vec<String>, rusqlite::Error>(vec![])
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
    _text: String,
    _context: Option<String>,
    _truth_score: Option<f32>,
}

/// POST /statements
#[post("/statements")]
async fn add_statement(
    pool: web::Data<DbPool>,
    payload: web::Json<NewStatement>,
) -> impl Responder {
    let pool = pool.clone();
    let _body = payload.into_inner();

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        // TODO: implement insert_new_statement in core_lib
        Ok::<String, rusqlite::Error>("TODO".to_string())
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
async fn get_events(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    // Читаем заголовки подписи
    let public_key = req
        .headers()
        .get("X-Public-Key")
        .map(|v| v.to_str().unwrap_or(""))
        .unwrap_or("");
    let signature = req
        .headers()
        .get("X-Signature")
        .map(|v| v.to_str().unwrap_or(""))
        .unwrap_or("");

    // Формируем сообщение для проверки подписи
    let message = format!("sync_request:{}", Utc::now().timestamp());

    // Проверяем подпись
    if !verify_signature(public_key, signature, &message) {
        return HttpResponse::Unauthorized().body("Invalid signature");
    }

    // Если подпись верна — читаем события из БД
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        storage::load_truth_events(&_conn)
    })
    .await;

    match result {
        Ok(Ok(list)) => HttpResponse::Ok().json(list),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[derive(Deserialize)]
struct AddEventRequest {
    description: String,
    context_id: i64,
    vector: bool,
}

/// POST /events
#[post("/events")]
async fn add_event(pool: web::Data<DbPool>, payload: web::Json<AddEventRequest>) -> impl Responder {
    let pool = pool.clone();
    let req = payload.into_inner();

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let new_event = NewTruthEvent {
            description: req.description,
            context_id: req.context_id,
            vector: req.vector,
            timestamp_start: chrono::Utc::now().timestamp(),
            code: 1, // Default code for new events
        };
        storage::add_truth_event(&_conn, new_event)
    })
    .await;

    match result {
        Ok(Ok(id)) => HttpResponse::Ok().json(serde_json::json!({"id": id})),
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
        let _conn = pool.blocking_lock();
        storage::add_impact(&_conn, im_copy.event_id.parse().unwrap_or(0), im_copy.type_id, im_copy.value, im_copy.notes)
    })
    .await;

    match result {
        Ok(Ok(id)) => HttpResponse::Ok().json(serde_json::json!({"id": id})),
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
