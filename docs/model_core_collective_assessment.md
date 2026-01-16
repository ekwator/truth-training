-- **Document Version:** v1.1.0
-- **Status:** Specification
-- **Updated:** 2025-12-28
-- **Status:** Approved
-- SQL Triggers for Collective Event Assessment Logic

-- Trigger to create event record when a participant submits a new event
-- Creates the initial event record in the truth_event table when a participant submits a new event
CREATE TRIGGER create_event_record
AFTER INSERT ON truth_event
FOR EACH ROW
WHEN NEW.participant_id IS NOT NULL
BEGIN
    -- Create corresponding entry in event_ci table (event neuron)
    INSERT INTO event_ci (created_by, event_type, status, old_status, resolution_data, created_at)
    VALUES (
        NEW.id,
        'judgment',  -- Default event type
        'active',    -- Default status
        'active',    -- Default old status
        'unstable',  -- Default resolution data
        (SELECT strftime('%s', 'now'))  -- Current timestamp
    );
END;

-- Trigger to initialize event metrics when a new event is created
-- Initializes all metric fields (collective_score, impact_score, judgment_score) to default values
CREATE TRIGGER initialize_event_metrics
BEFORE INSERT ON truth_event
FOR EACH ROW
BEGIN
    -- Initialize collective_score to default value (0.5 - neutral)
    SELECT CASE
        WHEN NEW.collective_score IS NULL THEN 0.5
        ELSE NEW.collective_score
    END;
    
    -- Initialize impact_score to default value (0.0)
    SELECT CASE
        WHEN NEW.impact_score IS NULL THEN 0.0
        ELSE NEW.impact_score
    END;
    
    -- Initialize judgment_score to default value (NULL - undefined)
    SELECT CASE
        WHEN NEW.judgment_score IS NULL THEN NULL
        ELSE NEW.judgment_score
    END;
END;

-- Trigger to create impact predictions when corrected flag is set
-- When the corrected flag is set during impact assessment, creates a new impact prediction record
CREATE TRIGGER create_impact_prediction_on_correction
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN NEW.corrected = 1 AND OLD.corrected = 0  -- Only when corrected flag changes from 0 to 1
BEGIN
    -- Insert a new record into impact_predictions based on the event's impact data
    INSERT INTO impact_predictions (
        event_id,
        predicted_impact_type,
        expected_strength,
        probability,
        horizon,
        created_at
    )
    SELECT
        ec.id,                                    -- event_id from event_ci
        NEW.effect_id,                           -- predicted_impact_type from truth_event.effect_id
        -- Calculate expected_strength based on collective_score and impact horizon
        (SELECT SUM(te.collective_score / (COALESCE(ipt.horizon, 1) + 0.001))
         FROM truth_event te
         JOIN event_ci ec2 ON te.id = ec2.created_by
         LEFT JOIN impact_predictions ipt ON ec2.id = ipt.event_id
         WHERE ec2.id = ec.id),                  -- expected_strength using formula from documentation
        -- Calculate probability based on impact accuracy
        (SELECT 1 - ABS(COALESCE(AVG(i.value), 0) - te.collective_score) / (COALESCE(te.collective_score, 0.5) + 0.001)
         FROM impact i
         JOIN truth_event te ON i.event_id = te.id
         WHERE te.id = NEW.id),                  -- probability based on comparison of impact and collective score
        -- Calculate horizon as (t_end - created_at) / (t_end - t_start) as specified in documentation
        (SELECT (et.t_end - (SELECT created_at FROM event_ci WHERE created_by = NEW.id)) /
                (et.t_end - et.t_start + 0.001)  -- Adding small constant to avoid division by zero
         FROM event_timeline et
         WHERE et.id = NEW.timeline_id),         -- horizon from event timeline
        (SELECT strftime('%s', 'now'))           -- created_at timestamp
    FROM event_ci ec
    WHERE ec.created_by = NEW.id
    AND NOT EXISTS (                              -- Make sure we don't create duplicate predictions
        SELECT 1 FROM impact_predictions
        WHERE event_id = ec.id
        AND predicted_impact_type = NEW.effect_id
        AND DATE(created_at, 'unixepoch') = DATE('now', 'unixepoch')  -- Same day check to avoid duplicates
    );
    
    -- Reset the corrected flag to 0 after processing
    UPDATE truth_event
    SET corrected = 0
    WHERE id = NEW.id;
END;

-- Trigger to create impact prediction when a participant creates an impact record
-- This trigger creates a new record in the impact_predictions table when a participant creates an impact record
-- that relates to a future predicted factual consequence.
CREATE TRIGGER create_impact_prediction_on_impact_creation
AFTER INSERT ON impact
FOR EACH ROW
WHEN NEW.value IS NOT NULL  -- Only when the impact has a factual value (not NULL)
BEGIN
    -- Insert a new record into impact_predictions based on the newly created impact
    INSERT INTO impact_predictions (
        event_id,
        predicted_impact_type,
        expected_strength,
        probability,
        horizon,
        created_at
    )
    SELECT
        ec.id,                                    -- event_id from event_ci
        NEW.type_id,                             -- predicted_impact_type from the new impact record
        -- Calculate expected_strength based on collective_score and impact horizon
        (SELECT COALESCE(te.collective_score, 0.5)
         FROM truth_event te
         WHERE te.id = NEW.event_id),            -- Use the collective score of the event
        -- Set probability to a moderate value when creating prediction from impact
        0.6,                                     -- Default probability for new predictions
        -- Calculate horizon as a default value (could be adjusted based on event timeline)
        30.0,                                    -- Default horizon of 30 days
        (SELECT strftime('%s', 'now'))           -- created_at timestamp
    FROM event_ci ec
    WHERE ec.created_by = NEW.event_id
    AND NOT EXISTS (                             -- Make sure we don't create duplicate predictions
        SELECT 1 FROM impact_predictions
        WHERE event_id = ec.id
        AND predicted_impact_type = NEW.type_id
        AND DATE(created_at, 'unixepoch') = DATE('now', 'unixepoch')  -- Same day check to avoid duplicates
    );
END;

-- Trigger to create impact predictions when event status changes
-- This trigger creates new records in the impact_predictions table when an event's status changes in event_ci.status
-- (e.g. from "active"/"resolved" to "archived"), preserving historical prediction data for aggregation
CREATE TRIGGER create_impact_predictions_on_status_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    -- Insert new prediction records based on the status change, preserving historical data
    INSERT INTO impact_predictions (
        event_id,
        predicted_impact_type,
        expected_strength,
        probability,
        horizon,
        created_at
    )
    SELECT
        ec.id,                                    -- event_id from event_ci
        te.effect_id,                            -- predicted_impact_type from truth_event.effect_id
        -- Calculate expected_strength based on collective_score
        COALESCE(te.collective_score, 0.5) as expected_strength,
        -- Calculate probability based on status change and impact accuracy
        CASE
            -- When an event moves to resolved or archived status, calculate probability based on actual outcomes
            WHEN NEW.status IN ('resolved', 'archived') THEN
                -- Calculate probability based on comparison between expected and actual impact
                (SELECT
                    1.0 - ABS(
                        (SELECT COALESCE(AVG(i.value), 0.5)
                         FROM impact i
                         WHERE i.event_id = te.id) -
                        (te.collective_score / 10.0)
                    )
                 FROM truth_event te2
                 WHERE te2.id = te.id)
            ELSE
                -- Default probability for non-resolved statuses
                0.6
        END as probability,
        -- Calculate horizon based on status
        CASE
            WHEN NEW.status = 'archived' THEN
                -- If archived, set horizon to 0 as the prediction period has ended
                0.0
            ELSE
                -- Default horizon for other statuses
                30.0
        END as horizon,
        (SELECT strftime('%s', 'now')) as created_at  -- Current timestamp
    FROM event_ci ec
    JOIN truth_event te ON te.id = ec.created_by
    WHERE ec.id = NEW.id
    AND NOT EXISTS (                              -- Make sure we don't create duplicate predictions for the same status change on the same day
        SELECT 1 FROM impact_predictions
        WHERE event_id = ec.id
        AND DATE(created_at, 'unixepoch') = DATE('now', 'unixepoch')  -- Same day check to avoid duplicates
    );
END;

-- Trigger to update participant reputation based on impact_predictions accuracy
-- This trigger aggregates prediction accuracy across all events where the participant is the event creator
-- Connection: impact_predictions.event_id -> event_ci.id -> event_ci.created_by -> truth_event.id -> truth_event.participant_id
-- Updates reputation considering horizon (predictions made earlier have more weight)
-- Reputation is calculated by aggregating all predictions for all events created by the participant
CREATE TRIGGER update_participant_reputation_on_prediction_accuracy
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN NEW.status IN ('resolved', 'archived') AND OLD.status NOT IN ('resolved', 'archived')
BEGIN
    -- Update reputation for the participant who created the event (identified via truth_event.participant_id)
    -- Aggregate all predictions across all events and calculate weighted accuracy
    UPDATE participants
    SET
        -- Recalculate reputation score considering horizon (predictions made earlier have more weight)
        -- Aggregate all predictions for all events where participant created impact records
        reputation_score = (
            SELECT COALESCE(
                -- Weighted accuracy: predictions with larger horizon (made earlier) have more weight
                SUM(
                    CASE
                        -- Calculate accuracy: compare expected_strength with actual collective_score
                        -- Prediction is accurate if the difference is small relative to expected_strength
                        WHEN ABS(ip.expected_strength - COALESCE(te.collective_score, 0.5)) <=
                            GREATEST(ABS(ip.expected_strength) * 0.2, 0.1)
                        THEN (ip.horizon + 1.0)  -- Weight by horizon: larger horizon = more weight
                        ELSE 0.0
                    END
                ) * 1.0 / NULLIF(
                    SUM(ip.horizon + 1.0), 0
                ),
                0.5  -- Default reputation if no predictions
            )
            FROM impact_predictions ip
            JOIN event_ci ec ON ip.event_id = ec.id
            JOIN truth_event te ON ec.created_by = te.id
            WHERE te.participant_id = participants.public_key
            AND ec.status IN ('resolved', 'archived')
        )
    WHERE public_key = (
        SELECT te.participant_id
        FROM truth_event te
        JOIN event_ci ec ON te.id = ec.created_by
        WHERE ec.id = NEW.id
    );
END;

-- Function to update participant reputation based on impact accuracy
-- Uses collective_score as a reference/anchor value for system state
CREATE TRIGGER update_participant_reputation_on_impact
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update participant's impact metrics based on new impact
    UPDATE participants
    SET
        total_impact = total_impact + 1,
        -- Check if the impact aligns with the collective_score (as a measure of accuracy)
        accurate_impact = accurate_impact + CASE
            WHEN (SELECT collective_score FROM truth_event WHERE id = NEW.event_id) > 0.5 AND NEW.value = 1 THEN 1
            WHEN (SELECT collective_score FROM truth_event WHERE id = NEW.event_id) < 0.5 AND NEW.value = 0 THEN 1
            ELSE 0
        END
    WHERE public_key = (
        SELECT participant_id FROM truth_event WHERE id = NEW.event_id
    );
    
    -- Update reputation score based on combined accuracy of both impact and judgment assessments
        UPDATE participants
        SET reputation_score = CASE
            WHEN (total_impact + total_judgment) > 0 THEN
                (accurate_impact + accurate_judgment) * 1.0 / (total_impact + total_judgment)
            ELSE 0.5
        END
        WHERE public_key = (
            SELECT participant_id FROM truth_event WHERE id = NEW.event_id
        );
    END;

-- Function to update participant reputation based on judgment accuracy
-- Uses collective_score as a reference/anchor value for system state
CREATE TRIGGER update_participant_reputation_on_judgment
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update participant's judgment metrics based on new judgment
    UPDATE participants
    SET
        total_judgment = total_judgment + 1,
        -- Check if the judgment aligns with the collective_score (as a measure of accuracy)
        accurate_judgment = accurate_judgment + CASE
            WHEN (SELECT collective_score FROM truth_event WHERE id = NEW.event_id) > 0.5 AND NEW.assessment = 'true' THEN 1
            WHEN (SELECT collective_score FROM truth_event WHERE id = NEW.event_id) < 0.5 AND NEW.assessment = 'false' THEN 1
            ELSE 0
        END
    WHERE public_key = (
        SELECT participant_id FROM truth_event WHERE id = (
            SELECT created_by FROM event_ci WHERE id = NEW.event_id
        )
    );
    
    -- Update reputation score based on combined accuracy of both impact and judgment assessments
        UPDATE participants
        SET reputation_score = CASE
            WHEN (total_impact + total_judgment) > 0 THEN
                (accurate_impact + accurate_judgment) * 1.0 / (total_impact + total_judgment)
            ELSE 0.5
        END
        WHERE public_key = (
            SELECT participant_id FROM truth_event WHERE id = (
                SELECT created_by FROM event_ci WHERE id = NEW.event_id
            )
        );
    END;

-- Function to aggregate local collective scores for global processing
-- This populates the statements table with local assessments for global calculation
CREATE TRIGGER aggregate_local_scores_for_global
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN NEW.collective_score != OLD.collective_score
BEGIN
    -- Insert or update the statement with the new collective score
    INSERT OR REPLACE INTO statements (event_id, truth_score, created_at, updated_at)
    VALUES (
        NEW.id,
        NEW.collective_score,
        CASE
            WHEN (SELECT COUNT(*) FROM statements WHERE event_id = NEW.id) = 0
            THEN (SELECT strftime('%s', 'now'))
            ELSE (SELECT created_at FROM statements WHERE event_id = NEW.id LIMIT 1)
        END,
        (SELECT strftime('%s', 'now'))
    );
END;

-- Trigger to validate incoming event structure and signatures
-- Validates the structure and cryptographic signatures of events received from other nodes before processing
CREATE TRIGGER validate_incoming_event
BEFORE INSERT ON truth_event
FOR EACH ROW
WHEN NEW.participant_id IS NOT NULL  -- This indicates it's coming from a node (not internal system operation)
BEGIN
    -- Verify that the global_id is properly formatted (UUID-like)
    SELECT CASE
        WHEN LENGTH(NEW.global_id) < 10 THEN RAISE(ABORT, 'Invalid global_id format')
        ELSE NULL
    END;
    
    -- Verify that required fields are present
    SELECT CASE
        WHEN NEW.description IS NULL OR LENGTH(TRIM(NEW.description)) = 0 THEN RAISE(ABORT, 'Event description is required')
        ELSE NULL
    END;
    
    -- Verify that signature exists
    SELECT CASE
        WHEN NEW.signature IS NULL OR LENGTH(TRIM(NEW.signature)) = 0 THEN RAISE(ABORT, 'Event signature is required')
        ELSE NULL
    END;
    
    -- Verify that context fields are valid references
    SELECT CASE
        WHEN NOT EXISTS (SELECT 1 FROM category WHERE id = NEW.category_id) THEN RAISE(ABORT, 'Invalid category_id')
        WHEN NOT EXISTS (SELECT 1 FROM forma WHERE id = NEW.forma_id) THEN RAISE(ABORT, 'Invalid forma_id')
        WHEN NOT EXISTS (SELECT 1 FROM cause WHERE id = NEW.cause_id) THEN RAISE(ABORT, 'Invalid cause_id')
        WHEN NOT EXISTS (SELECT 1 FROM develop WHERE id = NEW.develop_id) THEN RAISE(ABORT, 'Invalid develop_id')
        WHEN NOT EXISTS (SELECT 1 FROM effect WHERE id = NEW.effect_id) THEN RAISE(ABORT, 'Invalid effect_id')
        ELSE NULL
    END;
    
    -- Verify that participant_id exists in participants table
    SELECT CASE
        WHEN NOT EXISTS (SELECT 1 FROM participants WHERE public_key = NEW.participant_id) THEN RAISE(ABORT, 'Invalid participant_id')
        ELSE NULL
    END;
END;

-- Trigger to process sync event record during synchronization
-- Handles the creation of event records from other nodes during synchronization, potentially with different validation rules
CREATE TRIGGER process_sync_event_record
AFTER INSERT ON truth_event
FOR EACH ROW
WHEN NEW.participant_id IS NOT NULL  -- Indicates the event came from synchronization
BEGIN
    -- Check if this is a duplicate event (same global_id and participant_id combination)
    SELECT CASE
        WHEN (SELECT COUNT(*) FROM truth_event WHERE global_id = NEW.global_id AND participant_id = NEW.participant_id) > 1
        THEN RAISE(ABORT, 'Duplicate event detected')
        ELSE NULL
    END;
    
    -- Update or create corresponding entry in event_ci table
    INSERT OR REPLACE INTO event_ci (created_by, event_type, status, old_status, resolution_data, created_at)
    VALUES (
        NEW.id,
        'judgment',  -- Default event type
        'active',    -- Default status
        'active',    -- Default old status
        'unstable',  -- Default resolution data
        (SELECT strftime('%s', 'now'))  -- Current timestamp
    );
    
    -- Update participant's last activity timestamp
    UPDATE participants
    SET last_activity = (SELECT strftime('%s', 'now'))
    WHERE public_key = NEW.participant_id;
END;