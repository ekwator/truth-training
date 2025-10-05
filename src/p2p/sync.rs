use reqwest::Client;
use std::time::Duration;
use crate::p2p::encryption::CryptoIdentity;
use core_lib::models::{TruthEvent, Statement, Impact, ProgressMetrics};
use core_lib::storage;
use rusqlite::{Connection, params};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

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
    let message = format!("sync_request:{}", chrono::Utc::now().timestamp());

    // Подписываем сообщение приватным ключом
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    // Выполняем асинхронный GET-запрос для получения данных
    let response = client
        .get(format!("{peer_url}/get_data"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .send()
        .await?;

    // Проверяем HTTP-код ответа
    if !response.status().is_success() {
        anyhow::bail!("Peer returned non-success status: {}", response.status());
    }

    // Асинхронно читаем тело ответа
    let body = response.text().await?;
    let sync_data: SyncData = serde_json::from_str(&body)?;

    log::info!("Received sync data from {peer_url}: {} events, {} statements, {} impacts", 
               sync_data.events.len(), sync_data.statements.len(), sync_data.impacts.len());

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
    let message = format!("sync_push:{}", chrono::Utc::now().timestamp());

    // Подписываем сообщение приватным ключом
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    // Отправляем данные
    let response = client
        .post(format!("{peer_url}/sync"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
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
        "SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code 
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
        "SELECT id, event_id, text, context, truth_score, created_at, updated_at 
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
        "SELECT id, event_id, type_id, value, notes, created_at 
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
