use serde::{Deserialize, Serialize};
use tauri::{command, State};
use crate::storage::Db;

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
pub async fn submit_judgment_fast(request: SubmitJudgmentRequest, db: State<'_, Db>) -> Result<Judgment, String> {
    let id = format!("judg_{}", uuid::Uuid::new_v4());
    let submitted_at = chrono::Utc::now().to_rfc3339();
    if !(request.assessment == "true" || request.assessment == "false" || request.assessment == "uncertain") {
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
pub async fn judgments_list_fast(eventId: String, page: u32, perPage: u32, db: State<'_, Db>) -> Result<JudgmentListResponse, String> {
    let limit = perPage as i64;
    let offset = (page.saturating_sub(1) as i64) * limit;
    let (rows, total) = db.list_judgments_for_event(&eventId, limit, offset)?;
    let data = rows.into_iter().map(|(id, event_id, assessment, confidence_level, reasoning, submitted_at)| Judgment {
        id,
        event_id,
        assessment,
        confidence_level,
        reasoning,
        submitted_at,
    }).collect();
    Ok(JudgmentListResponse { data, total })
}

#[command]
pub async fn get_judgment_stats(eventId: String, db: State<'_, Db>) -> Result<JudgmentStatsResponse, String> {
    let (t_true, t_false, t_uncertain, avg, last) = db.get_judgment_stats(&eventId)?;
    Ok(JudgmentStatsResponse {
        true_count: t_true,
        false_count: t_false,
        uncertain_count: t_uncertain,
        avg_confidence: avg,
        last_submitted_at: last,
    })
}

use serde::{Deserialize, Serialize};
use tauri::command;

#[derive(Debug, Serialize, Deserialize)]
pub struct Judgment {
    pub id: String,
    pub event_id: String,
    pub participant_id: String,
    pub value: f64,
    pub confidence: f64,
    pub created_at: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SubmitJudgmentRequest {
    pub event_id: String,
    pub participant_id: String,
    pub value: f64,
    pub confidence: f64,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ConsensusResult {
    pub event_id: String,
    pub consensus_value: f64,
    pub confidence: f64,
    pub participant_count: usize,
    pub judgments_used: Vec<Judgment>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct JudgmentStats {
    pub total_judgments: usize,
    pub average_value: f64,
    pub average_confidence: f64,
    pub participant_count: usize,
}

#[command]
pub async fn submit_judgment_fast(request: SubmitJudgmentRequest) -> Result<Judgment, String> {
    // TODO: Implement actual API call to core backend
    // For now, return a mock response
    Ok(Judgment {
        id: format!("judgment_{}", uuid::Uuid::new_v4()),
        event_id: request.event_id,
        participant_id: request.participant_id,
        value: request.value,
        confidence: request.confidence,
        created_at: chrono::Utc::now().to_rfc3339(),
    })
}

#[command]
pub async fn calculate_consensus_fast(event_id: String) -> Result<ConsensusResult, String> {
    // TODO: Implement actual API call to core backend
    // For now, return a mock response
    Ok(ConsensusResult {
        event_id,
        consensus_value: 0.5,
        confidence: 0.8,
        participant_count: 3,
        judgments_used: vec![],
    })
}

#[command]
pub async fn get_judgment_stats(_event_id: String) -> Result<JudgmentStats, String> {
    // TODO: Implement actual API call to core backend
    // For now, return a mock response
    Ok(JudgmentStats {
        total_judgments: 3,
        average_value: 0.5,
        average_confidence: 0.8,
        participant_count: 3,
    })
}