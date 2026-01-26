-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Participant Reputation Calculation Based on Historical Accuracy  

-- Computes participant reputation scores based on historical accuracy of their judgments and impact predictions
-- Implements the reputation update algorithm described in section 2.1
-- Links: participants.id → truth_event.participant_id, participants.id → impact.participant_id, participants.id → judgment.participant_id
```sql
CREATE VIEW participant_reputation_calculation AS
SELECT
    p.id as participant_id,
    p.public_key,
    -- Calculate reputation based on impact accuracy
    CASE
        WHEN p.total_impact > 0
        THEN CAST(p.accurate_impact AS REAL) / p.total_impact
        ELSE 0.5  -- default neutral reputation
    END as impact_reputation,
    -- Calculate reputation based on judgment accuracy
    CASE
        WHEN p.total_judgment > 0
        THEN CAST(p.accurate_judgment AS REAL) / p.total_judgment
        ELSE 0.5  -- default neutral reputation
    END as judgment_reputation,
    -- Combined reputation score
    CASE
        WHEN p.total_impact > 0 AND p.total_judgment > 0
        THEN (CAST(p.accurate_impact AS REAL) / p.total_impact + CAST(p.accurate_judgment AS REAL) / p.total_judgment) / 2.0
        WHEN p.total_impact > 0
        THEN CAST(p.accurate_impact AS REAL) / p.total_impact
        WHEN p.total_judgment > 0
        THEN CAST(p.accurate_judgment AS REAL) / p.total_judgment
        ELSE 0.5  -- default neutral reputation
    END as combined_reputation,
    -- Historical accuracy metrics
    p.total_impact,
    p.accurate_impact,
    p.total_judgment,
    p.accurate_judgment,
    p.created_at,
    p.last_activity
FROM participants p;
```

-- Alternative view that includes prediction accuracy in reputation calculation
-- This view incorporates the impact prediction accuracy as described in section 2.6.1
```sql
CREATE VIEW participant_prediction_reputation AS
SELECT
    p.id as participant_id,
    p.public_key,
    -- Weight predictions based on horizon (earlier predictions have higher weight)
    CASE
        WHEN p.total_impact > 0
        THEN (
            SELECT AVG(
                ip.horizon * (CASE 
                    WHEN ABS(ip.expected_strength - te.collective_score) < 0.2 
                    THEN 1.0 
                    ELSE 0.0 
                END)
            )
            FROM impact_predictions ip
            JOIN event_ci ec ON ip.event_id = ec.id
            JOIN truth_event te ON ec.created_by = te.id
            WHERE te.participant_id = p.id
        )
        ELSE 0.5
    END as prediction_accuracy_score,
    p.total_impact,
    p.accurate_impact,
    p.reputation_score
FROM participants p;
```

-- View for calculating reputation trends over time
-- This view tracks how participant reputation changes over time as described in section 2.1
```sql
CREATE VIEW participant_reputation_trend AS
SELECT
    p.id as participant_id,
    p.public_key,
    p.reputation_score as current_reputation,
    rh.old_reputation,
    rh.new_reputation,
    rh.change_reason,
    rh.updated_at as change_timestamp,
    -- Calculate reputation change
    (rh.new_reputation - rh.old_reputation) as reputation_change
FROM participants p
LEFT JOIN reputation_history rh ON p.reputation_history = rh.id;