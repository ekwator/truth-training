# Participant Reputation Calculation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view calculates participant reputation scores based on historical accuracy of their judgments and impact predictions, implementing the reputation update algorithm described in section 2.1.

## Purpose
The `participant_reputation_calculation` view calculates dynamic reputation scores for participants based on their historical accuracy in making judgments and impact predictions. This ensures that participants who consistently make accurate assessments have higher influence in the collective intelligence system.

## SQL Implementation

```sql
-- View to calculate participant reputation scores based on historical accuracy
CREATE VIEW participant_reputation_calculation AS
SELECT 
    p.id AS participant_id,
    p.public_key,
    
    -- Overall reputation score based on accuracy
    CASE 
        WHEN (p.total_judgment + p.total_impact) > 0 THEN
            (p.accurate_judgment + p.accurate_impact) * 1.0 / (p.total_judgment + p.total_impact)
        ELSE 0.5 -- Neutral score for new participants
    END AS reputation_score,
    
    -- Judgment-specific accuracy
    CASE 
        WHEN p.total_judgment > 0 THEN
            p.accurate_judgment * 1.0 / p.total_judgment
        ELSE 0.5
    END AS judgment_accuracy,
    
    -- Impact-specific accuracy
    CASE 
        WHEN p.total_impact > 0 THEN
            p.accurate_impact * 1.0 / p.total_impact
        ELSE 0.5
    END AS impact_accuracy,
    
    -- Prediction accuracy based on impact_predictions
    (
        SELECT 
            CASE 
                WHEN COUNT(*) > 0 THEN
                    SUM(CASE WHEN ABS(expected_strength - actual_outcome) <= 0.2 * expected_strength THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
                ELSE 0.5
            END
        FROM impact_predictions ip
        JOIN truth_event te ON ip.event_id = (
            SELECT id FROM event_ci WHERE created_by = te.id
        )
        WHERE te.participant_id = p.id
    ) AS prediction_accuracy,
    
    -- Weighted reputation considering different aspects
    (
        SELECT 
            (COALESCE(pr.judgment_accuracy, 0.5) * 0.4 +
             COALESCE(pr.impact_accuracy, 0.5) * 0.3 +
             COALESCE(pr.prediction_accuracy, 0.5) * 0.3)
        FROM (SELECT 
                 CASE WHEN p.total_judgment > 0 THEN p.accurate_judgment * 1.0 / p.total_judgment ELSE 0.5 END AS judgment_accuracy,
                 CASE WHEN p.total_impact > 0 THEN p.accurate_impact * 1.0 / p.total_impact ELSE 0.5 END AS impact_accuracy,
                 (SELECT 
                      CASE 
                          WHEN COUNT(*) > 0 THEN
                              SUM(CASE WHEN ABS(expected_strength - actual_outcome) <= 0.2 * expected_strength THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
                          ELSE 0.5
                      END
                  FROM impact_predictions ip
                  JOIN truth_event te ON ip.event_id = te.id
                  WHERE te.participant_id = p.id) AS prediction_accuracy
             ) pr
    ) AS weighted_reputation,
    
    -- Recency-weighted reputation (more recent contributions have higher weight)
    (
        SELECT 
            CASE 
                WHEN SUM(weight) > 0 THEN
                    SUM(outcome * weight) / SUM(weight)
                ELSE 0.5
            END
        FROM (
            SELECT 
                CASE WHEN j.accuracy_confirmed = 1 THEN 1.0 ELSE 0.0 END AS outcome,
                1.0 / (1.0 + (julianday('now') - julianday(jh.updated_at))) AS weight
            FROM judgment j
            JOIN participants p2 ON j.participant_id = p2.id
            JOIN reputation_history jh ON p2.reputation_history = jh.id
            WHERE j.participant_id = p.id
            
            UNION ALL
            
            SELECT 
                CASE WHEN i.accuracy_confirmed = 1 THEN 1.0 ELSE 0.0 END AS outcome,
                1.0 / (1.0 + (julianday('now') - julianday(ih.updated_at))) AS weight
            FROM impact i
            JOIN participants p3 ON i.participant_id = p3.id
            JOIN reputation_history ih ON p3.reputation_history = ih.id
            WHERE i.participant_id = p.id
        )
    ) AS recency_weighted_reputation,
    
    p.total_judgment,
    p.accurate_judgment,
    p.total_impact,
    p.accurate_impact,
    p.created_at,
    p.last_activity
    
FROM participants p;

-- View to track reputation changes over time
CREATE VIEW participant_reputation_history AS
SELECT 
    pr.participant_id,
    pr.public_key,
    rh.old_reputation,
    rh.new_reputation,
    rh.change_reason,
    rh.updated_at,
    pr.reputation_score AS current_reputation
FROM participant_reputation_calculation pr
JOIN reputation_history rh ON pr.participant_id = (
    SELECT participants.id
    FROM participants
    WHERE participants.reputation_history = rh.id
);

-- View for reputation-based participant rankings
CREATE VIEW participant_reputation_rankings AS
SELECT 
    participant_id,
    public_key,
    reputation_score,
    judgment_accuracy,
    impact_accuracy,
    prediction_accuracy,
    ROW_NUMBER() OVER (ORDER BY reputation_score DESC) AS reputation_rank,
    PERCENT_RANK() OVER (ORDER BY reputation_score) AS reputation_percentile
FROM participant_reputation_calculation
ORDER BY reputation_score DESC;
```

## Key Features

### Multi-Dimensional Accuracy Assessment
The view calculates reputation based on multiple dimensions:
- Judgment accuracy: How often the participant's truth assessments are correct
- Impact accuracy: How often the participant's consequence predictions are correct
- Prediction accuracy: How well the participant predicts future outcomes

### Weighted Reputation Calculation
Different aspects of participation are weighted differently to provide a balanced reputation score that reflects overall contribution quality.

### Recency-Weighted Reputation
More recent contributions are given higher weight, allowing the system to adapt to changes in participant behavior over time.

### Historical Tracking
The view includes mechanisms to track how reputation changes over time, which is important for understanding participant behavior evolution.

## Relationship to Model Core
This view implements the participant reputation model described in the core documentation, where:
- R(u) = A(u) / T(u) (reputation equals accurate assessments divided by total assessments)
- Reputation is updated asynchronously after accuracy confirmation
- Higher reputation participants have more weight in consensus calculations

## Usage Examples

```sql
-- Get current reputation for a specific participant
SELECT * FROM participant_reputation_calculation WHERE participant_id = ?;

-- Get top-ranked participants by reputation
SELECT * FROM participant_reputation_rankings LIMIT 10;

-- Track reputation changes over time for a participant
SELECT * FROM participant_reputation_history 
WHERE participant_id = ? 
ORDER BY updated_at DESC;

-- Find participants with high prediction accuracy
SELECT * FROM participant_reputation_calculation 
WHERE prediction_accuracy > 0.7;
```

## Integration with Other Components
- Used by `judgment_weights` to determine participant influence weights
- Feeds into `consensus_ci` calculations for weighted consensus
- Supports `impact_predictions` by providing participant reliability scores
- Used in `group_ratings` for group-level reputation calculations

## Notes
- The view is designed to be refreshed periodically to reflect new assessment data
- Reputation scores are bounded between 0 and 1 to maintain consistency
- The system handles new participants with neutral reputation until sufficient data is available