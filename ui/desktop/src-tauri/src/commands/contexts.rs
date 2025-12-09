use crate::storage::Db;
use core_lib::models::{Context, NewContext};
use serde::{Deserialize, Serialize};
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

#[command]
pub async fn clear_context_templates(db: State<'_, Db>) -> Result<String, String> {
    let conn = db.0.lock();
    
    // Delete all context templates
    let deleted = conn.execute("DELETE FROM context", [])
        .map_err(|e| format!("Failed to clear context templates: {}", e))?;

    log::info!("Cleared {} context templates", deleted);

    Ok(format!("Cleared {} context templates", deleted))
}

#[derive(Debug, Deserialize)]
pub struct CreateContextRequest {
    pub name: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub description: Option<String>,
}

#[command]
pub async fn create_context(
    db: State<'_, Db>,
    request: CreateContextRequest,
) -> Result<ContextOption, String> {
    let conn = db.0.lock();
    
    // Convert request to NewContext
    let new_ctx = NewContext {
        name: request.name,
        category_id: request.category_id,
        forma_id: request.forma_id,
        cause_id: request.cause_id,
        develop_id: request.develop_id,
        effect_id: request.effect_id,
        description: request.description,
    };

    // Use core_lib::storage::add_context which handles duplicate detection
    let id = core_lib::storage::add_context(&conn, new_ctx)
        .map_err(|e| {
            // Match Android error message exactly
            let error_msg = format!("{}", e);
            if error_msg.contains("identical fields already exists") || error_msg.contains("409") {
                "Template with identical fields already exists (409 Conflict)".to_string()
            } else {
                format!("Failed to create context template: {}", error_msg)
            }
        })?;

    // Fetch the created context
    let context = core_lib::storage::get_context_by_id(&conn, id)
        .map_err(|e| format!("Failed to fetch created context: {}", e))?
        .ok_or_else(|| format!("Created context not found: {}", id))?;

    Ok(ContextOption::from(context))
}

#[derive(Debug, Deserialize)]
pub struct CheckDuplicateRequest {
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub exclude_id: Option<i64>,
}

#[command]
pub async fn check_duplicate_template(
    db: State<'_, Db>,
    request: CheckDuplicateRequest,
) -> Result<bool, String> {
    let conn = db.0.lock();
    
    // Call core storage function to count duplicates
    let count = core_lib::storage::count_duplicate_templates(
        &conn,
        request.category_id,
        request.forma_id,
        request.cause_id,
        request.develop_id,
        request.effect_id,
        request.exclude_id,
    )
    .map_err(|e| format!("Failed to check for duplicate template: {}", e))?;
    
    // Return true if duplicate exists (count > 0)
    Ok(count > 0)
}
