use crate::storage::Db;
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
    let (total, avg, last) = db.get_overall_metrics()?;
    Ok(OverallMetrics {
        total_events: total,
        average_impact_level: avg,
        last_updated: last,
    })
}

#[tauri::command]
pub async fn list_event_rows(db: State<'_, Db>) -> Result<Vec<EventRow>, String> {
    let items = db.list_event_summaries()?;
    Ok(items
        .into_iter()
        .map(|(title, desc, impact, created)| EventRow {
            event: title,
            summary: desc,
            impact,
            date: created,
        })
        .collect())
}

#[tauri::command]
pub async fn export_overall_summary_txt(db: State<'_, Db>) -> Result<String, String> {
    // gather data
    let (total, avg, last) = db.get_overall_metrics()?;
    let rows = db.list_event_summaries()?;

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
    for (title, desc, impact, created) in rows {
        let impact_txt = impact
            .map(|v| format!("{:.1}", v))
            .unwrap_or_else(|| "-".to_string());
        content.push_str(&format!(
            "{} | {} | {} | {}\n",
            title, desc, impact_txt, created
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
