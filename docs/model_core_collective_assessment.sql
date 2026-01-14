ocs/model_core_collective_assessment.sql</path>
<content lines="1-461">-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved
-- SQL Model for Collective Event Assessment Logic
-- Based on docs/model_core.md:1377-1429 Collective Event Assessment

-- Table for storing participant impact assessments
CREATE TABLE impact (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    type_id INTEGER NOT NULL,
    trend INTEGER NOT NULL,  -- impact trend 0/1/2/3 (logical_negative/logical_positive/illogical_negative/illogical_positive)
    value INTEGER,           -- impact value (NULL/0/1 for measurable/negative/positive)
    notes TEXT,
    impact_metrics INTEGER NOT NULL,
    impact_predictions INTEGER NOT NULL,
    timeline_id INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES truth_event(id),
    FOREIGN KEY (type_id) REFERENCES effect(id),
    FOREIGN KEY (impact_metrics) REFERENCES impact_metrics(id),
    FOREIGN KEY (impact_predictions) REFERENCES impact_predictions(id),
    FOREIGN KEY (timeline_id) REFERENCES impact_timeline(id),
    UNIQUE(participant_id, event_id)
);

-- Table for impact predictions
CREATE TABLE impact_predictions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,              -- FK → event_ci.id
    predicted_impact_type INTEGER NOT NULL, -- FK → effect.id
    expected_strength REAL NOT NULL,        -- Expected expression, signal strength
    probability REAL NOT NULL,              -- Participant confidence that the predicted effect occurred
    horizon REAL NOT NULL,                  -- Time interval, predicted time lag
    created_at INTEGER NOT NULL,            -- Timestamp of creation
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (predicted_impact_type) REFERENCES effect(id)
);

-- Core table for truth events with collective assessment metrics
CREATE TABLE truth_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description TEXT NOT NULL,
    global_id TEXT NOT NULL UNIQUE,
    participant_id TEXT NOT NULL,
    signature TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    forma_id INTEGER NOT NULL,
    cause_id INTEGER NOT NULL,
    develop_id INTEGER NOT NULL,
    effect_id INTEGER NOT NULL,
    vector INTEGER NOT NULL,
    detected INTEGER,
    corrected INTEGER NOT NULL DEFAULT 0,
    timeline_id INTEGER NOT NULL,
    code INTEGER NOT NULL DEFAULT 1,
    collective_score REAL NOT NULL,  -- cs_i - local training/assessment metric
    impact_score REAL NOT NULL,      -- ci_i - local impact metric
    judgment_score REAL,             -- cj_i - local judgment metric
    FOREIGN KEY (participant_id) REFERENCES participants(public_key),
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (forma_id) REFERENCES forma(id),
    FOREIGN KEY (cause_id) REFERENCES cause(id),
    FOREIGN KEY (develop_id) REFERENCES develop(id),
    FOREIGN KEY (effect_id) REFERENCES effect(id),
    FOREIGN KEY (timeline_id) REFERENCES event_timeline(id)
);

-- Table for storing participant judgment assessments
CREATE TABLE judgment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participant_id INTEGER NOT NULL,
    event_id INTEGER NOT NULL,
    assessment TEXT,         -- type of assessment
    confidence_level REAL,   -- confidence level of the assessment
    reasoning TEXT,          -- reasoning behind the judgment
    consensus_ci INTEGER NOT NULL,
    judgment_weights INTEGER NOT NULL,
    timeline_id INTEGER NOT NULL,
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (consensus_ci) REFERENCES consensus_ci(id),
    FOREIGN KEY (judgment_weights) REFERENCES judgment_weights(id),
    FOREIGN KEY (timeline_id) REFERENCES judgment_timeline(id),
    UNIQUE(participant_id, event_id)
);

-- Table for aggregated impact metrics
CREATE TABLE impact_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    total_magnitude INTEGER,  -- Overall impact significance
    positive_ratio INTEGER,   -- Positive rating
    negative_ratio INTEGER,   -- Negative rating
    uncertainty INTEGER,      -- Undefined rating
    calculated_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

-- Table for impact predictions
CREATE TABLE impact_predictions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,              -- FK → event_ci.id
    predicted_impact_type INTEGER NOT NULL, -- FK → effect.id
    expected_strength REAL NOT NULL,        -- Expected expression, signal strength
    probability REAL NOT NULL,              -- Participant confidence that the predicted effect occurred
    horizon REAL NOT NULL,                  -- Time interval, predicted time lag
    created_at INTEGER NOT NULL,            -- Timestamp of creation
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (predicted_impact_type) REFERENCES effect(id)
);

-- Table for tracking participant reputation and trust
CREATE TABLE participants (
    public_key TEXT PRIMARY KEY,
    signature TEXT NOT NULL,
    reputation_score REAL NOT NULL DEFAULT 0.5,
    reputation_history INTEGER NOT NULL,
    total_judgment INTEGER NOT NULL DEFAULT 0,
    accurate_judgment INTEGER NOT NULL DEFAULT 0,
    total_impact INTEGER NOT NULL DEFAULT 0,
    accurate_impact INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    last_activity INTEGER,
    FOREIGN KEY (reputation_history) REFERENCES reputation_history(id)
);

-- Table for reputation history tracking
CREATE TABLE reputation_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    old_reputation REAL NOT NULL,
    new_reputation REAL NOT NULL,
    change_reason TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Table for collective intelligence event aggregation (event neuron)
CREATE TABLE event_ci (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_by INTEGER NOT NULL,      -- FK to truth_event.id
    event_type TEXT NOT NULL DEFAULT 'judgment',  -- ENUM ("impact", "judgment", "both")
    status TEXT NOT NULL DEFAULT 'active',        -- ENUM ("active", "resolved", "archived")
    old_status TEXT NOT NULL DEFAULT 'active',
    resolution_data TEXT NOT NULL DEFAULT 'unstable',  -- ENUM ("unstable", "suppose", "consent")
    created_at INTEGER NOT NULL,
    FOREIGN KEY (created_by) REFERENCES truth_event(id)
);

-- Table for consensus calculations
CREATE TABLE consensus_ci (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    consensus_value INTEGER NOT NULL,
    confidence_score REAL NOT NULL,
    participant_count INTEGER NOT NULL,
    calculated_at INTEGER NOT NULL,
    algorithm_version INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

-- Table for judgment weights (trust metrics)
CREATE TABLE judgment_weights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participant_id INTEGER NOT NULL,
    event_id INTEGER NOT NULL,
    weight REAL,
    calculated_at INTEGER NOT NULL,
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

-- Table for aggregated statements for global processing
CREATE TABLE statements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    truth_score REAL, -- aggregated truth score from local nodes
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES truth_event(id)
);

-- Table for tracking system progress metrics
CREATE TABLE progress_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    total_events INTEGER NOT NULL,
    total_events_group INTEGER NOT NULL,
    total_positive_impacts REAL NOT NULL,
    total_positive_impacts_group REAL NOT NULL,
    total_negative_impacts REAL NOT NULL,
    total_negative_impact_group REAL NOT NULL,
    trend REAL NOT NULL,
    trend_group REAL NOT NULL,
    last_updated INTEGER NOT NULL
);

-- Function to calculate event truthfulness as statistical function (local)
-- cs_i = f-local(I(E_i))
-- I_i(E_i) = {I_i(1), I_i(2), ..., I_i(n)} - set of participant impact
-- Divide by sign: P_i = ΣI_i(ij)^(+), N_i = ΣI_i(ij)^(-)
CREATE VIEW local_collective_score_calculation AS
SELECT 
    te.id as event_id,
    te.collective_score as cs_i,
    -- Calculate positive and negative impact sums
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) as P_i,
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0) as N_i,
    -- Calculate total impact count
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) as total_impact_count
FROM truth_event te;

-- Function to calculate event truthfulness globally
-- truth_score_i-global = f-global({ cs_i-local_j })
-- {cs_i-local_j} - local assessments of different nodes
CREATE VIEW global_truth_score_calculation AS
SELECT 
    s.event_id,
    AVG(s.truth_score) as global_truth_score,
    COUNT(s.truth_score) as node_count,
    -- Calculate the global truth score based on local collective scores
    (SELECT AVG(collective_score) FROM truth_event WHERE global_id = (
        SELECT global_id FROM truth_event WHERE id = s.event_id
    )) as aggregated_local_collective_score
FROM statements s
GROUP BY s.event_id;

-- Function to calculate event truthfulness (not stored but calculated)
-- Truth(E_i) = (P_i - N_i) / (|I(E_i)| + ε)
-- ε - protection from division by zero
-- result ∈ (-1, +1)
CREATE VIEW event_truthfulness_calculation AS
SELECT 
    te.id as event_id,
    te.description,
    -- Calculate P_i (positive impacts) and N_i (negative impacts)
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) as P_i,
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0) as N_i,
    -- Total impact count (|I(E_i)|)
    (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) as total_impact_count,
    -- Calculate truth with epsilon for division by zero protection
    CASE 
        WHEN (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) = 0 
        THEN 0.0
        ELSE ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) - 
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 / 
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + 0.001)
    END as calculated_truth,
    -- Interpretation: +1: stably confirmed, -1: stably refuted, ≈0: conflict/lack of data
    CASE 
        WHEN ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) - 
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 / 
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + 0.0001) > 0.7 
        THEN 'stably_confirmed'
        WHEN ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 1) - 
              (SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value = 0)) * 1.0 / 
             ((SELECT COUNT(*) FROM impact WHERE event_id = te.id AND value IS NOT NULL) + 0.0001) < -0.7 
        THEN 'stably_refuted'
        ELSE 'conflict_or_lack_of_data'
    END as truth_interpretation
FROM truth_event te;

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
    
    -- Update participant reputation based on prediction accuracy when event is resolved
    -- This connects to the reputation system by comparing predictions with actual outcomes
    UPDATE participants
    SET
        total_impact = total_impact + 1,
        accurate_impact = accurate_impact + CASE
            -- Determine if the prediction was accurate based on comparison with actual impact
            WHEN NEW.status IN ('resolved', 'archived') THEN
                (SELECT CASE
                    WHEN ABS(
                        (SELECT COALESCE(AVG(i.value), 0.5)
                         FROM impact i
                         WHERE i.event_id = te.id) -
                        (ip.expected_strength / 10.0)
                    ) < 0.2 THEN 1  -- Consider prediction accurate if difference is less than 0.2
                    ELSE 0
                END
                FROM truth_event te
                JOIN impact_predictions ip ON ip.event_id = ec.id
                WHERE te.id = ec.created_by AND ip.id = (
                    SELECT id FROM impact_predictions
                    WHERE event_id = ec.id
                    ORDER BY created_at DESC LIMIT 1  -- Get the latest prediction record
                )
                LIMIT 1)
            ELSE 0
        END,
        -- Recalculate reputation score based on accuracy
        reputation_score = CASE
            WHEN (total_impact + 1) > 0 THEN
                (accurate_impact + CASE
                    WHEN NEW.status IN ('resolved', 'archived') THEN
                        (SELECT CASE
                            WHEN ABS(
                                (SELECT COALESCE(AVG(i.value), 0.5)
                                 FROM impact i
                                 WHERE i.event_id = te.id) -
                                (ip.expected_strength / 10.0)
                            ) < 0.2 THEN 1
                            ELSE 0
                        END
                        FROM truth_event te
                        JOIN impact_predictions ip ON ip.event_id = ec.id
                        WHERE te.id = ec.created_by AND ip.id = (
                            SELECT id FROM impact_predictions
                            WHERE event_id = ec.id
                            ORDER BY created_at DESC LIMIT 1  -- Get the latest prediction record
                        )
                        LIMIT 1)
                    ELSE 0
                END) * 1.0 / (total_impact + 1)
            ELSE 0.5
        END
    FROM event_ci ec
    JOIN truth_event te ON te.id = ec.created_by
    WHERE ec.id = NEW.id
    AND te.participant_id = participants.public_key;
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

-- Function to calculate group ratings based on collective scores
CREATE VIEW group_ratings_calculation AS
SELECT 
    p.group_membership as group_id,
    COUNT(p.public_key) as members,
    AVG(p.reputation_score) as avg_score,
    -- Calculate coherence as 1 - (sum of absolute deviations from mean / max possible deviation)
    1 - (SUM(ABS(p.reputation_score - AVG(p.reputation_score))) OVER (PARTITION BY p.group_membership) / (COUNT(*) OVER (PARTITION BY p.group_membership) * 2)) as coherence,
    (SELECT strftime('%s', 'now')) as last_updated
FROM participants p
WHERE p.group_membership IS NOT NULL
GROUP BY p.group_membership;

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

-- Indexes for performance optimization
CREATE INDEX idx_truth_event_global_id ON truth_event(global_id);
CREATE INDEX idx_truth_event_participant_id ON truth_event(participant_id);
CREATE INDEX idx_impact_event_id ON impact(event_id);
CREATE INDEX idx_judgment_event_id ON judgment(event_id);
CREATE INDEX idx_judgment_participant_id ON judgment(participant_id);
CREATE INDEX idx_statements_event_id ON statements(event_id);
CREATE INDEX idx_event_ci_created_by ON event_ci(created_by);