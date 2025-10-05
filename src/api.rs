use actix_web::{get, post, web, HttpRequest, HttpResponse, Responder};
use serde::Deserialize;
use std::sync::Arc;
use tokio::sync::Mutex;

use core_lib::models::{Impact, NewTruthEvent};
use core_lib::storage;
use crate::p2p::encryption::CryptoIdentity;
use chrono::Utc;
use std::fmt;

type DbPool = Arc<Mutex<rusqlite::Connection>>;

/// Проверяет подпись сообщения, полученную от другого узла
#[derive(Debug)]
pub enum VerifyError {
    PublicKeyHex(hex::FromHexError),
    PublicKeyLength(usize),
    PublicKeyParse(String),
    SignatureHex(hex::FromHexError),
    SignatureParse(String),
    VerificationFailed(String),
}

impl fmt::Display for VerifyError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            VerifyError::PublicKeyHex(e) => write!(f, "public key hex decode error: {}", e),
            VerifyError::PublicKeyLength(len) => write!(f, "public key has invalid length: {}", len),
            VerifyError::PublicKeyParse(s) => write!(f, "public key parse error: {}", s),
            VerifyError::SignatureHex(e) => write!(f, "signature hex decode error: {}", e),
            VerifyError::SignatureParse(s) => write!(f, "signature parse error: {}", s),
            VerifyError::VerificationFailed(s) => write!(f, "verification failed: {}", s),
        }
    }
}

impl std::error::Error for VerifyError {}

/// Проверяет подпись и возвращает Ok(()) при успехе, или Err(VerifyError) при любой ошибке.
pub fn verify_signature(
    public_key_hex: &str,
    signature_hex: &str,
    message: &str,
) -> Result<(), VerifyError> {
    // Создаем CryptoIdentity из публичного ключа
    let identity = CryptoIdentity::from_public_key_hex(public_key_hex)?;
    
    // Проверяем подпись используя метод CryptoIdentity
    identity.verify_from_hex(message.as_bytes(), signature_hex)
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
    let public_key = req
        .headers()
        .get("X-Public-Key")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");

    let signature = req
        .headers()
        .get("X-Signature")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");

    // Важное замечание: message должен быть **тем же самым** строковым payload,
    // который подписывает отправитель. Надёжнее — чтобы отправитель включал
    // отдельный заголовок X-Timestamp или X-Nonce и подписывал canonical payload.
    let message = format!("sync_request:{}", Utc::now().timestamp());

    // Используем Result: если ошибка — вернём 401 + причину (или generic)
    match verify_signature(public_key, signature, &message) {
        Ok(()) => {
            // подпись валидна — возвращаем события из БД
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
        Err(err) => {
            // Можно не отдавать подробности в проде (логировать), но во время разработки
            // удобно видеть причину.
            log::warn!("Signature verification failed: {}", err);
            HttpResponse::Unauthorized().body(format!("Invalid signature: {}", err))
        }
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::p2p::encryption::CryptoIdentity;
    use hex;

    #[test]
    fn test_signature_verification_refactor() {
        // Создаем новую криптографическую идентичность
        let identity = CryptoIdentity::new();
        
        // Создаем сообщение для подписи
        let message = "test message";
        
        // Подписываем сообщение
        let signature = identity.sign(message.as_bytes());
        let signature_hex = hex::encode(signature.to_bytes());
        let public_key_hex = identity.public_key_hex();
        
        // Проверяем подпись используя рефакторенную функцию
        let result = verify_signature(&public_key_hex, &signature_hex, message);
        
        // Проверяем, что верификация прошла успешно
        assert!(result.is_ok(), "Signature verification should succeed");
        
        // Проверяем с неправильным сообщением
        let wrong_message = "wrong message";
        let wrong_result = verify_signature(&public_key_hex, &signature_hex, wrong_message);
        
        // Проверяем, что верификация с неправильным сообщением не прошла
        assert!(wrong_result.is_err(), "Signature verification with wrong message should fail");
    }
}
