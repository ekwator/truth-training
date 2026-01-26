-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Triggers for Impact and Judgment Score Calculations  

-- Trigger to update impact_score when new impact is added or modified
-- For local user (participants.id = 1), filter by participant_id = 1
-- For global/group calculations, group by impact.participant_id
```sql
CREATE TRIGGER update_impact_score_after_impact_change
AFTER INSERT ON impact
BEGIN
    -- Update impact score for local participant (participant_id = 1)
    UPDATE truth_event
    SET impact_score = (
        SELECT
            CASE
                WHEN COUNT(i.id) = 0 THEN 0.0
                ELSE (
                    COALESCE(SUM(
                        CASE
                            WHEN i.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                            WHEN i.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                            ELSE 0.0
                        END
                    ), 0.0) / COUNT(i.id)
                )
            END
        FROM impact i
        JOIN truth_event te ON i.event_id = te.id
        JOIN participants p ON te.participant_id = p.id  -- Fixed: was p.public_key, now p.id
        WHERE i.event_id = NEW.event_id
          AND p.id = 1  -- Filter for local participant
    )
    WHERE id = NEW.event_id;
    
    -- For global/group calculations, we would update aggregate tables grouped by participant_id
    -- This would typically update separate aggregate tables rather than the main truth_event table
END;
```
-- Trigger to update judgment_score when new judgment is added or modified
-- For local user (participants.id = 1), filter by participant_id = 1
-- For global/group calculations, group by judgment.participant_id
```sql
CREATE TRIGGER update_judgment_score_after_judgment_change
AFTER INSERT ON judgment
BEGIN
    -- Update judgment score for local participant (participant_id = 1)
    UPDATE truth_event
    SET judgment_score = (
        SELECT
            CASE
                WHEN COUNT(j.id) = 0 THEN NULL
                ELSE (
                    COALESCE(SUM(
                        CASE
                            WHEN j.assessment = 'true' THEN
                                COALESCE(j.confidence_level, 0.5) * COALESCE(p.reputation_score, 0.5)
                            WHEN j.assessment = 'false' THEN
                                -1.0 * COALESCE(j.confidence_level, 0.5) * COALESCE(p.reputation_score, 0.5)
                            ELSE 0.0
                        END
                    ), 0.0) / COUNT(j.id)
                )
            END
        FROM judgment j
        JOIN event_ci ec ON j.event_id = ec.id
        JOIN truth_event te ON ec.created_by = te.id
        JOIN participants p ON te.participant_id = p.id  -- Fixed: was p.public_key, now p.id
        WHERE ec.id = NEW.event_id
          AND p.id = 1  -- Filter for local participant
    )
    WHERE id = (
        SELECT created_by FROM event_ci WHERE id = NEW.event_id
    );
    
    -- For global/group calculations, we would update aggregate tables grouped by participant_id
    -- This would typically update separate aggregate tables rather than the main truth_event table
END;
```

-- Trigger to update collective_score when impact or judgment scores change
-- Implements the relationship: collective_score ← impact_score + judgment_score
```sql
CREATE TRIGGER update_collective_score_after_impact_or_judgment_change
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN (NEW.impact_score != OLD.impact_score OR NEW.judgment_score != OLD.judgment_score)
BEGIN
    UPDATE truth_event
    SET collective_score = (
        SELECT 
            CASE
                WHEN te.impact_score IS NOT NULL AND te.judgment_score IS NOT NULL
                THEN (te.impact_score + te.judgment_score) / 2.0
                WHEN te.impact_score IS NOT NULL
                THEN te.impact_score
                WHEN te.judgment_score IS NOT NULL
                THEN te.judgment_score
                ELSE 0.5  -- Default neutral value
            END
        FROM truth_event te
        WHERE te.id = NEW.id
    )
    WHERE id = NEW.id;
END;
```

-- Trigger to update impact score after impact change for all participants
-- Implements the relationship: impact.participant_id → participants.id
```sql
CREATE TRIGGER update_impact_score_after_impact_change_all_participants
AFTER INSERT ON impact
BEGIN
    -- Update impact score based on all participants' contributions
    UPDATE truth_event
    SET impact_score = (
        SELECT
            CASE
                WHEN COUNT(i.id) = 0 THEN 0.0
                ELSE (
                    COALESCE(SUM(
                        CASE
                            WHEN i.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                            WHEN i.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                            ELSE 0.0
                        END
                    ), 0.0) / COUNT(i.id)
                )
            END
        FROM impact i
        JOIN truth_event te ON i.event_id = te.id
        JOIN participants p ON te.participant_id = p.id
        WHERE i.event_id = NEW.event_id
    )
    WHERE id = NEW.event_id;
END;
```

-- Trigger to update judgment score after judgment change for all participants
-- Implements the relationship: judgment.participant_id → participants.id
```sql
CREATE TRIGGER update_judgment_score_after_judgment_change_all_participants
AFTER INSERT ON judgment
BEGIN
    -- Update judgment score based on all participants' contributions
    UPDATE truth_event
    SET judgment_score = (
        SELECT
            CASE
                WHEN COUNT(j.id) = 0 THEN NULL
                ELSE (
                    COALESCE(SUM(
                        CASE
                            WHEN j.assessment = 'true' THEN
                                COALESCE(j.confidence_level, 0.5) * COALESCE(p.reputation_score, 0.5)
                            WHEN j.assessment = 'false' THEN
                                -1.0 * COALESCE(j.confidence_level, 0.5) * COALESCE(p.reputation_score, 0.5)
                            ELSE 0.0
                        END
                    ), 0.0) / COUNT(j.id)
                )
            END
        FROM judgment j
        JOIN event_ci ec ON j.event_id = ec.id
        JOIN truth_event te ON ec.created_by = te.id
        JOIN participants p ON te.participant_id = p.id
        WHERE ec.id = NEW.event_id
    )
    WHERE id = (
        SELECT created_by FROM event_ci WHERE id = NEW.event_id
    );
END;
```

-- Trigger to update impact_metrics after impact is added
-- Implements the relationship: impact_metrics.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_impact_metrics_after_impact_insert
AFTER INSERT ON impact
BEGIN
    INSERT OR REPLACE INTO impact_metrics (
        id,
        event_id,
        total_magnitude,
        positive_ratio,
        negative_ratio,
        uncertainty,
        calculated_at
    )
    SELECT
        (SELECT COALESCE(MAX(id), 0) + 1 FROM impact_metrics WHERE id = (
            SELECT COALESCE(MAX(im.id), 0) + 1 FROM impact_metrics im
        )) as id,
        ec.id as event_id,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id) as total_magnitude,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value = 1) as positive_ratio,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value = 0) as negative_ratio,
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id AND value IS NULL) as uncertainty,
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    JOIN participants p ON te.participant_id = p.id
    WHERE ec.id = (
        SELECT event_id FROM impact WHERE id = NEW.id
    );
END;
```

-- Trigger to update judgment_weights after judgment is added
-- Implements the relationship: judgment_weights.event_id → event_ci.id → judgment_weights.participant_id → participants.id
```sql
CREATE TRIGGER update_judgment_weights_after_judgment_insert
AFTER INSERT ON judgment
BEGIN
    INSERT OR REPLACE INTO judgment_weights (
        id,
        participant_id,
        event_id,
        weight,
        calculated_at
    )
    SELECT
        (SELECT COALESCE(MAX(id), 0) + 1 FROM judgment_weights WHERE id = (
            SELECT COALESCE(MAX(jw.id), 0) + 1 FROM judgment_weights jw
        )) as id,
        p.id as participant_id,
        ec.id as event_id,
        p.reputation_score as weight,
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM participants p
    JOIN judgment j ON p.id = j.participant_id
    JOIN event_ci ec ON j.event_id = ec.id
    WHERE j.id = NEW.id;
END;
```

-- Trigger to update consensus_ci after judgment is added
-- Implements the relationship: consensus_ci.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_consensus_ci_after_judgment_insert
AFTER INSERT ON judgment
BEGIN
    INSERT OR REPLACE INTO consensus_ci (
        id,
        event_id,
        consensus_value,
        confidence_score,
        participant_count,
        calculated_at,
        algorithm_version
    )
    SELECT
        (SELECT COALESCE(MAX(id), 0) + 1 FROM consensus_ci WHERE id = (
            SELECT COALESCE(MAX(cci.id), 0) + 1 FROM consensus_ci cci
        )) as id,
        ec.id as event_id,
        ROUND(AVG(j.assessment * jw.weight)) as consensus_value,
        AVG(j.confidence_level * jw.weight) as confidence_score,
        COUNT(DISTINCT j.participant_id) as participant_count,
        (SELECT strftime('%s', 'now')) as calculated_at,
        1 as algorithm_version
    FROM judgment j
    JOIN judgment_weights jw ON j.participant_id = jw.participant_id
    JOIN event_ci ec ON j.event_id = ec.id
    WHERE ec.id = NEW.event_id
    GROUP BY ec.id;
END;
```

-- Trigger to update event_projection after scores are updated
-- Implements the relationship: event_projection.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_event_projection_after_scores_change
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN (NEW.collective_score != OLD.collective_score OR NEW.impact_score != OLD.impact_score)
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
        NEW.collective_score as truth_score,
        NEW.impact_score as impact_score,
        CASE
            WHEN NEW.collective_score >= 0.5 AND NEW.impact_score >= 0 THEN 'Q1'
            WHEN NEW.collective_score >= 0.5 AND NEW.impact_score < 0 THEN 'Q2'
            WHEN NEW.collective_score < 0.5 AND NEW.impact_score >= 0 THEN 'Q3'
            ELSE 'Q4'
        END as quadrant,
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    WHERE ec.created_by = NEW.id
    AND EXISTS (
        SELECT 1 FROM participants p
        WHERE p.id = NEW.participant_id
    );
END;
```

-- Trigger to update truth_state after event state changes
-- Implements the relationship: truth_state.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
```sql
CREATE TRIGGER update_truth_state_after_event_state_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN (NEW.status != OLD.status)
BEGIN
    INSERT OR REPLACE INTO truth_state (
        id,
        event_id,
        time_axis_id,
        truth_state,
        truth_score,
        dispersion,
        confidence,
        calculated_at
    )
    SELECT
        (SELECT COALESCE(MAX(id), 0) + 1 FROM truth_state WHERE id = (
            SELECT COALESCE(MAX(ts.id), 0) + 1 FROM truth_state ts
        )) as id,
        ec.id as event_id,
        2 as time_axis_id, -- Using 'present' time axis as default
        CASE
            WHEN ec.status = 'active' THEN 'active'
            WHEN ec.status = 'resolved' THEN 'resolved'
            ELSE 'archived'
        END as truth_state,
        te.collective_score as truth_score,
        0.1 as dispersion, -- Placeholder value
        0.8 as confidence, -- Placeholder value
        (SELECT strftime('%s', 'now')) as calculated_at
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    WHERE ec.id = NEW.id
    AND EXISTS (
        SELECT 1 FROM participants p
        WHERE p.id = te.participant_id
    );
END;
```