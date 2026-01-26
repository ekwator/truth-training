-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Quadrant Classification Based on Truth and Impact Scores  

-- Determines event quadrant classification (Q1-Q4) based on truth and impact scores
-- Implements the classification algorithm described in section 2.3.1
-- Links: event_projection.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE VIEW quadrant_classification_calculation AS
SELECT
    ep.event_id,
    ep.truth_score,
    ep.impact_score,
    -- Determine quadrant classification based on truth and impact scores
    CASE
        WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 'Q1'  -- Critical real event
        WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN 'Q2'   -- Fact without significant consequences
        WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 'Q3'   -- Dangerous disinformation
        ELSE 'Q4'                                                       -- Noise / information garbage
    END as quadrant,
    -- Quadrant descriptions
    CASE
        WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 'Critical real event'
        WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN 'Fact without significant consequences'
        WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 'Dangerous disinformation'
        ELSE 'Noise / information garbage'
    END as quadrant_description,
    -- Distance from quadrant center
    SQRT(
        POWER(
            ep.truth_score - CASE
                WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 0.75
                WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN 0.75
                WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 0.25
                ELSE 0.25
            END, 
            2
        ) + 
        POWER(
            ep.impact_score - CASE
                WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 0.5
                WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN -0.5
                WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 0.5
                ELSE -0.5
            END, 
            2
        )
    ) as distance_from_center,
    -- Timestamp
    ep.calculated_at
FROM event_projection ep;
```

-- Alternative view with more detailed quadrant analysis
-- This view provides additional metrics for quadrant classification
```sql
CREATE VIEW quadrant_classification_detailed AS
SELECT
    qcc.event_id,
    qcc.truth_score,
    qcc.impact_score,
    qcc.quadrant,
    qcc.quadrant_description,
    qcc.distance_from_center,
    -- Confidence in classification (based on distance from decision boundary)
    CASE
        WHEN qcc.quadrant = 'Q1' THEN 
            LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0))
        WHEN qcc.quadrant = 'Q2' THEN 
            LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0))
        WHEN qcc.quadrant = 'Q3' THEN 
            LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0))
        ELSE 
            LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0))
    END as classification_confidence,
    -- Risk level based on quadrant
    CASE
        WHEN qcc.quadrant = 'Q1' THEN 'HIGH'      -- Critical real event
        WHEN qcc.quadrant = 'Q2' THEN 'LOW'       -- Fact without significant consequences
        WHEN qcc.quadrant = 'Q3' THEN 'CRITICAL'  -- Dangerous disinformation
        ELSE 'IGNORE'                             -- Noise / information garbage
    END as risk_level,
    -- Priority level based on quadrant
    CASE
        WHEN qcc.quadrant = 'Q1' THEN 4          -- Highest priority
        WHEN qcc.quadrant = 'Q3' THEN 3          -- High priority
        WHEN qcc.quadrant = 'Q2' THEN 2          -- Medium priority
        ELSE 1                                   -- Lowest priority
    END as priority_level
FROM quadrant_classification_calculation qcc;
```

-- View for analyzing quadrant distribution
-- This view provides statistics about the distribution of events across quadrants
```sql
CREATE VIEW quadrant_distribution_analysis AS
SELECT
    quadrant,
    COUNT(*) as event_count,
    -- Percentage of events in this quadrant
    COUNT(*) * 100.0 / (SELECT COUNT(*) FROM quadrant_classification_calculation) as percentage,
    -- Average truth and impact scores for this quadrant
    AVG(truth_score) as avg_truth_score,
    AVG(impact_score) as avg_impact_score,
    -- Standard deviation of scores in this quadrant
    SQRT(AVG(POWER(truth_score - (SELECT AVG(truth_score) FROM quadrant_classification_calculation qcc2 WHERE qcc2.quadrant = qcc.quadrant), 2))) as truth_score_stddev,
    SQRT(AVG(POWER(impact_score - (SELECT AVG(impact_score) FROM quadrant_classification_calculation qcc2 WHERE qcc2.quadrant = qcc.quadrant), 2))) as impact_score_stddev,
    -- Most recent event in this quadrant
    MAX(calculated_at) as last_event_time
FROM quadrant_classification_calculation qcc
GROUP BY quadrant
ORDER BY event_count DESC;
```

-- View for identifying events near quadrant boundaries
-- This view finds events that are close to decision boundaries between quadrants
```sql
CREATE VIEW boundary_events_analysis AS
SELECT
    qcc.event_id,
    qcc.truth_score,
    qcc.impact_score,
    qcc.quadrant,
    -- Distance to nearest boundary
    LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0)) as distance_to_boundary,
    -- Flag for events very close to boundary (within 0.1 unit)
    CASE
        WHEN LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0)) < 0.1 THEN 1
        ELSE 0
    END as is_boundary_event,
    -- Potential alternative classification
    CASE
        WHEN ABS(qcc.truth_score - 0.5) < 0.1 AND qcc.impact_score >= 0 THEN 'Could be Q2 if truth < 0.5'
        WHEN ABS(qcc.truth_score - 0.5) < 0.1 AND qcc.impact_score < 0 THEN 'Could be Q1 if truth >= 0.5'
        WHEN ABS(qcc.impact_score - 0) < 0.1 AND qcc.truth_score >= 0.5 THEN 'Could be Q3 if impact < 0, or Q2 if impact < 0'
        WHEN ABS(qcc.impact_score - 0) < 0.1 AND qcc.truth_score < 0.5 THEN 'Could be Q1 if impact >= 0, or Q4 if impact < 0'
        ELSE 'Stable classification'
    END as boundary_analysis
FROM quadrant_classification_calculation qcc
WHERE LEAST(ABS(qcc.truth_score - 0.5), ABS(qcc.impact_score - 0)) < 0.15  -- Within 0.15 units of boundary
ORDER BY distance_to_boundary ASC;