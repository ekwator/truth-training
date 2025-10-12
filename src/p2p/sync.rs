#[cfg(any(test, feature = "p2p-client-sync"))]
use reqwest::Client;
#[cfg(any(test, feature = "p2p-client-sync"))]
use std::time::Duration;
#[cfg(any(test, feature = "p2p-client-sync"))]
use crate::p2p::encryption::CryptoIdentity;
use core_lib::models::{TruthEvent, Statement, Impact, ProgressMetrics, NodeRating, GroupRating};
use core_lib::storage;
// trust_propagation используется внутри core-lib/storage::merge_ratings
use rusqlite::{Connection, params, OptionalExtension};
use serde::{Deserialize, Serialize};
#[cfg(any(test, feature = "p2p-client-sync"))]
use chrono::Utc;
use sha2::{Sha256, Digest};
use once_cell::sync::Lazy;
use tokio::sync::Mutex as TokioMutex;
use std::collections::HashMap;

/// Данные для синхронизации
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncData {
    pub events: Vec<TruthEvent>,
    pub statements: Vec<Statement>,
    pub impacts: Vec<Impact>,
    pub metrics: Vec<ProgressMetrics>,
    pub node_ratings: Vec<NodeRating>,
    pub group_ratings: Vec<GroupRating>,
    pub last_sync: i64,
}

/// Результат синхронизации
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncResult {
    pub conflicts_resolved: u32,
    pub events_added: u32,
    pub statements_added: u32,
    pub impacts_added: u32,
    pub errors: Vec<String>,
    // Новые поля для доверия
    pub nodes_trust_changed: u32,
    pub trust_diff: Vec<TrustDelta>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrustDelta {
    pub node_id: String,
    pub delta: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RelayStat {
    pub peer_url: String,
    pub relayed: u64,
    pub dropped: u64,
    pub relay_rate: f32,
}

static RELAY_METRICS: Lazy<TokioMutex<HashMap<String, (u64, u64)>>> = Lazy::new(|| TokioMutex::new(HashMap::new()));

async fn record_relay_result(peer_url: &str, success: bool) {
    let mut g = RELAY_METRICS.lock().await;
    let entry = g.entry(peer_url.to_string()).or_insert((0, 0));
    if success { entry.0 = entry.0.saturating_add(1); } else { entry.1 = entry.1.saturating_add(1); }
}

pub async fn get_relay_stats() -> Vec<RelayStat> {
    let g = RELAY_METRICS.lock().await;
    let mut out = Vec::new();
    for (peer, (relayed, dropped)) in g.iter() {
        let total = *relayed + *dropped;
        let rate = if total == 0 { 0.0 } else { (*relayed as f32) / (total as f32) };
        out.push(RelayStat { peer_url: peer.clone(), relayed: *relayed, dropped: *dropped, relay_rate: rate });
    }
    out
}

/// Flush relay metrics to database and update node_metrics table
pub async fn flush_relay_metrics_to_db(conn: &Connection) -> anyhow::Result<()> {
    let stats = get_relay_stats().await;
    let mut relay_data = Vec::new();
    
    for stat in stats {
        // Extract pubkey from peer_url (assuming it's in the URL or we need to map it)
        // For now, we'll use the peer_url as the identifier
        let pubkey = stat.peer_url.clone(); // This should be mapped to actual pubkey
        relay_data.push((pubkey, stat.relay_rate));
    }
    
    if !relay_data.is_empty() {
        core_lib::storage::flush_relay_metrics(conn, &relay_data)?;
    }
    
    Ok(())
}

#[derive(thiserror::Error, Debug)]
pub enum SyncError {
    #[error("Network error: {0}")]
    Network(#[from] reqwest::Error),
    #[error("Serialization error: {0}")]
    Serde(#[from] serde_json::Error),
    #[error("Other: {0}")]
    Other(String),
}

pub fn compute_ratings_hash(node_ratings: &[NodeRating], group_ratings: &[GroupRating]) -> Result<String, serde_json::Error> {
    let v = serde_json::json!({
        "node_ratings": node_ratings,
        "group_ratings": group_ratings,
    });
    let bytes = serde_json::to_vec(&v)?;
    let mut hasher = Sha256::new();
    hasher.update(&bytes);
    Ok(hex::encode(hasher.finalize()))
}

/// Асинхронная синхронизация с peer'ом
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub async fn sync_with_peer(peer_url: &str, identity: &CryptoIdentity) -> anyhow::Result<SyncResult> {
    // Создаём асинхронный HTTP клиент
    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    // Формируем сообщение для подписи
    let ts = chrono::Utc::now().timestamp();
    let message = format!("sync_request:{}", ts);

    // Подписываем сообщение приватным ключом
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    // Выполняем асинхронный GET-запрос для получения данных
    let response = client
        .get(format!("{peer_url}/get_data"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts.to_string())
        .send()
        .await?;

    // Проверяем HTTP-код ответа
    if !response.status().is_success() {
        anyhow::bail!("Peer returned non-success status: {}", response.status());
    }

    // Асинхронно читаем тело ответа
    let body = response.text().await?;
    let sync_data: SyncData = serde_json::from_str(&body)?;

    log::info!(
        "Received sync data from {peer_url}: {} events, {} statements, {} impacts",
        sync_data.events.len(),
        sync_data.statements.len(),
        sync_data.impacts.len()
    );

    // Здесь будет обработка полученных данных
    // Пока возвращаем заглушку
    Ok(SyncResult {
        conflicts_resolved: 0,
        events_added: sync_data.events.len() as u32,
        statements_added: sync_data.statements.len() as u32,
        impacts_added: sync_data.impacts.len() as u32,
        errors: Vec::new(),
        nodes_trust_changed: 0,
        trust_diff: Vec::new(),
    })
}

/// Двунаправленная синхронизация - отправка и получение данных
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub async fn bidirectional_sync_with_peer(
    peer_url: &str, 
    identity: &CryptoIdentity,
    conn: &Connection,
) -> anyhow::Result<SyncResult> {
    // Получаем локальные данные для отправки
    let local_events = storage::load_truth_events(conn)?;
    let local_statements = storage::load_statements(conn)?;
    let local_impacts = storage::load_impacts(conn)?;
    let local_metrics = storage::load_metrics(conn)?;

    let local_node_ratings = core_lib::storage::load_node_ratings(conn)?;
    let local_group_ratings = core_lib::storage::load_group_ratings(conn)?;
    let sync_data = SyncData {
        events: local_events,
        statements: local_statements,
        impacts: local_impacts,
        metrics: local_metrics,
        node_ratings: local_node_ratings.clone(),
        group_ratings: local_group_ratings.clone(),
        last_sync: chrono::Utc::now().timestamp(),
    };

    // Создаём асинхронный HTTP клиент
    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    // Формируем сообщение для подписи
    let ts = chrono::Utc::now().timestamp();
    let ratings_hash = compute_ratings_hash(&sync_data.node_ratings, &sync_data.group_ratings)?;
    let message = format!("sync_push:{}:{}", ts, ratings_hash);

    // Подписываем сообщение приватным ключом
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    // Отправляем данные
    let response = client
        .post(format!("{peer_url}/sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts.to_string())
        .header("X-Ratings-Hash", ratings_hash)
        .json(&sync_data)
        .send()
        .await?;

    let ok = response.status().is_success();
    record_relay_result(peer_url, ok).await;
    if !ok {
        anyhow::bail!("Peer sync push failed: {}", response.status());
    }

    // Получаем ответ с результатами синхронизации
    let sync_result: SyncResult = response.json().await?;
    
    // Обновляем метрики ретрансляции в базе данных
    let _ = flush_relay_metrics_to_db(conn).await;

    log::info!(
        "Bidirectional sync with {peer_url} completed: conflicts {}, events {}, trust changes {}",
        sync_result.conflicts_resolved,
        sync_result.events_added,
        sync_result.nodes_trust_changed
    );

    Ok(sync_result)
}

/// Push all local data to a peer's /sync endpoint with signing
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub async fn push_local_data(
    peer_url: &str,
    identity: &CryptoIdentity,
    conn: &Connection,
) -> anyhow::Result<SyncResult> {
    let node_ratings = core_lib::storage::load_node_ratings(conn)?;
    let group_ratings = core_lib::storage::load_group_ratings(conn)?;
    let sync_data = SyncData {
        events: storage::load_truth_events(conn)?,
        statements: storage::load_statements(conn)?,
        impacts: storage::load_impacts(conn)?,
        metrics: storage::load_metrics(conn)?,
        node_ratings: node_ratings.clone(),
        group_ratings: group_ratings.clone(),
        last_sync: Utc::now().timestamp(),
    };

    let client = Client::builder().timeout(Duration::from_secs(30)).build()?;
    let ts = Utc::now().timestamp();
    let ratings_hash = compute_ratings_hash(&sync_data.node_ratings, &sync_data.group_ratings)?;
    let message = format!("sync_push:{}:{}", ts, ratings_hash);
    let sig = identity.sign(message.as_bytes());
    let signature_hex = hex::encode(sig.to_bytes());
    let public_key_hex = identity.public_key_hex();

    let resp = client
        .post(format!("{peer_url}/sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts.to_string())
        .header("X-Ratings-Hash", ratings_hash)
        .json(&sync_data)
        .send()
        .await?;

    let ok = resp.status().is_success();
    record_relay_result(peer_url, ok).await;
    if !ok {
        anyhow::bail!("Peer sync push failed: {}", resp.status());
    }
    Ok(resp.json().await?)
}

/// Pull remote data from peer by combining /get_data and /statements
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub async fn pull_remote_data(peer_url: &str, identity: &CryptoIdentity) -> anyhow::Result<SyncData> {
    let client = Client::builder().timeout(Duration::from_secs(30)).build()?;
    let ts = Utc::now().timestamp();
    let message = format!("sync_request:{}", ts);
    let sig = identity.sign(message.as_bytes());
    let signature_hex = hex::encode(sig.to_bytes());
    let public_key_hex = identity.public_key_hex();

    // get events, impacts, metrics
    let resp_main = client
        .get(format!("{peer_url}/get_data"))
        .header("X-Public-Key", public_key_hex.clone())
        .header("X-Signature", signature_hex.clone())
        .header("X-Timestamp", ts.to_string())
        .send()
        .await?;
    if !resp_main.status().is_success() {
        anyhow::bail!("Peer returned non-success status: {}", resp_main.status());
    }
    let v = resp_main.json::<serde_json::Value>().await?;
    let events: Vec<TruthEvent> = serde_json::from_value(v.get("events").cloned().unwrap_or_default())?;
    let impacts: Vec<Impact> = serde_json::from_value(v.get("impacts").cloned().unwrap_or_default())?;
    let metrics: Vec<ProgressMetrics> = serde_json::from_value(v.get("metrics").cloned().unwrap_or_default())?;

    // get statements
    let resp_stmts = client
        .get(format!("{peer_url}/statements"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts.to_string())
        .send()
        .await?;
    if !resp_stmts.status().is_success() {
        anyhow::bail!("Peer returned non-success status: {}", resp_stmts.status());
    }
    let statements: Vec<Statement> = resp_stmts.json().await?;

    Ok(SyncData {
        events,
        statements,
        impacts,
        metrics,
        node_ratings: Vec::new(),
        group_ratings: Vec::new(),
        last_sync: ts,
    })
}

/// Reconcile remote data into local DB using timestamp-based conflict resolution and log via sync_log
pub fn reconcile(conn: &Connection, remote: &SyncData) -> anyhow::Result<SyncResult> {
    let mut conflicts_resolved = 0u32;
    let mut events_added = 0u32;
    let mut statements_added = 0u32;
    let mut impacts_added = 0u32;
    // trust_changes будет заполнен после merge

    // Events
    for ev in &remote.events {
        let existing = storage::get_truth_event(conn, ev.id).map_err(|e| anyhow::anyhow!(e.to_string()))?;
        match existing {
            Some(local) => {
                if ev.timestamp_start > local.timestamp_start {
                    // remote wins -> update
                    conn.execute(
                        r#"UPDATE truth_events SET
                            description=?2, context_id=?3, vector=?4, detected=?5, corrected=?6,
                            timestamp_start=?7, timestamp_end=?8, code=?9, signature=?10, public_key=?11
                          WHERE id=?1"#,
                        params![
                            ev.id,
                            ev.description,
                            ev.context_id,
                            if ev.vector { 1 } else { 0 },
                            ev.detected.map(|v| if v { 1 } else { 0 }),
                            if ev.corrected { 1 } else { 0 },
                            ev.timestamp_start,
                            ev.timestamp_end,
                            ev.code as i64,
                            ev.signature,
                            ev.public_key,
                        ],
                    )?;
                    conflicts_resolved += 1;
                    events_added += 1; // count updates as added for simplicity
                    storage::log_sync(
                        conn,
                        "update",
                        "truth_events",
                        &ev.id.to_string(),
                        ev.signature.clone(),
                        ev.public_key.clone(),
                    )?;
                }
            }
            None => {
                conn.execute(
                    r#"INSERT OR IGNORE INTO truth_events
                        (id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code, signature, public_key)
                      VALUES
                        (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)"#,
                    params![
                        ev.id,
                        ev.description,
                        ev.context_id,
                        if ev.vector { 1 } else { 0 },
                        ev.detected.map(|v| if v { 1 } else { 0 }),
                        if ev.corrected { 1 } else { 0 },
                        ev.timestamp_start,
                        ev.timestamp_end,
                        ev.code as i64,
                        ev.signature,
                        ev.public_key,
                    ],
                )?;
                events_added += 1;
                storage::log_sync(
                    conn,
                    "insert",
                    "truth_events",
                    &ev.id.to_string(),
                    ev.signature.clone(),
                    ev.public_key.clone(),
                )?;
            }
        }
    }

    // Statements
    for st in &remote.statements {
        let mut stmt = conn.prepare("SELECT updated_at FROM statements WHERE id=?1")?;
        let upd: Option<i64> = stmt.query_row(params![st.id], |r| r.get(0)).optional()?;
        match upd {
            Some(local_updated_at) => {
                if st.updated_at > local_updated_at {
                    conn.execute(
                        r#"UPDATE statements SET
                            event_id=?2, text=?3, context=?4, truth_score=?5, created_at=?6, updated_at=?7, signature=?8, public_key=?9
                          WHERE id=?1"#,
                        params![
                            st.id,
                            st.event_id,
                            st.text,
                            st.context,
                            st.truth_score,
                            st.created_at,
                            st.updated_at,
                            st.signature,
                            st.public_key,
                        ],
                    )?;
                    conflicts_resolved += 1;
                    statements_added += 1;
                    storage::log_sync(
                        conn,
                        "update",
                        "statements",
                        &st.id.to_string(),
                        st.signature.clone(),
                        st.public_key.clone(),
                    )?;
                }
            }
            None => {
                conn.execute(
                    r#"INSERT OR IGNORE INTO statements
                        (id, event_id, text, context, truth_score, created_at, updated_at, signature, public_key)
                      VALUES
                        (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)"#,
                    params![
                        st.id,
                        st.event_id,
                        st.text,
                        st.context,
                        st.truth_score,
                        st.created_at,
                        st.updated_at,
                        st.signature,
                        st.public_key,
                    ],
                )?;
                statements_added += 1;
                storage::log_sync(
                    conn,
                    "insert",
                    "statements",
                    &st.id.to_string(),
                    st.signature.clone(),
                    st.public_key.clone(),
                )?;
            }
        }
    }

    // Impacts (append-only)
    for im in &remote.impacts {
        let mut stmt = conn.prepare("SELECT 1 FROM impact WHERE id=?1")?;
        let exists: Option<i64> = stmt.query_row(params![im.id.clone()], |r| r.get(0)).optional()?;
        if exists.is_none() {
            conn.execute(
                r#"INSERT OR IGNORE INTO impact
                    (id, event_id, type_id, value, notes, created_at, signature, public_key)
                  VALUES
                    (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)"#,
                params![
                    im.id,
                    im.event_id,
                    im.type_id,
                    if im.value { 1 } else { 0 },
                    im.notes,
                    im.created_at,
                    im.signature,
                    im.public_key,
                ],
            )?;
            impacts_added += 1;
            storage::log_sync(
                conn,
                "insert",
                "impact",
                &im.id,
                im.signature.clone(),
                im.public_key.clone(),
            )?;
        }
    }

    // Ratings merge: принимаем все входящие записи без фильтра доверия
    let diffs = core_lib::storage::merge_ratings(
        conn,
        &remote.node_ratings,
        &remote.group_ratings,
    ).map_err(|e| anyhow::anyhow!(e.to_string()))?;

    // Пересчёт агрегатов групп после обновления (для корректного avg_score)
    core_lib::storage::recalc_ratings(conn, chrono::Utc::now().timestamp())
        .map_err(|e| anyhow::anyhow!(e.to_string()))?;

    let trust_changes: Vec<TrustDelta> = diffs
        .into_iter()
        .map(|(node_id, delta)| TrustDelta { node_id, delta })
        .collect();

    // Лог высокого уровня о доверии
    let avg_trust: f64 = conn
        .query_row("SELECT COALESCE(AVG(trust_score),0.0) FROM node_ratings", [], |r| r.get(0))
        .unwrap_or(0.0);
    let gains = trust_changes.iter().filter(|d| d.delta > 0.0).count();
    let losses = trust_changes.iter().filter(|d| d.delta < 0.0).count();
    let equals = trust_changes.len().saturating_sub(gains + losses);
    let details = format!(
        "trust propagation: avg={:.3}, changes={} (gains {}, losses {}, equal {}); sample={}",
        avg_trust,
        trust_changes.len(),
        gains,
        losses,
        equals,
        trust_changes
            .iter()
            .take(3)
            .map(|d| format!("{}:{:+.3}", &d.node_id.get(0..8).unwrap_or(""), d.delta))
            .collect::<Vec<String>>()
            .join(",")
    );
    let _ = core_lib::storage::log_sync_event(
        conn,
        "peer",
        "reconcile",
        "success",
        &details,
    );

    Ok(SyncResult {
        conflicts_resolved,
        events_added,
        statements_added,
        impacts_added,
        errors: vec![],
        nodes_trust_changed: trust_changes.len() as u32,
        trust_diff: trust_changes,
    })
}

/// Инкрементальная синхронизация - только изменения с последней синхронизации
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub async fn incremental_sync_with_peer(
    peer_url: &str, 
    identity: &CryptoIdentity,
    conn: &Connection,
    last_sync_timestamp: i64,
) -> anyhow::Result<SyncResult> {
    // Получаем только изменения с последней синхронизации
    let recent_events = get_events_since(conn, last_sync_timestamp)?;
    let recent_statements = get_statements_since(conn, last_sync_timestamp)?;
    let recent_impacts = get_impacts_since(conn, last_sync_timestamp)?;

    let node_ratings = core_lib::storage::load_node_ratings(conn)?;
    let group_ratings = core_lib::storage::load_group_ratings(conn)?;
    let sync_data = SyncData {
        events: recent_events,
        statements: recent_statements,
        impacts: recent_impacts,
        metrics: Vec::new(), // Метрики не инкрементальные
        node_ratings: node_ratings.clone(),
        group_ratings: group_ratings.clone(),
        last_sync: last_sync_timestamp,
    };

    // Отправляем только изменения
    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    let now = chrono::Utc::now().timestamp();
    let ratings_hash = compute_ratings_hash(&sync_data.node_ratings, &sync_data.group_ratings)?;
    let message = format!("incremental_sync:{}:{}", now, ratings_hash);
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    let response = client
        .post(format!("{peer_url}/incremental_sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Ratings-Hash", ratings_hash)
        .json(&sync_data)
        .send()
        .await?;

    let ok = response.status().is_success();
    record_relay_result(peer_url, ok).await;
    if !ok { anyhow::bail!("Incremental sync failed: {}", response.status()); }

    let sync_result: SyncResult = response.json().await?;
    
    // Обновляем метрики ретрансляции в базе данных
    let _ = flush_relay_metrics_to_db(conn).await;

    log::info!(
        "Incremental sync with {peer_url} completed: {} items synced; trust changes {}",
        sync_result.events_added + sync_result.statements_added + sync_result.impacts_added,
        sync_result.nodes_trust_changed
    );

    Ok(sync_result)
}

/// Получить события с определенного времени
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
fn get_events_since(conn: &Connection, timestamp: i64) -> anyhow::Result<Vec<TruthEvent>> {
    let mut stmt = conn.prepare(
        "SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code, signature, public_key \
         FROM truth_events WHERE timestamp_start > ?1 ORDER BY timestamp_start"
    )?;

    let rows = stmt.query_map(params![timestamp], |row| {
        Ok(TruthEvent {
            id: row.get(0)?,
            description: row.get(1)?,
            context_id: row.get(2)?,
            vector: row.get::<_, i64>(3)? != 0,
            detected: row.get::<_, Option<i64>>(4)?.map(|v| v != 0),
            corrected: row.get::<_, i64>(5)? != 0,
            timestamp_start: row.get(6)?,
            timestamp_end: row.get::<_, Option<i64>>(7)?,
            code: row.get(8)?,
            signature: row.get(9)?,
            public_key: row.get(10)?,
        })
    })?;

    let mut events = Vec::new();
    for e in rows {
        events.push(e?);
    }
    Ok(events)
}

/// Получить утверждения с определенного времени
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
fn get_statements_since(conn: &Connection, timestamp: i64) -> anyhow::Result<Vec<Statement>> {
    let mut stmt = conn.prepare(
        "SELECT id, event_id, text, context, truth_score, created_at, updated_at, signature, public_key \
         FROM statements WHERE created_at > ?1 ORDER BY created_at"
    )?;

    let rows = stmt.query_map(params![timestamp], |row| {
        Ok(Statement {
            id: row.get(0)?,
            event_id: row.get(1)?,
            text: row.get(2)?,
            context: row.get(3)?,
            truth_score: row.get(4)?,
            created_at: row.get(5)?,
            updated_at: row.get(6)?,
            signature: row.get(7)?,
            public_key: row.get(8)?,
        })
    })?;

    let mut statements = Vec::new();
    for s in rows {
        statements.push(s?);
    }
    Ok(statements)
}

/// Получить воздействия с определенного времени
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
fn get_impacts_since(conn: &Connection, timestamp: i64) -> anyhow::Result<Vec<Impact>> {
    let mut stmt = conn.prepare(
        "SELECT id, event_id, type_id, value, notes, created_at, signature, public_key \
         FROM impact WHERE created_at > ?1 ORDER BY created_at"
    )?;

    let rows = stmt.query_map(params![timestamp], |row| {
        Ok(Impact {
            id: row.get(0)?,
            event_id: row.get(1)?,
            type_id: row.get(2)?,
            value: row.get::<_, i64>(3)? != 0,
            notes: row.get(4)?,
            created_at: row.get(5)?,
            signature: row.get(6)?,
            public_key: row.get(7)?,
        })
    })?;

    let mut impacts = Vec::new();
    for i in rows {
        impacts.push(i?);
    }
    Ok(impacts)
}

/// Конфликт-резолюшн для событий
#[cfg(any(test, feature = "p2p-client-sync"))]
#[allow(dead_code)]
pub fn resolve_event_conflicts(
    local_events: &[TruthEvent],
    remote_events: &[TruthEvent],
) -> Vec<TruthEvent> {
    let mut resolved_events = Vec::new();
    let remote_map: HashMap<i64, &TruthEvent> = remote_events.iter().map(|e| (e.id, e)).collect();

    // Добавляем локальные события, проверяя конфликты
    for local_event in local_events {
        if let Some(remote_event) = remote_map.get(&local_event.id) {
            // Конфликт: выбираем более свежую версию
            if local_event.timestamp_start > remote_event.timestamp_start {
                resolved_events.push(local_event.clone());
            } else {
                resolved_events.push((*remote_event).clone());
            }
        } else {
            // Нет конфликта, добавляем локальное событие
            resolved_events.push(local_event.clone());
        }
    }

    // Добавляем новые удаленные события
    for remote_event in remote_events {
        if !local_events.iter().any(|e| e.id == remote_event.id) {
            resolved_events.push(remote_event.clone());
        }
    }

    resolved_events
}
