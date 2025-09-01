use serde::{Serialize, Deserialize};
use uuid::Uuid;

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
