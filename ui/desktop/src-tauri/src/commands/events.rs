use crate::storage::Db;
use core_lib::storage as truth_storage;
use core_lib::models::NewTruthEvent;
use rusqlite::params;
use serde::{Deserialize, Serialize};
use tauri::{command, State};

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
pub struct UpdateEventRequest {
    pub detected: Option<bool>,
    pub corrected: Option<bool>,
    pub timestamp_end: Option<i64>,
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
pub async fn create_event_fast(
    request: CreateEventRequest,
    db: State<'_, Db>,
) -> Result<Event, String> {
    if request.description.trim().is_empty() {
        return Err("Description is required".to_string());
    }

    let timestamp_start = chrono::Utc::now().timestamp();
    
    // Use core function to add event
    let conn = db.0.lock();
    let new_event = NewTruthEvent {
        description: request.description,
        category_id: request.category_id,
        forma_id: request.forma_id,
        cause_id: request.cause_id,
        develop_id: request.develop_id,
        effect_id: request.effect_id,
        vector: request.vector,
        timestamp_start,
        code: 1, // Default code
    };
    
    let event_id = truth_storage::add_truth_event(&conn, new_event)
        .map_err(|e| format!("Failed to create event: {}", e))?;
    
    // Load the created event (entity names will be resolved in frontend)
    let truth_event = truth_storage::get_truth_event(&conn, event_id)
        .map_err(|e| format!("Failed to load created event: {}", e))?
        .ok_or_else(|| "Event not found after creation".to_string())?;
    
    // Convert to Event struct (entity names will be resolved in frontend)
    Ok(Event {
        id: truth_event.id,
        description: truth_event.description,
        category_id: truth_event.category_id,
        forma_id: truth_event.forma_id,
        cause_id: truth_event.cause_id,
        develop_id: truth_event.develop_id,
        effect_id: truth_event.effect_id,
        vector: truth_event.vector,
        detected: truth_event.detected,
        corrected: truth_event.corrected,
        timestamp_start: truth_event.timestamp_start,
        timestamp_end: truth_event.timestamp_end,
        code: truth_event.code,
        collective_score: truth_event.collective_score,
        category_name: None, // Will be resolved in frontend
        forma_name: None,
        cause_name: None,
        develop_name: None,
        effect_name: None,
    })
}

#[command]
pub async fn get_event_fast(event_id: i64, db: State<'_, Db>) -> Result<Option<Event>, String> {
    let conn = db.0.lock();
    let truth_event = truth_storage::get_truth_event(&conn, event_id)
        .map_err(|e| format!("Failed to load event: {}", e))?;
    
    match truth_event {
        Some(te) => Ok(Some(Event {
            id: te.id,
            description: te.description,
            category_id: te.category_id,
            forma_id: te.forma_id,
            cause_id: te.cause_id,
            develop_id: te.develop_id,
            effect_id: te.effect_id,
            vector: te.vector,
            detected: te.detected,
            corrected: te.corrected,
            timestamp_start: te.timestamp_start,
            timestamp_end: te.timestamp_end,
            code: te.code,
            collective_score: te.collective_score,
            category_name: None, // Will be resolved in frontend
            forma_name: None,
            cause_name: None,
            develop_name: None,
            effect_name: None,
        })),
        None => Ok(None),
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
pub async fn list_events_fast(
    page: u32,
    per_page: u32,
    db: State<'_, Db>,
) -> Result<ListEventsResponse, String> {
    let conn = db.0.lock();
    
    // Load all events using core function
    let all_events = truth_storage::load_truth_events(&conn)
        .map_err(|e| format!("Failed to load events: {}", e))?;
    
    // Calculate pagination
    let total = all_events.len() as i64;
    let offset = (page.saturating_sub(1) as usize) * (per_page as usize);
    // Note: limit is calculated but not used directly - pagination uses skip/take instead
    let _limit = offset + (per_page as usize);
    
    // Get paginated events
    let paginated_events: Vec<_> = all_events
        .into_iter()
        .rev() // Reverse to get newest first (load_truth_events returns in creation order)
        .skip(offset)
        .take(per_page as usize)
        .map(|te| Event {
            id: te.id,
            description: te.description,
            category_id: te.category_id,
            forma_id: te.forma_id,
            cause_id: te.cause_id,
            develop_id: te.develop_id,
            effect_id: te.effect_id,
            vector: te.vector,
            detected: te.detected,
            corrected: te.corrected,
            timestamp_start: te.timestamp_start,
            timestamp_end: te.timestamp_end,
            code: te.code,
            collective_score: te.collective_score,
            category_name: None, // Will be resolved in frontend
            forma_name: None,
            cause_name: None,
            develop_name: None,
            effect_name: None,
        })
        .collect();
    
    Ok(ListEventsResponse {
        data: paginated_events,
        total,
    })
}

#[command]
pub async fn update_event_fast(
    event_id: i64,
    request: UpdateEventRequest,
    db: State<'_, Db>,
) -> Result<Event, String> {
    let conn = db.0.lock();
    
    // Verify event exists
    let _existing_event = truth_storage::get_truth_event(&conn, event_id)
        .map_err(|e| format!("Failed to load event: {}", e))?
        .ok_or_else(|| "Event not found".to_string())?;
    
    // Build UPDATE query - only update detected, corrected, and timestamp_end (Android rules)
    // Use separate UPDATE statements for each field to avoid dynamic parameter issues
    if let Some(detected) = request.detected {
        conn.execute(
            "UPDATE truth_events SET detected = ?1 WHERE id = ?2",
            params![if detected { 1i64 } else { 0i64 }, event_id],
        )
        .map_err(|e| format!("Failed to update detected flag: {}", e))?;
    }
    
    if let Some(corrected) = request.corrected {
        conn.execute(
            "UPDATE truth_events SET corrected = ?1 WHERE id = ?2",
            params![if corrected { 1i64 } else { 0i64 }, event_id],
        )
        .map_err(|e| format!("Failed to update corrected flag: {}", e))?;
    }
    
    if let Some(timestamp_end) = request.timestamp_end {
        conn.execute(
            "UPDATE truth_events SET timestamp_end = ?1 WHERE id = ?2",
            params![timestamp_end, event_id],
        )
        .map_err(|e| format!("Failed to update timestamp_end: {}", e))?;
    }
    
    if request.detected.is_none() && request.corrected.is_none() && request.timestamp_end.is_none() {
        return Err("No fields to update".to_string());
    }
    
    // Load updated event
    let updated_event = truth_storage::get_truth_event(&conn, event_id)
        .map_err(|e| format!("Failed to load updated event: {}", e))?
        .ok_or_else(|| "Event not found after update".to_string())?;
    
    // Convert to Event struct
    Ok(Event {
        id: updated_event.id,
        description: updated_event.description,
        category_id: updated_event.category_id,
        forma_id: updated_event.forma_id,
        cause_id: updated_event.cause_id,
        develop_id: updated_event.develop_id,
        effect_id: updated_event.effect_id,
        vector: updated_event.vector,
        detected: updated_event.detected,
        corrected: updated_event.corrected,
        timestamp_start: updated_event.timestamp_start,
        timestamp_end: updated_event.timestamp_end,
        code: updated_event.code,
        collective_score: updated_event.collective_score,
        category_name: None, // Will be resolved in frontend
        forma_name: None,
        cause_name: None,
        develop_name: None,
        effect_name: None,
    })
}
