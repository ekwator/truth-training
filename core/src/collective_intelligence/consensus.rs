// consensus calculation service

use chrono::Utc;
use uuid::Uuid;
use crate::CoreError;
use super::models::{Consensus, Judgment};

pub struct ConsensusCalcInput<'a> {
	pub event_id: Uuid,
	pub judgments: &'a [Judgment],
	pub algorithm_version: &'a str,
}

pub fn calculate_consensus(input: ConsensusCalcInput) -> Result<Consensus, CoreError> {
	if input.judgments.is_empty() {
		return Err(CoreError::InvalidArg("no judgments provided".into()));
	}
	let participant_count = input.judgments.len() as u32;
	// Placeholder weighted logic: majority by assessment, confidence-averaged score
	let mut score_sum: f32 = 0.0;
	for j in input.judgments {
		let v = match j.assessment.as_str() {
			"true" => 1.0,
			"false" => 0.0,
			_ => 0.5,
		};
		score_sum += v * j.confidence_level.max(0.0).min(1.0);
	}
	let avg = score_sum / (participant_count as f32).max(1.0);
	let (consensus_value, confidence_score) = if avg >= 0.66 {
		("true".to_string(), avg)
	} else if avg <= 0.33 {
		("false".to_string(), 1.0 - avg)
	} else {
		("uncertain".to_string(), 1.0 - (avg - 0.5).abs() * 2.0)
	};
	Ok(Consensus {
		id: Uuid::new_v4(),
		event_id: input.event_id,
		consensus_value,
		confidence_score: confidence_score.clamp(0.0, 1.0),
		participant_count,
		calculated_at: Utc::now(),
		algorithm_version: input.algorithm_version.to_string(),
	})
}
