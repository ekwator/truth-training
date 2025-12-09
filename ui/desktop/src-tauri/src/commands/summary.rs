use crate::storage::Db;
use core_lib::storage as truth_storage;
use directories::ProjectDirs;
use serde::Serialize;
use std::fs;
use std::path::PathBuf;
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
    let conn = db.0.lock();
    
    // Load metrics from core function
    let metrics = truth_storage::load_metrics(&conn)
        .map_err(|e| format!("Failed to load metrics: {}", e))?;
    
    // Get the latest metrics entry (most recent timestamp)
    let latest = metrics.iter()
        .max_by_key(|m| m.timestamp)
        .ok_or_else(|| "No metrics available".to_string())?;
    
    // Calculate average impact level from latest metrics
    // Using total_positive_impact / total_events as approximation
    let avg_impact = if latest.total_events > 0 {
        (latest.total_positive_impact / latest.total_events as f64) as f64
    } else {
        0.0
    };
    
    // Convert timestamp to ISO string
    let last_updated = chrono::DateTime::<chrono::Utc>::from_timestamp(latest.timestamp, 0)
        .map(|dt| dt.to_rfc3339());
    
    Ok(OverallMetrics {
        total_events: latest.total_events,
        average_impact_level: avg_impact,
        last_updated,
    })
}

#[tauri::command]
pub async fn list_event_rows(db: State<'_, Db>) -> Result<Vec<EventRow>, String> {
    let conn = db.0.lock();
    
    // Load all events using core function
    let events = truth_storage::load_truth_events(&conn)
        .map_err(|e| format!("Failed to load events: {}", e))?;
    
    // Load judgments to calculate average confidence per event
    // Note: This is a simplified approach. For better performance, we could
    // create a dedicated query or cache this data.
    let mut rows = Vec::new();
    for event in events.iter().rev() { // Newest first
        // Calculate average confidence from judgments (if available)
        // For now, we'll set impact to None and let frontend calculate if needed
        let impact: Option<f64> = None; // TODO: Calculate from judgments if needed
        
        // Format date
        let date = chrono::DateTime::<chrono::Utc>::from_timestamp(event.timestamp_start, 0)
            .map(|dt| dt.format("%Y-%m-%d %H:%M:%S").to_string())
            .unwrap_or_else(|| "-".to_string());
        
        rows.push(EventRow {
            event: event.description.clone(),
            summary: event.description.clone(), // Use description as summary for now
            impact,
            date,
        });
    }
    
    Ok(rows)
}

#[tauri::command]
pub async fn export_overall_summary_txt(db: State<'_, Db>) -> Result<String, String> {
    // gather data using core functions directly
    let conn = db.0.lock();
    
    // Load metrics
    let metrics = truth_storage::load_metrics(&conn)
        .map_err(|e| format!("Failed to load metrics: {}", e))?;
    
    let latest = metrics.iter()
        .max_by_key(|m| m.timestamp)
        .ok_or_else(|| "No metrics available".to_string())?;
    
    let total = latest.total_events;
    let avg = if latest.total_events > 0 {
        (latest.total_positive_impact / latest.total_events as f64) as f64
    } else {
        0.0
    };
    let last = chrono::DateTime::<chrono::Utc>::from_timestamp(latest.timestamp, 0)
        .map(|dt| dt.to_rfc3339());
    
    // Load events
    let events = truth_storage::load_truth_events(&conn)
        .map_err(|e| format!("Failed to load events: {}", e))?;
    
    let rows_result: Vec<EventRow> = events.iter()
        .rev()
        .map(|event| {
            let date = chrono::DateTime::<chrono::Utc>::from_timestamp(event.timestamp_start, 0)
                .map(|dt| dt.format("%Y-%m-%d %H:%M:%S").to_string())
                .unwrap_or_else(|| "-".to_string());
            
            EventRow {
                event: event.description.clone(),
                summary: event.description.clone(),
                impact: None,
                date,
            }
        })
        .collect();

    // format text
    let mut content = String::new();
    content.push_str("Overall Summary of Training Sessions\n\n");
    content.push_str(&format!("Total Events: {}\n", total));
    content.push_str(&format!("Average Impact Level: {:.1}\n", avg));
    content.push_str(&format!(
        "Last Updated: {}\n\n",
        last.unwrap_or_else(|| "-".to_string())
    ));
    content.push_str("Event | Summary | Impact | Date\n");
    for row in rows_result {
        let impact_txt = row.impact
            .map(|v| format!("{:.1}", v))
            .unwrap_or_else(|| "-".to_string());
        content.push_str(&format!(
            "{} | {} | {} | {}\n",
            row.event, row.summary, impact_txt, row.date
        ));
    }

    // resolve path
    let proj = ProjectDirs::from("com", "truth-training", "TruthTraining")
        .ok_or_else(|| "cannot resolve data dir".to_string())?;
    let mut dir: PathBuf = proj.data_dir().to_path_buf();
    dir.push("reports");
    fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    let ts = chrono::Utc::now().format("%Y%m%d_%H%M%S");
    let mut path = dir;
    path.push(format!("overall_summary_{}.txt", ts));

    // write file
    fs::write(&path, content).map_err(|e| e.to_string())?;
    Ok(path.to_string_lossy().to_string())
}
