-- **Document Version:** v1.1.0
-- **Status:** Specification
-- **Updated:** 2025-12-28
-- **Status:** Approved
-- SQL Triggers for Impact and Judgment Score Calculations

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