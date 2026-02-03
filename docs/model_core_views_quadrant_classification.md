# Quadrant Classification Calculation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view determines event quadrant classification (Q1-Q4) based on truth and impact scores, implementing the classification algorithm described in section 2.3.1.

## Purpose
The `quadrant_classification_calculation` view assigns each event to one of four quadrants based on its truth score and impact score, enabling systematic categorization and prioritization of events according to their truth value and potential consequences.

## SQL Implementation

```sql
-- View to determine event quadrant classification based on truth and impact scores
CREATE VIEW quadrant_classification_calculation AS
SELECT 
    ep.event_id,
    ep.truth_score,
    ep.impact_score,
    
    -- Determine quadrant based on truth and impact thresholds
    CASE 
        WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 'Q1'  -- High truth, High impact
        WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN 'Q2'   -- High truth, Low impact  
        WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 'Q3'   -- Low truth, High impact
        ELSE 'Q4'                                                       -- Low truth, Low impact
    END AS quadrant,
    
    -- Quadrant descriptions
    CASE 
        WHEN ep.truth_score >= 0.5 AND ep.impact_score >= 0 THEN 'Critical real event'
        WHEN ep.truth_score >= 0.5 AND ep.impact_score < 0 THEN 'Fact without significant consequences'
        WHEN ep.truth_score < 0.5 AND ep.impact_score >= 0 THEN 'Dangerous disinformation'
        ELSE 'Noise / information garbage'
    END AS quadrant_description,
    
    -- Distance from quadrant center
    SQRT(
        POWER(ep.truth_score - 
            CASE 
                WHEN ep.quadrant = 'Q1' THEN 0.75
                WHEN ep.quadrant = 'Q2' THEN 0.75
                WHEN ep.quadrant = 'Q3' THEN 0.25
                ELSE 0.25
            END, 2) +
        POWER(ep.impact_score - 
            CASE 
                WHEN ep.quadrant IN ('Q1', 'Q3') THEN 0.5
                ELSE -0.5
            END, 2)
    ) AS distance_from_quadrant_center,
    
    -- Confidence in classification (distance from decision boundaries)
    CASE 
        WHEN ep.quadrant = 'Q1' THEN LEAST(ep.truth_score - 0.5, ep.impact_score - 0)  -- Distance from boundaries
        WHEN ep.quadrant = 'Q2' THEN LEAST(ep.truth_score - 0.5, 0 - ep.impact_score)
        WHEN ep.quadrant = 'Q3' THEN LEAST(0.5 - ep.truth_score, ep.impact_score - 0)
        ELSE LEAST(0.5 - ep.truth_score, 0 - ep.impact_score)
    END AS classification_confidence,
    
    -- Risk level based on quadrant
    CASE 
        WHEN ep.quadrant = 'Q1' THEN 'MEDIUM'      -- High truth, High impact - important but valid
        WHEN ep.quadrant = 'Q2' THEN 'LOW'         -- High truth, Low impact - low priority
        WHEN ep.quadrant = 'Q3' THEN 'CRITICAL'    -- Low truth, High impact - dangerous disinformation
        ELSE 'LOW'                                 -- Low truth, Low impact - ignore
    END AS risk_level,
    
    -- Action recommendation based on quadrant
    CASE 
        WHEN ep.quadrant = 'Q1' THEN 'MONITOR'         -- Critical real events - monitor for changes
        WHEN ep.quadrant = 'Q2' THEN 'ARCHIVE'         -- Historical facts - archive for reference
        WHEN ep.quadrant = 'Q3' THEN 'ALERT'           -- Dangerous disinformation - alert moderators
        ELSE 'IGNORE'                                  -- Noise - ignore
    END AS recommended_action,
    
    -- Truth and impact categories
    CASE 
        WHEN ep.truth_score >= 0.5 THEN 'HIGH_TRUTH'
        ELSE 'LOW_TRUTH'
    END AS truth_category,
    
    CASE 
        WHEN ep.impact_score >= 0 THEN 'POSITIVE_IMPACT'
        ELSE 'NEGATIVE_IMPACT'
    END AS impact_category,
    
    -- Magnitude of impact
    ABS(ep.impact_score) AS impact_magnitude,
    
    -- Stability indicator (how close to the boundary)
    CASE 
        WHEN ABS(ep.truth_score - 0.5) > 0.2 AND ABS(ep.impact_score) > 0.2 THEN 1  -- Stable classification
        ELSE 0  -- Unstable classification near boundaries
    END AS is_classification_stable,
    
    -- Sensitivity to change (closer to boundary = more sensitive)
    1.0 - LEAST(ABS(ep.truth_score - 0.5) * 2, ABS(ep.impact_score) * 2, 1.0) AS sensitivity_score,
    
    -- Event information
    te.description AS event_description,
    te.global_id AS event_global_id,
    te.participant_id AS event_creator_id,
    p.public_key AS event_creator_public_key,
    te.created_at AS event_created_at,
    
    -- Event context
    cat.name AS category_name,
    f.name AS forma_name,
    ca.name AS cause_name,
    d.name AS develop_name,
    e.name AS effect_name,
    
    -- Timeline information
    et.time_axis_id AS event_time_axis,
    et.t_start AS event_start_time,
    et.t_end AS event_end_time,
    
    -- Additional metrics
    CASE 
        WHEN ep.impact_score > 0.5 THEN 'HIGH_POSITIVE'
        WHEN ep.impact_score > 0 THEN 'LOW_POSITIVE'
        WHEN ep.impact_score > -0.5 THEN 'LOW_NEGATIVE'
        ELSE 'HIGH_NEGATIVE'
    END AS impact_severity,
    
    -- Truth certainty
    CASE 
        WHEN ABS(ep.truth_score - 0.5) > 0.4 THEN 'CERTAIN'
        WHEN ABS(ep.truth_score - 0.5) > 0.2 THEN 'PROBABLE'
        ELSE 'UNCERTAIN'
    END AS truth_certainty,
    
    -- Timestamp of calculation
    ep.calculated_at AS classification_timestamp,
    CURRENT_TIMESTAMP AS view_calculation_timestamp

FROM event_projection ep
JOIN truth_event te ON ep.event_id = (
    SELECT event_ci.id
    FROM event_ci
    WHERE event_ci.created_by = te.id
)
JOIN participants p ON te.participant_id = p.id
LEFT JOIN category cat ON te.category_id = cat.id
LEFT JOIN forma f ON te.forma_id = f.id
LEFT JOIN cause ca ON te.cause_id = ca.id
LEFT JOIN develop d ON te.develop_id = d.id
LEFT JOIN effect e ON te.effect_id = e.id
LEFT JOIN event_timeline et ON te.timeline_id = et.id;

-- View for quadrant-based event prioritization
CREATE VIEW quadrant_based_prioritization AS
SELECT 
    qcc.event_id,
    qcc.quadrant,
    qcc.quadrant_description,
    qcc.risk_level,
    qcc.recommended_action,
    qcc.classification_confidence,
    qcc.is_classification_stable,
    
    -- Priority score (higher = more priority)
    CASE 
        WHEN qcc.quadrant = 'Q3' THEN 100  -- Dangerous disinformation
        WHEN qcc.quadrant = 'Q1' THEN 80   -- Critical real event
        WHEN qcc.quadrant = 'Q2' THEN 30   -- Fact without consequences
        ELSE 10                            -- Noise
    END AS priority_score,
    
    -- Urgency indicator
    CASE 
        WHEN qcc.risk_level = 'CRITICAL' AND qcc.classification_confidence > 0.3 THEN 'HIGH'
        WHEN qcc.risk_level = 'MEDIUM' AND qcc.classification_confidence > 0.2 THEN 'MEDIUM'
        WHEN qcc.risk_level = 'LOW' AND qcc.classification_confidence > 0.1 THEN 'LOW'
        ELSE 'NONE'
    END AS urgency_level,
    
    -- Resource allocation recommendation
    CASE 
        WHEN qcc.quadrant = 'Q3' AND qcc.impact_magnitude > 0.5 THEN 'HIGH_RESOURCES'
        WHEN qcc.quadrant = 'Q1' AND qcc.impact_magnitude > 0.5 THEN 'MEDIUM_RESOURCES'
        WHEN qcc.quadrant = 'Q2' THEN 'LOW_RESOURCES'
        ELSE 'MINIMAL_RESOURCES'
    END AS resource_allocation,
    
    -- Monitoring frequency recommendation
    CASE 
        WHEN qcc.quadrant = 'Q3' THEN 'CONTINUOUS'  -- Monitor constantly for dangerous disinfo
        WHEN qcc.quadrant = 'Q1' THEN 'FREQUENT'    -- Regular monitoring for critical events
        WHEN qcc.quadrant = 'Q2' THEN 'OCCASIONAL'  -- Occasional checks for historical facts
        ELSE 'NEVER'                                -- No monitoring for noise
    END AS monitoring_frequency,
    
    qcc.event_description,
    qcc.event_creator_public_key,
    qcc.impact_magnitude,
    qcc.truth_certainty,
    qcc.impact_severity,
    qcc.distance_from_quadrant_center,
    qcc.sensitivity_score,
    
    -- Weighted score combining risk and confidence
    CASE 
        WHEN qcc.risk_level = 'CRITICAL' THEN qcc.classification_confidence * 10
        WHEN qcc.risk_level = 'MEDIUM' THEN qcc.classification_confidence * 5
        WHEN qcc.risk_level = 'LOW' THEN qcc.classification_confidence * 1
        ELSE 0
    END AS weighted_risk_score

FROM quadrant_classification_calculation qcc
ORDER BY 
    CASE 
        WHEN qcc.quadrant = 'Q3' THEN 1  -- Disinformation first
        WHEN qcc.quadrant = 'Q1' THEN 2  -- Critical events second
        WHEN qcc.quadrant = 'Q2' THEN 3  -- Facts third
        ELSE 4                           -- Noise last
    END,
    qcc.classification_confidence DESC;

-- View for quadrant statistics and analysis
CREATE VIEW quadrant_statistics AS
SELECT 
    quadrant,
    quadrant_description,
    risk_level,
    recommended_action,
    
    -- Counts
    COUNT(*) AS event_count,
    SUM(CASE WHEN is_classification_stable = 1 THEN 1 ELSE 0 END) AS stable_classifications,
    SUM(CASE WHEN is_classification_stable = 0 THEN 1 ELSE 0 END) AS unstable_classifications,
    
    -- Percentages
    COUNT(*) * 100.0 / SUM(COUNT(*)) OVER () AS percentage_of_total,
    AVG(classification_confidence) AS avg_classification_confidence,
    MIN(classification_confidence) AS min_classification_confidence,
    MAX(classification_confidence) AS max_classification_confidence,
    
    -- Truth and impact statistics
    AVG(truth_score) AS avg_truth_score,
    MIN(truth_score) AS min_truth_score,
    MAX(truth_score) AS max_truth_score,
    AVG(impact_score) AS avg_impact_score,
    MIN(impact_score) AS min_impact_score,
    MAX(impact_score) AS max_impact_score,
    AVG(impact_magnitude) AS avg_impact_magnitude,
    
    -- Age statistics
    AVG(julianday('now') - julianday(created_at, 'unixepoch')) AS avg_days_since_classification,
    MIN(julianday('now') - julianday(created_at, 'unixepoch')) AS min_days_since_classification,
    MAX(julianday('now') - julianday(created_at, 'unixepoch')) AS max_days_since_classification,
    
    -- Sensitivity analysis
    AVG(sensitivity_score) AS avg_sensitivity,
    MIN(sensitivity_score) AS min_sensitivity,
    MAX(sensitivity_score) AS max_sensitivity,
    
    -- Certainty analysis
    SUM(CASE WHEN truth_certainty = 'CERTAIN' THEN 1 ELSE 0 END) AS certain_truth_events,
    SUM(CASE WHEN truth_certainty = 'PROBABLE' THEN 1 ELSE 0 END) AS probable_truth_events,
    SUM(CASE WHEN truth_certainty = 'UNCERTAIN' THEN 1 ELSE 0 END) AS uncertain_truth_events,
    
    -- Impact severity analysis
    SUM(CASE WHEN impact_severity = 'HIGH_POSITIVE' THEN 1 ELSE 0 END) AS high_positive_impact,
    SUM(CASE WHEN impact_severity = 'LOW_POSITIVE' THEN 1 ELSE 0 END) AS low_positive_impact,
    SUM(CASE WHEN impact_severity = 'LOW_NEGATIVE' THEN 1 ELSE 0 END) AS low_negative_impact,
    SUM(CASE WHEN impact_severity = 'HIGH_NEGATIVE' THEN 1 ELSE 0 END) AS high_negative_impact

FROM quadrant_classification_calculation
GROUP BY quadrant, quadrant_description, risk_level, recommended_action
ORDER BY 
    CASE 
        WHEN quadrant = 'Q3' THEN 1  -- Disinformation first
        WHEN quadrant = 'Q1' THEN 2  -- Critical events second
        WHEN quadrant = 'Q2' THEN 3  -- Facts third
        ELSE 4                       -- Noise last
    END;

-- View for monitoring quadrant shifts
CREATE VIEW quadrant_shift_monitoring AS
SELECT 
    esh.event_id,
    qcc.quadrant AS current_quadrant,
    prev_qcc.quadrant AS previous_quadrant,
    
    -- Check if quadrant changed
    CASE 
        WHEN qcc.quadrant != prev_qcc.quadrant THEN 1
        ELSE 0
    END AS quadrant_changed,
    
    -- Time since last classification change
    julianday('now') - julianday(prev_qcc.classification_timestamp, 'unixepoch') AS days_in_previous_quadrant,
    
    -- Direction of change
    CASE 
        WHEN qcc.quadrant = 'Q1' AND prev_qcc.quadrant = 'Q2' THEN 'TRUTH_IMPROVEMENT'
        WHEN qcc.quadrant = 'Q2' AND prev_qcc.quadrant = 'Q1' THEN 'TRUTH_DETERIORATION'
        WHEN qcc.quadrant = 'Q3' AND prev_qcc.quadrant = 'Q4' THEN 'DANGER_INCREASE'
        WHEN qcc.quadrant = 'Q4' AND prev_qcc.quadrant = 'Q3' THEN 'DANGER_DECREASE'
        WHEN qcc.quadrant = 'Q1' AND prev_qcc.quadrant = 'Q3' THEN 'VALIDATION'
        WHEN qcc.quadrant = 'Q3' AND prev_qcc.quadrant = 'Q1' THEN 'DISCREDITATION'
        ELSE 'OTHER_TRANSITION'
    END AS transition_type,
    
    -- Magnitude of change
    ABS(qcc.truth_score - prev_qcc.truth_score) + ABS(qcc.impact_score - prev_qcc.impact_score) AS change_magnitude,
    
    qcc.event_description,
    qcc.event_creator_public_key,
    qcc.risk_level AS current_risk_level,
    prev_qcc.risk_level AS previous_risk_level,
    
    -- Severity of transition
    CASE 
        WHEN qcc.quadrant = 'Q3' AND prev_qcc.quadrant != 'Q3' THEN 'HIGH'
        WHEN qcc.risk_level != prev_qcc.risk_level THEN 'MEDIUM'
        ELSE 'LOW'
    END AS transition_severity,
    
    qcc.classification_timestamp AS current_classification_time,
    prev_qcc.classification_timestamp AS previous_classification_time

FROM event_state_history esh
JOIN quadrant_classification_calculation qcc ON esh.event_id = qcc.event_id
JOIN (
    SELECT 
        event_id,
        quadrant,
        truth_score,
        impact_score,
        risk_level,
        event_description,
        event_creator_public_key,
        classification_timestamp,
        ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY classification_timestamp DESC) as rn
    FROM quadrant_classification_calculation
) prev_qcc ON esh.event_id = prev_qcc.event_id AND prev_qcc.rn = 2  -- Previous classification
WHERE qcc.quadrant != prev_qcc.quadrant;  -- Only show events that changed quadrants
```

## Key Features

### Quadrant Classification Algorithm
Implements the exact quadrant classification algorithm specified in the model:
- Q1: High truth (≥0.5), High impact (≥0) → Critical real event
- Q2: High truth (≥0.5), Low impact (<0) → Fact without significant consequences
- Q3: Low truth (<0.5), High impact (≥0) → Dangerous disinformation
- Q4: Low truth (<0.5), Low impact (<0) → Noise/information garbage

### Confidence and Stability Metrics
Provides metrics to assess the confidence and stability of quadrant classifications, including:
- Distance from quadrant center
- Classification confidence based on distance from decision boundaries
- Stability indicators for near-boundary classifications

### Actionable Recommendations
Generates practical recommendations for handling events in each quadrant:
- Risk levels
- Recommended actions
- Resource allocation suggestions
- Monitoring frequency recommendations

### Quadrant Statistics
Provides aggregate statistics for analysis of the distribution of events across quadrants.

### Quadrant Shift Monitoring
Tracks when events move between quadrants and categorizes the nature of these transitions.

## Relationship to Model Core
This view implements the quadrant classification system described in the model where:
- Events are classified into four quadrants based on truth and impact scores
- Q1 events require action as verified information
- Q2 events are historical facts with low priority
- Q3 events represent potential threats requiring attention
- Q4 events are noise to be ignored
- The system uses quadrants for event prioritization and response planning

## Usage Examples

```sql
-- Get quadrant classification for a specific event
SELECT * FROM quadrant_classification_calculation WHERE event_id = ?;

-- Get prioritized list of events by quadrant
SELECT * FROM quadrant_based_prioritization ORDER BY priority_score DESC;

-- Get statistics about quadrant distribution
SELECT * FROM quadrant_statistics;

-- Find events that have recently changed quadrants
SELECT * FROM quadrant_shift_monitoring 
WHERE days_in_previous_quadrant < 7;

-- Find critical disinformation events (Q3)
SELECT * FROM quadrant_classification_calculation 
WHERE quadrant = 'Q3' AND risk_level = 'CRITICAL';
```

## Integration with Other Components
- Works with `event_projection` table to get truth and impact scores
- Connects to `truth_event`, `participants`, and context tables for event details
- Feeds into event prioritization and resource allocation decisions
- Supports monitoring and alerting systems based on quadrant classifications
- Used in `event_stability` analysis to track classification changes over time

## Notes
- The view uses standardized thresholds (0.5 for truth, 0 for impact) as specified in the model
- Classification confidence is calculated based on distance from decision boundaries
- The system provides both individual event classification and aggregate statistics
- Quadrant shifts are monitored to detect changes in event characteristics over time