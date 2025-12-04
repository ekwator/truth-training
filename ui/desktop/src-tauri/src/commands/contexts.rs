use crate::storage::Db;
use core_lib::models::Context;
use serde::Serialize;
use tauri::{command, State};

#[derive(Debug, Serialize)]
pub struct ContextOption {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
}

impl From<Context> for ContextOption {
    fn from(ctx: Context) -> Self {
        ContextOption {
            id: ctx.id,
            name: ctx.name,
            description: ctx.description,
            category_id: ctx.category_id,
            forma_id: ctx.forma_id,
            cause_id: ctx.cause_id,
            develop_id: ctx.develop_id,
            effect_id: ctx.effect_id,
        }
    }
}

#[derive(Debug, Serialize)]
pub struct ContextListResponse {
    pub data: Vec<ContextOption>,
    pub fetched_at: String,
}

#[command]
pub async fn list_contexts(db: State<'_, Db>) -> Result<ContextListResponse, String> {
    let conn = db.0.lock();
    let contexts = core_lib::storage::get_all_contexts(&conn)
        .map_err(|e| format!("Failed to fetch contexts: {}", e))?;

    let options: Vec<ContextOption> = contexts.into_iter().map(ContextOption::from).collect();
    let fetched_at = chrono::Utc::now().to_rfc3339();

    Ok(ContextListResponse {
        data: options,
        fetched_at,
    })
}
