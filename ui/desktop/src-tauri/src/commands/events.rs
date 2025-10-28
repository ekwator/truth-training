use serde::{Deserialize, Serialize};
use tauri::{command, State};
use crate::storage::Db;
use rusqlite::params;

#[derive(Debug, Serialize, Deserialize)]
pub struct Event {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub context_id: String,
    pub start_date: Option<String>,
    pub end_date: Option<String>,
    pub created_at: String,
    pub updated_at: Option<String>,
    pub status: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateEventRequest {
    pub title: String,
    pub description: Option<String>,
    pub context_id: String,
    pub start_date: Option<String>,
    pub end_date: Option<String>,
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
    // Validate context_id is not empty
    if request.context_id.is_empty() {
        return Err("Context selection is required".to_string());
    }

    let new_id = format!("event_{}", uuid::Uuid::new_v4());
    let created_at = chrono::Utc::now().to_rfc3339();
    
    db.insert_event(
        &new_id,
        &request.title,
        request.description.as_deref(),
        &request.context_id,
        request.start_date.as_deref(),
        request.end_date.as_deref(),
        &created_at,
        "active",
    )?;

    Ok(Event {
        id: new_id,
        title: request.title,
        description: request.description,
        context_id: request.context_id,
        start_date: request.start_date,
        end_date: request.end_date,
        created_at,
        updated_at: None,
        status: "active".to_string(),
    })
}

#[command]
pub async fn get_event_fast(event_id: String, db: State<'_, Db>) -> Result<Option<Event>, String> {
    let conn = db.0.lock();
    let mut stmt = conn
        .prepare("SELECT id, title, description, context_id, start_date, end_date, created_at, updated_at, status FROM events WHERE id = ?1 LIMIT 1")
        .map_err(|e| e.to_string())?;
    let mut rows = stmt.query(params![event_id]).map_err(|e| e.to_string())?;
    if let Some(row) = rows.next().map_err(|e| e.to_string())? {
        let event = Event {
            id: row.get(0).map_err(|e| e.to_string())?,
            title: row.get(1).map_err(|e| e.to_string())?,
            description: row.get(2).ok().unwrap_or(None),
            context_id: row.get(3).map_err(|e| e.to_string())?,
            start_date: row.get(4).ok().unwrap_or(None),
            end_date: row.get(5).ok().unwrap_or(None),
            created_at: row.get(6).map_err(|e| e.to_string())?,
            updated_at: row.get(7).ok().unwrap_or(None),
            status: row.get(8).map_err(|e| e.to_string())?,
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
pub async fn list_events_fast(page: u32, per_page: u32, db: State<'_, Db>) -> Result<ListEventsResponse, String> {
    let conn = db.0.lock();
    let total: i64 = conn
        .query_row("SELECT COUNT(1) FROM events", [], |row| row.get(0))
        .map_err(|e| e.to_string())?;

    let offset = (page.saturating_sub(1) as i64) * (per_page as i64);
    let mut stmt = conn
        .prepare("SELECT id, title, description, context_id, start_date, end_date, created_at, updated_at, status FROM events ORDER BY datetime(created_at) DESC LIMIT ?1 OFFSET ?2")
        .map_err(|e| e.to_string())?;

    let mut rows = stmt.query(params![per_page as i64, offset]).map_err(|e| e.to_string())?;
    let mut data: Vec<Event> = Vec::new();
    while let Some(row) = rows.next().map_err(|e| e.to_string())? {
        data.push(Event {
            id: row.get(0).map_err(|e| e.to_string())?,
            title: row.get(1).map_err(|e| e.to_string())?,
            description: row.get(2).ok().unwrap_or(None),
            context_id: row.get(3).map_err(|e| e.to_string())?,
            start_date: row.get(4).ok().unwrap_or(None),
            end_date: row.get(5).ok().unwrap_or(None),
            created_at: row.get(6).map_err(|e| e.to_string())?,
            updated_at: row.get(7).ok().unwrap_or(None),
            status: row.get(8).map_err(|e| e.to_string())?,
        });
    }

    Ok(ListEventsResponse { data, total })
}