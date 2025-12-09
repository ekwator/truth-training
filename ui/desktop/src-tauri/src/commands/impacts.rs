use crate::storage::Db;
use core_lib::storage as truth_storage;
use serde::{Deserialize, Serialize};
use tauri::{command, State};

#[derive(Debug, Serialize, Deserialize)]
pub struct Impact {
    pub id: String,
    pub event_id: String,
    pub impact_level: i32,
    pub notes: Option<String>,
    pub created_at: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct AddImpactRequest {
    pub event_id: String,
    pub impact_level: i32,
    pub notes: Option<String>,
}

#[command]
pub async fn add_impact(request: AddImpactRequest, db: State<'_, Db>) -> Result<Impact, String> {
    // Validate impact level is in range [1..5]
    if request.impact_level < 1 || request.impact_level > 5 {
        return Err("impact_level must be between 1 and 5".to_string());
    }

    // Parse event_id from string to i64
    let event_id = request.event_id.parse::<i64>()
        .map_err(|_| format!("Invalid event_id: {}", request.event_id))?;

    // Map impact_level (1-5) to core's type_id and value
    // For now, use impact_level as type_id, and value = true (positive impact)
    // TODO: Consider mapping impact_level > 3 to positive, <= 3 to negative
    let type_id = request.impact_level as i64;
    let value = true; // Default to positive impact

    let conn = db.0.lock();
    let impact_id = truth_storage::add_impact(
        &conn,
        event_id,
        type_id,
        value,
        request.notes.clone(),
    )
    .map_err(|e| format!("Failed to add impact: {}", e))?;

    // Load the created impact to get created_at timestamp
    let all_impacts = truth_storage::load_impacts(&conn)
        .map_err(|e| format!("Failed to load impacts: {}", e))?;
    
    let created_impact = all_impacts.iter()
        .find(|i| i.id == impact_id)
        .ok_or_else(|| "Failed to find created impact".to_string())?;

    // Convert core Impact to Desktop Impact format
    // Map type_id back to impact_level (assuming 1:1 mapping for now)
    let impact_level = created_impact.type_id as i32;
    let created_at = chrono::DateTime::<chrono::Utc>::from_timestamp(created_impact.created_at, 0)
        .ok_or_else(|| "Invalid timestamp".to_string())?
        .to_rfc3339();

    Ok(Impact {
        id: impact_id.to_string(),
        event_id: created_impact.event_id.to_string(),
        impact_level,
        notes: created_impact.notes.clone(),
        created_at,
    })
}
