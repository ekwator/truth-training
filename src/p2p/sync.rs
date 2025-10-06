use reqwest::Client;
use std::time::Duration;
use crate::p2p::encryption::CryptoIdentity;
use core_lib::models::{TruthEvent, Statement, Impact, ProgressMetrics};
use core_lib::storage;
use rusqlite::{Connection, params, OptionalExtension};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use chrono::Utc;

/// Данные для синхронизации
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncData {
    pub events: Vec<TruthEvent>,
    pub statements: Vec<Statement>,
    pub impacts: Vec<Impact>,
    pub metrics: Vec<ProgressMetrics>,
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
}

/// Асинхронная синхронизация с peer'ом
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
    })
}

/// Двунаправленная синхронизация - отправка и получение данных
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

    let sync_data = SyncData {
        events: local_events,
        statements: local_statements,
        impacts: local_impacts,
        metrics: local_metrics,
        last_sync: chrono::Utc::now().timestamp(),
    };

    // Создаём асинхронный HTTP клиент
    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    // Формируем сообщение для подписи
    let ts = chrono::Utc::now().timestamp();
    let message = format!("sync_push:{}", ts);

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
        .json(&sync_data)
        .send()
        .await?;

    if !response.status().is_success() {
        anyhow::bail!("Peer sync push failed: {}", response.status());
    }

    // Получаем ответ с результатами синхронизации
    let sync_result: SyncResult = response.json().await?;

    log::info!("Bidirectional sync with {peer_url} completed: {} conflicts resolved, {} events added", 
               sync_result.conflicts_resolved, sync_result.events_added);

    Ok(sync_result)
}

/// Push all local data to a peer's /sync endpoint with signing
pub async fn push_local_data(
    peer_url: &str,
    identity: &CryptoIdentity,
    conn: &Connection,
) -> anyhow::Result<SyncResult> {
    let sync_data = SyncData {
        events: storage::load_truth_events(conn)?,
        statements: storage::load_statements(conn)?,
        impacts: storage::load_impacts(conn)?,
        metrics: storage::load_metrics(conn)?,
        last_sync: Utc::now().timestamp(),
    };

    let client = Client::builder().timeout(Duration::from_secs(30)).build()?;
    let ts = Utc::now().timestamp();
    let message = format!("sync_push:{}", ts);
    let sig = identity.sign(message.as_bytes());
    let signature_hex = hex::encode(sig.to_bytes());
    let public_key_hex = identity.public_key_hex();

    let resp = client
        .post(format!("{peer_url}/sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts.to_string())
        .json(&sync_data)
        .send()
        .await?;

    if !resp.status().is_success() {
        anyhow::bail!("Peer sync push failed: {}", resp.status());
    }
    Ok(resp.json().await?)
}

/// Pull remote data from peer by combining /get_data and /statements
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
        last_sync: ts,
    })
}

/// Reconcile remote data into local DB using timestamp-based conflict resolution and log via sync_log
pub fn reconcile(conn: &Connection, remote: &SyncData) -> anyhow::Result<SyncResult> {
    let mut conflicts_resolved = 0u32;
    let mut events_added = 0u32;
    let mut statements_added = 0u32;
    let mut impacts_added = 0u32;

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

    Ok(SyncResult {
        conflicts_resolved,
        events_added,
        statements_added,
        impacts_added,
        errors: vec![],
    })
}

/// Инкрементальная синхронизация - только изменения с последней синхронизации
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

    let sync_data = SyncData {
        events: recent_events,
        statements: recent_statements,
        impacts: recent_impacts,
        metrics: Vec::new(), // Метрики не инкрементальные
        last_sync: last_sync_timestamp,
    };

    // Отправляем только изменения
    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    let message = format!("incremental_sync:{}", chrono::Utc::now().timestamp());
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    let response = client
        .post(format!("{peer_url}/incremental_sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .json(&sync_data)
        .send()
        .await?;

    if !response.status().is_success() {
        anyhow::bail!("Incremental sync failed: {}", response.status());
    }

    let sync_result: SyncResult = response.json().await?;

    log::info!("Incremental sync with {peer_url} completed: {} items synced", 
               sync_result.events_added + sync_result.statements_added + sync_result.impacts_added);

    Ok(sync_result)
}

/// Получить события с определенного времени
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
