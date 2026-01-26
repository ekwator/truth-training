-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Correlation Between Truth Event Collective Score and Impact Assessments  

-- Calculates correlation between truth_event collective_score and impact assessments
-- Identifies patterns in how truth assessments relate to impact observations
-- Links: truth_event.id → event_ci.created_by → impact.event_id → event_ci.id
```sql
CREATE VIEW truth_event_impact_correlation AS
SELECT
    te.id as event_id,
    te.description,
    te.collective_score as truth_score,
    -- Aggregate impact data for this event
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as total_impacts,
    (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) as average_impact_value,
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value = 1) as positive_impacts,
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value = 0) as negative_impacts,
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NULL) as uncertain_impacts,
    -- Calculate correlation coefficient between truth and impact
    (te.collective_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL)) *
    ((SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) - 
     (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE i.value IS NOT NULL)) as correlation_component,
    -- Calculate impact variance
    (SELECT AVG(POWER(i.value - (SELECT AVG(value) FROM impact WHERE value IS NOT NULL), 2)) 
     FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) as impact_variance,
    -- Determine correlation direction
    CASE
        WHEN te.collective_score > 0.5 AND (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) > 0.5 THEN 'POSITIVE_CONCORDANCE'
        WHEN te.collective_score > 0.5 AND (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) < 0.5 THEN 'DISCORDANCE'
        WHEN te.collective_score < 0.5 AND (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) > 0.5 THEN 'DISCORDANCE'
        WHEN te.collective_score < 0.5 AND (SELECT AVG(i.value) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id AND i.value IS NOT NULL) < 0.5 THEN 'POSITIVE_CONCORDANCE'
        ELSE 'NEUTRAL'
    END as correlation_type,
    -- Confidence in correlation (based on number of impact assessments)
    CASE
        WHEN (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) >= 10 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) >= 5 THEN 'MEDIUM'
        ELSE 'LOW'
    END as correlation_confidence,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as calculated_at
FROM truth_event te
WHERE te.collective_score IS NOT NULL;
```

-- View for calculating detailed correlation statistics
-- This view provides more detailed statistics on truth-impact correlation
```sql
CREATE VIEW truth_impact_correlation_detailed AS
SELECT
    teic.event_id,
    teic.description,
    teic.truth_score,
    teic.total_impacts,
    teic.average_impact_value,
    teic.positive_impacts,
    teic.negative_impacts,
    teic.uncertain_impacts,
    -- Calculate Pearson correlation coefficient (simplified)
    CASE
        WHEN teic.total_impacts > 1 AND teic.correlation_confidence != 'LOW'
        THEN (
            (teic.truth_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL)) *
            (teic.average_impact_value - (SELECT AVG(i.value) FROM impact i WHERE i.value IS NOT NULL))
        ) / (
            SQRT((SELECT AVG(POWER(collective_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL), 2)) FROM truth_event WHERE collective_score IS NOT NULL)) *
            SQRT(COALESCE(teic.impact_variance, 1.0))
        )
        ELSE 0
    END as pearson_correlation_coefficient,
    -- Strength of correlation
    CASE
        WHEN ABS(CASE
            WHEN teic.total_impacts > 1 AND teic.correlation_confidence != 'LOW'
            THEN (
                (teic.truth_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL)) *
                (teic.average_impact_value - (SELECT AVG(i.value) FROM impact i WHERE i.value IS NOT NULL))
            ) / (
                SQRT((SELECT AVG(POWER(collective_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL), 2)) FROM truth_event WHERE collective_score IS NOT NULL)) *
                SQRT(COALESCE(teic.impact_variance, 1.0))
            )
            ELSE 0
        END) >= 0.7 THEN 'STRONG'
        WHEN ABS(CASE
            WHEN teic.total_impacts > 1 AND teic.correlation_confidence != 'LOW'
            THEN (
                (teic.truth_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL)) *
                (teic.average_impact_value - (SELECT AVG(i.value) FROM impact i WHERE i.value IS NOT NULL))
            ) / (
                SQRT((SELECT AVG(POWER(collective_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL), 2)) FROM truth_event WHERE collective_score IS NOT NULL)) *
                SQRT(COALESCE(teic.impact_variance, 1.0))
            )
            ELSE 0
        END) >= 0.3 THEN 'MODERATE'
        WHEN ABS(CASE
            WHEN teic.total_impacts > 1 AND teic.correlation_confidence != 'LOW'
            THEN (
                (teic.truth_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL)) *
                (teic.average_impact_value - (SELECT AVG(i.value) FROM impact i WHERE i.value IS NOT NULL))
            ) / (
                SQRT((SELECT AVG(POWER(collective_score - (SELECT AVG(collective_score) FROM truth_event WHERE collective_score IS NOT NULL), 2)) FROM truth_event WHERE collective_score IS NOT NULL)) *
                SQRT(COALESCE(teic.impact_variance, 1.0))
            )
            ELSE 0
        END) >= 0.1 THEN 'WEAK'
        ELSE 'VERY_WEAK'
    END as correlation_strength,
    -- Impact ratio
    CASE
        WHEN teic.total_impacts > 0
        THEN teic.positive_impacts * 1.0 / teic.total_impacts
        ELSE 0
    END as positive_impact_ratio,
    CASE
        WHEN teic.total_impacts > 0
        THEN teic.negative_impacts * 1.0 / teic.total_impacts
        ELSE 0
    END as negative_impact_ratio,
    -- Concordance measure
    CASE
        WHEN teic.truth_score > 0.5 AND teic.positive_impact_ratio > 0.5 THEN 1
        WHEN teic.truth_score < 0.5 AND teic.negative_impact_ratio > 0.5 THEN 1
        WHEN teic.truth_score > 0.5 AND teic.negative_impact_ratio > 0.5 THEN -1
        WHEN teic.truth_score < 0.5 AND teic.positive_impact_ratio > 0.5 THEN -1
        ELSE 0
    END as concordance_measure
FROM truth_event_impact_correlation teic;
```

-- View for identifying events with unusual truth-impact relationships
-- This view highlights events where truth and impact assessments diverge significantly
```sql
CREATE VIEW anomalous_truth_impact_events AS
SELECT
    tice.event_id,
    tice.description,
    tice.truth_score,
    tice.average_impact_value,
    tice.pearson_correlation_coefficient,
    tice.correlation_strength,
    tice.correlation_type,
    -- Calculate how much the truth-impact relationship deviates from the norm
    ABS(tice.truth_score - tice.average_impact_value) as truth_impact_delta,
    -- Flag for anomalies (large deltas or negative correlations where positive expected)
    CASE
        WHEN ABS(tice.truth_score - tice.average_impact_value) > 0.5 THEN 'HIGH_DELTA_ANOMALY'
        WHEN tice.pearson_correlation_coefficient < -0.3 AND tice.correlation_confidence = 'HIGH' THEN 'NEGATIVE_CORRELATION_ANOMALY'
        WHEN tice.correlation_type = 'DISCORDANCE' AND tice.correlation_confidence = 'HIGH' THEN 'CONCORDANCE_ANOMALY'
        ELSE 'NORMAL'
    END as anomaly_type,
    -- Severity score (0-100)
    CASE
        WHEN ABS(tice.truth_score - tice.average_impact_value) > 0.7 THEN 90
        WHEN ABS(tice.truth_score - tice.average_impact_value) > 0.5 THEN 70
        WHEN ABS(tice.truth_score - tice.average_impact_value) > 0.3 THEN 50
        WHEN tice.pearson_correlation_coefficient < -0.4 THEN 80
        WHEN tice.pearson_correlation_coefficient < -0.2 THEN 60
        ELSE 30
    END as anomaly_severity,
    -- Possible explanations
    CASE
        WHEN tice.truth_score > 0.7 AND tice.average_impact_value < 0.3 THEN 'TRUE_BUT_NEGATIVE_IMPACT'
        WHEN tice.truth_score < 0.3 AND tice.average_impact_value > 0.7 THEN 'FALSE_BUT_POSITIVE_IMPACT'
        WHEN tice.truth_score > 0.7 AND tice.average_impact_value > 0.7 THEN 'TRUE_AND_POSITIVE_IMPACT'
        WHEN tice.truth_score < 0.3 AND tice.average_impact_value < 0.3 THEN 'FALSE_AND_NEGATIVE_IMPACT'
        ELSE 'MIXED_SIGNALS'
    END as relationship_type
FROM truth_impact_correlation_detailed tice
WHERE 
    ABS(tice.truth_score - tice.average_impact_value) > 0.3  -- Significant difference
    OR tice.pearson_correlation_coefficient < -0.2  -- Negative correlation
ORDER BY ABS(tice.truth_score - tice.average_impact_value) DESC;
```

-- View for overall truth-impact correlation statistics
-- This view provides system-wide statistics on truth-impact relationships
```sql
CREATE VIEW truth_impact_correlation_statistics AS
SELECT
    COUNT(*) as total_analyzed_events,
    AVG(pearson_correlation_coefficient) as average_correlation,
    MIN(pearson_correlation_coefficient) as min_correlation,
    MAX(pearson_correlation_coefficient) as max_correlation,
    STDDEV(pearson_correlation_coefficient) as correlation_std_dev,
    -- Distribution of correlation strengths
    SUM(CASE WHEN correlation_strength = 'STRONG' THEN 1 ELSE 0 END) as strong_correlations,
    SUM(CASE WHEN correlation_strength = 'MODERATE' THEN 1 ELSE 0 END) as moderate_correlations,
    SUM(CASE WHEN correlation_strength = 'WEAK' THEN 1 ELSE 0 END) as weak_correlations,
    SUM(CASE WHEN correlation_strength = 'VERY_WEAK' THEN 1 ELSE 0 END) as very_weak_correlations,
    -- Distribution of correlation types
    SUM(CASE WHEN correlation_type = 'POSITIVE_CONCORDANCE' THEN 1 ELSE 0 END) as positive_concordances,
    SUM(CASE WHEN correlation_type = 'DISCORDANCE' THEN 1 ELSE 0 END) as discordances,
    SUM(CASE WHEN correlation_type = 'NEUTRAL' THEN 1 ELSE 0 END) as neutral_relationships,
    -- Average truth and impact scores
    AVG(truth_score) as average_truth_score,
    AVG(average_impact_value) as average_impact_score,
    -- Percentage of high-confidence correlations
    (COUNT(CASE WHEN correlation_confidence = 'HIGH' THEN 1 END) * 100.0 / COUNT(*)) as pct_high_confidence,
    -- Percentage of anomalies
    (COUNT(CASE WHEN anomaly_type != 'NORMAL' THEN 1 END) * 100.0 / COUNT(*)) as pct_anomalies,
    -- Overall system coherence measure
    1.0 - (AVG(ABS(truth_score - average_impact_value))) as system_coherence
FROM truth_impact_correlation_detailed tice
LEFT JOIN anomalous_truth_impact_events ate ON tice.event_id = ate.event_id;