-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Trust Propagation Through the Network Based on Participant Relationships  

-- Calculates trust propagation through the network based on participant relationships and validation accuracy
-- Implements the trust propagation algorithm described in section 5
-- Links: participants.id → truth_event.participant_id → event_links.source_impact_id → truth_event.id → participants.id (for trust propagation)
```sql
CREATE VIEW participant_trust_propagation AS
SELECT
    p.id as participant_id,
    p.public_key,
    p.reputation_score as direct_reputation,
    -- Calculate propagated trust from other participants who trust this participant
    COALESCE(
        (SELECT AVG(p2.reputation_score)
         FROM participants p2
         JOIN truth_event te ON p2.id = te.participant_id
         JOIN event_links el ON te.id = el.source_impact_id
         JOIN truth_event te2 ON el.target_impact_id = te2.id
         WHERE te2.participant_id = p.id
         AND el.relation_type = 'equal'),  -- Assuming 'equal' relation implies trust
        p.reputation_score  -- Default to direct reputation if no trust links
    ) as propagated_reputation_from_others,
    -- Calculate propagated trust to other participants this participant trusts
    COALESCE(
        (SELECT AVG(p2.reputation_score)
         FROM participants p2
         JOIN truth_event te ON p2.id = te.participant_id
         JOIN event_links el ON p.id = te.participant_id
         JOIN truth_event te2 ON el.target_impact_id = te2.id
         WHERE el.relation_type = 'equal'),  -- Assuming 'equal' relation implies trust
        p.reputation_score  -- Default to direct reputation if no trust links
    ) as propagated_reputation_to_others,
    -- Combined trust score with propagation factors
    CASE
        WHEN p.reputation_score IS NOT NULL AND 
             (SELECT AVG(p2.reputation_score)
              FROM participants p2
              JOIN truth_event te ON p2.id = te.participant_id
              JOIN event_links el ON te.id = el.source_impact_id
              JOIN truth_event te2 ON el.target_impact_id = te2.id
              WHERE te2.participant_id = p.id
              AND el.relation_type = 'equal') IS NOT NULL
        THEN (p.reputation_score + 
              (SELECT AVG(p2.reputation_score)
               FROM participants p2
               JOIN truth_event te ON p2.id = te.participant_id
               JOIN event_links el ON te.id = el.source_impact_id
               JOIN truth_event te2 ON el.target_impact_id = te2.id
               WHERE te2.participant_id = p.id
               AND el.relation_type = 'equal')) / 2.0
        ELSE p.reputation_score
    END as combined_trust_score,
    -- Count of trust relationships
    (SELECT COUNT(*)
     FROM participants p2
     JOIN truth_event te ON p2.id = te.participant_id
     JOIN event_links el ON te.id = el.source_impact_id
     JOIN truth_event te2 ON el.target_impact_id = te2.id
     WHERE te2.participant_id = p.id
     AND el.relation_type = 'equal') as incoming_trust_connections,
    (SELECT COUNT(*)
     FROM participants p2
     JOIN truth_event te ON p2.id = te.participant_id
     JOIN event_links el ON p.id = te.participant_id
     JOIN truth_event te2 ON el.target_impact_id = te2.id
     WHERE el.relation_type = 'equal') as outgoing_trust_connections
FROM participants p;
```

-- Alternative view for calculating trust propagation with more sophisticated weighting
-- This view implements a more nuanced trust propagation algorithm
```sql
CREATE VIEW participant_trust_propagation_advanced AS
SELECT
    ptp.participant_id,
    ptp.public_key,
    ptp.direct_reputation,
    ptp.propagated_reputation_from_others,
    ptp.propagated_reputation_to_others,
    ptp.combined_trust_score,
    -- Calculate trust propagation with damping factor (similar to PageRank)
    (0.15 * ptp.direct_reputation) + -- 15% direct reputation
    (0.85 * ptp.propagated_reputation_from_others) as damped_trust_score,  -- 85% propagated trust
    -- Trust centrality measure
    (ptp.incoming_trust_connections + 1.0) / (ptp.outgoing_trust_connections + 1.0) as trust_centrality_ratio,
    -- Normalized trust propagation score
    CASE
        WHEN (ptp.incoming_trust_connections + ptp.outgoing_trust_connections) > 0
        THEN (ptp.combined_trust_score * (ptp.incoming_trust_connections + ptp.outgoing_trust_connections)) / 
             (ptp.incoming_trust_connections + ptp.outgoing_trust_connections + 1.0)
        ELSE ptp.combined_trust_score
    END as normalized_propagated_trust
FROM participant_trust_propagation ptp;
```

-- View for calculating network-level trust propagation metrics
-- This view provides aggregate metrics for trust propagation in the network
```sql
CREATE VIEW network_trust_propagation_metrics AS
SELECT
    COUNT(*) as total_participants,
    AVG(combined_trust_score) as average_network_trust,
    STDDEV(combined_trust_score) as trust_std_deviation,
    MIN(combined_trust_score) as min_trust_score,
    MAX(combined_trust_score) as max_trust_score,
    -- Calculate network trust cohesion (how much trust scores vary)
    1.0 - (STDDEV(combined_trust_score) / AVG(combined_trust_score + 0.0001)) as trust_cohesion,  -- Adding small value to prevent division by zero
    -- Percentage of participants with above-average trust
    (SELECT COUNT(*) * 100.0 / COUNT(*) 
     FROM participant_trust_propagation_advanced 
     WHERE combined_trust_score > (SELECT AVG(combined_trust_score) FROM participant_trust_propagation)) as pct_above_avg_trust,
    -- Average number of trust connections per participant
    AVG(incoming_trust_connections) as avg_incoming_trust_connections,
    AVG(outgoing_trust_connections) as avg_outgoing_trust_connections,
    -- Network density (ratio of actual connections to possible connections)
    (SELECT SUM(incoming_trust_connections) FROM participant_trust_propagation) * 1.0 / 
    (COUNT(*) * (COUNT(*) - 1)) as network_density
FROM participant_trust_propagation;
```

-- View for identifying trust hubs in the network
-- This view identifies participants with high trust centrality
```sql
CREATE VIEW trust_hubs_identification AS
SELECT
    ptpa.participant_id,
    ptpa.public_key,
    ptpa.combined_trust_score,
    ptpa.trust_centrality_ratio,
    ptpa.damped_trust_score,
    ptp.incoming_trust_connections,
    ptp.outgoing_trust_connections,
    -- Hub score calculation
    CASE
        WHEN ptp.incoming_trust_connections > (SELECT AVG(incoming_trust_connections) FROM participant_trust_propagation) * 1.5
             AND ptpa.combined_trust_score > (SELECT AVG(combined_trust_score) FROM participant_trust_propagation) * 1.2
        THEN 'HIGH_TRUST_HUB'
        WHEN ptp.incoming_trust_connections > (SELECT AVG(incoming_trust_connections) FROM participant_trust_propagation) * 1.2
             AND ptpa.combined_trust_score > (SELECT AVG(combined_trust_score) FROM participant_trust_propagation)
        THEN 'MODERATE_TRUST_HUB'
        ELSE 'STANDARD_PARTICIPANT'
    END as participant_type,
    -- Authority score (received trust) vs Hub score (given trust)
    ptp.incoming_trust_connections * ptpa.combined_trust_score as authority_score,
    ptp.outgoing_trust_connections * ptpa.combined_trust_score as hub_score
FROM participant_trust_propagation_advanced ptpa
JOIN participant_trust_propagation ptp ON ptpa.participant_id = ptp.participant_id
ORDER BY ptpa.damped_trust_score DESC, ptp.incoming_trust_connections DESC;