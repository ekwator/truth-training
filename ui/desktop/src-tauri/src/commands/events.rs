use serde::{Deserialize, Serialize};
use tauri::{command, State};
use crate::storage::Db;

#[derive(Debug, Serialize, Deserialize)]
pub struct Event {
    pub id: i64,
    pub description: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub vector: bool,
    pub detected: Option<bool>,
    pub corrected: bool,
    pub timestamp_start: i64,
    pub timestamp_end: Option<i64>,
    pub code: u8,
    pub collective_score: Option<f64>,
    // Display helpers
    pub category_name: Option<String>,
    pub forma_name: Option<String>,
    pub cause_name: Option<String>,
    pub develop_name: Option<String>,
    pub effect_name: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateEventRequest {
    pub description: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub vector: bool,
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
    if request.description.trim().is_empty() {
        return Err("Description is required".to_string());
    }

    let timestamp_start = chrono::Utc::now().timestamp();
    let event_id = db.insert_truth_event(
        &request.description,
        request.category_id,
        request.forma_id,
        request.cause_id,
        request.develop_id,
        request.effect_id,
        request.vector,
        timestamp_start,
    )?;

    // Load the created event with FK names
    db.get_truth_event_with_names(event_id)
        .ok_or_else(|| "Failed to load created event".to_string())
}

#[command]
pub async fn get_event_fast(event_id: i64, db: State<'_, Db>) -> Result<Option<Event>, String> {
    db.get_truth_event_with_names(event_id)
        .map(Some)
        .ok_or_else(|| "Failed to load event".to_string())
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
    db.list_truth_events_with_names(page, per_page)
}