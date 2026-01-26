-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Analyzing the Accuracy of Impact Predictions Based on Their Temporal Horizon  

-- Analyzes the accuracy of impact predictions based on their temporal horizon to improve prediction models
-- Links: impact_predictions.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```sql
CREATE VIEW prediction_horizon_analysis AS
SELECT
    ip.id as prediction_id,
    ip.event_id,
    ip.predicted_impact_type,
    ip.expected_strength,
    ip.probability as predicted_probability,
    ip.horizon as prediction_horizon,
    ip.created_at as prediction_time,
    -- Get the actual outcome
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
    -- Time between prediction and outcome
    (SELECT te2.created_at FROM truth_event te2 WHERE te2.id = (SELECT created_by FROM event_ci WHERE id = ip.event_id)) - ip.created_at as time_to_outcome,
    -- Categorize horizon length
    CASE
        WHEN ip.horizon < 1 THEN 'SHORT_TERM'      -- Less than 1 day
        WHEN ip.horizon < 7 THEN 'MEDIUM_TERM'     -- 1-7 days
        WHEN ip.horizon < 30 THEN 'LONG_TERM'      -- 1-30 days
        ELSE 'VERY_LONG_TERM'                       -- More than 30 days
    END as horizon_category,
    -- Participant who made the prediction
    te.participant_id as predictor_id,
    p.public_key as predictor_public_key,
    p.reputation_score as predictor_reputation,
    -- Prediction confidence based on predictor reputation
    ip.probability * p.reputation_score as weighted_confidence,
    -- Prediction utility (accuracy * confidence * horizon factor)
    (1.0 - MIN(ABS(ip.expected_strength - te.collective_score), 1.0)) * 
    (ip.probability * p.reputation_score) *
    CASE
        WHEN ip.horizon < 1 THEN 1.0              -- Short term: full weight
        WHEN ip.horizon < 7 THEN 0.8              -- Medium term: slight discount
        WHEN ip.horizon < 30 THEN 0.6             -- Long term: moderate discount
        ELSE 0.4                                  -- Very long term: heavy discount
    END as prediction_utility,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM impact_predictions ip
JOIN event_ci ec ON ip.event_id = ec.id
JOIN truth_event te ON ec.created_by = te.id
JOIN participants p ON te.participant_id = p.id;
```

-- View for analyzing prediction accuracy by horizon
-- This view examines how prediction accuracy varies with temporal distance
```sql
CREATE VIEW prediction_accuracy_by_horizon AS
SELECT
    pha.horizon_category,
    COUNT(*) as total_predictions,
    SUM(pha.is_accurate) as accurate_predictions,
    -- Accuracy rate by horizon
    CASE
        WHEN COUNT(*) > 0
        THEN SUM(pha.is_accurate) * 1.0 / COUNT(*)
        ELSE 0.0
    END as accuracy_rate,
    -- Average error by horizon
    AVG(pha.prediction_error) as avg_error,
    -- Average prediction utility by horizon
    AVG(pha.prediction_utility) as avg_utility,
    -- Standard deviation of errors by horizon
    SQRT(AVG(POWER(pha.prediction_error - avg_err, 2))) as error_std_deviation,
    -- Confidence interval for accuracy rate (normal approximation)
    CASE
        WHEN COUNT(*) > 0
        THEN SUM(pha.is_accurate) * 1.0 / COUNT(*) - 1.96 * SQRT((SUM(pha.is_accurate) * 1.0 / COUNT(*)) * (1 - SUM(pha.is_accurate) * 1.0 / COUNT(*)) / COUNT(*))
        ELSE 0.0
    END as accuracy_lower_bound,
    CASE
        WHEN COUNT(*) > 0
        THEN SUM(pha.is_accurate) * 1.0 / COUNT(*) + 1.96 * SQRT((SUM(pha.is_accurate) * 1.0 / COUNT(*)) * (1 - SUM(pha.is_accurate) * 1.0 / COUNT(*)) / COUNT(*))
        ELSE 0.0
    END as accuracy_upper_bound,
    -- Sample size for reliability
    COUNT(*) as sample_size,
    -- Reliability of the estimate
    CASE
        WHEN COUNT(*) >= 100 THEN 'HIGH'
        WHEN COUNT(*) >= 30 THEN 'MEDIUM'
        WHEN COUNT(*) >= 10 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as reliability_level
FROM prediction_horizon_analysis pha
CROSS JOIN (SELECT AVG(prediction_error) as avg_err FROM prediction_horizon_analysis WHERE horizon_category = pha.horizon_category) avg_subq
GROUP BY pha.horizon_category, avg_err
ORDER BY 
    CASE pha.horizon_category
        WHEN 'SHORT_TERM' THEN 1
        WHEN 'MEDIUM_TERM' THEN 2
        WHEN 'LONG_TERM' THEN 3
        WHEN 'VERY_LONG_TERM' THEN 4
        ELSE 5
    END;
```

-- View for analyzing individual predictor performance across horizons
-- This view evaluates how well different participants predict across different time horizons
```sql
CREATE VIEW predictor_performance_by_horizon AS
SELECT
    pha.predictor_id,
    pha.predictor_public_key,
    pha.predictor_reputation,
    pha.horizon_category,
    COUNT(*) as predictions_in_horizon,
    AVG(pha.accuracy_score) as avg_accuracy_in_horizon,
    AVG(pha.prediction_error) as avg_error_in_horizon,
    AVG(pha.prediction_utility) as avg_utility_in_horizon,
    -- Rank predictors within each horizon
    ROW_NUMBER() OVER (
        PARTITION BY pha.horizon_category 
        ORDER BY AVG(pha.accuracy_score) DESC
    ) as accuracy_rank_in_horizon,
    -- Overall rank across all horizons
    ROW_NUMBER() OVER (
        ORDER BY AVG(pha.accuracy_score) DESC
    ) as overall_accuracy_rank,
    -- Performance category
    CASE
        WHEN AVG(pha.accuracy_score) >= 0.8 THEN 'EXCELLENT'
        WHEN AVG(pha.accuracy_score) >= 0.7 THEN 'VERY_GOOD'
        WHEN AVG(pha.accuracy_score) >= 0.6 THEN 'GOOD'
        WHEN AVG(pha.accuracy_score) >= 0.5 THEN 'FAIR'
        WHEN AVG(pha.accuracy_score) >= 0.4 THEN 'POOR'
        ELSE 'VERY_POOR'
    END as performance_category,
    -- Consistency across horizons (how similarly they perform across different horizons)
    (SELECT AVG(ABS(ppbh2.avg_accuracy_in_horizon - AVG(pha.accuracy_score)))
     FROM predictor_performance_by_horizon ppbh2
     WHERE ppbh2.predictor_id = pha.predictor_id) as consistency_score,
    -- Specialization indicator (do they perform better in specific horizons?)
    CASE
        WHEN (SELECT MAX(avg_accuracy_in_horizon) FROM predictor_performance_by_horizon ppbh3 WHERE ppbh3.predictor_id = pha.predictor_id) - 
             (SELECT MIN(avg_accuracy_in_horizon) FROM predictor_performance_by_horizon ppbh4 WHERE ppbh4.predictor_id = pha.predictor_id) > 0.2
        THEN 'SPECIALIZED'
        ELSE 'GENERAL'
    END as prediction_style,
    -- Confidence in performance estimate
    CASE
        WHEN COUNT(*) >= 50 THEN 'HIGH'
        WHEN COUNT(*) >= 20 THEN 'MEDIUM'
        WHEN COUNT(*) >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as confidence_level
FROM prediction_horizon_analysis pha
GROUP BY pha.predictor_id, pha.predictor_public_key, pha.predictor_reputation, pha.horizon_category
HAVING COUNT(*) >= 3  -- Minimum sample size
ORDER BY pha.predictor_id, 
    CASE pha.horizon_category
        WHEN 'SHORT_TERM' THEN 1
        WHEN 'MEDIUM_TERM' THEN 2
        WHEN 'LONG_TERM' THEN 3
        WHEN 'VERY_LONG_TERM' THEN 4
        ELSE 5
    END;
```

-- View for identifying optimal prediction horizons
-- This view determines which temporal distances yield the most accurate predictions
```sql
CREATE VIEW optimal_prediction_horizon_identification AS
SELECT
    pabh.horizon_category,
    pabh.accuracy_rate,
    pabh.avg_error,
    pabh.avg_utility,
    pabh.error_std_deviation,
    pabh.sample_size,
    pabh.reliability_level,
    -- Efficiency ratio (accuracy per unit time)
    CASE
        WHEN pabh.avg_error > 0 THEN pabh.accuracy_rate / pabh.avg_error
        ELSE pabh.accuracy_rate
    END as efficiency_ratio,
    -- Rank by efficiency
    ROW_NUMBER() OVER (ORDER BY CASE WHEN pabh.avg_error > 0 THEN pabh.accuracy_rate / pabh.avg_error ELSE pabh.accuracy_rate END DESC) as efficiency_rank,
    -- Optimal horizon classification
    CASE
        WHEN pabh.accuracy_rate >= 0.7 AND pabh.reliability_level IN ('HIGH', 'MEDIUM') THEN 'HIGHLY_OPTIMAL'
        WHEN pabh.accuracy_rate >= 0.6 AND pabh.reliability_level IN ('HIGH', 'MEDIUM') THEN 'MODERATELY_OPTIMAL'
        WHEN pabh.accuracy_rate >= 0.5 AND pabh.reliability_level = 'HIGH' THEN 'MINIMALLY_OPTIMAL'
        WHEN pabh.accuracy_rate < 0.4 THEN 'SUBOPTIMAL'
        ELSE 'UNCERTAIN'
    END as optimality_classification,
    -- Recommended usage
    CASE
        WHEN pabh.accuracy_rate >= 0.7 AND pabh.reliability_level IN ('HIGH', 'MEDIUM') THEN 'PREFER_FOR_PREDICTIONS'
        WHEN pabh.accuracy_rate >= 0.6 AND pabh.reliability_level = 'HIGH' THEN 'USE_WITH_CAUTION'
        WHEN pabh.accuracy_rate < 0.4 THEN 'AVOID_FOR_CRITICAL_PREDICTIONS'
        ELSE 'EVALUATE_CONTINUOUSLY'
    END as usage_recommendation,
    -- Improvement potential
    CASE
        WHEN pabh.accuracy_rate < 0.5 THEN 'SIGNIFICANT_IMPROVEMENT_NEEDED'
        WHEN pabh.accuracy_rate < 0.6 THEN 'MODERATE_IMPROVEMENT_NEEDED'
        WHEN pabh.accuracy_rate < 0.7 THEN 'MINIMAL_IMPROVEMENT_NEEDED'
        ELSE 'OPTIMAL_PERFORMANCE'
    END as improvement_potential,
    -- Confidence-adjusted utility
    pabh.avg_utility * CASE 
        WHEN pabh.reliability_level = 'HIGH' THEN 1.0
        WHEN pabh.reliability_level = 'MEDIUM' THEN 0.8
        WHEN pabh.reliability_level = 'LOW' THEN 0.6
        ELSE 0.4
    END as confidence_adjusted_utility
FROM prediction_accuracy_by_horizon pabh
ORDER BY pabh.accuracy_rate DESC;
```

-- View for prediction model recommendations
-- This view provides recommendations for improving prediction models based on horizon analysis
```sql
CREATE VIEW prediction_model_recommendations AS
SELECT
    oppi.horizon_category,
    oppi.accuracy_rate,
    oppi.optimality_classification,
    oppi.usage_recommendation,
    oppi.improvement_potential,
    -- Recommended model adjustments for this horizon
    CASE
        WHEN oppi.horizon_category = 'SHORT_TERM' AND oppi.accuracy_rate > 0.7 THEN 
            'Use more recent data, emphasize current trends, minimize long-term assumptions'
        WHEN oppi.horizon_category = 'SHORT_TERM' AND oppi.accuracy_rate <= 0.7 THEN 
            'Incorporate more real-time data sources, reduce model complexity, focus on immediate factors'
        WHEN oppi.horizon_category = 'MEDIUM_TERM' AND oppi.accuracy_rate > 0.6 THEN 
            'Maintain current approach, fine-tune parameters, consider seasonal patterns'
        WHEN oppi.horizon_category = 'MEDIUM_TERM' AND oppi.accuracy_rate <= 0.6 THEN 
            'Add more historical context, incorporate cyclical patterns, improve feature engineering'
        WHEN oppi.horizon_category = 'LONG_TERM' AND oppi.accuracy_rate > 0.5 THEN 
            'Use structural models, consider macro trends, emphasize stable factors'
        WHEN oppi.horizon_category = 'LONG_TERM' AND oppi.accuracy_rate <= 0.5 THEN 
            'Reduce reliance on predictions, focus on scenario modeling, increase uncertainty ranges'
        WHEN oppi.horizon_category = 'VERY_LONG_TERM' AND oppi.accuracy_rate > 0.4 THEN 
            'Use trend extrapolation, emphasize fundamental drivers, acknowledge high uncertainty'
        ELSE 
            'Avoid making predictions in this horizon, use qualitative assessments, acknowledge limitations'
    END as model_adjustment_recommendation,
    -- Recommended confidence adjustment
    CASE
        WHEN oppi.accuracy_rate > 0.7 THEN 1.0  -- Full confidence
        WHEN oppi.accuracy_rate > 0.6 THEN 0.8  -- Mild discount
        WHEN oppi.accuracy_rate > 0.5 THEN 0.6  -- Moderate discount
        WHEN oppi.accuracy_rate > 0.4 THEN 0.4  -- Heavy discount
        ELSE 0.2                                -- Minimal confidence
    END as confidence_adjustment_factor,
    -- Recommended prediction frequency
    CASE
        WHEN oppi.horizon_category = 'SHORT_TERM' THEN 'HIGH_FREQUENCY_UPDATES'
        WHEN oppi.horizon_category = 'MEDIUM_TERM' THEN 'MODERATE_FREQUENCY_UPDATES'
        WHEN oppi.horizon_category = 'LONG_TERM' THEN 'LOW_FREQUENCY_UPDATES'
        WHEN oppi.horizon_category = 'VERY_LONG_TERM' THEN 'PERIODIC_REEVALUATION_ONLY'
    END as update_frequency_recommendation,
    -- Recommended approach
    CASE
        WHEN oppi.optimality_classification IN ('HIGHLY_OPTIMAL', 'MODERATELY_OPTIMAL') THEN 'ACTIVE_PREDICTION_MODELING'
        WHEN oppi.optimality_classification = 'MINIMALLY_OPTIMAL' THEN 'CAUTIOUS_PREDICTION_MODELING'
        WHEN oppi.optimality_classification = 'SUBOPTIMAL' THEN 'QUALITATIVE_ASSESSMENT_NEEDED'
        ELSE 'EXPERIMENTAL_APPROACH_ONLY'
    END as recommended_approach,
    -- Risk level
    CASE
        WHEN oppi.accuracy_rate < 0.4 THEN 'HIGH_RISK'
        WHEN oppi.accuracy_rate < 0.6 THEN 'MEDIUM_RISK'
        WHEN oppi.accuracy_rate < 0.7 THEN 'LOW_RISK'
        ELSE 'VERY_LOW_RISK'
    END as risk_level
FROM optimal_prediction_horizon_identification oppi
ORDER BY oppi.efficiency_rank;