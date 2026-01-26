-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Analyzing Trends in Participant Accuracy Over Time  

-- Analyzes trends in participant accuracy over time
-- Identifies consistent performers and adjusts reputation weights accordingly
-- Links: participants.id → truth_event.participant_id → impact.participant_id → participants.id, judgment.participant_id → participants.id
```sql
CREATE VIEW participant_accuracy_trends AS
SELECT
    p.id as participant_id,
    p.public_key,
    p.reputation_score,
    p.total_judgment,
    p.accurate_judgment,
    p.total_impact,
    p.accurate_impact,
    -- Calculate accuracy rates
    CASE
        WHEN p.total_judgment > 0 THEN p.accurate_judgment * 1.0 / p.total_judgment
        ELSE 0.5  -- Default neutral accuracy
    END as judgment_accuracy_rate,
    CASE
        WHEN p.total_impact > 0 THEN p.accurate_impact * 1.0 / p.total_impact
        ELSE 0.5  -- Default neutral accuracy
    END as impact_accuracy_rate,
    -- Combined accuracy rate
    CASE
        WHEN p.total_judgment + p.total_impact > 0 
        THEN (p.accurate_judgment + p.accurate_impact) * 1.0 / (p.total_judgment + p.total_impact)
        ELSE 0.5
    END as combined_accuracy_rate,
    -- Recent performance (last 30 days)
    (SELECT COUNT(*) FROM truth_event te WHERE te.participant_id = p.id AND te.created_at > (SELECT strftime('%s', 'now') - 86400*30)) as events_last_30_days,
    (SELECT COUNT(*) FROM impact i WHERE i.participant_id = p.id AND i.signature IS NOT NULL AND i.timeline_id > (SELECT strftime('%s', 'now') - 86400*30)) as impacts_last_30_days,
    (SELECT COUNT(*) FROM judgment j WHERE j.participant_id = p.id AND j.signature IS NOT NULL AND j.timeline_id > (SELECT strftime('%s', 'now') - 86400*30)) as judgments_last_30_days,
    -- Recent accuracy rates
    (SELECT AVG(CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
     FROM truth_event te 
     WHERE te.participant_id = p.id 
       AND te.created_at > (SELECT strftime('%s', 'now') - 86400*30)
       AND te.collective_score IS NOT NULL) as recent_truth_accuracy,
    -- Performance trend (comparing last 15 days to previous 15 days)
    CASE
        WHEN p.total_judgment > 10 AND p.total_impact > 10 THEN
            -- Calculate trend based on recent performance vs historical
            (SELECT AVG(CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
             FROM truth_event te 
             WHERE te.participant_id = p.id 
               AND te.created_at > (SELECT strftime('%s', 'now') - 86400*15)
               AND te.collective_score IS NOT NULL) -
            (SELECT AVG(CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
             FROM truth_event te 
             WHERE te.participant_id = p.id 
               AND te.created_at BETWEEN (SELECT strftime('%s', 'now') - 86400*30) AND (SELECT strftime('%s', 'now') - 86400*15)
               AND te.collective_score IS NOT NULL)
        ELSE 0
    END as performance_trend,
    -- Consistency measure (variance in performance)
    (SELECT AVG(POWER(performance_score - avg_performance, 2))
     FROM (
         SELECT 
             CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END as performance_score,
             (SELECT AVG(CASE WHEN ABS(te2.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
              FROM truth_event te2 
              WHERE te2.participant_id = p.id AND te2.collective_score IS NOT NULL) as avg_performance
         FROM truth_event te 
         WHERE te.participant_id = p.id AND te.collective_score IS NOT NULL
     )) as performance_variance,
    -- Reliability score based on consistency
    CASE
        WHEN (SELECT AVG(POWER(performance_score - avg_performance, 2))
              FROM (
                  SELECT 
                      CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END as performance_score,
                      (SELECT AVG(CASE WHEN ABS(te2.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
                       FROM truth_event te2 
                       WHERE te2.participant_id = p.id AND te2.collective_score IS NOT NULL) as avg_performance
                  FROM truth_event te 
                  WHERE te.participant_id = p.id AND te.collective_score IS NOT NULL
              )) < 0.1 THEN 'HIGH'
        WHEN (SELECT AVG(POWER(performance_score - avg_performance, 2))
              FROM (
                  SELECT 
                      CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END as performance_score,
                      (SELECT AVG(CASE WHEN ABS(te2.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END) 
                       FROM truth_event te2 
                       WHERE te2.participant_id = p.id AND te2.collective_score IS NOT NULL) as avg_performance
                  FROM truth_event te 
                  WHERE te.participant_id = p.id AND te.collective_score IS NOT NULL
              )) < 0.2 THEN 'MEDIUM'
        ELSE 'LOW'
    END as reliability_rating,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM participants p;
```

-- View for tracking accuracy trends over time
-- This view provides historical data on how participant accuracy has evolved
```sql
CREATE VIEW participant_accuracy_over_time AS
WITH participant_weekly_stats AS (
    SELECT 
        p.id as participant_id,
        p.public_key,
        date('now', '-' || (week_offset * 7) || ' days') as week_start,
        -- Count events in this week
        (SELECT COUNT(*) 
         FROM truth_event te 
         WHERE te.participant_id = p.id 
           AND date(te.created_at, 'unixepoch') BETWEEN 
               date('now', '-' || ((week_offset + 1) * 7) || ' days') AND 
               date('now', '-' || (week_offset * 7) || ' days')) as events_this_week,
        -- Calculate accuracy for this week
        (SELECT AVG(CASE WHEN ABS(te.collective_score - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END)
         FROM truth_event te 
         WHERE te.participant_id = p.id 
           AND date(te.created_at, 'unixepoch') BETWEEN 
               date('now', '-' || ((week_offset + 1) * 7) || ' days') AND 
               date('now', '-' || (week_offset * 7) || ' days')
           AND te.collective_score IS NOT NULL) as accuracy_this_week,
        -- Impact accuracy for this week
        (SELECT AVG(CASE WHEN i.value IS NOT NULL AND ABS(i.value - 0.5) > 0.4 THEN 1.0 ELSE 0.0 END)
         FROM impact i
         WHERE i.participant_id = p.id
           AND date(i.timeline_id, 'unixepoch') BETWEEN 
               date('now', '-' || ((week_offset + 1) * 7) || ' days') AND 
               date('now', '-' || (week_offset * 7) || ' days')) as impact_accuracy_this_week,
        week_offset
    FROM participants p
    CROSS JOIN (SELECT 0 as week_offset UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) weeks  -- Last 5 weeks
)
SELECT
    pwos.participant_id,
    pwos.public_key,
    pwos.week_start,
    pwos.events_this_week,
    pwos.accuracy_this_week,
    pwos.impact_accuracy_this_week,
    -- Calculate rolling average accuracy (last 3 weeks)
    AVG(pwos.accuracy_this_week) OVER (
        PARTITION BY pwos.participant_id 
        ORDER BY pwos.week_start 
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) as rolling_avg_accuracy,
    -- Calculate trend (current week vs 3 weeks ago)
    pwos.accuracy_this_week - 
    (SELECT accuracy_this_week 
     FROM participant_weekly_stats pwos2 
     WHERE pwos2.participant_id = pwos.participant_id 
       AND pwos2.week_offset = pwos.week_offset + 3) as weekly_improvement,
    -- Consistency measure (std deviation of weekly accuracy)
    SQRT(AVG(POWER(pwos.accuracy_this_week - avg_acc, 2)) OVER (
        PARTITION BY pwos.participant_id
        ORDER BY pwos.week_start
        ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
    )) as weekly_accuracy_variance
FROM participant_weekly_stats pwos
JOIN (SELECT participant_id, AVG(accuracy_this_week) as avg_acc FROM participant_weekly_stats GROUP BY participant_id) avg_table
ON pwos.participant_id = avg_table.participant_id
ORDER BY pwos.participant_id, pwos.week_start DESC;
```

-- View for identifying high-performing participants
-- This view highlights participants with consistently high accuracy
```sql
CREATE VIEW high_performing_participants AS
SELECT
    pat.participant_id,
    pat.public_key,
    pat.reputation_score,
    pat.judgment_accuracy_rate,
    pat.impact_accuracy_rate,
    pat.combined_accuracy_rate,
    pat.performance_trend,
    pat.reliability_rating,
    -- Performance tier
    CASE
        WHEN pat.combined_accuracy_rate >= 0.8 AND pat.reliability_rating = 'HIGH' THEN 'ELITE'
        WHEN pat.combined_accuracy_rate >= 0.7 AND pat.reliability_rating IN ('HIGH', 'MEDIUM') THEN 'EXPERT'
        WHEN pat.combined_accuracy_rate >= 0.6 THEN 'PROFICIENT'
        WHEN pat.combined_accuracy_rate >= 0.5 THEN 'COMPETENT'
        ELSE 'DEVELOPING'
    END as performance_tier,
    -- Improvement potential
    CASE
        WHEN pat.performance_trend > 0.1 THEN 'RAPIDLY_IMPROVING'
        WHEN pat.performance_trend > 0.05 THEN 'IMPROVING'
        WHEN pat.performance_trend < -0.1 THEN 'DECLINING'
        WHEN pat.performance_trend < -0.05 THEN 'DETERIORATING'
        ELSE 'STABLE'
    END as performance_trajectory,
    -- Weight adjustment recommendation
    CASE
        WHEN pat.performance_tier = 'ELITE' THEN 1.5  -- 50% higher weight
        WHEN pat.performance_tier = 'EXPERT' THEN 1.3  -- 30% higher weight
        WHEN pat.performance_tier = 'PROFICIENT' THEN 1.1  -- 10% higher weight
        WHEN pat.performance_tier = 'DEVELOPING' THEN 0.7  -- 30% lower weight
        ELSE 1.0  -- Standard weight
    END as recommended_weight_adjustment,
    -- Confidence in performance assessment
    CASE
        WHEN pat.events_last_30_days >= 20 THEN 'HIGH'
        WHEN pat.events_last_30_days >= 10 THEN 'MEDIUM'
        WHEN pat.events_last_30_days >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as assessment_confidence,
    -- Special recognition flags
    CASE
        WHEN pat.combined_accuracy_rate >= 0.85 AND pat.performance_trend > 0.05 THEN 'CONSISTENTLY_EXCELLENT'
        WHEN pat.combined_accuracy_rate >= 0.9 THEN 'ACCURACY_SPECIALIST'
        WHEN pat.reliability_rating = 'HIGH' AND pat.performance_trend > 0.1 THEN 'RAPID_LEARNER'
        ELSE 'STANDARD_PERFORMER'
    END as special_designation
FROM participant_accuracy_trends pat
WHERE pat.total_judgment + pat.total_impact >= 5  -- Minimum activity threshold
ORDER BY pat.combined_accuracy_rate DESC, pat.reputation_score DESC;
```

-- View for identifying participants needing support
-- This view highlights participants who may need additional guidance
```sql
CREATE VIEW participants_needing_support AS
SELECT
    pat.participant_id,
    pat.public_key,
    pat.reputation_score,
    pat.judgment_accuracy_rate,
    pat.impact_accuracy_rate,
    pat.combined_accuracy_rate,
    pat.performance_trend,
    pat.reliability_rating,
    -- Support needs classification
    CASE
        WHEN pat.combined_accuracy_rate < 0.4 THEN 'CRITICAL_NEED'
        WHEN pat.combined_accuracy_rate < 0.5 AND pat.performance_trend < 0 THEN 'SIGNIFICANT_SUPPORT_NEEDED'
        WHEN pat.reliability_rating = 'LOW' AND pat.combined_accuracy_rate < 0.6 THEN 'CONSISTENCY_TRAINING_NEEDED'
        WHEN pat.performance_trend < -0.1 THEN 'PERFORMANCE_DECLINING'
        WHEN pat.judgment_accuracy_rate < 0.4 THEN 'JUDGMENT_TRAINING_NEEDED'
        WHEN pat.impact_accuracy_rate < 0.4 THEN 'IMPACT_ASSESSMENT_TRAINING_NEEDED'
        ELSE 'NO_IMMEDIATE_SUPPORT_NEEDED'
    END as support_classification,
    -- Recommended interventions
    CASE
        WHEN pat.combined_accuracy_rate < 0.4 THEN 'Mandatory retraining program required'
        WHEN pat.judgment_accuracy_rate < 0.4 THEN 'Focus on truth assessment skills'
        WHEN pat.impact_accuracy_rate < 0.4 THEN 'Focus on consequence prediction skills'
        WHEN pat.reliability_rating = 'LOW' THEN 'Emphasize consistent methodology'
        WHEN pat.performance_trend < -0.1 THEN 'Review recent performance patterns'
        ELSE 'Continue standard participation'
    END as recommended_intervention,
    -- Risk level
    CASE
        WHEN pat.combined_accuracy_rate < 0.4 THEN 'HIGH'
        WHEN pat.reliability_rating = 'LOW' AND pat.combined_accuracy_rate < 0.5 THEN 'MEDIUM_HIGH'
        WHEN pat.performance_trend < -0.1 THEN 'MEDIUM'
        WHEN pat.reliability_rating = 'LOW' THEN 'MEDIUM_LOW'
        ELSE 'LOW'
    END as risk_level,
    -- Weight adjustment for low performers
    CASE
        WHEN pat.combined_accuracy_rate < 0.4 THEN 0.3  -- Significantly reduce weight
        WHEN pat.combined_accuracy_rate < 0.5 THEN 0.6  -- Reduce weight
        WHEN pat.reliability_rating = 'LOW' THEN 0.8    -- Slightly reduce weight
        ELSE 1.0  -- No adjustment
    END as adjusted_weight_for_assessments
FROM participant_accuracy_trends pat
WHERE pat.support_classification != 'NO_IMMEDIATE_SUPPORT_NEEDED'
ORDER BY pat.combined_accuracy_rate ASC, pat.performance_trend ASC;