-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Calculates consensus values based on participant judgments and impact assessments

-- Function to calculate consensus based on participant judgments and impact assessments
-- Implements the aggregation function that computes collective agreement on event truth and impact values
-- Connects through the relationships: consensus_ci.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
-- Also connects: judgment.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
-- Also connects: impact.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```sql
CREATE VIEW consensus_calculation AS
SELECT
    ec.id as event_id,
    -- Calculate consensus based on judgments with participant reputation weighting
    CASE
        WHEN COUNT(j.id) = 0 THEN NULL
        ELSE SUM(
            CASE 
                WHEN j.assessment = 'true' THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                WHEN j.assessment = 'false' THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                ELSE 0.0
            END
        ) / COUNT(j.id)
    END as judgment_consensus,
    -- Calculate consensus based on impact assessments with participant reputation weighting
    CASE
        WHEN COUNT(i.id) = 0 THEN NULL
        ELSE SUM(
            CASE 
                WHEN i.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                WHEN i.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                ELSE 0.0
            END
        ) / COUNT(i.id)
    END as impact_consensus,
    -- Combined consensus score
    CASE
        WHEN COUNT(j.id) = 0 AND COUNT(i.id) = 0 THEN NULL
        WHEN COUNT(j.id) = 0 THEN (
            SELECT SUM(
                CASE 
                    WHEN i2.value = 1 THEN 1.0 * COALESCE(p2.reputation_score, 0.5)
                    WHEN i2.value = 0 THEN -1.0 * COALESCE(p2.reputation_score, 0.5)
                    ELSE 0.0
                END
            ) / COUNT(i2.id)
            FROM impact i2
            JOIN event_ci ec2 ON i2.event_id = ec2.id
            JOIN truth_event te2 ON ec2.created_by = te2.id
            JOIN participants p2 ON te2.participant_id = p2.id
            WHERE ec2.id = ec.id
        )
        WHEN COUNT(i.id) = 0 THEN (
            SELECT SUM(
                CASE 
                    WHEN j2.assessment = 'true' THEN 1.0 * COALESCE(p3.reputation_score, 0.5)
                    WHEN j2.assessment = 'false' THEN -1.0 * COALESCE(p3.reputation_score, 0.5)
                    ELSE 0.0
                END
            ) / COUNT(j2.id)
            FROM judgment j2
            JOIN event_ci ec3 ON j2.event_id = ec3.id
            JOIN truth_event te3 ON ec3.created_by = te3.id
            JOIN participants p3 ON te3.participant_id = p3.id
            WHERE ec3.id = ec.id
        )
        ELSE (
            SUM(
                CASE 
                    WHEN j.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                    WHEN j.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                    ELSE 0.0
                END
            ) / COUNT(j.id) +
            SUM(
                CASE 
                    WHEN i.value = 1 THEN 1.0 * COALESCE(p.reputation_score, 0.5)
                    WHEN i.value = 0 THEN -1.0 * COALESCE(p.reputation_score, 0.5)
                    ELSE 0.0
                END
            ) / COUNT(i.id)
        ) / 2.0
    END as combined_consensus,
    -- Confidence in the consensus based on number of participants and agreement level
    CASE
        WHEN COUNT(j.id) + COUNT(i.id) = 0 THEN 0.0
        ELSE (
            CASE 
                WHEN AVG(ABS(COAALESCE(p.reputation_score, 0.5))) IS NOT NULL 
                THEN AVG(ABS(COALESCE(p.reputation_score, 0.5)))
                ELSE 0.0
            END
        ) * LEAST(1.0, (COUNT(j.id) + COUNT(i.id)) / 10.0)  -- Cap confidence at reasonable level
    END as consensus_confidence
FROM event_ci ec
LEFT JOIN truth_event te ON ec.created_by = te.id
LEFT JOIN participants p ON te.participant_id = p.id
LEFT JOIN judgment j ON ec.id = j.event_id
LEFT JOIN impact i ON ec.id = i.event_id
GROUP BY ec.id;
```
-- This view supports the consensus calculation logic by combining both judgment and impact assessments
-- weighted by participant reputation, providing a comprehensive consensus score that reflects both 
-- truth and consequence axes as described in the model.

-- Also reflects: consensus_ci.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
-- Also reflects: impact_metrics.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id
