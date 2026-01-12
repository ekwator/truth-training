-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved
-- SQL Model for Impact and Judgment Score Calculations
-- Based on docs/model_core.md:1368-1372

-- Function to calculate impact_score based on impact records
-- The impact_score field represents the cumulative impact assessment of the event at the local node level
-- It is calculated based on the impact records stored in the impact table that are associated with this event
-- The calculation algorithm aggregates the individual impact values taking into account their types, timestamps, and the reputation of the participants who made the impact assessments

CREATE VIEW impact_score_calculation AS
SELECT 
    te.id as event_id,
    -- Calculate impact score based on impact values and participant reputation
    CASE 
        WHEN COUNT(i.id) = 0 THEN 0.0
        ELSE (
            -- Weighted sum of impact values by participant reputation
            COALESCE(SUM(
                CASE 
                    WHEN i.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                    WHEN i.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                    ELSE 0.0
                END
            ), 0.0) / COUNT(i.id)
        )
    END as calculated_impact_score
FROM truth_event te
LEFT JOIN impact i ON te.id = i.event_id
LEFT JOIN truth_event te_part ON i.event_id = te_part.id
LEFT JOIN participants p ON te_part.participant_id = p.public_key
GROUP BY te.id;

-- Function to calculate judgment_score based on judgment records
-- The judgment_score field represents the cumulative truth assessment of the event at the local node level
-- It is calculated based on the judgment records stored in the judgment table that are associated with the corresponding event in the event_ci table
-- The calculation algorithm aggregates the individual judgments taking into account their confidence levels, assessment types, and the reputation of the participants who made the judgments

CREATE VIEW judgment_score_calculation AS
SELECT 
    te.id as event_id,
    -- Calculate judgment score based on judgment values, confidence levels and participant reputation
    CASE 
        WHEN COUNT(j.id) = 0 THEN NULL
        ELSE (
            -- Weighted sum of judgment values by confidence and participant reputation
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
    END as calculated_judgment_score
FROM truth_event te
LEFT JOIN event_ci ec ON te.id = ec.created_by
LEFT JOIN judgment j ON ec.id = j.event_id
LEFT JOIN participants p ON j.participant_id = p.public_key
GROUP BY te.id;

-- Trigger to update impact_score when new impact is added or modified
CREATE TRIGGER update_impact_score_after_impact_change
AFTER INSERT ON impact
BEGIN
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
        LEFT JOIN truth_event te ON i.event_id = te.id
        LEFT JOIN participants p ON te.participant_id = p.public_key
        WHERE i.event_id = NEW.event_id
    )
    WHERE id = NEW.event_id;
END;

-- Trigger to update judgment_score when new judgment is added or modified
CREATE TRIGGER update_judgment_score_after_judgment_change
AFTER INSERT ON judgment
BEGIN
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
        FROM event_ci ec
        JOIN judgment j ON ec.id = j.event_id
        LEFT JOIN participants p ON j.participant_id = p.public_key
        WHERE ec.created_by = (
            SELECT created_by FROM event_ci WHERE id = NEW.event_id
        )
    )
    WHERE id = (
        SELECT created_by FROM event_ci WHERE id = NEW.event_id
    );
END;

-- Function to recalculate all impact scores (for maintenance)
CREATE VIEW recalculate_all_impact_scores AS
SELECT 
    te.id as event_id,
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
    END as new_impact_score
FROM truth_event te
LEFT JOIN impact i ON te.id = i.event_id
LEFT JOIN truth_event te_part ON i.event_id = te_part.id
LEFT JOIN participants p ON te_part.participant_id = p.public_key
GROUP BY te.id;

-- Function to recalculate all judgment scores (for maintenance)
CREATE VIEW recalculate_all_judgment_scores AS
SELECT 
    te.id as event_id,
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
    END as new_judgment_score
FROM truth_event te
LEFT JOIN event_ci ec ON te.id = ec.created_by
LEFT JOIN judgment j ON ec.id = j.event_id
LEFT JOIN participants p ON j.participant_id = p.public_key
GROUP BY te.id;

-- Indexes for performance optimization
CREATE INDEX idx_impact_event_id ON impact(event_id);
CREATE INDEX idx_impact_participant_id ON impact(type_id); -- type_id refers to participants through the context system
CREATE INDEX idx_judgment_event_id ON judgment(event_id);
CREATE INDEX idx_judgment_participant_id ON judgment(participant_id);
CREATE INDEX idx_participants_public_key ON participants(public_key);