# Scoring Triggers

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This document describes the SQL triggers that implement the impact and judgment scoring calculations for the Truth Training system. These triggers automatically recalculate scores when new assessments are added to maintain data consistency.

## Purpose
The scoring triggers ensure that impact and judgment scores are automatically updated when new impact or judgment data is added, maintaining data consistency and enabling accurate collective intelligence calculations across all nodes in the network.

## Trigger Definitions

### 1. update_impact_score_after_impact_change
This trigger recalculates the impact score for an event when new impact data is added, considering participant reputation. For local user ("participants.id" = 1), filters by "participant_id" = 1; for global/group calculations, groups by "impact.participant_id".

```sql
CREATE TRIGGER update_impact_score_after_impact_change
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update impact score based on new impact data and participant reputation
    UPDATE truth_event
    SET 
        impact_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        SUM(i.value * p.reputation_score) / COUNT(*)
                    ELSE 0.0
                END
            FROM impact i
            JOIN participants p ON i.participant_id = p.id
            WHERE i.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        ),
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
            WHERE te.id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
    
    -- Update event projection with new scores
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
    WHERE ec.id = NEW.event_id;
END;
```

### 2. update_judgment_score_after_judgment_change
This trigger recalculates the judgment score for an event when new judgment data is added, considering participant reputation and confidence. For local user ("participants.id" = 1), filters by "participant_id" = 1; for global/group calculations, groups by "judgment.participant_id".

```sql
CREATE TRIGGER update_judgment_score_after_judgment_change
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update judgment score based on new judgment data, participant reputation and confidence
    UPDATE truth_event
    SET 
        judgment_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        SUM(
                            CASE 
                                WHEN j.assessment = 'true' THEN 1.0
                                WHEN j.assessment = 'false' THEN -1.0
                                ELSE 0.0
                            END * j.confidence_level * p.reputation_score
                        ) / COUNT(*)
                    ELSE NULL
                END
            FROM judgment j
            JOIN participants p ON j.participant_id = p.id
            WHERE j.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        ),
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
            WHERE te.id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
    
    -- Update consensus_ci with new judgment data
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
        ROUND(AVG(
            CASE 
                WHEN j.assessment = 'true' THEN 1.0
                WHEN j.assessment = 'false' THEN -1.0
                ELSE 0.0
            END * jw.weight
        )),
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
END;
```

### 3. update_collective_score_after_impact_or_judgment_change
This trigger recalculates the "collective_score" when impact or judgment scores change, implementing the relationship: "collective_score" ← "impact_score" + "judgment_score".

```sql
CREATE TRIGGER update_collective_score_after_impact_or_judgment_change
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN OLD.impact_score != NEW.impact_score OR OLD.judgment_score != NEW.judgment_score
BEGIN
    -- Update collective score based on impact and judgment scores
    UPDATE truth_event
    SET 
        collective_score = (
            CASE 
                WHEN NEW.impact_score IS NOT NULL AND NEW.judgment_score IS NOT NULL THEN
                    (NEW.impact_score + NEW.judgment_score) / 2.0
                WHEN NEW.impact_score IS NOT NULL THEN
                    NEW.impact_score
                WHEN NEW.judgment_score IS NOT NULL THEN
                    NEW.judgment_score
                ELSE 0.5  -- Default neutral score
            END
        )
    WHERE id = NEW.id;
    
    -- Update event_projection with new collective score
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
    WHERE ec.created_by = NEW.id;
    
    -- Update statements for global aggregation
    INSERT OR REPLACE INTO statements (
        event_id,
        truth_score,
        created_at,
        updated_at
    )
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
END;
```

### 4. update_impact_score_after_impact_change_all_participants
This trigger updates impact score based on all participants' contributions, implementing the relationship: "impact.participant_id" → "participants.id".

```sql
CREATE TRIGGER update_impact_score_after_impact_change_all_participants
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update impact score considering contributions from all participants
    UPDATE truth_event
    SET 
        impact_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        SUM(i.value * COALESCE(p.reputation_score, 0.5)) / COUNT(*)
                    ELSE 0.0
                END
            FROM impact i
            LEFT JOIN participants p ON i.participant_id = p.id
            WHERE i.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        ),
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
            WHERE te.id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
    
    -- Update impact_metrics table
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
        SUM(CASE WHEN i.value = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS positive_ratio,
        SUM(CASE WHEN i.value = 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS negative_ratio,
        SUM(CASE WHEN i.value IS NULL THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS uncertainty,
        CURRENT_TIMESTAMP
    FROM impact i
    WHERE i.event_id = NEW.event_id;
END;
```

### 5. update_judgment_score_after_judgment_change_all_participants
This trigger updates judgment score based on all participants' contributions, implementing the relationship: "judgment.participant_id" → "participants.id".

```sql
CREATE TRIGGER update_judgment_score_after_judgment_change_all_participants
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update judgment score considering contributions from all participants
    UPDATE truth_event
    SET 
        judgment_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        SUM(
                            CASE 
                                WHEN j.assessment = 'true' THEN 1.0
                                WHEN j.assessment = 'false' THEN -1.0
                                ELSE 0.0
                            END * j.confidence_level * COALESCE(p.reputation_score, 0.5)
                        ) / COUNT(*)
                    ELSE NULL
                END
            FROM judgment j
            LEFT JOIN participants p ON j.participant_id = p.id
            WHERE j.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        ),
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
            WHERE te.id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
    
    -- Update judgment_weights based on participant reputation
    INSERT OR REPLACE INTO judgment_weights (
        event_id,
        participant_id,
        weight,
        calculated_at
    )
    SELECT 
        NEW.event_id,
        j.participant_id,
        COALESCE(p.reputation_score, 0.5) AS weight,
        CURRENT_TIMESTAMP
    FROM judgment j
    LEFT JOIN participants p ON j.participant_id = p.id
    WHERE j.event_id = NEW.event_id;
END;
```

### 6. update_impact_metrics_after_impact_insert
This trigger updates "impact_metrics" after impact is added, implementing the relationship: "impact_metrics.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_impact_metrics_after_impact_insert
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
        COUNT(CASE WHEN value = 1 THEN 1 END) AS positive_ratio,
        COUNT(CASE WHEN value = 0 THEN 1 END) AS negative_ratio,
        COUNT(CASE WHEN value IS NULL THEN 1 END) AS uncertainty,
        CURRENT_TIMESTAMP
    FROM impact
    WHERE event_id = NEW.event_id;
    
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
        NEW.event_id,
        (SELECT COUNT(*) FROM judgment WHERE event_id = NEW.event_id),
        (SELECT collective_score FROM truth_event WHERE id = (
            SELECT created_by FROM event_ci WHERE id = NEW.event_id
        )),
        (SELECT COUNT(*) FROM impact WHERE event_id = NEW.event_id),
        (SELECT impact_score FROM truth_event WHERE id = (
            SELECT created_by FROM event_ci WHERE id = NEW.event_id
        )),
        CURRENT_TIMESTAMP;
END;
```

### 7. update_judgment_weights_after_judgment_insert
This trigger updates "judgment_weights" after judgment is added, implementing the relationship: "judgment_weights.event_id" → "event_ci.id" → "judgment_weights.participant_id" → "participants.id".

```sql
CREATE TRIGGER update_judgment_weights_after_judgment_insert
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update judgment weights based on participant reputation
    INSERT OR REPLACE INTO judgment_weights (
        event_id,
        participant_id,
        weight,
        calculated_at
    )
    SELECT 
        NEW.event_id,
        NEW.participant_id,
        (SELECT reputation_score FROM participants WHERE id = NEW.participant_id) AS weight,
        CURRENT_TIMESTAMP;
    
    -- Update consensus_ci with new judgment data
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
    LEFT JOIN judgment j ON j.event_id = ec.id
    LEFT JOIN judgment_weights jw ON j.participant_id = jw.participant_id AND j.event_id = jw.event_id
    WHERE ec.id = NEW.event_id
    GROUP BY ec.id;
END;
```

### 8. update_consensus_ci_after_judgment_insert
This trigger updates "consensus_ci" after judgment is added, implementing the relationship: "consensus_ci.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_consensus_ci_after_judgment_insert
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
END;
```

### 9. update_event_projection_after_scores_change
This trigger updates "event_projection" after scores change, implementing the relationship: "event_projection.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_event_projection_after_scores_change
AFTER UPDATE ON truth_event
FOR EACH ROW
WHEN OLD.impact_score != NEW.impact_score OR OLD.judgment_score != NEW.judgment_score OR OLD.collective_score != NEW.collective_score
BEGIN
    -- Update event projection based on new scores
    INSERT OR REPLACE INTO event_projection (
        event_id,
        truth_score,
        impact_score,
        quadrant,
        calculated_at
    )
    SELECT 
        ec.id,
        NEW.collective_score,
        NEW.impact_score,
        CASE 
            WHEN NEW.collective_score >= 0.5 AND NEW.impact_score >= 0 THEN 'Q1'
            WHEN NEW.collective_score >= 0.5 AND NEW.impact_score < 0 THEN 'Q2'
            WHEN NEW.collective_score < 0.5 AND NEW.impact_score >= 0 THEN 'Q3'
            ELSE 'Q4'
        END AS quadrant,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    WHERE ec.created_by = NEW.id;
    
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
        ec.id,
        (SELECT COUNT(*) FROM judgment WHERE event_id = ec.id),
        NEW.collective_score,
        (SELECT COUNT(*) FROM impact WHERE event_id = ec.id),
        NEW.impact_score,
        CURRENT_TIMESTAMP
    FROM event_ci ec
    WHERE ec.created_by = NEW.id;
END;
```

### 10. update_truth_state_after_event_state_change
This trigger updates "truth_state" after event state changes, implementing the relationship: "truth_state.event_id" → "event_ci.id" → "event_ci.created_by" → "truth_event.id" → "truth_event.participant_id".

```sql
CREATE TRIGGER update_truth_state_after_event_state_change
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status
BEGIN
    -- Update truth state based on event status changes
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
END;
```

### 11. update_progress_metrics_after_event
This trigger updates progress metrics when a new event is processed, recalculating total counts and trends.

```sql
CREATE TRIGGER update_progress_metrics_after_event
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
            WHERE pg.type = 'auto'  -- Assuming group events are auto groups
        )) AS total_events_group,
        (SELECT SUM(positive_ratio) FROM impact_metrics) AS total_positive_impacts,
        (SELECT SUM(positive_ratio) FROM impact_metrics WHERE event_id IN (
            SELECT id FROM event_ci WHERE created_by IN (
                SELECT id FROM truth_event WHERE participant_id IN (
                    SELECT DISTINCT pgm.participant_id
                    FROM participants_groups pg
                    JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.type = 'auto'
                )
            )
        )) AS total_positive_impacts_group,
        (SELECT SUM(negative_ratio) FROM impact_metrics) AS total_negative_impacts,
        (SELECT SUM(negative_ratio) FROM impact_metrics WHERE event_id IN (
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
                ((SELECT SUM(positive_ratio) FROM impact_metrics) - 
                 (SELECT SUM(negative_ratio) FROM impact_metrics)) / 
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
                ((SELECT SUM(positive_ratio) FROM impact_metrics WHERE event_id IN (
                    SELECT id FROM event_ci WHERE created_by IN (
                        SELECT id FROM truth_event WHERE participant_id IN (
                            SELECT DISTINCT pgm.participant_id
                            FROM participants_groups pg
                            JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                            WHERE pg.type = 'auto'
                        )
                    )
                )) - 
                 (SELECT SUM(negative_ratio) FROM impact_metrics WHERE event_id IN (
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

### 12. update_progress_metrics_after_impact_recording
This trigger updates progress metrics when new impact is recorded, recalculating impact totals and trends.

```sql
CREATE TRIGGER update_progress_metrics_after_impact_recording
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update progress metrics based on new impact recording
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

## Additional Utility Triggers

### update_impact_score_after_impact_change_local_user
Specialized trigger for local user impact scoring.

```sql
CREATE TRIGGER update_impact_score_after_impact_change_local_user
AFTER INSERT ON impact
FOR EACH ROW
WHEN NEW.participant_id = 1  -- Local user
BEGIN
    -- Update impact score for local user only
    UPDATE truth_event
    SET 
        impact_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        AVG(i.value)  -- Simple average for local user
                    ELSE 0.0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
            AND i.participant_id = 1  -- Filter for local user only
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
END;
```

### update_judgment_score_after_judgment_change_local_user
Specialized trigger for local user judgment scoring.

```sql
CREATE TRIGGER update_judgment_score_after_judgment_change_local_user
AFTER INSERT ON judgment
FOR EACH ROW
WHEN NEW.participant_id = 1  -- Local user
BEGIN
    -- Update judgment score for local user only
    UPDATE truth_event
    SET 
        judgment_score = (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 0 THEN
                        AVG(CASE 
                            WHEN j.assessment = 'true' THEN 1.0
                            WHEN j.assessment = 'false' THEN -1.0
                            ELSE 0.0
                        END * j.confidence_level)
                    ELSE NULL
                END
            FROM judgment j
            WHERE j.event_id = (
                SELECT created_by 
                FROM event_ci 
                WHERE id = NEW.event_id
            )
            AND j.participant_id = 1  -- Filter for local user only
        )
    WHERE id = (
        SELECT created_by 
        FROM event_ci 
        WHERE id = NEW.event_id
    );
END;
```

## Notes

- All triggers maintain data consistency across the scoring system
- Local user scoring can be filtered separately using participant_id = 1
- Global/group calculations use participant weights based on reputation
- The triggers update dependent metrics automatically when new data is added
- Scoring calculations consider participant reputation to weight assessments appropriately
- Temporal tracking is maintained through event_state_history updates