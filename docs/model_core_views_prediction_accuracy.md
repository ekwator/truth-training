-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Prediction Accuracy Metrics Compared to Actual Outcomes  

-- Calculates prediction accuracy for impact predictions compared to actual outcomes
-- Supports the participant reputation updates based on prediction accuracy as described in section 2.6.1
-- Links: impact_predictions.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE VIEW prediction_accuracy_metrics AS
SELECT
    ip.id as prediction_id,
    ip.event_id,
    ip.expected_strength,
    ip.probability as predicted_probability,
    ip.horizon as prediction_horizon,
    -- Get the actual outcome from truth_event
    te.collective_score as actual_outcome,
    -- Calculate prediction accuracy
    ABS(ip.expected_strength - te.collective_score) as prediction_error,
    -- Determine if prediction was accurate (within 20% tolerance)
    CASE
        WHEN ABS(ip.expected_strength - te.collective_score) <= GREATEST(0.2 * ip.expected_strength, 0.1)
        THEN 1
        ELSE 0
    END as is_accurate,
    -- Calculate accuracy score (inverse of error, normalized)
    1.0 - MIN(ABS(ip.expected_strength - te.collective_score), 1.0) as accuracy_score,
    -- Weight by horizon (earlier predictions have higher weight)
    (1.0 - MIN(ABS(ip.expected_strength - te.collective_score), 1.0)) * (ip.horizon + 1.0) as weighted_accuracy,
    -- Timestamp
    ip.created_at as prediction_time,
    te.created_at as actual_time
FROM impact_predictions ip
JOIN event_ci ec ON ip.event_id = ec.id
JOIN truth_event te ON ec.created_by = te.id;
```

-- View for calculating participant prediction accuracy
-- This view aggregates prediction accuracy by participant to support reputation updates
```sql
CREATE VIEW participant_prediction_accuracy AS
SELECT
    te.participant_id,
    p.public_key,
    COUNT(pa.prediction_id) as total_predictions,
    SUM(pa.is_accurate) as accurate_predictions,
    -- Overall accuracy rate
    CASE
        WHEN COUNT(pa.prediction_id) > 0
        THEN SUM(pa.is_accurate) * 1.0 / COUNT(pa.prediction_id)
        ELSE 0.0
    END as prediction_accuracy_rate,
    -- Average accuracy score
    AVG(pa.accuracy_score) as avg_accuracy_score,
    -- Weighted accuracy considering horizon
    CASE
        WHEN SUM(pa.weighted_accuracy) > 0
        THEN SUM(pa.weighted_accuracy) / COUNT(pa.prediction_id)
        ELSE 0.0
    END as weighted_accuracy_rate,
    -- Average prediction error
    AVG(pa.prediction_error) as avg_prediction_error
FROM truth_event te
JOIN event_ci ec ON te.id = ec.created_by
JOIN impact_predictions ip ON ec.id = ip.event_id
JOIN participants p ON te.participant_id = p.id
JOIN prediction_accuracy_metrics pa ON ip.id = pa.prediction_id
GROUP BY te.participant_id, p.public_key;
```

-- View for calculating prediction accuracy by time horizon
-- This view analyzes how prediction accuracy varies with the prediction horizon
```sql
CREATE VIEW prediction_accuracy_by_horizon AS
SELECT
    CASE
        WHEN ip.horizon < 1 THEN 'short_term'
        WHEN ip.horizon BETWEEN 1 AND 7 THEN 'medium_term'
        WHEN ip.horizon BETWEEN 7 AND 30 THEN 'long_term'
        ELSE 'very_long_term'
    END as horizon_category,
    COUNT(*) as total_predictions,
    SUM(pa.is_accurate) as accurate_predictions,
    -- Accuracy rate by horizon
    CASE
        WHEN COUNT(*) > 0
        THEN SUM(pa.is_accurate) * 1.0 / COUNT(*)
        ELSE 0.0
    END as accuracy_rate,
    -- Average error by horizon
    AVG(pa.prediction_error) as avg_error,
    -- Average horizon in category
    AVG(ip.horizon) as avg_horizon
FROM impact_predictions ip
JOIN event_ci ec ON ip.event_id = ec.id
JOIN truth_event te ON ec.created_by = te.id
JOIN prediction_accuracy_metrics pa ON ip.id = pa.prediction_id
GROUP BY 
    CASE
        WHEN ip.horizon < 1 THEN 'short_term'
        WHEN ip.horizon BETWEEN 1 AND 7 THEN 'medium_term'
        WHEN ip.horizon BETWEEN 7 AND 30 THEN 'long_term'
        ELSE 'very_long_term'
    END;
```

-- View for calculating prediction accuracy trends over time
-- This view tracks how prediction accuracy changes over time for analysis
```sql
CREATE VIEW prediction_accuracy_trends AS
SELECT
    -- Group by week
    (pa.prediction_time / 604800) * 604800 as week_start_timestamp,
    COUNT(*) as predictions_this_week,
    SUM(pa.is_accurate) as accurate_predictions_this_week,
    -- Weekly accuracy rate
    CASE
        WHEN COUNT(*) > 0
        THEN SUM(pa.is_accurate) * 1.0 / COUNT(*)
        ELSE 0.0
    END as weekly_accuracy_rate,
    AVG(pa.prediction_error) as avg_error_this_week
FROM prediction_accuracy_metrics pa
GROUP BY (pa.prediction_time / 604800) * 604800
ORDER BY week_start_timestamp DESC;