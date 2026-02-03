# Aggregated Metrics Triggers

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This document describes the SQL triggers that implement the system metrics and expert functions schema for the Truth Training system. These triggers update aggregated metrics when new data is added to maintain system-wide statistics.

## Purpose
The aggregated metrics triggers ensure that system-wide metrics are automatically updated when new events, impacts, or judgments are added, maintaining data consistency and enabling accurate analysis of system performance and expert function effectiveness.

## Trigger Definitions

### 1. update_heuristic_weight_after_accuracy_change
This trigger updates the weight of expert heuristics based on their proven accuracy when the accuracy changes.

```sql
CREATE TRIGGER update_heuristic_weight_after_accuracy_change
AFTER UPDATE ON expert_heuristics
FOR EACH ROW
WHEN OLD.proven_accuracy != NEW.proven_accuracy
BEGIN
    -- Update heuristic weight based on proven accuracy
    UPDATE expert_heuristics
    SET 
        weight = CASE 
            WHEN NEW.proven_accuracy > 0.8 THEN 1.0
            WHEN NEW.proven_accuracy > 0.6 THEN 0.7
            WHEN NEW.proven_accuracy > 0.4 THEN 0.5
            WHEN NEW.proven_accuracy > 0.2 THEN 0.3
            ELSE 0.1
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    
    -- Update the heuristic influence in the system
    INSERT OR REPLACE INTO heuristic_influence_calculation (
        heuristic_id,
        influence_score,
        calculated_at
    )
    VALUES (
        NEW.id,
        NEW.proven_accuracy * NEW.weight,
        CURRENT_TIMESTAMP
    );
END;
```

### 2. update_progress_metrics_after_event_processing
This trigger updates system-wide progress metrics when new events are processed.

```sql
CREATE TRIGGER update_progress_metrics_after_event_processing
AFTER INSERT ON truth_event
FOR EACH ROW
BEGIN
    -- Update progress metrics based on new event
    INSERT OR REPLACE INTO progress_metrics (
        id,
        total_events,
        total_events_group,
        total_positive_impacts,
        total_positive_impacts_group,
        total_negative_impacts,
        total_negative_impact_group,
        trend,
        trend_group,
        last_updated
    )
    SELECT 
        1,
        (SELECT COUNT(*) FROM truth_event) AS total_events,
        (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
            SELECT DISTINCT pgm.participant_id
            FROM participants_groups pg
            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
            WHERE pg.type = 'auto'
        )) AS total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) AS total_positive_impacts,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) AS total_negative_impacts,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_negative_impact_group,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) / 
                (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END AS trend,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                SELECT DISTINCT pgm.participant_id
                FROM participants_groups pg
                JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                WHERE pg.type = 'auto'
            )) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                    SELECT id FROM event_ci WHERE created_by IN (
                        SELECT id FROM truth_event WHERE participant_id IN (
                            SELECT DISTINCT pgm.participant_id
                            FROM participants_groups pg
                            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                            WHERE pg.type = 'auto'
                        )
                    )
                )) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                     SELECT id FROM event_ci WHERE created_by IN (
                         SELECT id FROM truth_event WHERE participant_id IN (
                             SELECT DISTINCT pgm.participant_id
                             FROM participants_groups pg
                             JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                             WHERE pg.type = 'auto'
                         )
                     )
                 ))) / 
                (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                ))
            ELSE 0.0
        END AS trend_group,
        CURRENT_TIMESTAMP AS last_updated;
END;
```

### 3. update_temporal_decay_metrics_trigger
This trigger applies temporal decay functions to trust weights and influence metrics over time, implementing the decay formula w(t) = w₀ * e^(-λt) as described in sections 3.6 and 3.7.

```sql
CREATE TRIGGER update_temporal_decay_metrics_trigger
AFTER INSERT ON sync_operations
FOR EACH ROW
BEGIN
    -- Apply temporal decay to various metrics
    -- Update participant reputation with decay
    UPDATE participants
    SET 
        reputation_score = reputation_score * EXP(
            -(julianday('now') - julianday(last_activity, 'unixepoch')) * 0.01  -- λ = 0.01 per day
        ),
        last_activity = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(last_activity, 'unixepoch') > 1;  -- Only update if inactive for more than 1 day
    
    -- Update node ratings with decay
    UPDATE node_ratings
    SET 
        trust_score = trust_score * EXP(
            -(julianday('now') - julianday(last_updated, 'unixepoch')) * 0.005  -- λ = 0.005 per day
        ),
        propagation_priority = (
            (events_true - events_false) * 1.0 / NULLIF((events_true + events_false), 0)
        ) * EXP(-(julianday('now') - julianday(last_updated, 'unixepoch')) * 0.005),
        last_updated = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(last_updated, 'unixepoch') > 1;
    
    -- Update judgment weights with decay
    UPDATE judgment_weights
    SET 
        weight = (
            SELECT reputation_score FROM participants WHERE id = participant_id
        ) * EXP(-(julianday('now') - julianday(calculated_at, 'unixepoch')) * 0.02),  -- λ = 0.02 per day
        calculated_at = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(calculated_at, 'unixepoch') > 1;
    
    -- Update impact metrics with decay
    UPDATE impact_metrics
    SET 
        calculated_at = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(calculated_at, 'unixepoch') > 7;  -- Weekly update
END;
```

### 4. update_event_classification_after_convergence
This trigger handles event classification updates and manages the `event_classification_calculation` view and related triggers that update the "event_ci" table's "resolution_data" field based on the convergence of impact and judgment axes.

```sql
CREATE TRIGGER update_event_classification_after_convergence
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN (OLD.event_type != NEW.event_type OR OLD.status != NEW.status) AND NEW.event_type = 'both'
BEGIN
    -- Update resolution data based on convergence of axes
    UPDATE event_ci
    SET 
        resolution_data = CASE 
            -- Check if both impact and judgment have converged
            WHEN (
                SELECT COUNT(*) >= 2  -- At least 2 assessments
                AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.1  -- Low variance
                FROM event_state_history
                WHERE event_id = NEW.id
                AND recorded_at > datetime('now', '-7 days')
            ) AND (
                SELECT COUNT(*) >= 2  -- At least 2 assessments
                AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.1  -- Low variance
                FROM event_state_history
                WHERE event_id = NEW.id
                AND recorded_at > datetime('now', '-7 days')
            ) AND NEW.status IN ('resolved', 'archived') THEN 'consent'
            -- Check if only one axis has converged
            WHEN (
                SELECT COUNT(*) >= 2
                AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.1
                FROM event_state_history
                WHERE event_id = NEW.id
                AND recorded_at > datetime('now', '-7 days')
            ) XOR (
                SELECT COUNT(*) >= 2
                AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.1
                FROM event_state_history
                WHERE event_id = NEW.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 'suppose'
            -- Neither axis converged
            ELSE 'unstable'
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    
    -- Update event_stability based on classification
    INSERT OR REPLACE INTO event_stability (
        event_id,
        truth_stable,
        impact_stable,
        stabilized_at
    )
    SELECT 
        ec.id,
        CASE 
            WHEN (
                SELECT ABS(AVG(truth_score) - MIN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS truth_stable,
        CASE 
            WHEN (
                SELECT ABS(AVG(impact_score) - MIN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS impact_stable,
        CASE 
            WHEN (
                SELECT ABS(AVG(truth_score) - MIN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) OR (
                SELECT ABS(AVG(impact_score) - MIN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN CURRENT_TIMESTAMP
            ELSE NULL
        END AS stabilized_at
    FROM event_ci ec
    WHERE ec.id = NEW.id;
END;
```

### 5. update_progress_metrics_after_impact_recording
This trigger updates progress metrics when new impact is recorded, recalculating impact totals and trends.

```sql
CREATE TRIGGER update_progress_metrics_after_impact_recording
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update progress metrics based on new impact
    INSERT OR REPLACE INTO progress_metrics (
        id,
        total_events,
        total_events_group,
        total_positive_impacts,
        total_positive_impacts_group,
        total_negative_impacts,
        total_negative_impact_group,
        trend,
        trend_group,
        last_updated
    )
    SELECT 
        1,
        (SELECT COUNT(*) FROM truth_event) AS total_events,
        (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
            SELECT DISTINCT pgm.participant_id
            FROM participants_groups pg
            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
            WHERE pg.type = 'auto'
        )) AS total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) AS total_positive_impacts,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) AS total_negative_impacts,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_negative_impact_group,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) / 
                (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END AS trend,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                SELECT DISTINCT pgm.participant_id
                FROM participants_groups pg
                JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                WHERE pg.type = 'auto'
            )) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                    SELECT id FROM event_ci WHERE created_by IN (
                        SELECT id FROM truth_event WHERE participant_id IN (
                            SELECT DISTINCT pgm.participant_id
                            FROM participants_groups pg
                            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                            WHERE pg.type = 'auto'
                        )
                    )
                )) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                     SELECT id FROM event_ci WHERE created_by IN (
                         SELECT id FROM truth_event WHERE participant_id IN (
                             SELECT DISTINCT pgm.participant_id
                             FROM participants_groups pg
                             JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                             WHERE pg.type = 'auto'
                         )
                     )
                 ))) / 
                (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                ))
            ELSE 0.0
        END AS trend_group,
        CURRENT_TIMESTAMP AS last_updated;
END;
```

### 6. update_event_projection_after_event_ci_change
This trigger updates the "event_projection" table when "event_ci" is updated, implementing the relationship: "event_projection.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_event_projection_after_event_ci_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status OR OLD.event_type != NEW.event_type OR OLD.resolution_data != NEW.resolution_data
BEGIN
    -- Update event projection based on event_ci changes
    INSERT OR REPLACE INTO event_projection (
        event_id,
        truth_score,
        impact_score,
        quadrant,
        calculated_at
    )
    SELECT 
        NEW.id,
        te.collective_score,
        te.impact_score,
        CASE 
            WHEN te.collective_score >= 0.5 AND te.impact_score >= 0 THEN 'Q1'
            WHEN te.collective_score >= 0.5 AND te.impact_score < 0 THEN 'Q2'
            WHEN te.collective_score < 0.5 AND te.impact_score >= 0 THEN 'Q3'
            ELSE 'Q4'
        END AS quadrant,
        CURRENT_TIMESTAMP
    FROM truth_event te
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = NEW.id
    );
    
    -- Update event_state_history for temporal tracking
    INSERT INTO event_state_history (
        event_id,
        judgment_count,
        truth_score,
        impact_count,
        impact_score,
        recorded_at
    )
    SELECT 
        NEW.id,
        (SELECT COUNT(*) FROM judgment WHERE event_id = NEW.id),
        te.collective_score,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.id),
        te.impact_score,
        CURRENT_TIMESTAMP
    FROM truth_event te
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = NEW.id
    );
END;
```

### 7. update_impact_metrics_after_impact_change
This trigger updates "impact_metrics" when impact is added or modified, implementing the relationship: "impact_metrics.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_impact_metrics_after_impact_change
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update impact metrics based on new impact data
    INSERT OR REPLACE INTO impact_metrics (
        event_id,
        total_magnitude,
        positive_ratio,
        negative_ratio,
        uncertainty,
        calculated_at
    )
    SELECT 
        NEW.event_id,
        COUNT(*) AS total_magnitude,
        SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) AS positive_ratio,
        SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) AS negative_ratio,
        SUM(CASE WHEN value IS NULL THEN 1 ELSE 0 END) AS uncertainty,
        CURRENT_TIMESTAMP
    FROM impact
    WHERE event_id = NEW.event_id;
    
    -- Update progress metrics
    INSERT OR REPLACE INTO progress_metrics (
        id,
        total_events,
        total_events_group,
        total_positive_impacts,
        total_positive_impacts_group,
        total_negative_impacts,
        total_negative_impact_group,
        trend,
        trend_group,
        last_updated
    )
    SELECT 
        1,
        (SELECT COUNT(*) FROM truth_event) AS total_events,
        (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
            SELECT DISTINCT pgm.participant_id
            FROM participants_groups pg
            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
            WHERE pg.type = 'auto'
        )) AS total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) AS total_positive_impacts,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) AS total_negative_impacts,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_negative_impact_group,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) / 
                (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END AS trend,
        CASE 
            WHEN (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                SELECT DISTINCT pgm.participant_id
                FROM participants_groups pg
                JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                WHERE pg.type = 'auto'
            )) > 0 THEN
                ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                    SELECT id FROM event_ci WHERE created_by IN (
                        SELECT id FROM truth_event WHERE participant_id IN (
                            SELECT DISTINCT pgm.participant_id
                            FROM participants_groups pg
                            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                            WHERE pg.type = 'auto'
                        )
                    )
                )) - 
                 (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact WHERE event_id IN (
                     SELECT id FROM event_ci WHERE created_by IN (
                         SELECT id FROM truth_event WHERE participant_id IN (
                             SELECT DISTINCT pgm.participant_id
                             FROM participants_groups pg
                             JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                             WHERE pg.type = 'auto'
                         )
                     )
                ))) / 
                (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                ))
            ELSE 0.0
        END AS trend_group,
        CURRENT_TIMESTAMP AS last_updated;
END;
```

### 8. update_impact_predictions_after_event_ci_status_change
This trigger updates "impact_predictions" when "event_ci" status changes, implementing the relationship: "impact_predictions.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_impact_predictions_after_event_ci_status_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status AND NEW.status IN ('resolved', 'archived')
BEGIN
    -- Update impact predictions when event status changes to resolved/archived
    UPDATE impact_predictions
    SET 
        probability = CASE 
            WHEN ABS(expected_strength - (
                SELECT collective_score 
                FROM truth_event 
                WHERE id = (SELECT created_by FROM event_ci WHERE id = NEW.id)
            )) < 0.1 THEN 1.0  -- Highly accurate prediction
            WHEN ABS(expected_strength - (
                SELECT collective_score 
                FROM truth_event 
                WHERE id = (SELECT created_by FROM event_ci WHERE id = NEW.id)
            )) < 0.2 THEN 0.7  -- Moderately accurate prediction
            WHEN ABS(expected_strength - (
                SELECT collective_score 
                FROM truth_event 
                WHERE id = (SELECT created_by FROM event_ci WHERE id = NEW.id)
            )) < 0.3 THEN 0.3  -- Somewhat accurate prediction
            ELSE 0.0  -- Inaccurate prediction
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE event_id = NEW.id;
    
    -- Update participant reputation based on prediction accuracy
    UPDATE participants
    SET 
        accurate_impact = (
            SELECT COUNT(*) 
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
            AND ABS(ip.expected_strength - te.collective_score) < 0.2
        ),
        total_impact = (
            SELECT COUNT(*) 
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
        ),
        reputation_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        SUM(CASE 
                            WHEN ABS(ip.expected_strength - te.collective_score) < 0.2 THEN 1.0
                            ELSE 0.0
                        END) / COUNT(*)
                    ELSE 0.5
                END
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
        ),
        last_activity = CURRENT_TIMESTAMP
    WHERE id IN (
        SELECT DISTINCT te.participant_id
        FROM truth_event te
        JOIN event_ci ec ON te.id = ec.created_by
        WHERE ec.id = NEW.id
    );
END;
```

### 9. update_consensus_ci_after_judgment_change
This trigger updates "consensus_ci" when judgment is added or modified, implementing the relationship: "consensus_ci.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_consensus_ci_after_judgment_change
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update consensus based on new judgment data
    INSERT OR REPLACE INTO consensus_ci (
        event_id,
        consensus_value,
        confidence_score,
        participant_count,
        calculated_at,
        algorithm_version
    )
    SELECT 
        ec.id,
        ROUND(AVG(CASE 
            WHEN j.assessment = 'true' THEN 1.0
            WHEN j.assessment = 'false' THEN -1.0
            ELSE 0.0
        END * jw.weight)),
        AVG(j.confidence_level * jw.weight),
        COUNT(DISTINCT j.participant_id),
        CURRENT_TIMESTAMP,
        1
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    LEFT JOIN judgment j ON j.event_id = ec.id
    LEFT JOIN judgment_weights jw ON j.participant_id = jw.participant_id AND j.event_id = jw.event_id
    WHERE ec.id = NEW.event_id
    GROUP BY ec.id;
    
    -- Update truth_state based on consensus
    INSERT OR REPLACE INTO truth_state (
        event_id,
        time_axis_id,
        truth_state,
        truth_score,
        dispersion,
        confidence,
        calculated_at
    )
    SELECT 
        ec.id,
        2,  -- Assuming 'present' time axis
        CASE 
            WHEN ABS(cs.confidence_score) > 0.7 THEN 'resolved'
            WHEN julianday('now') - julianday(te.created_at, 'unixepoch') > 30 THEN 'archived'
            ELSE 'active'
        END AS truth_state,
        cs.confidence_score AS truth_score,
        0.1 AS dispersion,  -- Simplified dispersion calculation
        ABS(cs.confidence_score) AS confidence,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    JOIN consensus_ci cs ON cs.event_id = ec.id
    WHERE ec.id = NEW.event_id;
    
    -- Update event_projection with new consensus data
    INSERT OR REPLACE INTO event_projection (
        event_id,
        truth_score,
        impact_score,
        quadrant,
        calculated_at
    )
    SELECT 
        ec.id,
        cs.confidence_score,
        te.impact_score,
        CASE 
            WHEN cs.confidence_score >= 0.5 AND te.impact_score >= 0 THEN 'Q1'
            WHEN cs.confidence_score >= 0.5 AND te.impact_score < 0 THEN 'Q2'
            WHEN cs.confidence_score < 0.5 AND te.impact_score >= 0 THEN 'Q3'
            ELSE 'Q4'
        END AS quadrant,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    JOIN consensus_ci cs ON cs.event_id = ec.id
    WHERE ec.id = NEW.event_id;
END;
```

### 10. update_judgment_weights_after_participant_change
This trigger updates "judgment_weights" when participant reputation changes, implementing the relationship: "judgment_weights.participant_id" → "participants.id" AND "judgment_weights.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_judgment_weights_after_participant_change
AFTER UPDATE ON participants
FOR EACH ROW
WHEN OLD.reputation_score != NEW.reputation_score
BEGIN
    -- Update judgment weights based on updated participant reputation
    UPDATE judgment_weights
    SET 
        weight = NEW.reputation_score,
        calculated_at = CURRENT_TIMESTAMP
    WHERE participant_id = NEW.id;
    
    -- Update all related consensus calculations
    INSERT OR REPLACE INTO consensus_ci (
        event_id,
        consensus_value,
        confidence_score,
        participant_count,
        calculated_at,
        algorithm_version
    )
    SELECT 
        j.event_id,
        ROUND(AVG(CASE 
            WHEN j.assessment = 'true' THEN 1.0
            WHEN j.assessment = 'false' THEN -1.0
            ELSE 0.0
        END * p.reputation_score)),
        AVG(j.confidence_level * p.reputation_score),
        COUNT(DISTINCT j.participant_id),
        CURRENT_TIMESTAMP,
        1
    FROM judgment j
    JOIN participants p ON j.participant_id = p.id
    WHERE j.participant_id = NEW.id
    GROUP BY j.event_id;
    
    -- Update truth_event collective scores based on updated weights
    UPDATE truth_event
    SET 
        collective_score = (
            SELECT 
                CASE 
                    WHEN te.impact_score IS NOT NULL AND te.judgment_score IS NOT NULL THEN
                        (te.impact_score + te.judgment_score) / 2.0
                    WHEN te.impact_score IS NOT NULL THEN
                        te.impact_score
                    WHEN te.judgment_score IS NOT NULL THEN
                        te.judgment_score
                    ELSE 0.5  -- Default neutral score
                END
            FROM truth_event te
            WHERE te.id = truth_event.id
        ),
        judgment_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        AVG(CASE 
                            WHEN j.assessment = 'true' THEN 1.0
                            WHEN j.assessment = 'false' THEN -1.0
                            ELSE 0.0
                        END * p.reputation_score)
                    ELSE NULL
                END
            FROM judgment j
            JOIN participants p ON j.participant_id = p.id
            WHERE j.event_id = (
                SELECT created_by FROM event_ci WHERE id = (
                    SELECT event_id FROM judgment WHERE participant_id = NEW.id LIMIT 1
                )
            )
        )
    WHERE id IN (
        SELECT DISTINCT te.id
        FROM truth_event te
        JOIN event_ci ec ON te.id = ec.created_by
        JOIN judgment j ON ec.id = j.event_id
        WHERE j.participant_id = NEW.id
    );
END;
```

### 11. update_truth_state_after_event_ci_change
This trigger updates "truth_state" when "event_ci" is updated, implementing the relationship: "truth_state.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_truth_state_after_event_ci_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status OR OLD.event_type != NEW.event_type OR OLD.resolution_data != NEW.resolution_data
BEGIN
    -- Update truth state based on event_ci changes
    INSERT OR REPLACE INTO truth_state (
        event_id,
        time_axis_id,
        truth_state,
        truth_score,
        dispersion,
        confidence,
        calculated_at
    )
    SELECT 
        NEW.id,
        ta.id,
        CASE 
            WHEN NEW.status = 'resolved' THEN 'resolved'
            WHEN NEW.status = 'archived' THEN 'archived'
            ELSE 'active'
        END AS truth_state,
        te.collective_score,
        0.1 AS dispersion,  -- Simplified dispersion calculation
        CASE 
            WHEN te.collective_score IS NOT NULL THEN ABS(te.collective_score)
            ELSE 0.0
        END AS confidence,
        CURRENT_TIMESTAMP
    FROM truth_event te
    CROSS JOIN time_axes ta  -- Assuming we want to track on all time axes
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = NEW.id
    )
    AND ta.time_type = 'present';  -- Using present time for current state
    
    -- Update event_state_history for temporal tracking
    INSERT INTO event_state_history (
        event_id,
        judgment_count,
        truth_score,
        impact_count,
        impact_score,
        recorded_at
    )
    SELECT 
        NEW.id,
        (SELECT COUNT(*) FROM judgment WHERE event_id = NEW.id),
        te.collective_score,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.id),
        te.impact_score,
        CURRENT_TIMESTAMP
    FROM truth_event te
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = NEW.id
    );
END;
```

### 12. update_event_state_history_after_event_change
This trigger updates "event_state_history" when event metrics change, implementing the relationship: "event_state_history.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_event_state_history_after_event_change
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN OLD.collective_score != NEW.collective_score OR OLD.impact_score != NEW.impact_score OR OLD.judgment_score != NEW.judgment_score
BEGIN
    -- Update event state history with new metrics
    INSERT INTO event_state_history (
        event_id,
        judgment_count,
        truth_score,
        impact_count,
        impact_score,
        recorded_at
    )
    SELECT 
        ec.id,
        (SELECT COUNT(*) FROM judgment WHERE event_id = ec.id),
        NEW.collective_score,
        (SELECT COUNT(*) FROM impact WHERE event_id = ec.id),
        NEW.impact_score,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    WHERE ec.created_by = NEW.id;
    
    -- Update event_stability based on state changes
    INSERT OR REPLACE INTO event_stability (
        event_id,
        truth_stable,
        impact_stable,
        stabilized_at
    )
    SELECT 
        ec.id,
        CASE 
            WHEN (
                SELECT ABS(AVG(truth_score) - MIN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS truth_stable,
        CASE 
            WHEN (
                SELECT ABS(AVG(impact_score) - MIN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS impact_stable,
        CASE 
            WHEN (
                SELECT ABS(AVG(truth_score) - MIN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) OR (
                SELECT ABS(AVG(impact_score) - MIN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN CURRENT_TIMESTAMP
            ELSE NULL
        END AS stabilized_at
    FROM event_ci ec
    WHERE ec.created_by = NEW.id;
END;
```

### 13. update_event_stability_after_event_state_change
This trigger updates "event_stability" when event state changes, implementing the relationship: "event_stability.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_event_stability_after_event_state_change
AFTER INSERT ON event_state_history
FOR EACH ROW
BEGIN
    -- Update event stability based on recent state history
    INSERT OR REPLACE INTO event_stability (
        event_id,
        truth_stable,
        impact_stable,
        stabilized_at
    )
    SELECT 
        NEW.event_id,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5  -- At least 5 measurements
                AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05  -- Low variance
                FROM event_state_history
                WHERE event_id = NEW.event_id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS truth_stable,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5  -- At least 5 measurements
                AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05  -- Low variance
                FROM event_state_history
                WHERE event_id = NEW.event_id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN 1
            ELSE 0
        END AS impact_stable,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5
                AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = NEW.event_id
                AND recorded_at > datetime('now', '-7 days')
            ) AND (
                SELECT COUNT(*) >= 5
                AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = NEW.event_id
                AND recorded_at > datetime('now', '-7 days')
            ) THEN CURRENT_TIMESTAMP
            ELSE NULL
        END AS stabilized_at
    WHERE NEW.event_id = (
        SELECT id FROM event_ci WHERE created_by = (
            SELECT id FROM truth_event WHERE id = (
                SELECT created_by FROM event_ci WHERE id = NEW.event_id
            )
        )
    );
END;
```

### 14. update_heuristic_weight
This trigger updates the weight of expert heuristics based on their proven accuracy when the accuracy changes.

```sql
CREATE TRIGGER update_heuristic_weight
AFTER UPDATE ON expert_heuristics
FOR EACH ROW
WHEN OLD.proven_accuracy != NEW.proven_accuracy
BEGIN
    -- Update heuristic weight based on proven accuracy
    UPDATE expert_heuristics
    SET 
        weight = CASE 
            WHEN NEW.proven_accuracy > 0.9 THEN 1.0
            WHEN NEW.proven_accuracy > 0.7 THEN 0.8
            WHEN NEW.proven_accuracy > 0.5 THEN 0.6
            WHEN NEW.proven_accuracy > 0.3 THEN 0.4
            WHEN NEW.proven_accuracy > 0.1 THEN 0.2
            ELSE 0.1
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    
    -- Update applicable heuristics view
    INSERT OR REPLACE INTO applicable_heuristics (
        heuristic_id,
        is_applicable,
        threshold_met,
        calculated_at
    )
    VALUES (
        NEW.id,
        CASE WHEN NEW.proven_accuracy > 0.5 THEN 1 ELSE 0 END,
        NEW.proven_accuracy,
        CURRENT_TIMESTAMP
    );
END;
```

### 15. update_temporal_decay_metrics
This trigger applies temporal decay functions to trust weights and influence metrics over time.

```sql
CREATE TRIGGER update_temporal_decay_metrics
AFTER UPDATE ON sync_operations
FOR EACH ROW
BEGIN
    -- Apply temporal decay to various metrics using the formula w(t) = w₀ * e^(-λt)
    -- Update participant reputation with decay
    UPDATE participants
    SET 
        reputation_score = reputation_score * EXP(
            -(julianday('now') - julianday(last_activity, 'unixepoch')) * 0.01
        ),
        last_activity = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(last_activity, 'unixepoch') > 0.5;  -- Update if inactive for more than 12 hours
    
    -- Update node ratings with decay
    UPDATE node_ratings
    SET 
        trust_score = trust_score * EXP(
            -(julianday('now') - julianday(last_updated, 'unixepoch')) * 0.005
        ),
        propagation_priority = propagation_priority * EXP(
            -(julianday('now') - julianday(last_updated, 'unixepoch')) * 0.005
        ),
        last_updated = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(last_updated, 'unixepoch') > 0.5;
    
    -- Update judgment weights with decay
    UPDATE judgment_weights
    SET 
        weight = weight * EXP(-(julianday('now') - julianday(calculated_at, 'unixepoch')) * 0.02),
        calculated_at = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(calculated_at, 'unixepoch') > 0.5;
    
    -- Update impact metrics with decay
    UPDATE impact_metrics
    SET 
        calculated_at = CURRENT_TIMESTAMP
    WHERE julianday('now') - julianday(calculated_at, 'unixepoch') > 1;  -- Daily update
END;
```

### 16. update_event_classification
This trigger handles event classification updates and manages the `event_classification_calculation` view and related triggers that update the "event_ci" table's "resolution_data" field based on the convergence of impact and judgment axes.

```sql
CREATE TRIGGER update_event_classification
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN (OLD.event_type != NEW.event_type OR OLD.status != NEW.status) AND NEW.event_type IN ('impact', 'judgment', 'both')
BEGIN
    -- Update resolution data based on convergence of axes
    UPDATE event_ci
    SET 
        resolution_data = CASE 
            -- Both axes present and converged
            WHEN NEW.event_type = 'both' AND NEW.status IN ('resolved', 'archived') THEN
                CASE 
                    WHEN (
                        SELECT COUNT(*) >= 3  -- At least 3 assessments
                        AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.1  -- Low variance for truth
                        FROM event_state_history
                        WHERE event_id = NEW.id
                        AND recorded_at > datetime('now', '-7 days')
                    ) AND (
                        SELECT COUNT(*) >= 3  -- At least 3 assessments
                        AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.1  -- Low variance for impact
                        FROM event_state_history
                        WHERE event_id = NEW.id
                        AND recorded_at > datetime('now', '-7 days')
                    ) THEN 'consent'
                    ELSE 'suppose'
                END
            -- Only impact axis present
            WHEN NEW.event_type = 'impact' THEN
                CASE 
                    WHEN (
                        SELECT COUNT(*) >= 3
                        AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.1
                        FROM event_state_history
                        WHERE event_id = NEW.id
                        AND recorded_at > datetime('now', '-7 days')
                    ) THEN 'suppose'
                    ELSE 'unstable'
                END
            -- Only judgment axis present
            WHEN NEW.event_type = 'judgment' THEN
                CASE 
                    WHEN (
                        SELECT COUNT(*) >= 3
                        AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.1
                        FROM event_state_history
                        WHERE event_id = NEW.id
                        AND recorded_at > datetime('now', '-7 days')
                    ) THEN 'suppose'
                    ELSE 'unstable'
                END
            ELSE 'unstable'
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    
    -- Update event_projection based on classification
    INSERT OR REPLACE INTO event_projection (
        event_id,
        truth_score,
        impact_score,
        quadrant,
        calculated_at
    )
    SELECT 
        ec.id,
        te.collective_score,
        te.impact_score,
        CASE 
            WHEN te.collective_score >= 0.5 AND te.impact_score >= 0 THEN 'Q1'
            WHEN te.collective_score >= 0.5 AND te.impact_score < 0 THEN 'Q2'
            WHEN te.collective_score < 0.5 AND te.impact_score >= 0 THEN 'Q3'
            ELSE 'Q4'
        END AS quadrant,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    WHERE ec.id = NEW.id;
END;
```

## Additional Utility Triggers

### update_group_ratings_calculation
Updates group ratings based on participant reputation and internal agreement.

```sql
CREATE TRIGGER update_group_ratings_calculation
AFTER UPDATE ON participants
FOR EACH ROW
WHEN OLD.reputation_score != NEW.reputation_score
BEGIN
    -- Update group ratings based on participant reputation changes
    UPDATE group_ratings
    SET 
        avg_score = (
            SELECT AVG(p.reputation_score)
            FROM participants p
            JOIN participants_groups_members pgm ON p.id = pgm.participant_id
            WHERE pgm.group_id = group_ratings.group_id
        ),
        coherence = (
            SELECT 1 - (SUM(ABS(p.reputation_score - avg_score)) / (COUNT(*) * 1.0))
            FROM participants p
            JOIN participants_groups_members pgm ON p.id = pgm.participant_id
            WHERE pgm.group_id = group_ratings.group_id
        ),
        last_updated = CURRENT_TIMESTAMP
    WHERE group_id IN (
        SELECT DISTINCT group_id
        FROM participants_groups_members
        WHERE participant_id = NEW.id
    );
END;
```

## Notes

- All triggers maintain data consistency across the aggregated metrics system
- Temporal decay functions use the formula w(t) = w₀ * e^(-λt) with appropriate decay rates
- Event classification is updated based on convergence of impact and judgment axes
- Group ratings are updated automatically when participant reputations change
- The triggers maintain historical data in event_state_history for temporal analysis
- Stability metrics are calculated based on recent state history
- Heuristic weights are adjusted based on proven accuracy