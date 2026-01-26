-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Temporal Decay Functions for Trust Weights and Influence Metrics Over Time  

-- Implements temporal decay functions for trust weights and influence metrics over time as described in sections 3.6 and 3.7
-- Applies the formula w(t) = w₀ * e^(-λt) to calculate how weights decay over time
```sql
CREATE VIEW temporal_decay_calculation AS
SELECT
    p.id as participant_id,
    p.public_key,
    p.reputation_score as current_reputation,
    p.last_activity,
    -- Calculate days since last activity
    (julianday('now') - julianday(p.last_activity, 'unixepoch')) as days_since_activity,
    -- Apply decay function based on time since last activity (assuming λ = 0.1 per day)
    p.reputation_score * EXP(- (julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.1) as decayed_reputation,
    -- Small constants for threshold calculations
    (CASE
        WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
        THEN 0.000001
        ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
    END) as small_constant_epsilon
FROM participants p;
```

-- View for calculating temporal decay for node trust ratings
-- This view implements the decay function for node trust scores as mentioned in section 3.6
```sql
CREATE VIEW node_temporal_decay_calculation AS
SELECT
    nr.node_id,
    nr.trust_score as current_trust_score,
    nr.last_updated,
    -- Calculate days since last update
    (julianday('now') - julianday(nr.last_updated, 'unixepoch')) as days_since_update,
    -- Apply decay function based on time since last update (assuming λ = 0.05 per day for nodes)
    nr.trust_score * EXP(- (julianday('now') - julianday(nr.last_updated, 'unixepoch')) * 0.05) as decayed_trust_score,
    -- Calculate propagation priority with temporal decay
    CASE
        WHEN (nr.events_true + nr.events_false) > 0 THEN
            (nr.events_true - nr.events_false) * 1.0 / (nr.events_true + nr.events_false) * 
            EXP(- (julianday('now') - julianday(nr.last_updated, 'unixepoch')) * 0.05)
        ELSE 0.0
    END as decayed_propagation_priority
FROM node_ratings nr;
```

-- View for calculating temporal decay for influence metrics
-- This view implements the decay function for various influence metrics
```sql
CREATE VIEW influence_temporal_decay_calculation AS
SELECT
    p.id as participant_id,
    p.public_key,
    -- Apply decay to reputation score
    p.reputation_score * EXP(- (julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.1) as decayed_reputation,
    -- Apply decay to judgment weights
    jw.weight as current_weight,
    jw.weight * EXP(- (julianday('now') - julianday(jw.calculated_at, 'unixepoch')) * 0.1) as decayed_weight,
    -- Apply decay to impact metrics
    im.total_magnitude as current_impact_magnitude,
    im.total_magnitude * EXP(- (julianday('now') - julianday(im.calculated_at, 'unixepoch')) * 0.1) as decayed_impact_magnitude,
    -- Calculate time factor
    EXP(- (julianday('now') - julianday(COALESCE(p.last_activity, jw.calculated_at, im.calculated_at), 'unixepoch')) * 0.1) as time_decay_factor,
    -- Timestamp of calculation
    (SELECT strftime('%s', 'now')) as calculated_at
FROM participants p
LEFT JOIN judgment_weights jw ON p.id = jw.participant_id
LEFT JOIN impact_metrics im ON im.event_id IN (
    SELECT ec.id 
    FROM event_ci ec 
    JOIN truth_event te ON ec.created_by = te.id 
    WHERE te.participant_id = p.id
)
WHERE p.last_activity IS NOT NULL;
```

-- View for calculating decay-adjusted participant scores
-- This view provides the final decayed scores for participants based on their activity
```sql
CREATE VIEW participant_decay_adjusted_scores AS
SELECT
    p.id as participant_id,
    p.public_key,
    p.reputation_score as original_reputation,
    p.last_activity,
    -- Calculate decayed reputation score
    CASE
        WHEN p.last_activity IS NOT NULL THEN
            p.reputation_score * EXP(- (julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.1)
        ELSE p.reputation_score * 0.1  -- Strong decay if no activity recorded
    END as decayed_reputation_score,
    -- Calculate decay multiplier
    CASE
        WHEN p.last_activity IS NOT NULL THEN
            EXP(- (julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.1)
        ELSE 0.1
    END as decay_multiplier,
    -- Flag for inactive participants (threshold: 30 days)
    CASE
        WHEN p.last_activity IS NOT NULL AND 
             (julianday('now') - julianday(p.last_activity, 'unixepoch')) > 30 THEN 1
        ELSE 0
    END as is_inactive
FROM participants p;