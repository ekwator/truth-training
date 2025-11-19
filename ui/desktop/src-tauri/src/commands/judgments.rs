use crate::storage::Db;
use serde::{Deserialize, Serialize};
use tauri::{command, State};

#[derive(Debug, Serialize, Deserialize)]
pub struct Judgment {
    pub id: String,
    pub event_id: String,
    pub assessment: String,
    pub confidence_level: f64,
    pub reasoning: Option<String>,
    pub submitted_at: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SubmitJudgmentRequest {
    pub event_id: String,
    pub assessment: String,
    pub confidence_level: f64,
    pub reasoning: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct JudgmentListResponse {
    pub data: Vec<Judgment>,
    pub total: i64,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct JudgmentStatsResponse {
    pub true_count: i64,
    pub false_count: i64,
    pub uncertain_count: i64,
    pub avg_confidence: f64,
    pub last_submitted_at: Option<String>,
}

#[command]
pub async fn submit_judgment_fast(
    request: SubmitJudgmentRequest,
    db: State<'_, Db>,
) -> Result<Judgment, String> {
    let id = format!("judg_{}", uuid::Uuid::new_v4());
    let submitted_at = chrono::Utc::now().to_rfc3339();
    if !(request.assessment == "true"
        || request.assessment == "false"
        || request.assessment == "uncertain")
    {
        return Err("invalid assessment".into());
    }
    if request.confidence_level < 0.0 || request.confidence_level > 1.0 {
        return Err("confidence_level must be in [0,1]".into());
    }
    db.insert_judgment(
        &id,
        &request.event_id,
        &request.assessment,
        request.confidence_level,
        request.reasoning.as_deref(),
        &submitted_at,
    )?;
    Ok(Judgment {
        id,
        event_id: request.event_id,
        assessment: request.assessment,
        confidence_level: request.confidence_level,
        reasoning: request.reasoning,
        submitted_at,
    })
}

#[command]
pub async fn judgments_list_fast(
    event_id: String,
    page: u32,
    per_page: u32,
    db: State<'_, Db>,
) -> Result<JudgmentListResponse, String> {
    let limit = per_page as i64;
    let offset = (page.saturating_sub(1) as i64) * limit;
    let (rows, total) = db.list_judgments_for_event(&event_id, limit, offset)?;
    let data = rows
        .into_iter()
        .map(
            |(id, event_id, assessment, confidence_level, reasoning, submitted_at)| Judgment {
                id,
                event_id,
                assessment,
                confidence_level,
                reasoning,
                submitted_at,
            },
        )
        .collect();
    Ok(JudgmentListResponse { data, total })
}

#[command]
pub async fn get_judgment_stats(
    event_id: String,
    db: State<'_, Db>,
) -> Result<JudgmentStatsResponse, String> {
    let (t_true, t_false, t_uncertain, avg, last) = db.get_judgment_stats(&event_id)?;
    Ok(JudgmentStatsResponse {
        true_count: t_true,
        false_count: t_false,
        uncertain_count: t_uncertain,
        avg_confidence: avg,
        last_submitted_at: last,
    })
}
