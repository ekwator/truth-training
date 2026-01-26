-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Event Convergence Analysis Between Impact and Judgment Axes  

-- Analyzes convergence between impact and judgment axes for event classification
-- Implements the logic for determining when an event reaches stable truth and impact values
-- Uses the relationships: event_ci.id → impact_metrics.event_id, event_ci.id → judgment_weights.event_id
```sql
CREATE VIEW event_convergence_analysis AS
SELECT
    ec.id as event_id,
    ec.event_type,
    ec.status,
    -- Determine if impact axis is active/converged
    CASE
        WHEN EXISTS (
            SELECT 1 FROM impact_metrics im
            WHERE im.event_id = ec.id
            AND (im.positive_ratio IS NOT NULL OR im.negative_ratio IS NOT NULL OR im.uncertainty IS NOT NULL)
        )
        THEN 1
        ELSE 0
    END as impact_converged,
    -- Determine if judgment axis is active/converged
    CASE
        WHEN EXISTS (
            SELECT 1 FROM judgment_weights jw
            WHERE jw.event_id = ec.id
            AND jw.weight IS NOT NULL
        )
        THEN 1
        ELSE 0
    END as judgment_converged,
    -- Calculate convergence status
    CASE
        WHEN NOT (
            EXISTS (
                SELECT 1 FROM impact_metrics im
                WHERE im.event_id = ec.id
                AND (im.positive_ratio IS NOT NULL OR im.negative_ratio IS NOT NULL OR im.uncertainty IS NOT NULL)
            ) OR
            EXISTS (
                SELECT 1 FROM judgment_weights jw
                WHERE jw.event_id = ec.id
                AND jw.weight IS NOT NULL
            )
        ) THEN 'unstable'
        WHEN (
            EXISTS (
                SELECT 1 FROM impact_metrics im
                WHERE im.event_id = ec.id
                AND (im.positive_ratio IS NOT NULL OR im.negative_ratio IS NOT NULL OR im.uncertainty IS NOT NULL)
            ) XOR
            EXISTS (
                SELECT 1 FROM judgment_weights jw
                WHERE jw.event_id = ec.id
                AND jw.weight IS NOT NULL
            )
        ) THEN 'suppose'
        WHEN (
            EXISTS (
                SELECT 1 FROM impact_metrics im
                WHERE im.event_id = ec.id
                AND (im.positive_ratio IS NOT NULL OR im.negative_ratio IS NOT NULL OR im.uncertainty IS NOT NULL)
            ) AND
            EXISTS (
                SELECT 1 FROM judgment_weights jw
                WHERE jw.event_id = ec.id
                AND jw.weight IS NOT NULL
            ) AND
            ec.event_type = 'both' AND
            (ec.status = 'resolved' OR ec.status = 'archived')
        ) THEN 'consent'
        ELSE 'unstable'
    END as convergence_status,
    -- Calculate how close the event is to convergence
    CASE
        WHEN EXISTS (
            SELECT 1 FROM impact_metrics im
            WHERE im.event_id = ec.id
        ) AND EXISTS (
            SELECT 1 FROM judgment_weights jw
            WHERE jw.event_id = ec.id
        )
        THEN 1.0  -- Both axes active
        WHEN EXISTS (
            SELECT 1 FROM impact_metrics im
            WHERE im.event_id = ec.id
        ) OR EXISTS (
            SELECT 1 FROM judgment_weights jw
            WHERE jw.event_id = ec.id
        )
        THEN 0.5  -- One axis active
        ELSE 0.0  -- No axis active
    END as convergence_score,
    -- Timestamp of analysis
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM event_ci ec;
```

-- Alternative view that includes more detailed convergence metrics
-- This view provides additional metrics for measuring convergence between axes
```sql
CREATE VIEW event_convergence_detailed AS
SELECT
    ec.id as event_id,
    ec.event_type,
    ec.status,
    -- Impact metrics
    im.positive_ratio as impact_positive_ratio,
    im.negative_ratio as impact_negative_ratio,
    im.uncertainty as impact_uncertainty,
    im.total_magnitude as impact_magnitude,
    -- Judgment metrics
    jw.weight as judgment_weight,
    cc.confidence_score as judgment_confidence,
    -- Calculate convergence delta between axes
    ABS(
        COALESCE(im.total_magnitude, 0) - 
        COALESCE(cc.confidence_score, 0)
    ) as axis_convergence_delta,
    -- Stability indicators
    CASE
        WHEN ABS(
            COALESCE(im.total_magnitude, 0) - 
            COALESCE(cc.confidence_score, 0)
        ) < (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)
        THEN 1
        ELSE 0
    END as axis_stable,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM event_ci ec
LEFT JOIN impact_metrics im ON ec.id = im.event_id
LEFT JOIN judgment_weights jw ON ec.id = jw.event_id
LEFT JOIN consensus_ci cc ON ec.id = cc.event_id;
```

-- View for detecting when events reach convergence thresholds
-- This view identifies events that have reached stable truth and impact values
```sql
CREATE VIEW event_convergence_detection AS
SELECT
    ecd.event_id,
    ecd.convergence_score,
    ecd.axis_convergence_delta,
    ecd.axis_stable,
    ecd.analyzed_at,
    -- Determine if event has reached stable state
    CASE
        WHEN ecd.axis_stable = 1 
             AND ecd.convergence_score = 1.0
             AND ec.status IN ('resolved', 'archived')
        THEN 1
        ELSE 0
    END as is_stable,
    -- Days since event creation
    (SELECT julianday('now') - julianday(te.created_at, 'unixepoch')
     FROM truth_event te
     JOIN event_ci eci ON te.id = eci.created_by
     WHERE eci.id = ecd.event_id
    ) as days_since_creation
FROM event_convergence_detailed ecd
JOIN event_ci ec ON ecd.event_id = ec.id;