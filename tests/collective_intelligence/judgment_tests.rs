use truth_core::collective_intelligence::judgment::{submit_judgment, NewJudgmentInput};
use uuid::Uuid;

#[test]
fn invalid_assessment_rejected() {
    let r = submit_judgment(
        ":memory:",
        NewJudgmentInput {
            participant_id: Uuid::new_v4(),
            event_id: Uuid::new_v4(),
            assessment: "bad".into(),
            confidence_level: 0.5,
            reasoning: None,
            signature: String::new(),
        },
    );
    assert!(r.is_err());
}


