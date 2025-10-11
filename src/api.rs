use actix_web::{get, post, web, HttpRequest, HttpResponse, Responder};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::Mutex;

use core_lib::models::{Impact, NewTruthEvent, NewStatement, GraphData, GraphSummary, RbacUser};
use core_lib::storage;
use crate::p2p::encryption::CryptoIdentity;
use crate::p2p::sync::SyncData;
use crate::p2p::node::Node;
use chrono::Utc;
use std::fmt;
use jsonwebtoken::{encode, decode, Algorithm, DecodingKey, EncodingKey, Header, Validation};
use once_cell::sync::Lazy;
use utoipa::{OpenApi, ToSchema};
use crate::p2p::sync::get_relay_stats;

type DbPool = Arc<Mutex<rusqlite::Connection>>;

/// Параметры HTTP-сервера, необходимые для служебных эндпоинтов
#[derive(Clone)]
pub struct AppInfo {
    pub db_path: String,
    pub p2p_enabled: bool,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct NodeInfo {
    pub node_name: String,
    pub version: String,
    pub p2p_enabled: bool,
    pub db_path: String,
    pub peer_count: i32,
}

#[derive(Debug, Serialize, ToSchema)]
pub struct NodeStats {
    pub events: i32,
    pub statements: i32,
    pub impacts: i32,
    pub node_ratings: i32,
    pub group_ratings: i32,
    pub avg_trust_score: f32,
}

#[utoipa::path(
    get,
    path = "/api/v1/info",
    responses(
        (status = 200, description = "Информация об узле", body = NodeInfo)
    )
)]
#[get("/api/v1/info")]
async fn api_v1_info(node: web::Data<Node>, meta: web::Data<AppInfo>) -> impl Responder {
    // Имя узла формируем из публичного ключа (короткий вид)
    let pub_hex = node.crypto.public_key_hex();
    let short = pub_hex.get(0..8).unwrap_or("");
    let node_name = format!("node-{}", short);

    let version = env!("CARGO_PKG_VERSION").to_string();
    let peer_count = node.peers.len() as i32;

    let info = NodeInfo {
        node_name,
        version,
        p2p_enabled: meta.p2p_enabled,
        db_path: meta.db_path.clone(),
        peer_count,
    };
    HttpResponse::Ok().json(info)
}

#[utoipa::path(
    get,
    path = "/api/v1/stats",
    responses(
        (status = 200, description = "Агрегированная статистика по БД", body = NodeStats)
    )
)]
#[get("/api/v1/stats")]
async fn api_v1_stats(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let conn = pool.blocking_lock();
        let events = core_lib::storage::load_truth_events(&conn)?.len() as i32;
        let statements = core_lib::storage::load_statements(&conn)?.len() as i32;
        let impacts = core_lib::storage::load_impacts(&conn)?.len() as i32;
        let node_rs = core_lib::storage::load_node_ratings(&conn)?;
        let group_rs = core_lib::storage::load_group_ratings(&conn)?;
        let node_ratings = node_rs.len() as i32;
        let group_ratings = group_rs.len() as i32;
        let avg_trust_score: f32 = if node_rs.is_empty() {
            0.0
        } else {
            let sum: f64 = node_rs.iter().map(|r| r.trust_score as f64).sum();
            (sum / (node_rs.len() as f64)) as f32
        };
        Ok::<NodeStats, core_lib::models::CoreError>(NodeStats {
            events,
            statements,
            impacts,
            node_ratings,
            group_ratings,
            avg_trust_score,
        })
    })
    .await;

    match result {
        Ok(Ok(st)) => HttpResponse::Ok().json(st),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// OpenAPI спецификация для Swagger UI
#[derive(OpenApi)]
#[openapi(
    paths(api_v1_info, api_v1_stats, api_v1_users_list, api_v1_users_role, api_v1_trust_delegate, api_v1_peers_priorities),
    components(schemas(NodeInfo, NodeStats, RbacUser, Claims)),
    tags((name = "Truth API", description = "HTTP API для мобильной интеграции"))
)]
pub struct ApiDoc;

// ===================== JWT structures =====================
#[derive(Debug, Serialize, Deserialize, ToSchema)]
struct Claims {
    sub: String, // public key hex
    exp: usize,
    iat: usize,
    role: String,
    trust_score: f32,
}

static JWT_SECRET: Lazy<String> = Lazy::new(|| {
    std::env::var("JWT_SECRET").unwrap_or_else(|_| "dev-secret-change-me".to_string())
});

fn jwt_encoding_key() -> EncodingKey {
    EncodingKey::from_secret(JWT_SECRET.as_bytes())
}

fn jwt_decoding_key() -> DecodingKey {
    DecodingKey::from_secret(JWT_SECRET.as_bytes())
}

fn issue_jwt_pair_with(conn: &rusqlite::Connection, public_key_hex: &str) -> anyhow::Result<(String, String, i64)> {
    use chrono::Utc;
    let now = Utc::now().timestamp() as usize;
    let exp_access = (Utc::now() + chrono::Duration::hours(1)).timestamp() as usize;
    // lookup role and trust in users table (fallbacks)
    let (role, trust_score): (String, f32) = {
        if let Ok(mut stmt) = conn.prepare("SELECT role, trust_score FROM users WHERE pubkey=?1") {
            match stmt.query_row([public_key_hex], |r| Ok((r.get::<_, String>(0)?, r.get::<_, f64>(1)? as f32))) {
                Ok((role, ts)) => (role, ts),
                Err(_) => ("observer".to_string(), 0.0),
            }
        } else { ("observer".to_string(), 0.0) }
    };
    let claims = Claims { sub: public_key_hex.to_string(), exp: exp_access, iat: now, role, trust_score };
    let header = Header::new(Algorithm::HS256);
    let access = encode(&header, &claims, &jwt_encoding_key())?;
    // refresh token как случайная строка (32 байта hex)
    let mut rng = rand::rngs::OsRng;
    use rand::RngCore;
    let mut buf = [0u8; 32];
    rng.fill_bytes(&mut buf);
    let refresh = hex::encode(buf);
    let exp_refresh = (Utc::now() + chrono::Duration::hours(24)).timestamp();
    Ok((access, refresh, exp_refresh))
}

fn verify_jwt(token: &str) -> Result<Claims, jsonwebtoken::errors::Error> {
    let validation = Validation::new(Algorithm::HS256);
    decode::<Claims>(token, &jwt_decoding_key(), &validation).map(|d| d.claims)
}

// ===== RBAC helpers =====
fn role_level(role: &str) -> i32 {
    match role {
        "admin" => 3,
        "node" => 2,
        _ => 1,
    }
}

async fn require_role(req: HttpRequest, min_role: &str) -> Result<Claims, HttpResponse> {
    let Some(token) = extract_bearer(&req) else { return Err(unauthorized_json()); };
    let claims = match verify_jwt(&token) { Ok(c) => c, Err(_) => return Err(unauthorized_json()) };
    if role_level(&claims.role) < role_level(min_role) {
        return Err(HttpResponse::Forbidden().json(serde_json::json!({"error":"forbidden","code":403})));
    }
    Ok(claims)
}

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
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        storage::load_statements(&_conn)
    })
    .await;

    match result {
        Ok(Ok(statements)) => HttpResponse::Ok().json(statements),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[derive(Deserialize)]
struct AddStatementRequest {
    event_id: i64,
    text: String,
    context: Option<String>,
    truth_score: Option<f32>,
}

/// POST /statements
#[post("/statements")]
async fn add_statement(
    pool: web::Data<DbPool>,
    payload: web::Json<AddStatementRequest>,
) -> impl Responder {
    let pool = pool.clone();
    let req = payload.into_inner();

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let new_statement = NewStatement {
            event_id: req.event_id,
            text: req.text,
            context: req.context,
            truth_score: req.truth_score,
        };
        storage::add_statement(&_conn, new_statement)
    })
    .await;

    match result {
        Ok(Ok(id)) => HttpResponse::Ok().json(serde_json::json!({"id": id})),
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

/// POST /init - Инициализация базы данных
#[post("/init")]
async fn init_db(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        storage::init_db(&_conn)
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({"status": "initialized"})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[derive(Deserialize)]
struct SeedRequest {
    locale: Option<String>,
}

/// POST /seed - Загрузка исходных данных
#[post("/seed")]
async fn seed_db(pool: web::Data<DbPool>, payload: web::Json<SeedRequest>) -> impl Responder {
    let pool = pool.clone();
    let locale = payload.locale.clone().unwrap_or_else(|| "ru".to_string());
    let locale_for_response = locale.clone();
    
    let result = web::block(move || {
        let mut _conn = pool.blocking_lock();
        storage::seed_knowledge_base(&mut _conn, &locale)
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({"status": "seeded", "locale": locale_for_response})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// POST /detect - Анализ несоответствий (отметка события как обнаруженного)
#[post("/detect")]
async fn detect_event(pool: web::Data<DbPool>, payload: web::Json<serde_json::Value>) -> impl Responder {
    let pool = pool.clone();
    let event_id = payload.get("event_id")
        .and_then(|v| v.as_i64())
        .unwrap_or(0);
    let detected = payload.get("detected")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);
    let corrected = payload.get("corrected")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);
    
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        storage::set_event_detected(&_conn, event_id, detected, None, corrected)
            .and_then(|_| core_lib::storage::recalc_ratings(&_conn, chrono::Utc::now().timestamp()))
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({"status": "detected", "event_id": event_id})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// POST /recalc - Пересчет связей (метрики прогресса)
#[post("/recalc")]
async fn recalc_metrics(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let ts = chrono::Utc::now().timestamp();
        let metric_id = storage::recalc_progress_metrics(&_conn, ts)?;
        core_lib::storage::recalc_ratings(&_conn, ts)?;
        Ok::<i64, core_lib::models::CoreError>(metric_id)
    })
    .await;

    match result {
        Ok(Ok(metric_id)) => HttpResponse::Ok().json(serde_json::json!({"status": "recalculated", "metric_id": metric_id})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /get_data - Получение всех данных
#[get("/get_data")]
async fn get_all_data(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let events = storage::load_truth_events(&_conn)?;
        let impacts = storage::load_impacts(&_conn)?;
        let metrics = storage::load_metrics(&_conn)?;
        Ok::<(Vec<core_lib::models::TruthEvent>, Vec<core_lib::models::Impact>, Vec<core_lib::models::ProgressMetrics>), core_lib::models::CoreError>((events, impacts, metrics))
    })
    .await;

    match result {
        Ok(Ok((events, impacts, metrics))) => {
            HttpResponse::Ok().json(serde_json::json!({
                "events": events,
                "impacts": impacts,
                "metrics": metrics
            }))
        },
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /progress - Получение метрик прогресса
#[get("/progress")]
async fn get_progress(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        storage::load_metrics(&_conn)
    })
    .await;

    match result {
        Ok(Ok(metrics)) => HttpResponse::Ok().json(metrics),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// POST /sync - Двунаправленная синхронизация данных
#[post("/sync")]
async fn sync_data(req: HttpRequest, pool: web::Data<DbPool>, payload: web::Json<SyncData>) -> impl Responder {
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

    // Require caller to provide timestamp header used for signature
    let ts_hdr = req.headers().get("X-Timestamp").and_then(|v| v.to_str().ok());
    let ratings_hash_hdr = req.headers().get("X-Ratings-Hash").and_then(|v| v.to_str().ok());
    let message = match ts_hdr {
        Some(ts) => match ratings_hash_hdr {
            Some(h) if !h.is_empty() => format!("sync_push:{}:{}", ts, h),
            _ => format!("sync_push:{}", ts),
        },
        None => return HttpResponse::BadRequest().body("Missing X-Timestamp"),
    };

    match verify_signature(public_key, signature, &message) {
        Ok(()) => {
            let pool = pool.clone();
            let received_data = payload.into_inner();
            
            let result = web::block(move || {
                let _conn = pool.blocking_lock();
                // Reconcile into local DB and log
                crate::p2p::sync::reconcile(&_conn, &received_data)
                    .map_err(|e| core_lib::models::CoreError::InvalidArg(e.to_string()))
            })
            .await;

            match result {
                Ok(Ok(sync_result)) => HttpResponse::Ok().json(sync_result),
                Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
                Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
            }
        }
        Err(err) => {
            log::warn!("Sync signature verification failed: {}", err);
            HttpResponse::Unauthorized().body(format!("Invalid signature: {}", err))
        }
    }
}

/// POST /incremental_sync - Инкрементальная синхронизация
#[post("/incremental_sync")]
async fn incremental_sync(req: HttpRequest, pool: web::Data<DbPool>, payload: web::Json<SyncData>) -> impl Responder {
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

    let ts_hdr = req.headers().get("X-Timestamp").and_then(|v| v.to_str().ok());
    let ratings_hash_hdr = req.headers().get("X-Ratings-Hash").and_then(|v| v.to_str().ok());
    let message = match ts_hdr {
        Some(ts) => match ratings_hash_hdr {
            Some(h) if !h.is_empty() => format!("incremental_sync:{}:{}", ts, h),
            _ => format!("incremental_sync:{}", ts),
        },
        None => return HttpResponse::BadRequest().body("Missing X-Timestamp"),
    };

    match verify_signature(public_key, signature, &message) {
        Ok(()) => {
            let pool = pool.clone();
            let received_data = payload.into_inner();
            
            let result = web::block(move || {
                let _conn = pool.blocking_lock();
                crate::p2p::sync::reconcile(&_conn, &received_data)
                    .map_err(|e| core_lib::models::CoreError::InvalidArg(e.to_string()))
            })
            .await;

            match result {
                Ok(Ok(sync_result)) => HttpResponse::Ok().json(sync_result),
                Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
                Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
            }
        }
        Err(err) => {
            log::warn!("Incremental sync signature verification failed: {}", err);
            HttpResponse::Unauthorized().body(format!("Invalid signature: {}", err))
        }
    }
}

/// POST /ratings/sync — инициирует широковещательную отправку локальных рейтингов на пиров
#[post("/ratings/sync")]
async fn ratings_sync(req: HttpRequest, node: web::Data<Node>) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    match node.broadcast_ratings().await {
        Ok(()) => HttpResponse::Ok().json(serde_json::json!({"status":"broadcasted"})),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// Secured variants under /api/v1/* requiring Bearer
#[post("/api/v1/recalc")]
async fn api_v1_recalc(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let ts = chrono::Utc::now().timestamp();
        let metric_id = storage::recalc_progress_metrics(&_conn, ts)?;
        core_lib::storage::recalc_ratings(&_conn, ts)?;
        Ok::<i64, core_lib::models::CoreError>(metric_id)
    }).await;
    match result {
        Ok(Ok(_)) => HttpResponse::Ok().json(serde_json::json!({"status":"recalculated"})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[post("/api/v1/ratings/sync")]
async fn api_v1_ratings_sync(req: HttpRequest, node: web::Data<Node>) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    match node.broadcast_ratings().await {
        Ok(()) => HttpResponse::Ok().json(serde_json::json!({"status":"broadcasted"})),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[post("/api/v1/reset")]
async fn api_v1_reset(req: HttpRequest) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    HttpResponse::Ok().json(serde_json::json!({"status":"ok"}))
}

#[post("/api/v1/reinit")]
async fn api_v1_reinit(req: HttpRequest) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    HttpResponse::Ok().json(serde_json::json!({"status":"ok"}))
}

/// helper: зарегистрировать все маршруты
pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.service(health)
        .service(api_v1_info)
        .service(api_v1_stats)
        .service(api_v1_auth)
        .service(api_v1_refresh)
        .service(api_v1_recalc)
        .service(api_v1_ratings_sync)
        .service(api_v1_reset)
        .service(api_v1_reinit)
        .service(init_db)
        .service(seed_db)
        .service(detect_event)
        .service(recalc_metrics)
        .service(recalc_ratings)
        .service(get_all_data)
        .service(get_progress)
        .service(api_v1_peers_priorities)
        .service(get_node_ratings)
        .service(get_group_ratings)
        .service(get_graph)
        .service(get_graph_json)
        .service(get_graph_summary)
        .service(get_statements)
        .service(add_statement)
        .service(get_events)
        .service(add_event)
        .service(add_impact)
        .service(sync_data)
        .service(incremental_sync)
        .service(ratings_sync)
        .service(api_v1_users_list)
        .service(api_v1_users_role)
        .service(api_v1_trust_delegate);
}

#[derive(Serialize, ToSchema)]
pub struct PeerPriorityItem {
    pub peer_url: String,
    pub trust_score: f32,
    pub propagation_priority: f32,
    pub relay_rate: f32,
}

/// GET /api/v1/peers/priorities — список пиров с приоритетами распространения и метриками ретрансляции
#[utoipa::path(
    get,
    path = "/api/v1/peers/priorities",
    responses((status=200, description="Приоритеты ретрансляции по пирам", body = [PeerPriorityItem]))
)]
#[get("/api/v1/peers/priorities")]
async fn api_v1_peers_priorities(node: web::Data<Node>, pool: web::Data<DbPool>) -> impl Responder {
    let peers = node.peers.clone();
    let priorities = web::block(move || {
        let conn = pool.blocking_lock();
        let mut items: Vec<PeerPriorityItem> = Vec::new();
        let ratings = core_lib::storage::load_node_ratings(&conn).unwrap_or_default();
        let map: std::collections::HashMap<String, core_lib::models::NodeRating> = ratings.into_iter().map(|r| (r.node_id.clone(), r)).collect();
        for url in peers.iter() {
            // public_key hex в URL не хранится — в MVP ищем по совпадению начала ключа в URL или пропускаем
            // Для корректности ожидается, что Node.peers содержит URLы пиров, а соответствие ключ→URL хранится в приложении CLI/конфиге.
            // Здесь заполним trust_priority как среднее по сети, если ключ не найден.
            let (trust, prio) = if let Some((_id, r)) = map.iter().next() { (r.trust_score, r.propagation_priority) } else { (0.0f32, 0.5f32) };
            items.push(PeerPriorityItem { peer_url: url.clone(), trust_score: trust, propagation_priority: prio, relay_rate: 0.0 });
        }
        Ok::<Vec<PeerPriorityItem>, core_lib::models::CoreError>(items)
    }).await;

    let mut items = match priorities { Ok(Ok(v)) => v, _ => Vec::new() };
    // Вливаем метрики ретрансляции
    let stats = get_relay_stats().await;
    for it in &mut items {
        if let Some(s) = stats.iter().find(|s| s.peer_url == it.peer_url) {
            it.relay_rate = s.relay_rate;
        }
    }
    HttpResponse::Ok().json(items)
}

// ===================== Auth endpoints =====================

#[post("/api/v1/auth")]
async fn api_v1_auth(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    let public_key = req.headers().get("X-Public-Key").and_then(|v| v.to_str().ok());
    let signature = req.headers().get("X-Signature").and_then(|v| v.to_str().ok());
    let ts = req.headers().get("X-Timestamp").and_then(|v| v.to_str().ok());
    let Some(pk) = public_key else { return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401})); };
    let Some(sig) = signature else { return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401})); };
    let Some(ts) = ts else { return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401})); };
    let message = format!("auth:{}", ts);
    if let Err(e) = verify_signature(pk, sig, &message) {
        log::warn!("Auth signature failed: {}", e);
        return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401}));
    }
    // ok -> issue jwt and refresh, store refresh
    let (access, refresh, exp_refresh) = match web::block({
        let pool = pool.clone();
        let pk_owned = pk.to_string();
        move || {
            let conn = pool.blocking_lock();
            issue_jwt_pair_with(&conn, &pk_owned)
        }
    }).await {
        Ok(Ok(v)) => v,
        Ok(Err(e)) => return HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => return HttpResponse::InternalServerError().body(e.to_string()),
    };
    let pool = pool.clone();
    let pk_owned = pk.to_string();
    let refresh_owned = refresh.clone();
    let res = web::block(move || {
        let conn = pool.blocking_lock();
        core_lib::storage::register_refresh_token(&conn, &pk_owned, &refresh_owned, exp_refresh)
    }).await;
    match res {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({
            "access_token": access,
            "refresh_token": refresh,
            "token_type": "Bearer",
            "expires_in": 3600
        })),
        _ => HttpResponse::InternalServerError().finish(),
    }
}

// ===================== RBAC / Users endpoints =====================

#[utoipa::path(
    get,
    path = "/api/v1/users",
    responses((status=200, description="Список пользователей", body = [RbacUser]))
)]
#[get("/api/v1/users")]
async fn api_v1_users_list(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    if let Err(resp) = require_role(req, "admin").await.map(|_| ()) { return resp; }
    let pool = pool.clone();
    let result = web::block(move || {
        let conn = pool.blocking_lock();
        core_lib::storage::list_users(&conn)
    }).await;
    match result {
        Ok(Ok(users)) => HttpResponse::Ok().json(users),
        _ => HttpResponse::InternalServerError().finish(),
    }
}

#[derive(Deserialize)]
struct RoleRequest { pubkey: String, role: String }

#[utoipa::path(
    post,
    path = "/api/v1/users/role",
    responses((status=200, description="Роль обновлена"))
)]
#[post("/api/v1/users/role")]
async fn api_v1_users_role(req: HttpRequest, pool: web::Data<DbPool>, body: web::Json<RoleRequest>) -> impl Responder {
    if let Err(resp) = require_role(req, "admin").await.map(|_| ()) { return resp; }
    let RoleRequest { pubkey, role } = body.into_inner();
    let poolc = pool.clone();
    let res = web::block(move || {
        let conn = poolc.blocking_lock();
        core_lib::storage::update_user_role(&conn, &pubkey, &role)
    }).await;
    match res { Ok(Ok(())) => HttpResponse::Ok().finish(), _ => HttpResponse::InternalServerError().finish() }
}

#[derive(Deserialize)]
struct TrustDelegateRequest { target_pubkey: String, delta: f32 }

#[utoipa::path(
    post,
    path = "/api/v1/trust/delegate",
    responses((status=200, description="Делегирование доверия применено"))
)]
#[post("/api/v1/trust/delegate")]
async fn api_v1_trust_delegate(req: HttpRequest, pool: web::Data<DbPool>, body: web::Json<TrustDelegateRequest>) -> impl Responder {
    // Требуется роль не ниже node
    let claims = match require_role(req, "node").await { Ok(c) => c, Err(resp) => return resp };
    let TrustDelegateRequest { target_pubkey, delta } = body.into_inner();
    // ограничиваем делегирование по FidoNet-like: разрешено только положительное малое смещение и не на себя
    if target_pubkey == claims.sub || delta.abs() > 0.2 { // лимит шага делегирования
        return HttpResponse::BadRequest().body("invalid delegation request");
    }
    let poolc = pool.clone();
    let res = web::block(move || {
        let conn = poolc.blocking_lock();
        // применяем дельту
        let _new = core_lib::storage::adjust_trust_score(&conn, &target_pubkey, delta)?;
        // синхронизация с node_ratings произойдет при следующем пересчете; можно инициировать мягкий апдейт users уже сейчас
        Ok::<(), core_lib::models::CoreError>(())
    }).await;
    match res { Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({"status":"ok"})), _ => HttpResponse::InternalServerError().finish() }
}

#[derive(Deserialize)]
struct RefreshRequest { refresh_token: String }

#[post("/api/v1/refresh")]
async fn api_v1_refresh(pool: web::Data<DbPool>, body: web::Json<RefreshRequest>) -> impl Responder {
    let refresh = body.refresh_token.clone();
    let refresh_lookup = refresh.clone();
    let pool1 = pool.clone();
    let res = web::block(move || {
        let conn = pool1.blocking_lock();
        core_lib::storage::find_session_by_refresh(&conn, &refresh_lookup)
    }).await;
    let Ok(Ok(opt)) = res else { return HttpResponse::InternalServerError().finish(); };
    let Some((public_key, expires_at)) = opt else {
        return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401}));
    };
    if chrono::Utc::now().timestamp() >= expires_at {
        // expired
        let pool_del = pool.clone();
        let _ = web::block(move || {
            let conn = pool_del.blocking_lock();
            core_lib::storage::delete_refresh_token(&conn, &refresh)
        }).await;
        return HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401}));
    }
    // rotate refresh: delete old, issue new pair (include role/trust)
    let (access, new_refresh, exp_refresh) = match web::block({
        let pool = pool.clone();
        let public_key = public_key.clone();
        move || {
            let conn = pool.blocking_lock();
            issue_jwt_pair_with(&conn, &public_key)
        }
    }).await {
        Ok(Ok(v)) => v,
        _ => return HttpResponse::InternalServerError().finish(),
    };
    let new_refresh_for_closure = new_refresh.clone();
    let pool2 = pool.clone();
    let refresh_old = refresh.clone();
    let public_key2 = public_key.clone();
    let res2 = web::block(move || {
        let conn = pool2.blocking_lock();
        core_lib::storage::delete_refresh_token(&conn, &refresh_old)?;
        core_lib::storage::register_refresh_token(&conn, &public_key2, &new_refresh_for_closure, exp_refresh)
    }).await;
    match res2 {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({
            "access_token": access,
            "refresh_token": new_refresh,
            "token_type": "Bearer",
            "expires_in": 3600
        })),
        _ => HttpResponse::InternalServerError().finish(),
    }
}

// ===================== Middleware for Bearer auth =====================

fn unauthorized_json() -> HttpResponse {
    HttpResponse::Unauthorized().json(serde_json::json!({"error":"unauthorized","code":401}))
}

fn extract_bearer(req: &HttpRequest) -> Option<String> {
    req.headers()
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.strip_prefix("Bearer "))
        .map(|s| s.to_string())
}

async fn require_jwt(req: HttpRequest) -> Result<String, HttpResponse> {
    let Some(token) = extract_bearer(&req) else { return Err(unauthorized_json()); };
    match verify_jwt(&token) {
        Ok(c) => Ok(c.sub),
        Err(_) => Err(unauthorized_json()),
    }
}


/// POST /recalc_ratings - Пересчет рейтингов узлов и групп
#[post("/recalc_ratings")]
async fn recalc_ratings(req: HttpRequest, pool: web::Data<DbPool>) -> impl Responder {
    if let Err(resp) = require_jwt(req).await.map(|_| ()) { return resp; }
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        core_lib::storage::recalc_ratings(&_conn, chrono::Utc::now().timestamp())
    })
    .await;

    match result {
        Ok(Ok(())) => HttpResponse::Ok().json(serde_json::json!({"status": "recalculated"})),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /ratings/nodes
#[get("/ratings/nodes")]
async fn get_node_ratings(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let conn = pool.blocking_lock();
        let mut ratings = core_lib::storage::load_node_ratings(&conn)?;
        if ratings.is_empty() {
            // lazy recalc if empty
            let _ = core_lib::storage::recalc_ratings(&conn, chrono::Utc::now().timestamp());
            ratings = core_lib::storage::load_node_ratings(&conn)?;
        }
        Ok::<_, core_lib::models::CoreError>(ratings)
    })
    .await;

    match result {
        Ok(Ok(ratings)) => HttpResponse::Ok().json(ratings),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /ratings/groups
#[get("/ratings/groups")]
async fn get_group_ratings(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let conn = pool.blocking_lock();
        let mut ratings = core_lib::storage::load_group_ratings(&conn)?;
        if ratings.is_empty() {
            let _ = core_lib::storage::recalc_ratings(&conn, chrono::Utc::now().timestamp());
            ratings = core_lib::storage::load_group_ratings(&conn)?;
        }
        Ok::<_, core_lib::models::CoreError>(ratings)
    })
    .await;

    match result {
        Ok(Ok(ratings)) => HttpResponse::Ok().json(ratings),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /graph - данные графа доверия
#[get("/graph")]
async fn get_graph(pool: web::Data<DbPool>) -> impl Responder {
    let pool = pool.clone();
    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        core_lib::storage::load_graph(&_conn)
    })
    .await;

    match result {
        Ok(Ok(graph)) => HttpResponse::Ok().json(graph),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[derive(Deserialize)]
struct GraphQuery {
    min_score: Option<f64>,
    max_links: Option<usize>,
    depth: Option<usize>,
}

/// GET /graph/json — полный граф в JSON с параметрами фильтра
#[get("/graph/json")]
async fn get_graph_json(pool: web::Data<DbPool>, query: web::Query<GraphQuery>) -> impl Responder {
    let pool = pool.clone();
    let GraphQuery { min_score, max_links, depth } = query.into_inner();

    // Значения по умолчанию
    let min_score = min_score.unwrap_or(-1.0);
    let max_links = max_links.unwrap_or(10).max(0);

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        // ensure ratings up-to-date for graph queries
        let _ = core_lib::storage::recalc_ratings(&_conn, chrono::Utc::now().timestamp());
        core_lib::storage::load_graph_filtered(&_conn, min_score, max_links, depth)
    })
    .await;

    match result {
        Ok(Ok(graph)) => HttpResponse::Ok().json(graph),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// GET /graph/summary — агрегированные метрики по графу
#[get("/graph/summary")]
async fn get_graph_summary(pool: web::Data<DbPool>, query: web::Query<GraphQuery>) -> impl Responder {
    let pool = pool.clone();
    let GraphQuery { min_score, max_links, depth } = query.into_inner();

    let min_score = min_score.unwrap_or(-1.0);
    let max_links = max_links.unwrap_or(10).max(0);

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
        let graph: GraphData = core_lib::storage::load_graph_filtered(&_conn, min_score, max_links, depth)?;
        let summary: GraphSummary = core_lib::models::summarize_graph(&graph);
        Ok::<GraphSummary, core_lib::models::CoreError>(summary)
    })
    .await;

    match result {
        Ok(Ok(summary)) => HttpResponse::Ok().json(summary),
        Ok(Err(e)) => HttpResponse::InternalServerError().body(e.to_string()),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::p2p::encryption::CryptoIdentity;
    // use hex; // not needed here
    use actix_web::{test, App};
    use std::sync::Arc;
    use tokio::sync::Mutex;

    #[actix_web::test]
    async fn test_signature_verification_refactor() {
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

    #[actix_web::test]
    async fn ratings_endpoints_work() {
        // Prepare in-memory DB and app
        let conn = core_lib::storage::open_db(":memory:").unwrap();
        let conn_data = Arc::new(Mutex::new(conn));
        {
            let mut c = conn_data.lock().await;
            core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
        }

        let app = test::init_service(
            App::new()
                .app_data(actix_web::web::Data::new(conn_data.clone()))
                .configure(crate::api::routes)
        ).await;

        // Add event and statement + impact to have some ratings
        let add_event_req = serde_json::json!({
            "description": "Test for ratings",
            "context_id": 1,
            "vector": true
        });
        let req = test::TestRequest::post().uri("/events").set_json(&add_event_req).to_request();
        let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let ev_id = resp.get("id").and_then(|v| v.as_i64()).unwrap();

        // set author public key on event
        {
            let c = conn_data.lock().await;
            c.execute("UPDATE truth_events SET public_key='nodeA' WHERE id=?1", rusqlite::params![ev_id]).unwrap();
        }

        let add_stmt_req = serde_json::json!({
            "event_id": ev_id,
            "text": "true",
            "context": null,
            "truth_score": 0.8
        });
        let req = test::TestRequest::post().uri("/statements").set_json(&add_stmt_req).to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

        // direct DB insert impact to set validator pubkey
        {
            let c = conn_data.lock().await;
            let impact_id = core_lib::storage::add_impact(&c, ev_id, 1, true, Some("ok".into())).unwrap();
            c.execute("UPDATE impact SET public_key='nodeB' WHERE id=?1", rusqlite::params![impact_id]).unwrap();
        }

        // recalc
        let req = test::TestRequest::post().uri("/recalc").to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

        // GET ratings
        let req = test::TestRequest::get().uri("/ratings/nodes").to_request();
        let nodes: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        assert!(!nodes.as_array().unwrap().is_empty());

        let req = test::TestRequest::get().uri("/ratings/groups").to_request();
        let groups: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        assert!(!groups.as_array().unwrap().is_empty());

        let req = test::TestRequest::get().uri("/graph").to_request();
        let graph: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        assert!(!graph.get("nodes").unwrap().as_array().unwrap().is_empty());
    }

    #[actix_web::test]
    async fn graph_json_filters_work() {
        // Prepare in-memory DB and app
        let conn = core_lib::storage::open_db(":memory:").unwrap();
        let conn_data = Arc::new(Mutex::new(conn));
        {
            let mut c = conn_data.lock().await;
            core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
        }

        let app = test::init_service(
            App::new()
                .app_data(actix_web::web::Data::new(conn_data.clone()))
                .configure(crate::api::routes)
        ).await;

        // Event A (author nodeA) with positive statement; validator nodeB agrees
        let add_event_req = serde_json::json!({
            "description": "E1",
            "context_id": 1,
            "vector": true
        });
        let req = test::TestRequest::post().uri("/events").set_json(&add_event_req).to_request();
        let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let ev1 = resp.get("id").and_then(|v| v.as_i64()).unwrap();
        {
            let c = conn_data.lock().await;
            c.execute("UPDATE truth_events SET public_key='nodeA' WHERE id=?1", rusqlite::params![ev1]).unwrap();
        }
        let add_stmt_req = serde_json::json!({
            "event_id": ev1,
            "text": "true",
            "context": null,
            "truth_score": 0.9
        });
        let req = test::TestRequest::post().uri("/statements").set_json(&add_stmt_req).to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        {
            let c = conn_data.lock().await;
            let impact_id = core_lib::storage::add_impact(&c, ev1, 1, true, Some("ok".into())).unwrap();
            c.execute("UPDATE impact SET public_key='nodeB' WHERE id=?1", rusqlite::params![impact_id]).unwrap();
        }

        // Event C (author nodeC) with negative statement
        let add_event_req = serde_json::json!({
            "description": "E2",
            "context_id": 1,
            "vector": true
        });
        let req = test::TestRequest::post().uri("/events").set_json(&add_event_req).to_request();
        let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let ev2 = resp.get("id").and_then(|v| v.as_i64()).unwrap();
        {
            let c = conn_data.lock().await;
            c.execute("UPDATE truth_events SET public_key='nodeC' WHERE id=?1", rusqlite::params![ev2]).unwrap();
        }
        let add_stmt_req = serde_json::json!({
            "event_id": ev2,
            "text": "false",
            "context": null,
            "truth_score": -0.9
        });
        let req = test::TestRequest::post().uri("/statements").set_json(&add_stmt_req).to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

        // Recalculate
        let req = test::TestRequest::post().uri("/recalc").to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

        // min_score filter should exclude nodeB (0.0) and nodeC (<0)
        let req = test::TestRequest::get().uri("/graph/json?min_score=0.1&max_links=5").to_request();
        let graph_val: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let graph: core_lib::models::GraphData = serde_json::from_value(graph_val).unwrap();
        let ids: std::collections::HashSet<String> = graph.nodes.iter().map(|n| n.id.clone()).collect();
        assert!(ids.contains("nodeA"));
        assert!(!ids.contains("nodeB"));
        assert!(!ids.contains("nodeC"));
        assert!(graph.links.is_empty()); // no validator in set => no edges

        // depth=1 around top node should include nodeB but not nodeC
        let req = test::TestRequest::get().uri("/graph/json?min_score=-1&depth=1").to_request();
        let graph_val: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let graph: core_lib::models::GraphData = serde_json::from_value(graph_val).unwrap();
        let ids: std::collections::HashSet<String> = graph.nodes.iter().map(|n| n.id.clone()).collect();
        assert!(ids.contains("nodeA"));
        assert!(ids.contains("nodeB"));
        assert!(!ids.contains("nodeC"));
    }

    #[actix_web::test]
    async fn graph_summary_consistent() {
        // Prepare in-memory DB and app
        let conn = core_lib::storage::open_db(":memory:").unwrap();
        let conn_data = Arc::new(Mutex::new(conn));
        {
            let mut c = conn_data.lock().await;
            core_lib::storage::seed_knowledge_base(&mut c, "en").unwrap();
        }

        let app = test::init_service(
            App::new()
                .app_data(actix_web::web::Data::new(conn_data.clone()))
                .configure(crate::api::routes)
        ).await;

        // Create small dataset
        let add_event_req = serde_json::json!({"description":"E1","context_id":1,"vector":true});
        let req = test::TestRequest::post().uri("/events").set_json(&add_event_req).to_request();
        let resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let ev1 = resp.get("id").and_then(|v| v.as_i64()).unwrap();
        {
            let c = conn_data.lock().await;
            c.execute("UPDATE truth_events SET public_key='nodeA' WHERE id=?1", rusqlite::params![ev1]).unwrap();
        }
        let add_stmt_req = serde_json::json!({"event_id":ev1,"text":"t","context":null,"truth_score":0.7});
        let req = test::TestRequest::post().uri("/statements").set_json(&add_stmt_req).to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        {
            let c = conn_data.lock().await;
            let impact_id = core_lib::storage::add_impact(&c, ev1, 1, true, None).unwrap();
            c.execute("UPDATE impact SET public_key='nodeB' WHERE id=?1", rusqlite::params![impact_id]).unwrap();
        }

        let req = test::TestRequest::post().uri("/recalc").to_request();
        let _resp: serde_json::Value = test::call_and_read_body_json(&app, req).await;

        // Get graph and summary with the same filters
        let req = test::TestRequest::get().uri("/graph/json?min_score=-1&max_links=10&depth=1").to_request();
        let graph_val: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        let graph: core_lib::models::GraphData = serde_json::from_value(graph_val).unwrap();

        let req = test::TestRequest::get().uri("/graph/summary?min_score=-1&max_links=10&depth=1").to_request();
        let summary: core_lib::models::GraphSummary = test::call_and_read_body_json(&app, req).await;

        assert_eq!(summary.total_nodes, graph.nodes.len());
        assert_eq!(summary.total_links, graph.links.len());
        let avg: f64 = if graph.nodes.is_empty() { 0.0 } else { graph.nodes.iter().map(|n| n.score as f64).sum::<f64>() / (graph.nodes.len() as f64) };
        assert!((summary.avg_trust - avg).abs() < 1e-9);
        assert_eq!(summary.top_nodes.len(), std::cmp::min(10, graph.nodes.len()));
    }
}
