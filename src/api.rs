use actix_web::{get, post, web, HttpRequest, HttpResponse, Responder};
use serde::Deserialize;
use std::sync::Arc;
use tokio::sync::Mutex;

use core_lib::models::{Impact, NewTruthEvent, NewStatement, GraphData, GraphSummary};
use core_lib::storage;
use crate::p2p::encryption::CryptoIdentity;
use crate::p2p::sync::SyncData;
use crate::p2p::node::Node;
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
async fn recalc_metrics(pool: web::Data<DbPool>) -> impl Responder {
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
async fn ratings_sync(node: web::Data<Node>) -> impl Responder {
    match node.broadcast_ratings().await {
        Ok(()) => HttpResponse::Ok().json(serde_json::json!({"status":"broadcasted"})),
        Err(e) => HttpResponse::InternalServerError().body(e.to_string()),
    }
}

/// helper: зарегистрировать все маршруты
pub fn routes(cfg: &mut web::ServiceConfig) {
    cfg.service(health)
        .service(init_db)
        .service(seed_db)
        .service(detect_event)
        .service(recalc_metrics)
        .service(recalc_ratings)
        .service(get_all_data)
        .service(get_progress)
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
        .service(ratings_sync);
}

/// POST /recalc_ratings - Пересчет рейтингов узлов и групп
#[post("/recalc_ratings")]
async fn recalc_ratings(pool: web::Data<DbPool>) -> impl Responder {
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
        let _conn = pool.blocking_lock();
        core_lib::storage::load_node_ratings(&_conn)
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
        let _conn = pool.blocking_lock();
        core_lib::storage::load_group_ratings(&_conn)
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
    let depth = depth;

    let result = web::block(move || {
        let _conn = pool.blocking_lock();
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
    let depth = depth;

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
    use hex;
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
        assert!(nodes.as_array().unwrap().len() >= 1);

        let req = test::TestRequest::get().uri("/ratings/groups").to_request();
        let groups: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        assert!(groups.as_array().unwrap().len() >= 1);

        let req = test::TestRequest::get().uri("/graph").to_request();
        let graph: serde_json::Value = test::call_and_read_body_json(&app, req).await;
        assert!(graph.get("nodes").unwrap().as_array().unwrap().len() >= 1);
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
