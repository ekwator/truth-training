// judgment submission service

use crate::CoreError;
// Database operations are handled in storage.rs
use chrono::Utc;
use uuid::Uuid;

use super::models::{Judgment};

#[derive(Debug)]
pub struct NewJudgmentInput {
	pub participant_id: Uuid,
	pub event_id: Uuid,
	pub assessment: String,
	pub confidence_level: f32,
	pub reasoning: Option<String>,
	pub signature: String,
}

pub fn submit_judgment(_db_path: &str, input: NewJudgmentInput) -> Result<Judgment, CoreError> {
	// Basic validation
	if !(0.0..=1.0).contains(&input.confidence_level) {
		return Err(CoreError::InvalidArg("confidence_level must be 0.0..1.0".into()));
	}
	let allowed = ["true", "false", "uncertain"];
	if !allowed.contains(&input.assessment.as_str()) {
		return Err(CoreError::InvalidArg("invalid assessment".into()));
	}
	// TODO: verify signature against participant's public key
	// Stub: create a judgment instance (DB integration later)
	let j = Judgment {
		id: Uuid::new_v4(),
		participant_id: input.participant_id,
		event_id: input.event_id,
		assessment: input.assessment,
		confidence_level: input.confidence_level,
		reasoning: input.reasoning,
		submitted_at: Utc::now(),
		signature: input.signature,
	};
	Ok(j)
}
