// models for collective intelligence entities

use serde::{Deserialize, Serialize};
use uuid::Uuid;
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Participant {
	pub id: Uuid,
	pub public_key: String, // ed25519 public key (base64 or hex)
	pub reputation_score: f32, // 0.0 .. 1.0
	pub total_judgments: u32,
	pub accurate_judgments: u32,
	pub created_at: DateTime<Utc>,
	pub last_activity: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Event {
	pub id: Uuid,
	pub title: String,
	pub description: Option<String>,
	pub event_type: String, // e.g., "fact_check", "prediction", "assessment"
	pub created_by: Uuid,   // Participant.id
	pub created_at: DateTime<Utc>,
	pub status: String,     // "active" | "resolved" | "archived"
	pub resolution_data: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Judgment {
	pub id: Uuid,
	pub participant_id: Uuid,
	pub event_id: Uuid,
	pub assessment: String,     // "true" | "false" | "uncertain" (per spec)
	pub confidence_level: f32,  // 0.0 .. 1.0
	pub reasoning: Option<String>,
	pub submitted_at: DateTime<Utc>,
	pub signature: String,      // ed25519 signature (base64 or hex)
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Consensus {
	pub id: Uuid,
	pub event_id: Uuid,
	pub consensus_value: String,
	pub confidence_score: f32,   // 0.0 .. 1.0
	pub participant_count: u32,
	pub calculated_at: DateTime<Utc>,
	pub algorithm_version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReputationHistory {
	pub id: Uuid,
	pub participant_id: Uuid,
	pub old_reputation: f32, // 0.0 .. 1.0
	pub new_reputation: f32, // 0.0 .. 1.0
	pub change_reason: String,
	pub event_id: Option<Uuid>,
	pub updated_at: DateTime<Utc>,
}
