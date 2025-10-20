use truth_core::collective_intelligence::models::Judgment;
use truth_core::collective_intelligence::consensus::{calculate_consensus, ConsensusCalcInput};
use chrono::Utc;
use uuid::Uuid;

#[test]
fn consensus_basic_true() {
    let ev = Uuid::new_v4();
    let judgments = vec![
        Judgment {
            id: Uuid::new_v4(),
            participant_id: Uuid::new_v4(),
            event_id: ev,
            assessment: "true".into(),
            confidence_level: 1.0,
            reasoning: None,
            submitted_at: Utc::now(),
            signature: String::new(),
        },
        Judgment {
            id: Uuid::new_v4(),
            participant_id: Uuid::new_v4(),
            event_id: ev,
            assessment: "true".into(),
            confidence_level: 0.8,
            reasoning: None,
            submitted_at: Utc::now(),
            signature: String::new(),
        },
    ];

    let out = calculate_consensus(ConsensusCalcInput { event_id: ev, judgments: &judgments, algorithm_version: "1.0.0" }).unwrap();
    assert_eq!(out.consensus_value, "true");
    assert!(out.confidence_score >= 0.5);
    assert_eq!(out.participant_count, judgments.len() as u32);
}

#[test]
fn consensus_performance_under_100ms() {
    let ev = Uuid::new_v4();
    // 100 synthetic judgments leaning true
    let mut js: Vec<Judgment> = Vec::with_capacity(100);
    for _ in 0..100 {
        js.push(Judgment {
            id: Uuid::new_v4(),
            participant_id: Uuid::new_v4(),
            event_id: ev,
            assessment: "true".into(),
            confidence_level: 0.75,
            reasoning: None,
            submitted_at: Utc::now(),
            signature: String::new(),
        });
    }
    let start = std::time::Instant::now();
    let out = calculate_consensus(ConsensusCalcInput { event_id: ev, judgments: &js, algorithm_version: "1.0.0" }).unwrap();
    let elapsed = start.elapsed();
    assert_eq!(out.consensus_value, "true");
    assert!(elapsed.as_millis() < 100, "consensus took {:?}", elapsed);
}


