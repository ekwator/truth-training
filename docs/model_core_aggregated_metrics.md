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
            SELECT public_key FROM participants WHERE group_membership IS NOT NULL
        )) as total_events_group,
        (SELECT SUM(CASE WHEN value = 1 THEN 1 ELSE 0 END) FROM impact) as total_positive_impacts,
        (SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END)
         FROM impact i
         JOIN truth_event te ON i.event_id = te.id
         WHERE te.participant_id IN (
             SELECT public_key FROM participants WHERE group_membership IS NOT NULL
         )) as total_positive_impacts_group,
        (SELECT SUM(CASE WHEN value = 0 THEN 1 ELSE 0 END) FROM impact) as total_negative_impacts,
        (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END)
         FROM impact i
         JOIN truth_event te ON i.event_id = te.id
         WHERE te.participant_id IN (
             SELECT public_key FROM participants WHERE group_membership IS NOT NULL
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
                SELECT public_key FROM participants WHERE group_membership IS NOT NULL
            )) > 0
            THEN ((SELECT SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END)
                   FROM impact i
                   JOIN truth_event te ON i.event_id = te.id
                   WHERE te.participant_id IN (
                       SELECT public_key FROM participants WHERE group_membership IS NOT NULL
                   )) -
                  (SELECT SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END)
                   FROM impact i
                   JOIN truth_event te ON i.event_id = te.id
                   WHERE te.participant_id IN (
                       SELECT public_key FROM participants WHERE group_membership IS NOT NULL
                   ))) * 1.0 /
                 (SELECT COUNT(*) FROM truth_event WHERE participant_id IN (
                     SELECT public_key FROM participants WHERE group_membership IS NOT NULL
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
    AND public_key = NEW.public_key;
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