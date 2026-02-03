# Prediction Accuracy Metrics View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view calculates prediction accuracy for impact predictions compared to actual outcomes, supporting participant reputation updates based on prediction accuracy as described in section 2.6.1.

## Purpose
The `prediction_accuracy_metrics` view evaluates how accurately participants predict future impacts and consequences, which directly affects their reputation scores and influence in the system. This view supports the participant reputation update algorithm by providing quantifiable measures of predictive accuracy.

## SQL Implementation

```sql
-- View to calculate prediction accuracy for impact predictions compared to actual outcomes
CREATE VIEW prediction_accuracy_metrics AS
SELECT 
    ip.id AS prediction_id,
    ip.event_id AS prediction_event_id,
    ip.predicted_impact_type,
    ip.expected_strength,
    ip.probability AS prediction_confidence,
    ip.horizon AS prediction_horizon,
    ip.created_at AS prediction_created_at,
    
    -- Actual outcome data
    te.collective_score AS actual_outcome,
    te.impact_score AS actual_impact,
    te.judgment_score AS actual_judgment,
    
    -- Prediction accuracy calculation
    CASE 
        WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) <= 
             (0.2 * ABS(ip.expected_strength) + 0.1) THEN 1  -- Within 20% tolerance + 0.1 epsilon
        ELSE 0
    END AS is_accurate_prediction,
    
    -- Accuracy score (0 to 1, where 1 is perfect accuracy)
    CASE 
        WHEN ip.expected_strength != 0 THEN
            1.0 - MIN(ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) / ABS(ip.expected_strength), 1.0)
        ELSE
            CASE 
                WHEN ABS(te.collective_score) < 0.1 THEN 1.0  -- Accurate if both are near zero
                ELSE 1.0 - ABS(te.collective_score)  -- Penalize for non-zero actual when expected was zero
            END
    END AS accuracy_score,
    
    -- Absolute error
    ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) AS absolute_error,
    
    -- Relative error
    CASE 
        WHEN ip.expected_strength != 0 THEN
            ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) / ABS(ip.expected_strength)
        ELSE
            ABS(te.collective_score)  -- Use absolute value when expected is zero
    END AS relative_error,
    
    -- Horizon-adjusted accuracy (earlier predictions weighted more heavily)
    CASE 
        WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) <= 
             (0.2 * ABS(ip.expected_strength) + 0.1) THEN 
            ip.horizon + 1.0  -- Add 1 to avoid zero weights
        ELSE 
            0
    END AS horizon_weighted_accuracy,
    
    -- Participant who made the prediction
    p.id AS participant_id,
    p.public_key AS participant_public_key,
    p.reputation_score AS participant_reputation_at_prediction,
    
    -- Event information
    te.description AS event_description,
    te.global_id AS event_global_id,
    te.created_at AS event_created_at,
    ec.status AS event_status,
    ec.event_type AS event_type,
    
    -- Temporal information
    julianday('now') - julianday(ip.created_at, 'unixepoch') AS days_since_prediction,
    julianday(te.created_at, 'unixepoch') - julianday(ip.created_at, 'unixepoch') AS prediction_lead_time,
    
    -- Confidence-weighted accuracy
    CASE 
        WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) <= 
             (0.2 * ABS(ip.expected_strength) + 0.1) THEN ip.probability
        ELSE 0
    END AS confidence_weighted_accuracy,
    
    -- Brier score (for probabilistic predictions)
    CASE 
        WHEN ip.probability IS NOT NULL AND te.collective_score IS NOT NULL THEN
            POWER(CASE 
                WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) <= 
                     (0.2 * ABS(ip.expected_strength) + 0.1) THEN 0.0
                ELSE 1.0
            END - ip.probability, 2)
        ELSE NULL
    END AS brier_score,
    
    -- Calibration measure
    CASE 
        WHEN ip.probability IS NOT NULL AND te.collective_score IS NOT NULL THEN
            CASE 
                WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0)) <= 
                     (0.2 * ABS(ip.expected_strength) + 0.1) THEN ip.probability
                ELSE (1.0 - ip.probability)
            END
        ELSE NULL
    END AS calibration_score,
    
    CURRENT_TIMESTAMP AS calculated_at

FROM impact_predictions ip
JOIN truth_event te ON ip.event_id = (
    SELECT event_ci.created_by 
    FROM event_ci 
    WHERE event_ci.id = ip.event_id
)
JOIN participants p ON te.participant_id = p.id
JOIN event_ci ec ON te.id = ec.created_by;

-- View for participant-level prediction accuracy summaries
CREATE VIEW participant_prediction_accuracy_summary AS
SELECT 
    pam.participant_id,
    pam.participant_public_key,
    
    -- Overall prediction accuracy
    COUNT(*) AS total_predictions,
    SUM(pam.is_accurate_prediction) AS accurate_predictions,
    AVG(pam.accuracy_score) AS average_accuracy_score,
    SUM(pam.is_accurate_prediction) * 1.0 / COUNT(*) AS overall_accuracy_rate,
    
    -- Horizon-weighted accuracy
    SUM(pam.horizon_weighted_accuracy) AS total_horizon_weighted_accuracy,
    SUM(pam.horizon_weighted_accuracy) / COUNT(*) AS average_horizon_weighted_accuracy,
    
    -- Confidence-weighted accuracy
    SUM(pam.confidence_weighted_accuracy) AS total_confidence_weighted_accuracy,
    SUM(pam.confidence_weighted_accuracy) / COUNT(*) AS average_confidence_weighted_accuracy,
    
    -- Error metrics
    AVG(pam.absolute_error) AS mean_absolute_error,
    AVG(pam.relative_error) AS mean_relative_error,
    SQRT(AVG(POWER(pam.absolute_error, 2))) AS rmse,  -- Root mean square error
    
    -- Temporal metrics
    AVG(pam.days_since_prediction) AS avg_days_since_prediction,
    AVG(pam.prediction_lead_time) AS avg_prediction_lead_time,
    
    -- Brier score metrics (lower is better)
    AVG(pam.brier_score) AS average_brier_score,
    AVG(pam.calibration_score) AS average_calibration_score,
    
    -- Performance by time buckets
    SUM(CASE WHEN pam.days_since_prediction <= 7 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.days_since_prediction <= 7 THEN 1 ELSE 0 END), 0) AS accuracy_short_term,
    SUM(CASE WHEN pam.days_since_prediction > 7 AND pam.days_since_prediction <= 30 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.days_since_prediction > 7 AND pam.days_since_prediction <= 30 THEN 1 ELSE 0 END), 0) AS accuracy_medium_term,
    SUM(CASE WHEN pam.days_since_prediction > 30 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.days_since_prediction > 30 THEN 1 ELSE 0 END), 0) AS accuracy_long_term,
    
    -- Performance by prediction horizon
    SUM(CASE WHEN pam.prediction_horizon <= 10 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_horizon <= 10 THEN 1 ELSE 0 END), 0) AS accuracy_near_term_predictions,
    SUM(CASE WHEN pam.prediction_horizon > 10 AND pam.prediction_horizon <= 50 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_horizon > 10 AND pam.prediction_horizon <= 50 THEN 1 ELSE 0 END), 0) AS accuracy_medium_term_predictions,
    SUM(CASE WHEN pam.prediction_horizon > 50 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_horizon > 50 THEN 1 ELSE 0 END), 0) AS accuracy_long_term_predictions,
    
    -- Performance by confidence level
    SUM(CASE WHEN pam.prediction_confidence >= 0.8 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_confidence >= 0.8 THEN 1 ELSE 0 END), 0) AS accuracy_high_confidence,
    SUM(CASE WHEN pam.prediction_confidence >= 0.5 AND pam.prediction_confidence < 0.8 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_confidence >= 0.5 AND pam.prediction_confidence < 0.8 THEN 1 ELSE 0 END), 0) AS accuracy_medium_confidence,
    SUM(CASE WHEN pam.prediction_confidence < 0.5 THEN pam.is_accurate_prediction ELSE 0 END) * 1.0 / 
    NULLIF(SUM(CASE WHEN pam.prediction_confidence < 0.5 THEN 1 ELSE 0 END), 0) AS accuracy_low_confidence,
    
    -- Ranking metrics
    PERCENT_RANK() OVER (ORDER BY AVG(pam.accuracy_score) DESC) AS accuracy_percentile,
    
    CURRENT_TIMESTAMP AS summary_calculated_at

FROM prediction_accuracy_metrics pam
GROUP BY pam.participant_id, pam.participant_public_key
ORDER BY AVG(pam.accuracy_score) DESC;

-- View for updating participant reputation based on prediction accuracy
CREATE VIEW prediction_based_reputation_update AS
SELECT 
    ppa.participant_id,
    ppa.participant_public_key,
    
    -- Current reputation
    p.reputation_score AS current_reputation,
    
    -- Prediction-based reputation adjustment
    (
        SELECT 
            (p.accurate_impact + SUM(pam.is_accurate_prediction)) * 1.0 / 
            (p.total_impact + COUNT(pam.prediction_id))
        FROM prediction_accuracy_metrics pam
        WHERE pam.participant_id = ppa.participant_id
    ) AS potential_reputation_with_predictions,
    
    -- Weighted by horizon (longer-term predictions weighted more)
    (
        SELECT 
            SUM(pam.horizon_weighted_accuracy) / COUNT(pam.prediction_id)
        FROM prediction_accuracy_metrics pam
        WHERE pam.participant_id = ppa.participant_id
    ) AS horizon_weighted_reputation_component,
    
    -- Weighted by confidence (more confident accurate predictions weighted more)
    (
        SELECT 
            SUM(pam.confidence_weighted_accuracy) / COUNT(pam.prediction_id)
        FROM prediction_accuracy_metrics pam
        WHERE pam.participant_id = ppa.participant_id
    ) AS confidence_weighted_reputation_component,
    
    -- Adjusted reputation incorporating prediction accuracy
    (
        p.reputation_score * 0.7 +  -- Maintain 70% of existing reputation
        (ppa.overall_accuracy_rate * 0.2) +  -- Add 20% from prediction accuracy
        (ppa.average_horizon_weighted_accuracy * 0.1)  -- Add 10% from horizon-weighted accuracy
    ) AS reputation_with_prediction_adjustment,
    
    -- Amount of reputation change
    (
        (p.reputation_score * 0.7 + 
         (ppa.overall_accuracy_rate * 0.2) + 
         (ppa.average_horizon_weighted_accuracy * 0.1)) - p.reputation_score
    ) AS reputation_change_from_predictions,
    
    -- Number of predictions used for this calculation
    ppa.total_predictions,
    
    -- Threshold check for significant prediction history
    CASE 
        WHEN ppa.total_predictions >= 10 THEN 1  -- Has sufficient prediction history
        ELSE 0  -- Insufficient prediction history
    END AS has_significant_prediction_history,
    
    ppa.overall_accuracy_rate,
    ppa.average_accuracy_score,
    ppa.mean_absolute_error,
    
    p.total_impact AS total_impact_assessments,
    p.accurate_impact AS accurate_impact_assessments,
    
    CURRENT_TIMESTAMP AS update_recommendation_time

FROM participant_prediction_accuracy_summary ppa
JOIN participants p ON ppa.participant_id = p.id
WHERE ppa.total_predictions > 0;  -- Only include participants with predictions

-- View for prediction leaderboard
CREATE VIEW prediction_leaderboard AS
SELECT 
    participant_id,
    participant_public_key,
    overall_accuracy_rate,
    average_accuracy_score,
    total_predictions,
    accurate_predictions,
    mean_absolute_error,
    rmse,
    average_brier_score,
    ROW_NUMBER() OVER (ORDER BY overall_accuracy_rate DESC, total_predictions DESC) AS accuracy_rank,
    ROW_NUMBER() OVER (ORDER BY total_predictions DESC, overall_accuracy_rate DESC) AS volume_rank,
    CASE 
        WHEN overall_accuracy_rate >= 0.8 THEN 'Expert'
        WHEN overall_accuracy_rate >= 0.7 THEN 'Advanced'
        WHEN overall_accuracy_rate >= 0.6 THEN 'Intermediate'
        WHEN overall_accuracy_rate >= 0.5 THEN 'Beginner'
        ELSE 'Novice'
    END AS prediction_skill_level
FROM participant_prediction_accuracy_summary
WHERE total_predictions >= 5  -- Only rank participants with at least 5 predictions
ORDER BY overall_accuracy_rate DESC, total_predictions DESC;
```

## Key Features

### Comprehensive Accuracy Metrics
The view calculates multiple accuracy metrics:
- Binary accuracy (correct/incorrect prediction)
- Continuous accuracy score (0-1 scale)
- Absolute and relative error measures
- Horizon-weighted accuracy (longer-term predictions valued more)
- Confidence-weighted accuracy

### Temporal Analysis
Analyzes prediction accuracy across different time horizons and tracks how accuracy varies over time.

### Confidence Integration
Incorporates prediction confidence levels into accuracy calculations and evaluates calibration.

### Participant Reputation Linkage
Directly connects prediction accuracy to participant reputation updates, supporting the reputation system described in the model.

### Statistical Measures
Provides various statistical measures including Brier scores, RMSE, and other forecasting accuracy metrics.

### Leaderboard Functionality
Creates rankings of participants based on their prediction accuracy for competitive or incentive purposes.

## Relationship to Model Core
This view implements the prediction accuracy components of the model where:
- Participant reputation is updated based on prediction accuracy
- Predictions made earlier (with larger horizon values) receive greater weight in reputation calculations
- Prediction accuracy is determined by comparing expected strength with actual collective scores
- The system tracks prediction performance over time to adjust participant influence

## Usage Examples

```sql
-- Get prediction accuracy for a specific prediction
SELECT * FROM prediction_accuracy_metrics WHERE prediction_id = ?;

-- Get summary of prediction accuracy for a participant
SELECT * FROM participant_prediction_accuracy_summary WHERE participant_id = ?;

-- Get recommendation for reputation update based on predictions
SELECT * FROM prediction_based_reputation_update WHERE participant_id = ?;

-- View the prediction leaderboard
SELECT * FROM prediction_leaderboard LIMIT 10;

-- Find participants with high prediction accuracy
SELECT * FROM participant_prediction_accuracy_summary 
WHERE overall_accuracy_rate > 0.7 AND total_predictions >= 10;
```

## Integration with Other Components
- Connects to `impact_predictions` and `truth_event` for prediction vs. outcome comparison
- Updates `participants` table through reputation calculations
- Supports `reputation_history` by providing accuracy data for reputation updates
- Feeds into `consensus_ci` by providing participant reliability scores
- Used in `event_stability` analysis to evaluate prediction quality

## Notes
- The view includes tolerance thresholds to account for natural variance in predictions
- Horizon weighting rewards participants who can predict longer-term outcomes
- The system handles cases where expected values are zero separately
- Multiple accuracy measures are provided to give a comprehensive view of prediction quality