use serde::{Serialize, Deserialize};
use uuid::Uuid;
use chrono::Utc;

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct Statement {
    pub id: Uuid,                 // глобальный идентификатор
    pub text: String,
    pub truth_score: Option<f32>, // 0.0–1.0
    pub context: Option<String>,
    pub updated_at: i64,          // unix-epoch (секунды)
}

impl Statement {
    pub fn new(text: impl Into<String>, context: Option<String>, truth_score: Option<f32>) -> Self {
        Self {
            id: Uuid::new_v4(),
            text: text.into(),
            truth_score,
            context,
            updated_at: chrono::Utc::now().timestamp(),
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct TruthEvent {
    pub id: Uuid,
    pub description: String,
    pub context_id: i64,
    pub vector: bool,
    pub detected: Option<bool>,
    pub corrected: bool,
    pub timestamp_start: i64,
    pub timestamp_end: Option<i64>,
    pub created_at: i64,
    pub code: u8, // NEW FIELD
}

impl TruthEvent {
    #[allow(dead_code)]
    pub fn new(description: impl Into<String>, context_id: i64, vector: bool, code: u8) -> Self {
        let now = Utc::now().timestamp();
        Self {
            id: Uuid::new_v4(),
            description: description.into(),
            context_id,
            vector,
            detected: None,
            corrected: false,
            timestamp_start: now,
            timestamp_end: None,
            created_at: now,
            code: if code == 0 { 1 } else { code },
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Impact {
    pub id: Uuid,
    pub event_id: Uuid,
    pub type_id: i64,
    pub positive: bool,
    pub notes: Option<String>,
    pub created_at: i64,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ProgressMetrics {
    pub timestamp: i64,
    pub total_events: i64,
    pub total_events_group: i64,
    pub total_positive_impact: f64,
    pub total_positive_impact_group: f64,
    pub total_negative_impact: f64,
    pub total_negative_impact_group: f64,
    pub trend: f64,
    pub trend_group: f64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_truth_event_new() {
        let event = TruthEvent::new(
            "Test event".to_string(),
            1,    // context_id
            true, // vector
            1,    // code default 1
        );

        assert_eq!(event.description, "Test event");
        assert!(event.vector);
        assert_eq!(event.detected, None); // должно быть None по умолчанию
        assert_eq!(event.corrected, false); // должно быть false по умолчанию
        assert!(event.timestamp_start <= Utc::now().timestamp());
        assert_eq!(event.code, 1);
    }
}