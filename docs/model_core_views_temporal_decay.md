# Temporal Decay Calculation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view implements temporal decay functions for trust weights and influence metrics over time, applying the formula w(t) = w₀ * e^(-λt) as described in sections 3.6 and 3.7.

## Purpose
The `temporal_decay_calculation` view applies decay functions to various system metrics over time, ensuring that older contributions have less influence while newer contributions carry more weight. This prevents stagnation and ensures the system adapts to changing conditions.

## SQL Implementation

```sql
-- View to calculate temporal decay for various system metrics
CREATE VIEW temporal_decay_calculation AS
SELECT 
    p.id AS participant_id,
    p.public_key,
    
    -- Current reputation score
    p.reputation_score AS current_reputation,
    
    -- Exponential decay applied to reputation based on last activity
    p.reputation_score * EXP(
        -(julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.01  -- λ = 0.01 per day
    ) AS decayed_reputation,
    
    -- Decay factor for the participant
    EXP(
        -(julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.01
    ) AS reputation_decay_factor,
    
    -- Calculate decayed judgment weights
    (
        SELECT 
            AVG(jw.weight * EXP(
                -(julianday('now') - julianday(jw.calculated_at, 'unixepoch')) * 0.02  -- λ = 0.02 per day for weights
            ))
        FROM judgment_weights jw
        WHERE jw.participant_id = p.id
    ) AS decayed_judgment_weight,
    
    -- Calculate decayed impact weights
    (
        SELECT 
            AVG(im.total_magnitude * EXP(
                -(julianday('now') - julianday(im.calculated_at, 'unixepoch')) * 0.02
            )) / 
            (SELECT COUNT(*) FROM impact WHERE participant_id = p.id)
        FROM impact_metrics im
        JOIN impact i ON im.id = i.impact_metrics
        WHERE i.participant_id = p.id
    ) AS decayed_impact_weight,
    
    -- Node trust score with temporal decay
    CASE 
        WHEN nr.node_id IS NOT NULL THEN
            nr.trust_score * EXP(
                -(julianday('now') - julianday(nr.last_updated, 'unixepoch')) * 0.005  -- λ = 0.005 per day for nodes
            )
        ELSE NULL
    END AS decayed_node_trust,
    
    -- Node performance metrics with decay
    CASE 
        WHEN np.pubkey IS NOT NULL THEN
            np.quality_index * EXP(
                -(julianday('now') - julianday(np.last_seen, 'unixepoch')) * 0.01
            )
        ELSE NULL
    END AS decayed_node_quality,
    
    -- Impact predictions with temporal decay
    (
        SELECT AVG(
            ip.probability * EXP(
                -(julianday('now') - julianday(ip.created_at, 'unixepoch')) * 0.03  -- λ = 0.03 per day for predictions
            )
        )
        FROM impact_predictions ip
        JOIN truth_event te ON ip.event_id = te.id
        WHERE te.participant_id = p.id
    ) AS decayed_prediction_accuracy,
    
    -- Event-based decay for truth_event scores
    te.id AS event_id,
    te.collective_score AS event_current_score,
    te.impact_score AS event_impact_score,
    te.judgment_score AS event_judgment_score,
    
    -- Event decayed scores
    te.collective_score * EXP(
        -(julianday('now') - julianday(te.created_at, 'unixepoch')) * 0.001  -- λ = 0.001 per day for events
    ) AS decayed_collective_score,
    
    te.impact_score * EXP(
        -(julianday('now') - julianday(te.created_at, 'unixepoch')) * 0.001
    ) AS decayed_impact_score,
    
    CASE 
        WHEN te.judgment_score IS NOT NULL THEN
            te.judgment_score * EXP(
                -(julianday('now') - julianday(te.created_at, 'unixepoch')) * 0.001
            )
        ELSE NULL
    END AS decayed_judgment_score,
    
    -- Small constants for decay calculations (using the small_constants_view)
    (SELECT small_constant_value FROM small_constants_view LIMIT 1) AS small_epsilon,
    
    -- Time constants for different decay rates
    0.01 AS participant_decay_rate,  -- Per day
    0.02 AS weight_decay_rate,       -- Per day
    0.005 AS node_decay_rate,        -- Per day
    0.001 AS event_decay_rate,       -- Per day
    0.03 AS prediction_decay_rate,   -- Per day
    
    p.created_at AS participant_created_at,
    p.last_activity AS participant_last_activity,
    CURRENT_TIMESTAMP AS calculation_timestamp

FROM participants p
LEFT JOIN node_ratings nr ON nr.node_id = p.public_key
LEFT JOIN node_performance np ON np.pubkey = p.public_key
LEFT JOIN truth_event te ON te.participant_id = p.id;

-- View for decay-adjusted participant rankings
CREATE VIEW decayed_participant_rankings AS
SELECT 
    participant_id,
    public_key,
    current_reputation,
    decayed_reputation,
    reputation_decay_factor,
    ROW_NUMBER() OVER (ORDER BY decayed_reputation DESC) AS decayed_rank,
    PERCENT_RANK() OVER (ORDER BY decayed_reputation) AS decayed_percentile
FROM temporal_decay_calculation
WHERE decayed_reputation IS NOT NULL
ORDER BY decayed_reputation DESC;

-- View for monitoring decay effects
CREATE VIEW decay_monitoring AS
SELECT 
    COUNT(*) AS total_participants_monitored,
    AVG(current_reputation) AS avg_current_reputation,
    AVG(decayed_reputation) AS avg_decayed_reputation,
    AVG(reputation_decay_factor) AS avg_decay_factor,
    MIN(reputation_decay_factor) AS min_decay_factor,
    MAX(reputation_decay_factor) AS max_decay_factor,
    AVG(julianday('now') - julianday(participant_last_activity, 'unixepoch')) AS avg_days_inactive
FROM temporal_decay_calculation;

-- View for calculating decayed weights for consensus calculations
CREATE VIEW decayed_weights_for_consensus AS
SELECT 
    j.participant_id,
    j.event_id,
    jw.weight AS original_weight,
    jw.weight * EXP(
        -(julianday('now') - julianday(jw.calculated_at, 'unixepoch')) * 0.02
    ) AS decayed_weight,
    j.confidence_level,
    j.assessment,
    p.reputation_score AS base_reputation
FROM judgment j
JOIN judgment_weights jw ON j.participant_id = jw.participant_id AND j.event_id = jw.event_id
JOIN participants p ON j.participant_id = p.id
WHERE jw.weight IS NOT NULL;

-- Function to calculate decay for any given time difference and decay rate
-- This is a conceptual representation as SQLite doesn't support custom functions easily
-- In practice, this would be implemented in the application layer or as a view
CREATE VIEW decay_formula_helper AS
SELECT 
    1.0 AS base_value,
    0.01 AS default_decay_rate,
    30 AS default_time_period_days,
    EXP(-0.01 * 30) AS decayed_value_after_30_days,
    EXP(-0.01 * 7) AS decayed_value_after_7_days,
    EXP(-0.01 * 1) AS decayed_value_after_1_day,
    CURRENT_TIMESTAMP AS calculated_at;
```

## Key Features

### Multi-Type Decay Application
The view applies decay functions to various types of system metrics:
- Participant reputation scores
- Judgment weights
- Impact weights
- Node trust scores
- Prediction accuracies
- Event scores

### Configurable Decay Rates
Different decay rates (λ values) are applied to different types of data based on their expected volatility and importance over time.

### Time-Based Calculations
The view calculates the time elapsed since the last activity or update and applies the exponential decay function accordingly.

### Monitoring Capabilities
Includes views for monitoring the overall effect of decay on the system and identifying participants or nodes that may need attention due to excessive decay.

### Consensus Integration
Provides decay-adjusted weights for use in consensus calculations, ensuring that recent contributions have more influence.

## Relationship to Model Core
This view implements the temporal decay mechanisms described in the model, where:
- w(t) = w₀ * e^(-λt) is the fundamental decay formula
- Older evidence weakens over time by multiplication with decay factors
- System ensures that influence naturally fades if not reinforced
- Decay prevents stagnation and allows the system to adapt to changing conditions

## Usage Examples

```sql
-- Get decayed reputation for a specific participant
SELECT * FROM temporal_decay_calculation WHERE participant_id = ?;

-- Get participants ranked by decayed reputation
SELECT * FROM decayed_participant_rankings LIMIT 10;

-- Monitor overall decay effects in the system
SELECT * FROM decay_monitoring;

-- Get decayed weights for consensus calculations
SELECT * FROM decayed_weights_for_consensus WHERE event_id = ?;

-- Find participants with very low decayed reputation
SELECT * FROM temporal_decay_calculation 
WHERE decayed_reputation < 0.1 AND current_reputation > 0.5;
```

## Integration with Other Components
- Works with `participants` table to decay reputation scores
- Integrates with `judgment_weights` for decayed weight calculations
- Connects to `node_ratings` and `node_performance` for node decay
- Supports `impact_predictions` by decaying prediction accuracies
- Used in `consensus_ci` calculations for time-weighted consensus

## Notes
- The decay rates (λ values) are configurable and can be adjusted based on system requirements
- The view should be refreshed regularly to reflect current time-based decay
- Small epsilon values are used to prevent division by zero in calculations
- Different decay rates apply to different types of data based on their temporal relevance