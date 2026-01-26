-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Tracking Convergence Between Judgment and Impact Assessments Over Time  

-- Tracks convergence between judgment and impact assessments over time
-- Determines when event assessments stabilize
-- Links: truth_event.id → event_ci.created_by → event_ci.id → judgment.event_id → event_ci.id, impact.event_id → event_ci.id
```sql
CREATE VIEW judgment_impact_convergence AS
SELECT
    te.id as event_id,
    te.description,
    te.collective_score as truth_score,
    te.impact_score,
    te.judgment_score,
    -- Get latest judgment and impact values
    (SELECT MAX(j.created_at) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as latest_judgment_time,
    (SELECT MAX(i.created_at) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as latest_impact_time,
    -- Count of judgments and impacts
    (SELECT COUNT(*) FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as judgment_count,
    (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as impact_count,
    -- Average values over time
    (SELECT AVG(CASE WHEN j.assessment = 'true' THEN 1.0 WHEN j.assessment = 'false' THEN 0.0 ELSE 0.5 END) 
     FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as avg_judgment_value,
    (SELECT AVG(COALESCE(i.value, 0.5)) FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as avg_impact_value,
    -- Calculate convergence measure (difference between judgment and impact)
    ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) as judgment_impact_delta,
    -- Convergence status
    CASE
        WHEN ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) < (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END) THEN 'CONVERGED'
        WHEN ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) < 0.2 THEN 'NEARLY_CONVERGED'
        WHEN ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) < 0.5 THEN 'MODERATELY_CONVERGED'
        ELSE 'DIVERGED'
    END as convergence_status,
    -- Stability measure (based on variance of recent values)
    (SELECT AVG(ABS(j.confidence_level - (SELECT AVG(confidence_level) FROM judgment j2 JOIN event_ci ec2 ON j2.event_id = ec2.id WHERE ec2.created_by = te.id AND j2.created_at > j.created_at - 86400*7))) 
     FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id AND j.created_at > (SELECT strftime('%s', 'now') - 86400*7)
    ) as judgment_stability,
    -- Time since first assessment
    (SELECT julianday('now') - julianday(MIN(j.created_at), 'unixepoch') FROM judgment j JOIN event_ci ec ON j.event_id = ec.id WHERE ec.created_by = te.id) as days_since_first_judgment,
    (SELECT julianday('now') - julianday(MIN(i.created_at), 'unixepoch') FROM impact i JOIN event_ci ec ON i.event_id = ec.id WHERE ec.created_by = te.id) as days_since_first_impact,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM truth_event te
WHERE te.impact_score IS NOT NULL AND te.judgment_score IS NOT NULL;
```

-- View for tracking convergence over time for each event
-- This view provides historical data on how convergence has evolved
```sql
CREATE VIEW judgment_impact_convergence_history AS
WITH event_assessment_timeline AS (
    -- Combine judgment and impact assessments in chronological order
    SELECT 
        te.id as event_id,
        j.created_at as assessment_time,
        'judgment' as assessment_type,
        CASE WHEN j.assessment = 'true' THEN 1.0 WHEN j.assessment = 'false' THEN 0.0 ELSE 0.5 END as assessment_value,
        j.confidence_level as confidence
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    JOIN judgment j ON ec.id = j.event_id
    
    UNION ALL
    
    SELECT 
        te.id as event_id,
        i.created_at as assessment_time,
        'impact' as assessment_type,
        COALESCE(i.value, 0.5) as assessment_value,
        NULL as confidence
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    JOIN impact i ON ec.id = i.event_id
)
SELECT
    eat.event_id,
    eat.assessment_time,
    eat.assessment_type,
    eat.assessment_value,
    eat.confidence,
    -- Calculate running averages
    AVG(eat.assessment_value) OVER (
        PARTITION BY eat.event_id, eat.assessment_type 
        ORDER BY eat.assessment_time 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as running_avg_by_type,
    AVG(eat.assessment_value) OVER (
        PARTITION BY eat.event_id 
        ORDER BY eat.assessment_time 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as running_avg_combined,
    -- Calculate convergence at this point in time
    ABS(
        AVG(CASE WHEN eat.assessment_type = 'judgment' THEN eat.assessment_value END) OVER (
            PARTITION BY eat.event_id 
            ORDER BY eat.assessment_time 
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) - 
        AVG(CASE WHEN eat.assessment_type = 'impact' THEN eat.assessment_value END) OVER (
            PARTITION BY eat.event_id 
            ORDER BY eat.assessment_time 
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        )
    ) as running_convergence,
    -- Count of assessments so far
    COUNT(*) OVER (
        PARTITION BY eat.event_id 
        ORDER BY eat.assessment_time 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as total_assessments_so_far,
    COUNT(CASE WHEN eat.assessment_type = 'judgment' THEN 1 END) OVER (
        PARTITION BY eat.event_id 
        ORDER BY eat.assessment_time 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as judgments_so_far,
    COUNT(CASE WHEN eat.assessment_type = 'impact' THEN 1 END) OVER (
        PARTITION BY eat.event_id 
        ORDER BY eat.assessment_time 
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as impacts_so_far
FROM event_assessment_timeline eat
ORDER BY eat.event_id, eat.assessment_time;
```

-- View for identifying when events have reached convergence stability
-- This view determines when assessments have stabilized
```sql
CREATE VIEW convergence_stability_detection AS
SELECT
    jic.event_id,
    jic.description,
    jic.judgment_impact_delta,
    jic.convergence_status,
    jic.judgment_count,
    jic.impact_count,
    jic.days_since_first_judgment,
    jic.days_since_first_impact,
    -- Check if convergence has remained stable over recent period
    CASE
        WHEN jic.judgment_impact_delta < 0.1 AND jic.judgment_count >= 5 AND jic.impact_count >= 5 THEN
            CASE
                WHEN jic.days_since_first_judgment >= 7 AND jic.days_since_first_impact >= 7 THEN 'STABLE_CONVERGED'
                WHEN jic.days_since_first_judgment >= 3 AND jic.days_since_first_impact >= 3 THEN 'EARLY_CONVERGED'
                ELSE 'PREMATURELY_CONVERGED'
            END
        WHEN jic.judgment_impact_delta < 0.2 AND jic.judgment_count >= 3 AND jic.impact_count >= 3 THEN
            CASE
                WHEN jic.days_since_first_judgment >= 7 AND jic.days_since_first_impact >= 7 THEN 'STABLE_NEARLY_CONVERGED'
                ELSE 'VOLATILE_NEARLY_CONVERGED'
            END
        ELSE
            CASE
                WHEN jic.judgment_count < 3 OR jic.impact_count < 3 THEN 'INSUFFICIENT_DATA'
                WHEN jic.days_since_first_judgment < 3 OR jic.days_since_first_impact < 3 THEN 'TOO_EARLY_TO_ASSESS'
                ELSE 'GENUINE_DIVERENCE'
            END
    END as stability_classification,
    -- Calculate how long convergence has been maintained
    (SELECT MIN(abs_diff) FROM (
        SELECT ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) as abs_diff
        FROM truth_event te
        JOIN event_state_history esh ON te.id = (SELECT created_by FROM event_ci WHERE id = esh.event_id LIMIT 1)
        WHERE te.id = jic.event_id
        AND esh.recorded_at > (SELECT strftime('%s', 'now') - 86400*3)  -- Last 3 days
    )) as min_recent_delta,
    (SELECT MAX(abs_diff) FROM (
        SELECT ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) as abs_diff
        FROM truth_event te
        JOIN event_state_history esh ON te.id = (SELECT created_by FROM event_ci WHERE id = esh.event_id LIMIT 1)
        WHERE te.id = jic.event_id
        AND esh.recorded_at > (SELECT strftime('%s', 'now') - 86400*3)  -- Last 3 days
    )) as max_recent_delta,
    -- Variance in recent convergence
    (SELECT AVG(POWER(abs_diff - avg_diff, 2))
     FROM (
         SELECT 
             ABS(COALESCE(te.judgment_score, 0.5) - COALESCE(te.impact_score, 0.0)) as abs_diff,
             (SELECT AVG(ABS(COALESCE(te2.judgment_score, 0.5) - COALESCE(te2.impact_score, 0.0))
              FROM truth_event te2
              JOIN event_state_history esh2 ON te2.id = (SELECT created_by FROM event_ci WHERE id = esh2.event_id LIMIT 1)
              WHERE te2.id = jic.event_id
              AND esh2.recorded_at > (SELECT strftime('%s', 'now') - 86400*3)) as avg_diff
         FROM truth_event te
         JOIN event_state_history esh ON te.id = (SELECT created_by FROM event_ci WHERE id = esh.event_id LIMIT 1)
         WHERE te.id = jic.event_id
         AND esh.recorded_at > (SELECT strftime('%s', 'now') - 86400*3)  -- Last 3 days
     )) as recent_convergence_variance,
    -- Timestamp
    jic.analyzed_at
FROM judgment_impact_convergence jic;
```

-- View for forecasting convergence based on current trends
-- This view predicts when events might reach convergence
```sql
CREATE VIEW convergence_forecasting AS
SELECT
    csd.event_id,
    csd.description,
    csd.convergence_status,
    csd.stability_classification,
    csd.judgment_count,
    csd.impact_count,
    csd.days_since_first_judgment,
    csd.days_since_first_impact,
    -- Estimate of how many more assessments needed for convergence
    CASE
        WHEN csd.judgment_impact_delta < 0.1 THEN 0  -- Already converged
        WHEN csd.judgment_impact_delta < 0.3 THEN 
            CEIL(csd.judgment_impact_delta * 10) - (csd.judgment_count + csd.impact_count)
        ELSE 
            CEIL(csd.judgment_impact_delta * 20) - (csd.judgment_count + csd.impact_count)
    END as estimated_assessments_needed,
    -- Confidence in convergence (higher when more assessments exist)
    LEAST((csd.judgment_count + csd.impact_count) * 0.1, 1.0) as convergence_confidence,
    -- Predicted stability timeline
    CASE
        WHEN csd.stability_classification LIKE '%CONVERGED%' THEN 'STABLE'
        WHEN csd.estimated_assessments_needed <= 0 THEN 'IMMINENT_STABILITY'
        WHEN csd.estimated_assessments_needed <= 5 THEN 'SHORT_TERM_STABILITY'
        WHEN csd.estimated_assessments_needed <= 10 THEN 'MEDIUM_TERM_STABILITY'
        ELSE 'LONG_TERM_STABILITY'
    END as predicted_stability_timeline,
    -- Trend analysis
    CASE
        WHEN csd.min_recent_delta < csd.judgment_impact_delta THEN 'IMPROVING'
        WHEN csd.max_recent_delta > csd.judgment_impact_delta THEN 'DIVERGING'
        ELSE 'STABLE'
    END as convergence_trend,
    -- Risk level
    CASE
        WHEN csd.judgment_impact_delta > 0.5 THEN 'HIGH'
        WHEN csd.judgment_impact_delta > 0.3 THEN 'MEDIUM'
        ELSE 'LOW'
    END as divergence_risk_level
FROM convergence_stability_detection csd;