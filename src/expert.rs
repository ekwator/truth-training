use serde::{Serialize, Deserialize};
use std::collections::HashMap;

#[derive(Debug, Serialize, Deserialize)]
pub struct AssessmentQuestion {
    pub id: &'static str,
    pub text: &'static str,
}

pub fn default_questions() -> Vec<AssessmentQuestion> {
    vec![
        AssessmentQuestion { id: "src_independent", text: "Source independent from interested parties?" },
        AssessmentQuestion { id: "alt_hypothesis", text: "Are alternative hypotheses possible?" },
        AssessmentQuestion { id: "incentives", text: "Does the source have incentives to deceive?" },
        AssessmentQuestion { id: "reproducible", text: "Is the event reproducible or verifiable?" },
        AssessmentQuestion { id: "logs_evidence", text: "Are there logs/evidence (photo, video, docs)?" },
        AssessmentQuestion { id: "belief_pressure", text: "Is there pressure on audience (propaganda)?" },
        AssessmentQuestion { id: "time_distance", text: "Enough time passed for verification?" },
    ]
}

#[derive(Debug, Serialize, Deserialize)]
pub struct AssessmentAnswers(pub HashMap<String, String>); // "yes"/"no"/"unknown"

#[derive(Debug, Serialize, Deserialize)]
pub struct AssessmentResult {
    pub trust_score: f64, // 0..1
    pub hint: String,
}

pub fn assess_answers(ans: &AssessmentAnswers) -> AssessmentResult {
    let mut score: i32 = 0;
    for v in ans.0.values() {
        match v.as_str() {
            "yes" => score += 1,
            "no" => score -= 1,
            _ => {}
        }
    }
    let n = ans.0.len() as f64;
    let trust = ((score as f64) / (n.max(1.0) * 2.0) + 0.5).clamp(0.0,1.0);
    let hint = if trust > 0.7 {
        "High confidence: evidence and independence look good.".to_string()
    } else if trust < 0.3 {
        "Low confidence: consider verifying sources and incentives.".to_string()
    } else {
        "Medium confidence: partial evidence — seek corroboration.".to_string()
    };
    AssessmentResult { trust_score: trust, hint }
}
