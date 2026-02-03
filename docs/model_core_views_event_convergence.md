# Event Convergence Analysis View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view analyzes convergence between impact and judgment axes for event classification, implementing the logic for determining when an event reaches stable truth and impact values as described in section 2.6.3.

## Purpose
The `event_convergence_analysis` view monitors the convergence of truth and consequence axes for events, helping to determine when an event has reached a stable state where further assessment changes are minimal. This is crucial for event classification and stability detection.

## SQL Implementation

```sql
-- View to analyze convergence between impact and judgment axes for events
CREATE VIEW event_convergence_analysis AS
SELECT 
    ec.id AS event_id,
    ec.event_type,
    ec.status,
    ec.resolution_data,
    
    -- Truth convergence analysis
    (
        SELECT 
            CASE 
                WHEN COUNT(*) >= 5 AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05 THEN 1
                ELSE 0
            END
        FROM (
            SELECT truth_score
            FROM event_state_history
            WHERE event_id = ec.id
            ORDER BY recorded_at DESC
            LIMIT 10
        )
    ) AS truth_converged,
    
    -- Impact convergence analysis
    (
        SELECT 
            CASE 
                WHEN COUNT(*) >= 5 AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05 THEN 1
                ELSE 0
            END
        FROM (
            SELECT impact_score
            FROM event_state_history
            WHERE event_id = ec.id
            ORDER BY recorded_at DESC
            LIMIT 10
        )
    ) AS impact_converged,
    
    -- Combined convergence status
    CASE 
        WHEN (
            SELECT COUNT(*) >= 5 AND 
                   ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05
            FROM (
                SELECT truth_score
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 10
            )
        ) AND (
            SELECT COUNT(*) >= 5 AND 
                   ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05
            FROM (
                SELECT impact_score
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 10
            )
        ) THEN 'CONVERGED'
        WHEN (
            SELECT COUNT(*) >= 5 AND 
                   ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05
            FROM (
                SELECT truth_score
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 10
            )
        ) THEN 'TRUTH_ONLY'
        WHEN (
            SELECT COUNT(*) >= 5 AND 
                   ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05
            FROM (
                SELECT impact_score
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 10
            )
        ) THEN 'IMPACT_ONLY'
        ELSE 'DIVERGED'
    END AS convergence_status,
    
    -- Rate of change for truth (derivative approximation)
    (
        SELECT 
            CASE 
                WHEN COUNT(*) >= 2 THEN
                    ABS(MAX(truth_score) - MIN(truth_score)) / 
                    (julianday(MAX(recorded_at)) - julianday(MIN(recorded_at)))
                ELSE 0
            END
        FROM (
            SELECT truth_score, recorded_at
            FROM event_state_history
            WHERE event_id = ec.id
            ORDER BY recorded_at DESC
            LIMIT 5
        )
    ) AS truth_change_rate,
    
    -- Rate of change for impact (derivative approximation)
    (
        SELECT 
            CASE 
                WHEN COUNT(*) >= 2 THEN
                    ABS(MAX(impact_score) - MIN(impact_score)) / 
                    (julianday(MAX(recorded_at)) - julianday(MIN(recorded_at)))
                ELSE 0
            END
        FROM (
            SELECT impact_score, recorded_at
            FROM event_state_history
            WHERE event_id = ec.id
            ORDER BY recorded_at DESC
            LIMIT 5
        )
    ) AS impact_change_rate,
    
    -- Stability indicator (low change rate over time)
    CASE 
        WHEN (
            SELECT 
                CASE 
                    WHEN COUNT(*) >= 2 THEN
                        ABS(MAX(truth_score) - MIN(truth_score)) / 
                        (julianday(MAX(recorded_at)) - julianday(MIN(recorded_at)))
                    ELSE 999
                END
            FROM (
                SELECT truth_score, recorded_at
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 5
            )
        ) < 0.01 AND (
            SELECT 
                CASE 
                    WHEN COUNT(*) >= 2 THEN
                        ABS(MAX(impact_score) - MIN(impact_score)) / 
                        (julianday(MAX(recorded_at)) - julianday(MIN(recorded_at)))
                    ELSE 999
                END
            FROM (
                SELECT impact_score, recorded_at
                FROM event_state_history
                WHERE event_id = ec.id
                ORDER BY recorded_at DESC
                LIMIT 5
            )
        ) < 0.01 THEN 1
        ELSE 0
    END AS is_stable,
    
    -- Number of recent assessments contributing to current state
    (
        SELECT COUNT(*)
        FROM event_state_history
        WHERE event_id = ec.id
        AND recorded_at > datetime('now', '-1 day')
    ) AS recent_assessment_count,
    
    -- Time since last significant change (>0.1 threshold)
    (
        SELECT 
            CASE 
                WHEN MAX(significant_change_time) IS NOT NULL THEN
                    julianday('now') - julianday(MAX(significant_change_time))
                ELSE julianday('now') - julianday(ec.created_at)
            END
        FROM (
            SELECT recorded_at as significant_change_time
            FROM event_state_history
            WHERE event_id = ec.id
            AND ABS(truth_score - LAG(truth_score, 1, truth_score) OVER (ORDER BY recorded_at)) > 0.1
            OR ABS(impact_score - LAG(impact_score, 1, impact_score) OVER (ORDER BY recorded_at)) > 0.1
            ORDER BY recorded_at DESC
            LIMIT 1
        )
    ) AS days_since_last_significant_change,
    
    ec.created_at AS event_created_at,
    CURRENT_TIMESTAMP AS analysis_timestamp

FROM event_ci ec;

-- View to identify events ready for classification
CREATE VIEW convergence_ready_events AS
SELECT 
    eca.event_id,
    eca.convergence_status,
    eca.truth_change_rate,
    eca.impact_change_rate,
    eca.is_stable,
    eca.days_since_last_significant_change,
    -- Determine if event is ready for final classification
    CASE 
        WHEN eca.convergence_status = 'CONVERGED' 
             AND eca.is_stable = 1 
             AND eca.days_since_last_significant_change >= 7 THEN 1
        ELSE 0
    END AS ready_for_classification
FROM event_convergence_analysis eca
WHERE eca.recent_assessment_count > 0;

-- View to monitor convergence trends
CREATE VIEW convergence_trends AS
SELECT 
    convergence_status,
    COUNT(*) as event_count,
    AVG(truth_change_rate) as avg_truth_change_rate,
    AVG(impact_change_rate) as avg_impact_change_rate,
    AVG(days_since_last_significant_change) as avg_days_since_change,
    COUNT(CASE WHEN is_stable = 1 THEN 1 END) as stable_events,
    COUNT(CASE WHEN ready_for_classification = 1 THEN 1 END) as ready_for_classification
FROM (
    SELECT 
        event_id,
        convergence_status,
        truth_change_rate,
        impact_change_rate,
        days_since_last_significant_change,
        is_stable,
        ready_for_classification
    FROM convergence_ready_events
) cre
GROUP BY convergence_status;
```

## Key Features

### Multi-Axis Convergence Detection
The view analyzes convergence on both truth and impact axes independently, then determines the overall convergence status of the event.

### Rate of Change Analysis
Calculates the rate of change for both truth and impact scores to determine if an event is still evolving or has stabilized.

### Stability Indicators
Provides boolean flags to quickly identify whether an event has reached a stable state based on low change rates.

### Time-Based Analysis
Tracks how long it has been since the last significant change, which is important for determining when an event has truly converged.

### Classification Readiness
Determines when an event is ready for final classification based on convergence, stability, and time since last significant change.

## Relationship to Model Core
This view implements the convergence detection logic described in the model, where:
- Events have two orthogonal axes (truth and impact) that must converge
- Convergence is determined by low rates of change over time
- Stability is a prerequisite for event classification
- The system tracks how assessment axes evolve over time

## Usage Examples

```sql
-- Get convergence analysis for a specific event
SELECT * FROM event_convergence_analysis WHERE event_id = ?;

-- Find events ready for final classification
SELECT * FROM convergence_ready_events WHERE ready_for_classification = 1;

-- Monitor convergence trends across all events
SELECT * FROM convergence_trends;

-- Identify events that have recently changed significantly
SELECT * FROM event_convergence_analysis 
WHERE days_since_last_significant_change < 1;
```

## Integration with Other Components
- Works with `event_state_history` to track evolution over time
- Feeds into `event_ci` status updates when convergence is achieved
- Supports `event_stability` detection by providing stability indicators
- Used in `event_projection` for final quadrant classification

## Notes
- The view uses a sliding window approach to analyze recent history
- Convergence thresholds can be adjusted based on system requirements
- The view is optimized for periodic refresh to track ongoing convergence