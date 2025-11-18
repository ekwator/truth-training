use crate::storage::Db;
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

    let id = format!("impact_{}", uuid::Uuid::new_v4());
    let created_at = chrono::Utc::now().to_rfc3339();

    db.insert_impact(
        &id,
        &request.event_id,
        request.impact_level,
        request.notes.as_deref(),
        &created_at,
    )?;

    Ok(Impact {
        id,
        event_id: request.event_id,
        impact_level: request.impact_level,
        notes: request.notes,
        created_at,
    })
}
