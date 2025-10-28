use serde::{Deserialize, Serialize};
use tauri::{command, State};
use crate::storage::Db;
use rusqlite::params;

#[derive(Debug, Serialize, Deserialize)]
pub struct Event {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub created_at: String,
    pub status: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateEventRequest {
    pub title: String,
    pub description: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct HealthCheckResponse {
    pub status: String,
    pub timestamp: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ListEventsResponse {
    pub data: Vec<Event>,
    pub total: i64,
}

#[command]
pub async fn create_event_fast(request: CreateEventRequest, db: State<'_, Db>) -> Result<Event, String> {
    // TODO: Implement actual API call to core backend
    // For now, return a mock response
    let new_id = format!("event_{}", uuid::Uuid::new_v4());
    let created_at = chrono::Utc::now().to_rfc3339();
    db.insert_event(&new_id, &request.title, request.description.as_deref(), &created_at, "active")?;
    Ok(Event {
        id: new_id,
        title: request.title,
        description: request.description,
        created_at,
        status: "active".to_string(),
    })
}

#[command]
pub async fn get_event_fast(eventId: String, db: State<'_, Db>) -> Result<Option<Event>, String> {
    let conn = db.0.lock();
    let mut stmt = conn
        .prepare("SELECT id, title, description, created_at, status FROM events WHERE id = ?1 LIMIT 1")
        .map_err(|e| e.to_string())?;
    let mut rows = stmt.query(params![eventId]).map_err(|e| e.to_string())?;
    if let Some(row) = rows.next().map_err(|e| e.to_string())? {
        let event = Event {
            id: row.get(0).map_err(|e| e.to_string())?,
            title: row.get(1).map_err(|e| e.to_string())?,
            description: row.get(2).ok().unwrap_or(None),
            created_at: row.get(3).map_err(|e| e.to_string())?,
            status: row.get(4).map_err(|e| e.to_string())?,
        };
        Ok(Some(event))
    } else {
        Ok(None)
    }
}

#[command]
pub async fn health_check_core() -> Result<HealthCheckResponse, String> {
    // TODO: Implement actual health check to core backend
    // For now, return a mock response
    Ok(HealthCheckResponse {
        status: "healthy".to_string(),
        timestamp: chrono::Utc::now().to_rfc3339(),
    })
}

#[command]
pub async fn list_events_fast(page: u32, perPage: u32, db: State<'_, Db>) -> Result<ListEventsResponse, String> {
    let conn = db.0.lock();
    let total: i64 = conn
        .query_row("SELECT COUNT(1) FROM events", [], |row| row.get(0))
        .map_err(|e| e.to_string())?;

    let offset = (page.saturating_sub(1) as i64) * (perPage as i64);
    let mut stmt = conn
        .prepare("SELECT id, title, description, created_at, status FROM events ORDER BY datetime(created_at) DESC LIMIT ?1 OFFSET ?2")
        .map_err(|e| e.to_string())?;

    let mut rows = stmt.query(params![perPage as i64, offset]).map_err(|e| e.to_string())?;
    let mut data: Vec<Event> = Vec::new();
    while let Some(row) = rows.next().map_err(|e| e.to_string())? {
        data.push(Event {
            id: row.get(0).map_err(|e| e.to_string())?,
            title: row.get(1).map_err(|e| e.to_string())?,
            description: row.get(2).ok().unwrap_or(None),
            created_at: row.get(3).map_err(|e| e.to_string())?,
            status: row.get(4).map_err(|e| e.to_string())?,
        });
    }

    Ok(ListEventsResponse { data, total })
}