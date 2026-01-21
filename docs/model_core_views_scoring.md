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

-- Alternative function to calculate impact score calculation view as referenced in main document
-- This view implements the calculation logic mentioned in the main document for impact_score field
```sql
CREATE VIEW impact_score_calculation_detailed AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate the impact score based on the formula mentioned in the main document
    -- impact_score field represents the cumulative impact assessment of the event at the local node level
    -- calculated based on impact records and participant reputation
    CASE
        WHEN (SELECT COUNT(*) FROM impact WHERE event_id = te.id) = 0 THEN 0.0
        ELSE (
            SELECT
                SUM(impact_value_i * participant_reputation_i) / N
            FROM (
                SELECT
                    CASE
                        WHEN i.value = 1 THEN 1.0  -- positive impact contributes positively
                        WHEN i.value = 0 THEN 0.0  -- negative impact contributes negatively
                        ELSE NULL  -- undefined impact
                    END as impact_value_i,
                    COALESCE(pr.reputation_score, 0.5) as participant_reputation_i,
                    (SELECT COUNT(*) FROM impact WHERE event_id = te.id) as N
                FROM impact i
                JOIN truth_event te_imp ON i.event_id = te_imp.id
                JOIN participants pr ON te_imp.participant_id = pr.public_key
                WHERE i.event_id = te.id
            )
        )
    END as calculated_impact_score
FROM truth_event te;
```

-- Function to calculate judgment score calculation view as referenced in main document
-- This view implements the calculation logic mentioned in the main document for judgment_score field
```sql
CREATE VIEW judgment_score_calculation_detailed AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate the judgment score based on the formula mentioned in the main document
    -- judgment_score field represents the cumulative truth assessment of the event at the local node level
    -- calculated based on judgment records, confidence levels and participant reputation
    CASE
        WHEN (SELECT COUNT(*) FROM judgment j
              JOIN event_ci ec ON j.event_id = ec.id
              WHERE ec.created_by = te.id) = 0 THEN NULL
        ELSE (
            SELECT
                SUM(judgment_value_i * confidence_level_i * participant_reputation_i) / N
            FROM (
                SELECT
                    CASE
                        WHEN j.assessment = 'true' THEN 1.0   -- true judgments contribute positively
                        WHEN j.assessment = 'false' THEN -1.0 -- false judgments contribute negatively
                        ELSE 0.0  -- other values contribute neutral
                    END as judgment_value_i,
                    COALESCE(j.confidence_level, 0.5) as confidence_level_i,
                    COALESCE(pr.reputation_score, 0.5) as participant_reputation_i,
                    (SELECT COUNT(*) FROM judgment j2
                     JOIN event_ci ec2 ON j2.event_id = ec2.id
                     WHERE ec2.created_by = te.id) as N
                FROM judgment j
                JOIN event_ci ec ON j.event_id = ec.id
                JOIN truth_event te_j ON ec.created_by = te_j.id
                JOIN participants pr ON te_j.participant_id = pr.public_key
                WHERE ec.created_by = te.id
            )
        )
    END as calculated_judgment_score
FROM truth_event te;
```