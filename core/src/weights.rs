

/// Compute status weight from confirmations/rejections.
/// confirm = +1.0, reject = -1.0, abstain = 0.0. Result clamped to [-1, 1].
pub fn compute_status_weight(confirms: u32, rejects: u32, abstains: u32) -> f32 {
    let total = confirms + rejects + abstains;
    if total == 0 { return 0.0; }
    let score = (confirms as f32) - (rejects as f32);
    let denom = (confirms + rejects).max(1) as f32;
    (score / denom).clamp(-1.0, 1.0)
}

/// Compute inconsistency decay score based on proportion of conflicting signals.
/// Higher when confirms and rejects are both present. Range [0, 1].
pub fn compute_decay_score(confirms: u32, rejects: u32) -> f32 {
    let total = confirms + rejects;
    if total == 0 { return 0.0; }
    let min_side = confirms.min(rejects) as f32;
    (min_side / (total as f32)).clamp(0.0, 1.0)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn status_weight_basic() {
        assert!((compute_status_weight(10, 0, 0) - 1.0).abs() < 1e-6);
        assert!((compute_status_weight(0, 10, 0) - (-1.0)).abs() < 1e-6);
        assert!((compute_status_weight(5, 5, 0) - 0.0).abs() < 1e-6);
        assert!((compute_status_weight(0, 0, 10) - 0.0).abs() < 1e-6);
    }

    #[test]
    fn decay_score_conflict_ratio() {
        assert!((compute_decay_score(0, 0) - 0.0).abs() < 1e-6);
        assert!((compute_decay_score(10, 0) - 0.0).abs() < 1e-6);
        assert!((compute_decay_score(5, 5) - 0.5).abs() < 1e-6);
        assert!((compute_decay_score(2, 8) - 0.2).abs() < 1e-6);
    }
}


