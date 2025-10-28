use crate::storage::Db;
use serde::Serialize;
use tauri::State;

#[derive(Serialize)]
pub struct OverallMetrics {
    pub total_events: i64,
    pub average_impact_level: f64,
    pub last_updated: Option<String>,
}

#[derive(Serialize)]
pub struct EventRow {
    pub event: String,
    pub summary: String,
    pub impact: Option<f64>,
    pub date: String,
}

#[tauri::command]
pub async fn get_overall_metrics(db: State<'_, Db>) -> Result<OverallMetrics, String> {
    let (total, avg, last) = db.get_overall_metrics()?;
    Ok(OverallMetrics { total_events: total, average_impact_level: avg, last_updated: last })
}

#[tauri::command]
pub async fn list_event_rows(db: State<'_, Db>) -> Result<Vec<EventRow>, String> {
    let items = db.list_event_summaries()?;
    Ok(items
        .into_iter()
        .map(|(title, desc, impact, created)| EventRow { event: title, summary: desc, impact, date: created })
        .collect())
}


