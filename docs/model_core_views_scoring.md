-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Impact and Judgment Score Calculations  

-- Function to calculate impact_score based on impact records  
-- The impact_score field represents the cumulative impact assessment of the event at the local node level  
-- It is calculated based on the impact records stored in the impact table that are associated with this event  
-- The calculation algorithm aggregates the individual impact values taking into account their types, timestamps, and the reputation of the participants who made the impact assessments  
```sql
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
```
-- Function to calculate judgment_score based on judgment records  
-- The judgment_score field represents the cumulative truth assessment of the event at the local node level  
-- It is calculated based on the judgment records stored in the judgment table that are associated with the corresponding event in the event_ci table  
-- The calculation algorithm aggregates the individual judgments taking into account their confidence levels, assessment types, and the reputation of the participants who made the judgments  
```sql
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
```
-- Function to recalculate all impact scores (for maintenance)  
```sql
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
```
-- Function to recalculate all judgment scores (for maintenance)  
```sql
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
```