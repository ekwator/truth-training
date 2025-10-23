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