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