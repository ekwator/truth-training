use truth_core::collective_intelligence::reputation::{update_reputation, ReputationUpdateInput};
use uuid::Uuid;

#[test]
fn ema_updates_reputation() {
    let out = update_reputation(ReputationUpdateInput {
        participant_id: Uuid::new_v4(),
        previous_reputation: 0.5,
        judgment_accuracy: 1.0,
        alpha: 0.2,
        event_id: None,
        reason: "test".into(),
    }).unwrap();
    assert!(out.new_reputation > 0.5);
}


