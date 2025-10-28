use serde::{Deserialize, Serialize};
use tauri::{command, State};
use crate::storage::Db;

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
pub async fn get_event_fast(event_id: String) -> Result<Option<Event>, String> {
    // TODO: Implement actual API call to core backend
    // For now, return a mock response
    Ok(Some(Event {
        id: event_id,
        title: "Sample Event".to_string(),
        description: Some("This is a sample event".to_string()),
        created_at: chrono::Utc::now().to_rfc3339(),
        status: "active".to_string(),
    }))
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