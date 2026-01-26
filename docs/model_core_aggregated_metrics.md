-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Triggers for Aggregated System Metrics and Expert Functions  

-- Function to calculate heuristic weight based on accuracy  
-- w_i = f(accuracy_i, reliability_i, domain_relevance_i)  
```sql
CREATE TRIGGER update_heuristic_weight
AFTER UPDATE ON expert_heuristics
FOR EACH ROW
WHEN NEW.proven_accuracy != OLD.proven_accuracy
BEGIN
    UPDATE expert_heuristics
    SET
        weight = CASE
            WHEN NEW.proven_accuracy > 0.8 THEN 1.0 -- HIGH
            WHEN NEW.proven_accuracy > 0.6 THEN 0.7  -- MEDIUM
            ELSE 0.3  -- LOW
        END,
        updated_at = (SELECT strftime('%s', 'now'))
    WHERE id = NEW.id;
END;
```
-- Function to update progress metrics when new event is processed  
```sql
CREATE TRIGGER update_progress_metrics_after_event
AFTER INSERT ON truth_event
BEGIN
    INSERT OR REPLACE INTO progress_metrics (
        id,
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
        1,  -- Single record for overall metrics
        (SELECT COUNT(*) FROM truth_event) as total_events,
        (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
            SELECT id FROM participants WHERE group_membership IS NOT NULL
        )) as total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) as total_positive_impacts,
        (SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END)
         FROM impact i
         JOIN truth_event te ON i.event_id = te.id
         WHERE te.participant_id IN (
             SELECT id FROM participants WHERE group_membership IS NOT NULL
         )) as total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) as total_negative_impacts,
        (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END)
         FROM impact i
         JOIN truth_event te ON i.event_id = te.id
         WHERE te.participant_id IN (
             SELECT id FROM participants WHERE group_membership IS NOT NULL
         )) as total_negative_impact_group,
        CASE
            WHEN (SELECT COUNT(*) FROM truth_event) > 0
            THEN ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) -
                  (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) * 1.0 /
                 (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END as trend,
        CASE
            WHEN (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                SELECT id FROM participants WHERE group_membership IS NOT NULL
            )) > 0
            THEN ((SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END)
                   FROM impact i
                   JOIN truth_event te ON i.event_id = te.id
                   WHERE te.participant_id IN (
                       SELECT id FROM participants WHERE group_membership IS NOT NULL
                   )) -
                  (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END)
                   FROM impact i
                   JOIN truth_event te ON i.event_id = te.id
                   WHERE te.participant_id IN (
                       SELECT id FROM participants WHERE group_membership IS NOT NULL
                   ))) * 1.0 /
                 (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                     SELECT id FROM participants WHERE group_membership IS NOT NULL
                 ))
            ELSE 0.0
        END as trend_group,
        (SELECT strftime('%s', 'now')) as last_updated
    WHERE NOT EXISTS (SELECT 1 FROM progress_metrics WHERE id = 1);
END;
```
-- Trigger to update temporal decay metrics based on the decay function w(t) = w₀ * e^(-λt)
-- Applies temporal decay functions to trust weights and influence metrics over time as described in sections 3.6 and 3.7
```sql
CREATE TRIGGER update_temporal_decay_metrics
AFTER UPDATE ON participants
BEGIN
    UPDATE participants
    SET reputation_score = reputation_score * EXP(
        -(CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END) * (julianday('now') - julianday(last_activity, 'unixepoch'))
    )
    WHERE julianday('now') - julianday(last_activity, 'unixepoch') > 1  -- Only apply decay if more than 1 day has passed
    AND id = NEW.id;
END;
```

-- Trigger to update event classification based on convergence of impact and judgment axes
-- Handles event classification updates and manages the "event_classification_calculation" view and related triggers that update the "event_ci" table's "resolution_data" field based on the convergence of **impact** and **judgment** axes
```sql
CREATE TRIGGER update_event_classification
AFTER UPDATE ON event_ci
BEGIN
    UPDATE event_ci
    SET resolution_data = (
        SELECT CASE
            WHEN NOT (
                EXISTS (
                    SELECT 1 FROM impact_metrics
                    WHERE impact_metrics.event_id = event_ci.id
                    AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
                ) OR
                EXISTS (
                    SELECT 1 FROM judgment_weights
                    WHERE judgment_weights.event_id = event_ci.id
                    AND judgment_weights.weight IS NOT NULL
                )
            ) THEN 'unstable'
            WHEN (
                EXISTS (
                    SELECT 1 FROM impact_metrics
                    WHERE impact_metrics.event_id = event_ci.id
                    AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
                ) XOR
                EXISTS (
                    SELECT 1 FROM judgment_weights
                    WHERE judgment_weights.event_id = event_ci.id
                    AND judgment_weights.weight IS NOT NULL
                )
            ) THEN 'suppose'
            WHEN (
                EXISTS (
                    SELECT 1 FROM impact_metrics
                    WHERE impact_metrics.event_id = event_ci.id
                    AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
                ) AND
                EXISTS (
                    SELECT 1 FROM judgment_weights
                    WHERE judgment_weights.event_id = event_ci.id
                    AND judgment_weights.weight IS NOT NULL
                ) AND
                event_ci.event_type = 'both' AND
                (event_ci.status = 'resolved' OR event_ci.status = 'archived')
            ) THEN 'consent'
            ELSE 'unstable'
        END
    )
    WHERE id = NEW.id;
END;
```

-- Function to update progress metrics when new impact is recorded  
```sql
CREATE TRIGGER update_progress_metrics_after_impact
AFTER INSERT ON impact
BEGIN
    UPDATE progress_metrics
    SET
        total_positive_impacts = (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact),
        total_negative_impacts = (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact),
        trend = CASE
            WHEN (SELECT COUNT(*) FROM truth_event) > 0
            THEN ((SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) -
                  (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact)) * 1.0 /
                 (SELECT COUNT(*) FROM truth_event)
            ELSE 0.0
        END,
        last_updated = (SELECT strftime('%s', 'now'))
    WHERE id = 1;
END;
```

-- Trigger to update event_projection when event_ci is updated
-- Implements the relationship: event_projection.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_event_projection_after_event_ci_change
AFTER UPDATE ON event_ci
BEGIN
    INSERT OR REPLACE INTO event_projection (
        event_id,
        truth_score,
        impact_score,
        quadrant,
        calculated_at
    )
    SELECT
        ec.id as event_id,
        (SELECT confidence_score FROM consensus_ci WHERE event_id = ec.id) as truth_score,
        (SELECT total_magnitude FROM impact_metrics WHERE event_id = ec.id) as impact_score,
        CASE
            WHEN (SELECT confidence_score FROM consensus_ci WHERE event_id = ec.id) >= 0.5 
                 AND (SELECT total_magnitude FROM impact_metrics WHERE event_id = ec.id) >= 0 
            THEN 'Q1'
            WHEN (SELECT confidence_score FROM consensus_ci WHERE event_id = ec.id) >= 0.5 
                 AND (SELECT total_magnitude FROM impact_metrics WHERE event_id = ec.id) < 0 
            THEN 'Q2'
            WHEN (SELECT confidence_score FROM consensus_ci WHERE event_id = ec.id) < 0.5 
                 AND (SELECT total_magnitude FROM impact_metrics WHERE event_id = ec.id) >= 0 
            THEN 'Q3'
            ELSE 'Q4'
        END as quadrant,
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    WHERE ec.id = NEW.id
    AND EXISTS (
        SELECT 1 FROM truth_event te 
        WHERE te.id = ec.created_by 
        AND te.participant_id IN (SELECT id FROM participants)
    );
END;
```

-- Trigger to update impact_metrics when impact is added or modified
-- Implements the relationship: impact_metrics.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_impact_metrics_after_impact_change
AFTER INSERT ON impact
BEGIN
    INSERT OR REPLACE INTO impact_metrics (
        event_id,
        total_magnitude,
        positive_ratio,
        negative_ratio,
        uncertainty,
        calculated_at
    )
    SELECT
        NEW.event_id as event_id,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value IS NOT NULL) as total_magnitude,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value = 1) as positive_ratio,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value = 0) as negative_ratio,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value IS NULL) as uncertainty,
        (SELECT strftime('%s', 'now')) as calculated_at
    WHERE EXISTS (
        SELECT 1 FROM event_ci ec
        JOIN truth_event te ON ec.created_by = te.id
        JOIN participants p ON te.participant_id = p.id
        WHERE ec.id = NEW.event_id
    );
END;
```

-- Trigger to update impact_predictions when event_ci status changes
-- Implements the relationship: impact_predictions.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_impact_predictions_after_event_ci_status_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    INSERT INTO impact_predictions (
        event_id,
        predicted_impact_type,
        expected_strength,
        probability,
        horizon,
        created_at
    )
    SELECT
        NEW.id as event_id,
        te.effect_id as predicted_impact_type,
        COALESCE(te.collective_score, 0.5) as expected_strength,
        0.6 as probability,
        30.0 as horizon,
        (SELECT strftime('%s', 'now')) as created_at
    FROM truth_event te
    WHERE te.id = (
        SELECT created_by FROM event_ci WHERE id = NEW.id
    )
    AND te.participant_id IN (SELECT id FROM participants)
    AND NOT EXISTS (
        SELECT 1 FROM impact_predictions 
        WHERE event_id = NEW.id 
        AND DATE(created_at, 'unixepoch') = DATE('now', 'unixepoch')
    );
END;
```

-- Trigger to update consensus_ci when judgment is added or modified
-- Implements the relationship: consensus_ci.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_consensus_ci_after_judgment_change
AFTER INSERT ON judgment
BEGIN
    INSERT OR REPLACE INTO consensus_ci (
        event_id,
        consensus_value,
        confidence_score,
        participant_count,
        calculated_at,
        algorithm_version
    )
    SELECT
        NEW.event_id as event_id,
        ROUND(AVG(j.assessment * jw.weight)) as consensus_value,
        AVG(j.confidence_level * jw.weight) as confidence_score,
        COUNT(DISTINCT j.participant_id) as participant_count,
        (SELECT strftime('%s', 'now')) as calculated_at,
        1 as algorithm_version
    FROM judgment j
    JOIN judgment_weights jw ON j.participant_id = jw.participant_id
    WHERE j.event_id = NEW.event_id
    AND j.participant_id IN (SELECT id FROM participants);
END;
```

-- Trigger to update judgment_weights when participant reputation changes
-- Implements the relationship: judgment_weights.participant_id → participants.id AND judgment_weights.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_judgment_weights_after_participant_change
AFTER UPDATE ON participants
BEGIN
    INSERT OR REPLACE INTO judgment_weights (
        participant_id,
        event_id,
        weight,
        calculated_at
    )
    SELECT
        NEW.id as participant_id,
        ec.id as event_id,
        NEW.reputation_score as weight,
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    WHERE te.participant_id = NEW.id;
END;
```

-- Trigger to update truth_state when event_ci is updated
-- Implements the relationship: truth_state.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_truth_state_after_event_ci_change
AFTER UPDATE ON event_ci
BEGIN
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
        ec.id as event_id,
        2 as time_axis_id, -- Using 'present' time axis as default
        CASE
            WHEN ec.status = 'active' THEN 'active'
            WHEN ec.status = 'resolved' THEN 'resolved'
            ELSE 'archived'
        END as truth_state,
        (SELECT confidence_score FROM consensus_ci WHERE event_id = ec.id) as truth_score,
        0.1 as dispersion, -- Placeholder value
        0.8 as confidence, -- Placeholder value
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    WHERE ec.id = NEW.id
    AND EXISTS (
        SELECT 1 FROM truth_event te
        JOIN participants p ON te.participant_id = p.id
        WHERE te.id = ec.created_by
    );
END;
```

-- Trigger to update event_state_history when event metrics change
-- Implements the relationship: event_state_history.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_event_state_history_after_event_change
AFTER UPDATE ON truth_event
BEGIN
    INSERT INTO event_state_history (
        event_id,
        judgment_count,
        truth_score,
        impact_count,
        impact_score,
        recorded_at
    )
    SELECT
        ec.id as event_id,
        (SELECT COUNT(*) FROM judgment WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by = NEW.id
        )) as judgment_count,
        NEW.collective_score as truth_score,
        (SELECT COUNT(*) FROM impact WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by = NEW.id
        )) as impact_count,
        NEW.impact_score as impact_score,
        (SELECT strftime('%s', 'now')) as recorded_at
    FROM event_ci ec
    WHERE ec.created_by = NEW.id
    AND NEW.participant_id IN (SELECT id FROM participants);
END;
```

-- Trigger to update event_stability when event state changes
-- Implements the relationship: event_stability.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_event_stability_after_event_state_change
AFTER UPDATE ON event_state_history
BEGIN
    INSERT OR REPLACE INTO event_stability (
        event_id,
        truth_stable,
        impact_stable,
        stabilized_at
    )
    SELECT
        esh.event_id as event_id,
        CASE
            WHEN ABS((SELECT AVG(truth_score) - MIN(truth_score)
                      FROM event_state_history
                      WHERE event_id = esh.event_id
                      AND recorded_at >= (SELECT MAX(recorded_at) - 86400 FROM event_state_history WHERE event_id = esh.event_id)) /
                     (CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 
                           THEN 0.000001 
                           ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) 
                      END)) < 0.01
            THEN 1
            ELSE 0
        END as truth_stable,
        CASE
            WHEN ABS((SELECT AVG(impact_score) - MIN(impact_score)
                      FROM event_state_history
                      WHERE event_id = esh.event_id
                      AND recorded_at >= (SELECT MAX(recorded_at) - 86400 FROM event_state_history WHERE event_id = esh.event_id)) /
                     (CASE WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0 
                           THEN 0.000001 
                           ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 ) 
                      END)) < 0.01
            THEN 1
            ELSE 0
        END as impact_stable,
        CASE
            WHEN (SELECT truth_stable FROM event_stability WHERE event_id = esh.event_id) = 1
                 OR (SELECT impact_stable FROM event_stability WHERE event_id = esh.event_id) = 1
            THEN (SELECT MAX(recorded_at) FROM event_state_history WHERE event_id = esh.event_id)
            ELSE NULL
        END as stabilized_at
    FROM event_state_history esh
    WHERE esh.id = NEW.id
    AND EXISTS (
        SELECT 1 FROM event_ci ec
        JOIN truth_event te ON ec.created_by = te.id
        JOIN participants p ON te.participant_id = p.id
        WHERE ec.id = esh.event_id
    );
END;
```