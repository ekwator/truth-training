-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Validating the Effectiveness of Expert Heuristics  

-- Validates the effectiveness of expert heuristics by comparing their predictions against actual outcomes
-- Links: expert_heuristics.id → judgment_heuristics.heuristic_id → judgment.id, expert_heuristics.domain → various context domains
```sql
CREATE VIEW heuristic_validation_metrics AS
SELECT
    eh.id as heuristic_id,
    eh.name as heuristic_name,
    eh.description as heuristic_description,
    eh.domain as heuristic_domain,
    eh.weight as current_weight,
    eh.confidence as current_confidence,
    eh.proven_accuracy as historical_accuracy,
    -- Count of applications
    (SELECT COUNT(*) FROM judgment_heuristics jh WHERE jh.heuristic_id = eh.id) as total_applications,
    -- Accuracy of the heuristic
    (SELECT AVG(jh.influence) FROM judgment_heuristics jh WHERE jh.heuristic_id = eh.id) as average_influence,
    -- Correlation between heuristic application and actual outcomes
    (SELECT AVG(CASE 
        WHEN j.assessment = 'true' AND jh.influence > 0.5 THEN 1.0
        WHEN j.assessment = 'false' AND jh.influence < 0.5 THEN 1.0
        WHEN j.assessment IS NULL AND ABS(0.5 - jh.influence) < 0.1 THEN 1.0
        ELSE 0.0
    END)
    FROM judgment j
    JOIN judgment_heuristics jh ON j.id = jh.judgment_id
    WHERE jh.heuristic_id = eh.id) as outcome_correlation,
    -- Performance compared to random chance
    (SELECT AVG(CASE 
        WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
        WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
        WHEN jh.influence BETWEEN 0.4 AND 0.6 AND j.assessment IS NULL THEN 1.0
        ELSE 0.0
    END)
    FROM judgment j
    JOIN judgment_heuristics jh ON j.id = jh.judgment_id
    WHERE jh.heuristic_id = eh.id) as accuracy_vs_random,
    -- Recency-weighted performance (recent applications weighted more heavily)
    (SELECT AVG(CASE 
        WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
        WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
        WHEN jh.influence BETWEEN 0.4 AND 0.6 AND j.assessment IS NULL THEN 1.0
        ELSE 0.0
    END) * EXP(-0.1 * (julianday('now') - julianday(j.created_at, 'unixepoch')))
    FROM judgment j
    JOIN judgment_heuristics jh ON j.id = jh.judgment_id
    WHERE jh.heuristic_id = eh.id) as recency_weighted_accuracy,
    -- Consistency measure (how reliably the heuristic performs)
    (SELECT AVG(ABS(jh.influence - avg_influence))
     FROM judgment_heuristics jh
     JOIN judgment j ON jh.judgment_id = j.id
     CROSS JOIN (SELECT AVG(jh2.influence) as avg_influence
                FROM judgment_heuristics jh2
                JOIN judgment j2 ON jh2.judgment_id = j2.id
                WHERE jh2.heuristic_id = eh.id) avg_table
     WHERE jh.heuristic_id = eh.id) as consistency_measure,
    -- Performance by domain
    CASE
        WHEN eh.domain = 'logic' THEN 
            (SELECT AVG(CASE 
                WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
                WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
                ELSE 0.0
            END)
            FROM judgment j
            JOIN judgment_heuristics jh ON j.id = jh.judgment_id
            JOIN truth_event te ON j.event_id = (SELECT id FROM event_ci WHERE created_by = te.id LIMIT 1)
            WHERE jh.heuristic_id = eh.id AND te.category_id IN (
                SELECT id FROM category WHERE name LIKE '%logic%' OR description LIKE '%logic%'
            ))
        WHEN eh.domain = 'statistical' THEN 
            (SELECT AVG(CASE 
                WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
                WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
                ELSE 0.0
            END)
            FROM judgment j
            JOIN judgment_heuristics jh ON j.id = jh.judgment_id
            JOIN truth_event te ON j.event_id = (SELECT id FROM event_ci WHERE created_by = te.id LIMIT 1)
            WHERE jh.heuristic_id = eh.id AND te.category_id IN (
                SELECT id FROM category WHERE name LIKE '%stat%' OR description LIKE '%stat%'
            ))
        WHEN eh.domain = 'empirical' THEN 
            (SELECT AVG(CASE 
                WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
                WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
                ELSE 0.0
            END)
            FROM judgment j
            JOIN judgment_heuristics jh ON j.id = jh.judgment_id
            JOIN truth_event te ON j.event_id = (SELECT id FROM event_ci WHERE created_by = te.id LIMIT 1)
            WHERE jh.heuristic_id = eh.id AND te.category_id IN (
                SELECT id FROM category WHERE name LIKE '%empir%' OR description LIKE '%empir%'
            ))
        ELSE (SELECT AVG(CASE 
                WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
                WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
                ELSE 0.0
            END)
            FROM judgment j
            JOIN judgment_heuristics jh ON j.id = jh.judgment_id
            WHERE jh.heuristic_id = eh.id)
    END as domain_specific_accuracy,
    -- Confidence adjustment based on validation
    CASE
        WHEN (SELECT AVG(CASE 
            WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
            WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
            ELSE 0.0
        END)
        FROM judgment j
        JOIN judgment_heuristics jh ON j.id = jh.judgment_id
        WHERE jh.heuristic_id = eh.id) > 0.7 THEN LEAST(eh.confidence * 1.1, 1.0)
        WHEN (SELECT AVG(CASE 
            WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
            WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
            ELSE 0.0
        END)
        FROM judgment j
        JOIN judgment_heuristics jh ON j.id = jh.judgment_id
        WHERE jh.heuristic_id = eh.id) < 0.4 THEN GREATEST(eh.confidence * 0.8, 0.1)
        ELSE eh.confidence
    END as validated_confidence,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM expert_heuristics eh;
```

-- View for analyzing heuristic performance over time
-- This view tracks how heuristics perform over time to identify trends
```sql
CREATE VIEW heuristic_performance_over_time AS
WITH heuristic_weekly_performance AS (
    SELECT 
        eh.id as heuristic_id,
        eh.name as heuristic_name,
        date('now', '-' || (week_offset * 7) || ' days') as week_start,
        -- Applications in this week
        (SELECT COUNT(*) 
         FROM judgment_heuristics jh
         JOIN judgment j ON jh.judgment_id = j.id
         WHERE jh.heuristic_id = eh.id
           AND date(j.created_at, 'unixepoch') BETWEEN 
               date('now', '-' || ((week_offset + 1) * 7) || ' days') AND 
               date('now', '-' || (week_offset * 7) || ' days')) as applications_this_week,
        -- Accuracy in this week
        (SELECT AVG(CASE 
            WHEN jh.influence > 0.5 AND j.assessment = 'true' THEN 1.0
            WHEN jh.influence < 0.5 AND j.assessment = 'false' THEN 1.0
            WHEN jh.influence BETWEEN 0.4 AND 0.6 AND j.assessment IS NULL THEN 1.0
            ELSE 0.0
        END)
        FROM judgment j
        JOIN judgment_heuristics jh ON j.id = jh.judgment_id
        WHERE jh.heuristic_id = eh.id
          AND date(j.created_at, 'unixepoch') BETWEEN 
              date('now', '-' || ((week_offset + 1) * 7) || ' days') AND 
              date('now', '-' || (week_offset * 7) || ' days')) as accuracy_this_week,
        week_offset
    FROM expert_heuristics eh
    CROSS JOIN (SELECT 0 as week_offset UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) weeks  -- Last 5 weeks
)
SELECT
    hwop.heuristic_id,
    hwop.heuristic_name,
    hwop.week_start,
    hwop.applications_this_week,
    hwop.accuracy_this_week,
    -- Rolling average accuracy (last 3 weeks)
    AVG(hwop.accuracy_this_week) OVER (
        PARTITION BY hwop.heuristic_id 
        ORDER BY hwop.week_start 
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) as rolling_avg_accuracy,
    -- Performance trend (current week vs 3 weeks ago)
    hwop.accuracy_this_week - 
    (SELECT accuracy_this_week 
     FROM heuristic_weekly_performance hwop2 
     WHERE hwop2.heuristic_id = hwop.heuristic_id 
       AND hwop2.week_offset = hwop.week_offset + 3) as weekly_improvement,
    -- Volatility (how much accuracy fluctuates)
    SQRT(AVG(POWER(hwop.accuracy_this_week - avg_acc, 2)) OVER (
        PARTITION BY hwop.heuristic_id
        ORDER BY hwop.week_start
        ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
    )) as accuracy_volatility
FROM heuristic_weekly_performance hwop
JOIN (SELECT heuristic_id, AVG(accuracy_this_week) as avg_acc FROM heuristic_weekly_performance GROUP BY heuristic_id) avg_table
ON hwop.heuristic_id = avg_table.heuristic_id
ORDER BY hwop.heuristic_id, hwop.week_start DESC;
```

-- View for identifying high-performing and low-performing heuristics
-- This view ranks heuristics based on their validation metrics
```sql
CREATE VIEW heuristic_performance_rankings AS
SELECT
    hvm.heuristic_id,
    hvm.heuristic_name,
    hvm.heuristic_domain,
    hvm.current_weight,
    hvm.current_confidence,
    hvm.historical_accuracy,
    hvm.total_applications,
    hvm.outcome_correlation,
    hvm.accuracy_vs_random,
    hvm.recency_weighted_accuracy,
    -- Performance category
    CASE
        WHEN hvm.outcome_correlation >= 0.8 AND hvm.total_applications >= 20 THEN 'EXCELLENT'
        WHEN hvm.outcome_correlation >= 0.7 AND hvm.total_applications >= 15 THEN 'VERY_GOOD'
        WHEN hvm.outcome_correlation >= 0.6 AND hvm.total_applications >= 10 THEN 'GOOD'
        WHEN hvm.outcome_correlation >= 0.5 AND hvm.total_applications >= 5 THEN 'FAIR'
        WHEN hvm.outcome_correlation >= 0.4 AND hvm.total_applications >= 3 THEN 'POOR'
        WHEN hvm.total_applications < 3 THEN 'INSUFFICIENT_DATA'
        ELSE 'INEFFECTIVE'
    END as performance_category,
    -- Reliability score (combination of accuracy and consistency)
    (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) as reliability_score,
    -- Rank by reliability
    ROW_NUMBER() OVER (ORDER BY (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) DESC) as reliability_rank,
    -- Confidence adjustment recommendation
    CASE
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) >= 0.8 AND hvm.total_applications >= 20 THEN GREATEST(hvm.current_confidence * 1.2, 1.0)  -- Increase confidence significantly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) >= 0.7 AND hvm.total_applications >= 15 THEN GREATEST(hvm.current_confidence * 1.1, 1.0)  -- Increase confidence modestly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) <= 0.3 THEN LEAST(hvm.current_confidence * 0.6, 0.1)  -- Decrease confidence significantly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) <= 0.4 THEN LEAST(hvm.current_confidence * 0.8, 0.2)  -- Decrease confidence modestly
        ELSE hvm.current_confidence  -- No adjustment needed
    END as recommended_confidence,
    -- Weight adjustment recommendation
    CASE
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) >= 0.8 AND hvm.total_applications >= 20 THEN GREATEST(hvm.current_weight * 1.2, 1.0)  -- Increase weight significantly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) >= 0.7 AND hvm.total_applications >= 15 THEN GREATEST(hvm.current_weight * 1.1, 1.0)  -- Increase weight modestly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) <= 0.3 THEN LEAST(hvm.current_weight * 0.6, 0.05)  -- Decrease weight significantly
        WHEN (hvm.outcome_correlation * 0.6 + (1 - hvm.consistency_measure) * 0.4) <= 0.4 THEN LEAST(hvm.current_weight * 0.8, 0.1)  -- Decrease weight modestly
        ELSE hvm.current_weight  -- No adjustment needed
    END as recommended_weight,
    -- Recommendation based on performance
    CASE
        WHEN hvm.performance_category IN ('EXCELLENT', 'VERY_GOOD') THEN 'USE_FREQUENTLY'
        WHEN hvm.performance_category = 'GOOD' THEN 'USE_REGULARLY'
        WHEN hvm.performance_category = 'FAIR' THEN 'USE_CAUTIOUSLY'
        WHEN hvm.performance_category = 'POOR' THEN 'LIMIT_USE'
        WHEN hvm.performance_category = 'INSUFFICIENT_DATA' THEN 'COLLECT_MORE_DATA'
        ELSE 'CONSIDER_DEPRECATION'
    END as usage_recommendation,
    -- Risk level
    CASE
        WHEN hvm.outcome_correlation < 0.3 AND hvm.total_applications >= 10 THEN 'HIGH'
        WHEN hvm.outcome_correlation < 0.5 AND hvm.total_applications >= 5 THEN 'MEDIUM'
        WHEN hvm.outcome_correlation > 0.7 THEN 'LOW'
        ELSE 'MEDIUM'
    END as risk_level
FROM heuristic_validation_metrics hvm
ORDER BY reliability_score DESC;
```

-- View for identifying conflicting heuristics
-- This view identifies heuristics that tend to conflict with each other
```sql
CREATE VIEW conflicting_heuristics_analysis AS
SELECT
    hpr1.heuristic_id as heuristic1_id,
    hpr1.heuristic_name as heuristic1_name,
    hpr2.heuristic_id as heuristic2_id,
    hpr2.heuristic_name as heuristic2_name,
    -- Count of times these heuristics were applied to the same judgment
    (SELECT COUNT(*)
     FROM judgment j
     JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
     JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id) as co_application_count,
    -- Correlation between their influences (negative correlation suggests conflict)
    (SELECT AVG((jh1.influence - avg_inf1) * (jh2.influence - avg_inf2)) / (std_inf1 * std_inf2)
     FROM judgment j
     JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
     JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id
     CROSS JOIN (
         SELECT AVG(influence) as avg_inf1, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf1
         FROM judgment_heuristics jh1_inner
         CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr1.heuristic_id) avg_subquery
         WHERE jh1_inner.heuristic_id = hpr1.heuristic_id
     ) stats1
     CROSS JOIN (
         SELECT AVG(influence) as avg_inf2, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf2
         FROM judgment_heuristics jh2_inner
         CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr2.heuristic_id) avg_subquery
         WHERE jh2_inner.heuristic_id = hpr2.heuristic_id
     ) stats2
    ) as correlation_coefficient,
    -- Measure of conflict (negative correlation with high absolute value)
    CASE
        WHEN (SELECT AVG((jh1.influence - avg_inf1) * (jh2.influence - avg_inf2)) / (std_inf1 * std_inf2)
              FROM judgment j
              JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
              JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf1, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf1
                  FROM judgment_heuristics jh1_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr1.heuristic_id) avg_subquery
                  WHERE jh1_inner.heuristic_id = hpr1.heuristic_id
              ) stats1
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf2, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf2
                  FROM judgment_heuristics jh2_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr2.heuristic_id) avg_subquery
                  WHERE jh2_inner.heuristic_id = hpr2.heuristic_id
              ) stats2
        ) < -0.5 AND (SELECT COUNT(*)
                      FROM judgment j
                      JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
                      JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id) >= 5 THEN 'STRONG_CONFLICT'
        WHEN (SELECT AVG((jh1.influence - avg_inf1) * (jh2.influence - avg_inf2)) / (std_inf1 * std_inf2)
              FROM judgment j
              JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
              JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf1, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf1
                  FROM judgment_heuristics jh1_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr1.heuristic_id) avg_subquery
                  WHERE jh1_inner.heuristic_id = hpr1.heuristic_id
              ) stats1
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf2, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf2
                  FROM judgment_heuristics jh2_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr2.heuristic_id) avg_subquery
                  WHERE jh2_inner.heuristic_id = hpr2.heuristic_id
              ) stats2
        ) < -0.3 AND (SELECT COUNT(*)
                      FROM judgment j
                      JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
                      JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id) >= 3 THEN 'MODERATE_CONFLICT'
        ELSE 'NO_CONFLICT'
    END as conflict_level,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM heuristic_performance_rankings hpr1
CROSS JOIN heuristic_performance_rankings hpr2
WHERE hpr1.heuristic_id < hpr2.heuristic_id  -- Avoid duplicate pairs
  AND (SELECT COUNT(*)
       FROM judgment j
       JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
       JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id) >= 3  -- Minimum co-application threshold
ORDER BY 
    CASE
        WHEN (SELECT AVG((jh1.influence - avg_inf1) * (jh2.influence - avg_inf2)) / (std_inf1 * std_inf2)
              FROM judgment j
              JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
              JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf1, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf1
                  FROM judgment_heuristics jh1_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr1.heuristic_id) avg_subquery
                  WHERE jh1_inner.heuristic_id = hpr1.heuristic_id
              ) stats1
              CROSS JOIN (
                  SELECT AVG(influence) as avg_inf2, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf2
                  FROM judgment_heuristics jh2_inner
                  CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr2.heuristic_id) avg_subquery
                  WHERE jh2_inner.heuristic_id = hpr2.heuristic_id
              ) stats2
        ) < 0 THEN ABS((SELECT AVG((jh1.influence - avg_inf1) * (jh2.influence - avg_inf2)) / (std_inf1 * std_inf2)
                        FROM judgment j
                        JOIN judgment_heuristics jh1 ON j.id = jh1.judgment_id AND jh1.heuristic_id = hpr1.heuristic_id
                        JOIN judgment_heuristics jh2 ON j.id = jh2.judgment_id AND jh2.heuristic_id = hpr2.heuristic_id
                        CROSS JOIN (
                            SELECT AVG(influence) as avg_inf1, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf1
                            FROM judgment_heuristics jh1_inner
                            CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr1.heuristic_id) avg_subquery
                            WHERE jh1_inner.heuristic_id = hpr1.heuristic_id
                        ) stats1
                        CROSS JOIN (
                            SELECT AVG(influence) as avg_inf2, SQRT(AVG(POWER(influence - avg_inf, 2))) as std_inf2
                            FROM judgment_heuristics jh2_inner
                            CROSS JOIN (SELECT AVG(influence) as avg_inf FROM judgment_heuristics WHERE heuristic_id = hpr2.heuristic_id) avg_subquery
                            WHERE jh2_inner.heuristic_id = hpr2.heuristic_id
                        ) stats2
                       )) 
        ELSE 0
    END DESC; -- Order by conflict magnitude