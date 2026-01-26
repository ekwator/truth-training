-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Impact and Judgment Score Calculations  

-- Function to calculate impact_score based on impact records
-- The impact_score field represents the cumulative impact assessment of the event at the local node level
-- It is calculated based on the impact records stored in the impact table that are associated with this event
-- The calculation algorithm aggregates the individual impact values taking into account their types, timestamps, and the reputation of the participants who made the impact assessments
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by impact.participant_id
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
LEFT JOIN event_ci ec ON te.id = ec.created_by
LEFT JOIN impact i ON ec.id = i.event_id  -- Changed: impact connects through event_ci
JOIN participants p ON te.participant_id = p.id
WHERE p.id = 1  -- Filter for local participant
GROUP BY te.id;
-- Updated to reflect: impact.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to calculate judgment_score based on judgment records
-- The judgment_score field represents the cumulative truth assessment of the event at the local node level
-- It is calculated based on the judgment records stored in the judgment table that are associated with the corresponding event in the event_ci table
-- The calculation algorithm aggregates the individual judgments taking into account their confidence levels, assessment types, and the reputation of the participants who made the judgments
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by judgment.participant_id
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
JOIN truth_event te_j ON ec.created_by = te_j.id
JOIN participants p ON te_j.participant_id = p.id
WHERE p.id = 1  -- Filter for local participant
GROUP BY te.id;
-- Updated to reflect: judgment.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to recalculate all impact scores (for maintenance)
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by impact.participant_id
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
LEFT JOIN event_ci ec ON te.id = ec.created_by
LEFT JOIN impact i ON ec.id = i.event_id  -- Changed: impact connects through event_ci
JOIN participants p ON te.participant_id = p.id
WHERE p.id = 1  -- Filter for local participant
GROUP BY te.id;
-- Updated to reflect: impact.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to recalculate all judgment scores (for maintenance)
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by judgment.participant_id
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
JOIN truth_event te_j ON ec.created_by = te_j.id
JOIN participants p ON te_j.participant_id = p.id  -- Fixed: was p.public_key, now p.id
WHERE p.id = 1  -- Filter for local participant
GROUP BY te.id;
```

-- Alternative function to calculate impact score calculation view as referenced in main document
-- This view implements the calculation logic mentioned in the main document for impact_score field
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by impact.participant_id
```sql
CREATE VIEW impact_score_calculation_detailed AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate the impact score based on the formula mentioned in the main document
    -- impact_score field represents the cumulative impact assessment of the event at the local node level
    -- calculated based on impact records and participant reputation
    CASE
        WHEN (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id JOIN truth_event te2 ON ec.created_by = te2.id JOIN participants pr ON te2.participant_id = pr.id WHERE ec.created_by = te.id AND pr.id = 1) = 0 THEN 0.0
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
                    (SELECT COUNT(*) FROM impact i2 JOIN event_ci ec2 ON i2.event_id = ec2.id JOIN truth_event te3 ON ec2.created_by = te3.id JOIN participants pr2 ON te3.participant_id = pr2.id WHERE ec2.created_by = te.id AND pr2.id = 1) as N
                FROM impact i
                JOIN event_ci ec ON i.event_id = ec.id
                JOIN truth_event te_imp ON ec.created_by = te_imp.id
                JOIN participants pr ON te_imp.participant_id = pr.id
                WHERE ec.created_by = te.id
                  AND pr.id = 1  -- Filter for local participant
            )
        )
    END as calculated_impact_score
FROM truth_event te;
-- Updated to reflect: impact.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to calculate judgment score calculation view as referenced in main document
-- This view implements the calculation logic mentioned in the main document for judgment_score field
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by judgment.participant_id
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
              JOIN truth_event te_j ON ec.created_by = te_j.id
              JOIN participants pr ON te_j.participant_id = pr.id
              WHERE ec.created_by = te.id
                AND pr.id = 1) = 0 THEN NULL
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
                     JOIN truth_event te_j2 ON ec2.created_by = te_j2.id
                     JOIN participants pr2 ON te_j2.participant_id = pr2.id
                     WHERE ec2.created_by = te.id
                       AND pr2.id = 1) as N
                FROM judgment j
                JOIN event_ci ec ON j.event_id = ec.id
                JOIN truth_event te_j ON ec.created_by = te_j.id
                JOIN participants pr ON te_j.participant_id = pr.id
                WHERE ec.created_by = te.id
                  AND pr.id = 1  -- Filter for local participant
            )
        )
    END as calculated_judgment_score
FROM truth_event te;
-- Updated to reflect: judgment.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to calculate impact score calculation detailed as referenced in main document
-- This view implements the calculation logic mentioned in the main document for impact_score field
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by impact.participant_id
```sql
CREATE VIEW impact_score_calculation_detailed AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate the impact score based on the formula mentioned in the main document
    -- impact_score field represents the cumulative impact assessment of the event at the local node level
    -- calculated based on impact records and participant reputation
    CASE
        WHEN (SELECT COUNT(*) FROM impact i JOIN event_ci ec ON i.event_id = ec.id JOIN truth_event te2 ON ec.created_by = te2.id JOIN participants pr ON te2.participant_id = pr.id WHERE ec.created_by = te.id AND pr.id = 1) = 0 THEN 0.0
        ELSE (
            SELECT
                SUM(impact_value_i * participant_reputation_i) / N
            FROM (
                SELECT
                    CASE
                        WHEN i.value = 1 THEN 1.0  -- positive impact contributes positively
                        WHEN i.value = 0 THEN -1.0  -- negative impact contributes negatively
                        ELSE NULL  -- undefined impact
                    END as impact_value_i,
                    COALESCE(pr.reputation_score, 0.5) as participant_reputation_i,
                    (SELECT COUNT(*) FROM impact i2 JOIN event_ci ec2 ON i2.event_id = ec2.id JOIN truth_event te3 ON ec2.created_by = te3.id JOIN participants pr2 ON te3.participant_id = pr2.id WHERE ec2.created_by = te.id AND pr2.id = 1) as N
                FROM impact i
                JOIN event_ci ec ON i.event_id = ec.id
                JOIN truth_event te_imp ON ec.created_by = te_imp.id
                JOIN participants pr ON te_imp.participant_id = pr.id
                WHERE ec.created_by = te.id
                  AND pr.id = 1  -- Filter for local participant
            )
        )
    END as calculated_impact_score
FROM truth_event te;
-- Updated to reflect: impact.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Function to calculate judgment score calculation as referenced in main document
-- This view implements the calculation logic mentioned in the main document for judgment_score field
-- For local user (participants.id = 1), filters by participant_id = 1
-- For global/group calculations, groups by judgment.participant_id
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
              JOIN truth_event te_j ON ec.created_by = te_j.id
              JOIN participants pr ON te_j.participant_id = pr.id
              WHERE ec.created_by = te.id
                AND pr.id = 1) = 0 THEN NULL
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
                     JOIN truth_event te_j2 ON ec2.created_by = te_j2.id
                     JOIN participants pr2 ON te_j2.participant_id = pr2.id
                     WHERE ec2.created_by = te.id
                       AND pr2.id = 1) as N
                FROM judgment j
                JOIN event_ci ec ON j.event_id = ec.id
                JOIN truth_event te_j ON ec.created_by = te_j.id
                JOIN participants pr ON te_j.participant_id = pr.id
                WHERE ec.created_by = te.id
                  AND pr.id = 1  -- Filter for local participant
            )
        )
    END as calculated_judgment_score
FROM truth_event te;
-- Updated to reflect: judgment.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```

-- Alternative function to calculate impact score with additional analytical capabilities for impact assessment
-- Provides additional analytical capabilities for impact assessment as mentioned in section 2.6.1
```sql
CREATE VIEW impact_score_calculation_extended AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate impact score with additional analytical components
    (SELECT COUNT(*) FROM impact i
     JOIN event_ci ec ON i.event_id = ec.id
     JOIN truth_event te2 ON ec.created_by = te2.id
     JOIN participants p ON te2.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as impact_count,
    (SELECT AVG(CASE 
                  WHEN i.value = 1 THEN COALESCE(p.reputation_score, 0.5)
                  WHEN i.value = 0 THEN -COALESCE(p.reputation_score, 0.5)
                  ELSE 0
                END)
     FROM impact i
     JOIN event_ci ec ON i.event_id = ec.id
     JOIN truth_event te2 ON ec.created_by = te2.id
     JOIN participants p ON te2.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as weighted_average_impact,
    (SELECT MAX(i.created_at)
     FROM impact i
     JOIN event_ci ec ON i.event_id = ec.id
     JOIN truth_event te2 ON ec.created_by = te2.id
     JOIN participants p ON te2.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as last_impact_time,
    CASE
        WHEN (SELECT COUNT(*) FROM impact i
              JOIN event_ci ec ON i.event_id = ec.id
              JOIN truth_event te2 ON ec.created_by = te2.id
              JOIN participants p ON te2.participant_id = p.id
              WHERE ec.created_by = te.id
              AND p.id = 1) = 0 THEN 0.0
        ELSE (
            SELECT AVG(CASE 
                         WHEN i.value = 1 THEN COALESCE(p.reputation_score, 0.5)
                         WHEN i.value = 0 THEN -COALESCE(p.reputation_score, 0.5)
                         ELSE 0
                       END)
            FROM impact i
            JOIN event_ci ec ON i.event_id = ec.id
            JOIN truth_event te2 ON ec.created_by = te2.id
            JOIN participants p ON te2.participant_id = p.id
            WHERE ec.created_by = te.id
            AND p.id = 1  -- Filter for local participant
        )
    END as calculated_extended_impact_score
FROM truth_event te;
```

-- Detailed function to calculate judgment score with additional analytical capabilities for judgment assessment
-- Provides additional analytical capabilities for judgment assessment as mentioned in section 2.6.2
```sql
CREATE VIEW judgment_score_calculation_extended AS
SELECT
    te.id as event_id,
    te.description,
    -- Calculate judgment score with additional analytical components
    (SELECT COUNT(*) FROM judgment j
     JOIN event_ci ec ON j.event_id = ec.id
     JOIN truth_event te_j ON ec.created_by = te_j.id
     JOIN participants p ON te_j.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as judgment_count,
    (SELECT AVG(CASE 
                  WHEN j.assessment = 'true' THEN j.confidence_level * COALESCE(p.reputation_score, 0.5)
                  WHEN j.assessment = 'false' THEN -j.confidence_level * COALESCE(p.reputation_score, 0.5)
                  ELSE 0
                END)
     FROM judgment j
     JOIN event_ci ec ON j.event_id = ec.id
     JOIN truth_event te_j ON ec.created_by = te_j.id
     JOIN participants p ON te_j.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as weighted_average_judgment,
    (SELECT MAX(j.created_at)
     FROM judgment j
     JOIN event_ci ec ON j.event_id = ec.id
     JOIN truth_event te_j ON ec.created_by = te_j.id
     JOIN participants p ON te_j.participant_id = p.id
     WHERE ec.created_by = te.id
     AND p.id = 1  -- Filter for local participant
    ) as last_judgment_time,
    CASE
        WHEN (SELECT COUNT(*) FROM judgment j
              JOIN event_ci ec ON j.event_id = ec.id
              JOIN truth_event te_j ON ec.created_by = te_j.id
              JOIN participants p ON te_j.participant_id = p.id
              WHERE ec.created_by = te.id
              AND p.id = 1) = 0 THEN NULL
        ELSE (
            SELECT AVG(CASE 
                         WHEN j.assessment = 'true' THEN j.confidence_level * COALESCE(p.reputation_score, 0.5)
                         WHEN j.assessment = 'false' THEN -j.confidence_level * COALESCE(p.reputation_score, 0.5)
                         ELSE 0
                       END)
            FROM judgment j
            JOIN event_ci ec ON j.event_id = ec.id
            JOIN truth_event te_j ON ec.created_by = te_j.id
            JOIN participants p ON te_j.participant_id = p.id
            WHERE ec.created_by = te.id
            AND p.id = 1  -- Filter for local participant
        )
    END as calculated_extended_judgment_score
FROM truth_event te;
```