# Collective Assessment Triggers

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This document describes the SQL triggers that implement the collective assessment logic for the Truth Training system. These triggers automatically update metrics when new assessments are added to the system.

## Purpose
The collective assessment triggers ensure that collective intelligence metrics are automatically updated when new impact or judgment data is added, maintaining data consistency and system efficiency across all nodes in the network.

## Trigger Definitions

### 1. create_event_record
This trigger creates the initial event record in the "truth_event" table when a participant submits a new event. It also creates the corresponding entry in the "event_ci" table (event neuron) with default values.

```sql
CREATE TRIGGER create_event_record
AFTER INSERT ON truth_event
FOR EACH ROW
BEGIN
    INSERT INTO event_ci (created_by, event_type, status, old_status, resolution_data, created_at)
    VALUES (NEW.id, 'judgment', 'active', 'active', 'unstable', CURRENT_TIMESTAMP);
    
    -- Initialize event metrics
    INSERT INTO event_state_history (event_id, judgment_count, truth_score, impact_count, impact_score, recorded_at)
    VALUES (
        (SELECT id FROM event_ci WHERE created_by = NEW.id),
        0,
        0.5,  -- Default neutral truth score
        0,
        0.0,  -- Default impact score
        CURRENT_TIMESTAMP
    );
END;
```

### 2. initialize_event_metrics
This trigger initializes all metric fields ("collective_score", "impact_score", "judgment_score") to default values when a new event is inserted. Sets "collective_score" to 0.5 (neutral), "impact_score" to 0.0, and "judgment_score" to NULL.

```sql
CREATE TRIGGER initialize_event_metrics
AFTER INSERT ON truth_event
FOR EACH ROW
BEGIN
    UPDATE truth_event
    SET 
        collective_score = 0.5,
        impact_score = 0.0,
        judgment_score = NULL
    WHERE id = NEW.id;
    
    -- Also update the associated event_ci record
    UPDATE event_ci
    SET created_at = CURRENT_TIMESTAMP
    WHERE created_by = NEW.id;
END;
```

### 3. update_participant_reputation_on_impact
This trigger updates participant's reputation based on the accuracy of their impact assessments, comparing against the "collective_score" as a reference value. Increases "accurate_impact" counter when the impact aligns with the collective assessment.

```sql
CREATE TRIGGER update_participant_reputation_on_impact
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Get the collective score for the associated event
    DECLARE @collective_score REAL;
    SELECT @collective_score = te.collective_score
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
    
    -- Update participant reputation based on impact accuracy
    UPDATE participants
    SET 
        total_impact = total_impact + 1,
        accurate_impact = CASE 
            WHEN (@collective_score >= 0.5 AND NEW.value = 1) OR 
                 (@collective_score < 0.5 AND NEW.value = 0) THEN
                accurate_impact + 1
            ELSE accurate_impact
        END,
        reputation_score = (accurate_impact + 1) * 1.0 / (total_impact + 1),
        last_activity = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
    
    -- Update reputation history
    INSERT INTO reputation_history (old_reputation, new_reputation, change_reason, updated_at)
    SELECT 
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id),
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id),
        'impact_accuracy',
        CURRENT_TIMESTAMP;
END;
```

### 4. update_participant_reputation_on_judgment
This trigger updates participant's reputation based on the accuracy of their judgment assessments, comparing against the "collective_score" as a reference value. Increases "accurate_judgment" counter when the judgment aligns with the collective assessment.

```sql
CREATE TRIGGER update_participant_reputation_on_judgment
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Get the collective score for the associated event
    DECLARE @collective_score REAL;
    SELECT @collective_score = te.collective_score
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
    
    -- Update participant reputation based on judgment accuracy
    UPDATE participants
    SET 
        total_judgment = total_judgment + 1,
        accurate_judgment = CASE 
            WHEN (@collective_score >= 0.5 AND NEW.assessment > 0) OR 
                 (@collective_score < 0.5 AND NEW.assessment <= 0) THEN
                accurate_judgment + 1
            ELSE accurate_judgment
        END,
        reputation_score = (accurate_judgment + accurate_impact) * 1.0 / (total_judgment + total_impact + 1),
        last_activity = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
    
    -- Update reputation history
    INSERT INTO reputation_history (old_reputation, new_reputation, change_reason, updated_at)
    SELECT 
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id),
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id),
        'judgment_accuracy',
        CURRENT_TIMESTAMP;
END;
```

### 5. create_impact_prediction_on_impact_creation
This trigger creates a new record in the "impact_predictions" table when a participant creates an impact record that relates to a future predicted factual consequence. This trigger calculates prediction values based on the event's impact data and collective score when a new impact is recorded.

```sql
CREATE TRIGGER create_impact_prediction_on_impact_creation
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Only create prediction if the impact is for a future event
    -- Check if the event is still in 'active' status and has no end date set
    DECLARE @event_status TEXT;
    DECLARE @event_end_date INTEGER;
    
    SELECT @event_status = ec.status, @event_end_date = et.t_end
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    JOIN event_timeline et ON te.timeline_id = et.id
    WHERE ec.id = NEW.event_id;
    
    -- Create prediction if event is active and no end date is set
    IF @event_status = 'active' AND @event_end_date IS NULL THEN
        INSERT INTO impact_predictions (
            event_id, 
            predicted_impact_type, 
            expected_strength, 
            probability, 
            horizon, 
            created_at
        )
        VALUES (
            NEW.event_id,
            NEW.type_id,
            (SELECT collective_score FROM truth_event WHERE id = (
                SELECT created_by FROM event_ci WHERE id = NEW.event_id
            )),  -- Use current collective score as expected strength
            NEW.value,  -- Use the impact value as initial probability
            0.5,  -- Default horizon value
            CURRENT_TIMESTAMP
        );
    END IF;
END;
```

### 6. create_impact_predictions_on_status_change
This trigger creates new records in the "impact_predictions" table when an event's status changes in "event_ci.status" (e.g. from "active"/"resolved" to "archived"). This trigger preserves historical prediction data and adjusts prediction probabilities based on the actual outcomes compared to expected values when the event reaches a resolution state.

```sql
CREATE TRIGGER create_impact_predictions_on_status_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    -- Process when event transitions to resolved or archived state
    IF NEW.status IN ('resolved', 'archived') AND OLD.status = 'active' THEN
        -- Update existing predictions with actual outcomes
        UPDATE impact_predictions
        SET 
            probability = CASE
                WHEN (
                    SELECT ABS(ip.expected_strength - te.collective_score)
                    FROM truth_event te
                    WHERE te.id = (SELECT created_by FROM event_ci WHERE id = impact_predictions.event_id)
                ) < 0.2 THEN  -- If prediction was accurate
                    1.0
                ELSE  -- If prediction was inaccurate
                    0.0
            END,
            horizon = julianday('now') - julianday(created_at, 'unixepoch')  -- Update horizon to reflect actual time elapsed
        WHERE event_id = NEW.id;
        
        -- Also update participant reputation based on prediction accuracy
        UPDATE participants
        SET 
            accurate_impact = accurate_impact + (
                SELECT COUNT(*) 
                FROM impact_predictions ip
                JOIN truth_event te ON ip.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE te.participant_id = participants.id
                AND ABS(ip.expected_strength - te.collective_score) < 0.2
            ),
            total_impact = total_impact + (
                SELECT COUNT(*) 
                FROM impact_predictions ip
                JOIN truth_event te ON ip.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE te.participant_id = participants.id
            )
        WHERE id IN (
            SELECT DISTINCT te.participant_id
            FROM truth_event te
            JOIN event_ci ec ON te.id = ec.created_by
            WHERE ec.id = NEW.id
        );
    END IF;
END;
```

### 7. update_participant_reputation_on_prediction_accuracy
This trigger updates participant reputation based on the accuracy of impact predictions. This trigger aggregates prediction accuracy across all events where the participant created impact records, comparing "impact_predictions.expected_strength" against actual "truth_event.collective_score". The calculation considers the "horizon" value: predictions made earlier (with larger "horizon" values) receive greater weight in reputation calculations. Reputation is updated when events transition to "resolved" or "archived" status.

```sql
CREATE TRIGGER update_participant_reputation_on_prediction_accuracy
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN (OLD.status = 'active' OR OLD.status = 'resolved') AND NEW.status IN ('resolved', 'archived')
BEGIN
    -- Update participant reputation based on prediction accuracy
    UPDATE participants
    SET 
        accurate_impact = (
            SELECT SUM(CASE 
                WHEN ABS(ip.expected_strength - te.collective_score) <= 
                     (0.2 * ABS(ip.expected_strength) + 0.1) THEN 1 
                ELSE 0 
            END)
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
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
                            WHEN ABS(ip.expected_strength - te.collective_score) <= 
                                 (0.2 * ABS(ip.expected_strength) + 0.1) THEN 1 
                            ELSE 0 
                        END) * 1.0 / COUNT(*)
                    ELSE 0.5  -- Default neutral reputation
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

### 8. aggregate_local_scores_for_global
This trigger populates the statements table with local assessments for global calculation when "truth_event" is updated. This trigger fires when the "collective_score" changes and updates the statements table for cross-node aggregation.

```sql
CREATE TRIGGER aggregate_local_scores_for_global
AFTER UPDATE OF collective_score ON truth_event
FOR EACH ROW
WHEN OLD.collective_score != NEW.collective_score
BEGIN
    -- Update or insert statement record for global aggregation
    INSERT OR REPLACE INTO statements (event_id, truth_score, created_at, updated_at)
    VALUES (
        NEW.id,
        NEW.collective_score,
        CASE 
            WHEN (SELECT COUNT(*) FROM statements WHERE event_id = NEW.id) = 0 
            THEN CURRENT_TIMESTAMP 
            ELSE (SELECT created_at FROM statements WHERE event_id = NEW.id)
        END,
        CURRENT_TIMESTAMP
    );
    
    -- Also update event_state_history for temporal analysis
    INSERT INTO event_state_history (event_id, judgment_count, truth_score, impact_count, impact_score, recorded_at)
    SELECT 
        (SELECT id FROM event_ci WHERE created_by = NEW.id),
        (SELECT COUNT(*) FROM judgment WHERE event_id = (SELECT id FROM event_ci WHERE created_by = NEW.id)),
        NEW.collective_score,
        (SELECT COUNT(*) FROM impact WHERE event_id = (SELECT id FROM event_ci WHERE created_by = NEW.id)),
        NEW.impact_score,
        CURRENT_TIMESTAMP
    WHERE (SELECT COUNT(*) FROM event_ci WHERE created_by = NEW.id) > 0;
END;
```

### 9. validate_incoming_event
This trigger validates the structure and cryptographic signatures of events received from other nodes before processing. Verifies that required fields are present, "global_id" is properly formatted, signatures exist, and context fields reference valid entries in their respective tables.

```sql
CREATE TRIGGER validate_incoming_event
BEFORE INSERT ON truth_event
FOR EACH ROW
BEGIN
    -- Validate required fields
    SELECT RAISE(ROLLBACK, 'Missing global_id') WHERE NEW.global_id IS NULL OR NEW.global_id = '';
    SELECT RAISE(ROLLBACK, 'Missing participant_id') WHERE NEW.participant_id IS NULL;
    SELECT RAISE(ROLLBACK, 'Missing description') WHERE NEW.description IS NULL OR NEW.description = '';
    SELECT RAISE(ROLLBACK, 'Missing signature') WHERE NEW.signature IS NULL OR NEW.signature = '';
    
    -- Validate that participant_id exists in participants table
    SELECT RAISE(ROLLBACK, 'Invalid participant_id') 
    WHERE NOT EXISTS (SELECT 1 FROM participants WHERE id = NEW.participant_id);
    
    -- Validate that context fields reference valid entries
    SELECT RAISE(ROLLBACK, 'Invalid category_id') 
    WHERE NEW.category_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM category WHERE id = NEW.category_id);
    
    SELECT RAISE(ROLLBACK, 'Invalid forma_id') 
    WHERE NEW.forma_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM forma WHERE id = NEW.forma_id);
    
    SELECT RAISE(ROLLBACK, 'Invalid cause_id') 
    WHERE NEW.cause_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM cause WHERE id = NEW.cause_id);
    
    SELECT RAISE(ROLLBACK, 'Invalid develop_id') 
    WHERE NEW.develop_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM develop WHERE id = NEW.develop_id);
    
    SELECT RAISE(ROLLBACK, 'Invalid effect_id') 
    WHERE NEW.effect_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM effect WHERE id = NEW.effect_id);
    
    -- Validate timeline reference
    SELECT RAISE(ROLLBACK, 'Invalid timeline_id') 
    WHERE NOT EXISTS (SELECT 1 FROM event_timeline WHERE id = NEW.timeline_id);
END;
```

### 10. process_sync_event_record
This trigger handles the creation of event records from other nodes during synchronization, potentially with different validation rules. Checks for duplicate events, creates corresponding entries in the "event_ci" table, and updates participant activity timestamps.

```sql
CREATE TRIGGER process_sync_event_record
AFTER INSERT ON truth_event
FOR EACH ROW
WHEN NEW.participant_id != 1  -- Only for events from other nodes (not local participant)
BEGIN
    -- Check for duplicate event (same global_id and participant_id)
    SELECT RAISE(IGNORE, 'Duplicate event detected') 
    WHERE EXISTS (
        SELECT 1 FROM truth_event 
        WHERE global_id = NEW.global_id AND participant_id = NEW.participant_id
    );
    
    -- Create participant record if it doesn't exist
    INSERT OR IGNORE INTO participants (public_key, created_at, last_activity)
    SELECT p.public_key, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM participants p
    WHERE p.id = NEW.participant_id;
    
    -- Update participant's last activity
    UPDATE participants
    SET last_activity = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
    
    -- Create event_ci record if it doesn't exist
    INSERT OR IGNORE INTO event_ci (created_by, event_type, status, old_status, resolution_data, created_at)
    VALUES (NEW.id, 'judgment', 'active', 'active', 'unstable', CURRENT_TIMESTAMP);
    
    -- Update progress metrics
    INSERT OR REPLACE INTO progress_metrics (
        id, total_events, total_events_group, total_positive_impacts, 
        total_positive_impacts_group, total_negative_impacts, total_negative_impact_group, 
        trend, trend_group, last_updated
    )
    SELECT 
        1,
        (SELECT COUNT(*) FROM truth_event),
        total_events_group,
        total_positive_impacts,
        total_positive_impacts_group,
        total_negative_impacts,
        total_negative_impact_group,
        trend,
        trend_group,
        CURRENT_TIMESTAMP
    FROM progress_metrics
    WHERE id = 1;
END;
```

### 11. update_event_ci_state_from_impact
This trigger automatically updates the "event_type", "status", and "resolution_data" fields in the "event_ci" table based on changes in impact data, timeline information, and convergence of assessment axes. This trigger fires when new impact records are inserted.

```sql
CREATE TRIGGER update_event_ci_state_from_impact
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update event_type based on whether impact data exists
    UPDATE event_ci
    SET 
        event_type = CASE 
            WHEN EXISTS (
                SELECT 1 FROM impact i
                JOIN truth_event te ON i.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE i.event_id = NEW.event_id
            ) AND EXISTS (
                SELECT 1 FROM judgment j
                JOIN truth_event te ON j.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE j.event_id = NEW.event_id
            ) THEN 'both'
            WHEN EXISTS (
                SELECT 1 FROM impact i
                JOIN truth_event te ON i.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE i.event_id = NEW.event_id
            ) THEN 'impact'
            WHEN EXISTS (
                SELECT 1 FROM judgment j
                JOIN truth_event te ON j.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE j.event_id = NEW.event_id
            ) THEN 'judgment'
            ELSE 'judgment'  -- Default
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.event_id;
    
    -- Update impact metrics
    INSERT OR REPLACE INTO impact_metrics (event_id, total_magnitude, positive_ratio, negative_ratio, uncertainty, calculated_at)
    SELECT 
        NEW.event_id,
        COUNT(*) AS total_magnitude,
        COUNT(CASE WHEN value = 1 THEN 1 END) AS positive_ratio,
        COUNT(CASE WHEN value = 0 THEN 1 END) AS negative_ratio,
        COUNT(CASE WHEN value IS NULL THEN 1 END) AS uncertainty,
        CURRENT_TIMESTAMP
    FROM impact
    WHERE event_id = NEW.event_id;
END;
```

### 12. update_event_ci_state_from_judgment
This trigger automatically updates the "event_type" and "resolution_data" fields in the "event_ci" table based on changes in judgment data and convergence of assessment axes. This trigger fires when new judgment records are inserted.

```sql
CREATE TRIGGER update_event_ci_state_from_judgment
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update event_type based on whether judgment data exists
    UPDATE event_ci
    SET 
        event_type = CASE 
            WHEN EXISTS (
                SELECT 1 FROM impact i
                JOIN truth_event te ON i.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE i.event_id = NEW.event_id
            ) AND EXISTS (
                SELECT 1 FROM judgment j
                JOIN truth_event te ON j.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE j.event_id = NEW.event_id
            ) THEN 'both'
            WHEN EXISTS (
                SELECT 1 FROM impact i
                JOIN truth_event te ON i.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE i.event_id = NEW.event_id
            ) THEN 'impact'
            WHEN EXISTS (
                SELECT 1 FROM judgment j
                JOIN truth_event te ON j.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te.id
                )
                WHERE j.event_id = NEW.event_id
            ) THEN 'judgment'
            ELSE 'judgment'  -- Default
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.event_id;
    
    -- Update judgment weights
    INSERT OR REPLACE INTO judgment_weights (event_id, participant_id, weight, calculated_at)
    SELECT 
        (SELECT id FROM event_ci WHERE created_by = (
            SELECT event_id FROM judgment WHERE id = NEW.id
        )),
        NEW.participant_id,
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id),
        CURRENT_TIMESTAMP
    FROM judgment
    WHERE id = NEW.id;
END;
```

## Additional Utility Triggers

### update_participant_reputation_on_prediction_accuracy_comprehensive
Comprehensive trigger that updates participant reputation based on prediction accuracy across all events where the participant is the event creator.

```sql
CREATE TRIGGER update_participant_reputation_on_prediction_accuracy_comprehensive
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN NEW.status IN ('resolved', 'archived') AND OLD.status != NEW.status
BEGIN
    -- Update reputation for the event creator based on prediction accuracy
    UPDATE participants
    SET 
        accurate_impact = (
            SELECT COUNT(*)
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
            AND ABS(ip.expected_strength - te.collective_score) <= 0.2
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
                            WHEN ABS(ip.expected_strength - te.collective_score) <= 0.2 THEN 1.0
                            ELSE 0.0
                        END) / COUNT(*)
                    ELSE 0.5
                END
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE te.participant_id = participants.id
        )
    WHERE id IN (
        SELECT DISTINCT te.participant_id
        FROM truth_event te
        JOIN event_ci ec ON te.id = ec.created_by
        WHERE ec.id = NEW.id
    );
END;
```

### update_participant_reputation_on_impact_with_proper_ref
Updates participant reputation based on impact accuracy using "collective_score" as reference.

```sql
CREATE TRIGGER update_participant_reputation_on_impact_with_proper_ref
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Get the collective score for the associated event
    DECLARE @collective_score REAL;
    SELECT @collective_score = te.collective_score
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
    
    -- Update participant reputation based on impact accuracy
    UPDATE participants
    SET 
        total_impact = total_impact + 1,
        accurate_impact = CASE 
            WHEN (@collective_score >= 0.5 AND NEW.value = 1) OR 
                 (@collective_score < 0.5 AND NEW.value = 0) THEN
                accurate_impact + 1
            ELSE accurate_impact
        END,
        reputation_score = (accurate_impact * 1.0 + 0.5) / (total_impact + 1),
        last_activity = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
END;
```

### update_participant_reputation_on_judgment_proper_ref
Updates participant reputation based on judgment accuracy with proper participant_id reference.

```sql
CREATE TRIGGER update_participant_reputation_on_judgment_proper_ref
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Get the collective score for the associated event
    DECLARE @collective_score REAL;
    SELECT @collective_score = te.collective_score
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
    
    -- Update participant reputation based on judgment accuracy
    UPDATE participants
    SET 
        total_judgment = total_judgment + 1,
        accurate_judgment = CASE 
            WHEN (@collective_score >= 0.5 AND NEW.assessment > 0) OR 
                 (@collective_score < 0.5 AND NEW.assessment <= 0) THEN
                accurate_judgment + 1
            ELSE accurate_judgment
        END,
        reputation_score = (accurate_judgment * 1.0 + accurate_impact * 1.0) / (total_judgment + total_impact + 1),
        last_activity = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
END;
```

## Notes

- All triggers are designed to maintain data integrity and consistency across the system
- Triggers automatically update related metrics when new data is added
- The system maintains both local and global scoring mechanisms
- Reputation updates happen automatically based on assessment accuracy
- Validation triggers prevent invalid data from being entered into the system
- Synchronization triggers handle incoming data from other nodes in the network
- The triggers implement the core logic of the collective intelligence system