// reputation management service

use chrono::Utc;
use uuid::Uuid;
use crate::CoreError;
use super::models::{ReputationHistory};

pub struct ReputationUpdateInput {
	pub participant_id: Uuid,
	pub previous_reputation: f32, // 0..1
	pub judgment_accuracy: f32,   // 0..1 result of last evaluation
	pub alpha: f32,               // EMA smoothing factor (0..1)
	pub event_id: Option<Uuid>,
	pub reason: String,
}

pub struct ReputationUpdateResult {
	pub new_reputation: f32,
	pub history: ReputationHistory,
}

pub fn update_reputation(input: ReputationUpdateInput) -> Result<ReputationUpdateResult, CoreError> {
	if !(0.0..=1.0).contains(&input.previous_reputation) {
		return Err(CoreError::InvalidArg("previous_reputation must be 0..1".into()));
	}
	if !(0.0..=1.0).contains(&input.judgment_accuracy) {
		return Err(CoreError::InvalidArg("judgment_accuracy must be 0..1".into()));
	}
	let a = input.alpha.clamp(0.0, 1.0);
	let new_rep = a * input.judgment_accuracy + (1.0 - a) * input.previous_reputation;
	let hist = ReputationHistory {
		id: Uuid::new_v4(),
		participant_id: input.participant_id,
		old_reputation: input.previous_reputation,
		new_reputation: new_rep,
		change_reason: input.reason,
		event_id: input.event_id,
		updated_at: Utc::now(),
	};
	Ok(ReputationUpdateResult { new_reputation: new_rep, history: hist })
}
