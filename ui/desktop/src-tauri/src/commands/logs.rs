use crate::storage::Db;
use crate::LOGS_PAGE_SIZE;
use serde::Serialize;
use tauri::State;

#[derive(Serialize)]
pub struct LogItem {
    pub id: String,
    pub timestamp: String,
    pub source: String,
    pub level: String,
    pub message: String,
}

#[derive(Serialize)]
pub struct LogsPage {
    pub items: Vec<LogItem>,
    pub page: u32,
    pub total: i64,
}

#[tauri::command]
pub async fn list_logs(page: u32, db: State<'_, Db>) -> Result<LogsPage, String> {
    let page_size = LOGS_PAGE_SIZE as i64;
    let (rows, total) = db.list_logs(page as i64, page_size)?;
    Ok(LogsPage {
        items: rows
            .into_iter()
            .map(|(id, timestamp, source, level, message)| LogItem { id, timestamp, source, level, message })
            .collect(),
        page,
        total,
    })
}

#[tauri::command]
pub async fn clear_logs(db: State<'_, Db>) -> Result<(), String> {
    db.clear_logs()
}


