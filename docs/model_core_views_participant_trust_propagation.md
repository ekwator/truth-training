# Participant Trust Propagation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view calculates trust propagation through the network based on participant relationships and validation accuracy, implementing the trust propagation algorithm described in section 5.

## Purpose
The `participant_trust_propagation` view models how trust propagates through the network of participants based on their validation accuracy, relationship patterns, and the quality of their contributions. This ensures that reliable participants have greater influence while maintaining resistance to manipulation attempts.

## SQL Implementation

```sql
-- View to calculate trust propagation through the network based on participant relationships and validation accuracy
CREATE VIEW participant_trust_propagation AS
SELECT 
    p.id AS participant_id,
    p.public_key AS participant_public_key,
    p.reputation_score AS base_reputation,
    
    -- Direct trust from other participants (based on judgments and impacts)
    (
        SELECT AVG(jw.weight)
        FROM judgment_weights jw
        WHERE jw.participant_id = p.id
    ) AS direct_trust_weight,
    
    -- Trust from validation accuracy
    CASE 
        WHEN p.total_judgment + p.total_impact > 0 THEN
            (p.accurate_judgment + p.accurate_impact) * 1.0 / (p.total_judgment + p.total_impact)
        ELSE 0.5
    END AS validation_trust_score,
    
    -- Network-based trust propagation
    (
        SELECT AVG(neighbor_reputation * connection_strength)
        FROM (
            SELECT 
                p2.reputation_score AS neighbor_reputation,
                CASE 
                    WHEN j.event_id IS NOT NULL THEN 0.8  -- Strong connection through shared events
                    WHEN i.event_id IS NOT NULL THEN 0.6  -- Moderate connection through shared impacts
                    WHEN imp.event_id IS NOT NULL THEN 0.4 -- Weak connection through shared predictions
                    ELSE 0.1
                END AS connection_strength
            FROM participants p2
            LEFT JOIN judgment j ON p2.id = j.participant_id
            LEFT JOIN impact i ON p2.id = i.participant_id
            LEFT JOIN impact_predictions imp ON p2.id = (
                SELECT participant_id 
                FROM truth_event 
                WHERE id = (
                    SELECT created_by 
                    FROM event_ci 
                    WHERE id = imp.event_id
                )
            )
            WHERE j.participant_id = p.id OR i.participant_id = p.id OR imp.event_id = (
                SELECT id FROM event_ci WHERE created_by = (
                    SELECT id FROM truth_event WHERE participant_id = p.id
                )
            )
        ) connections
    ) AS propagated_trust_score,
    
    -- Temporal decay of trust (older contributions have less weight)
    p.reputation_score * EXP(
        -(julianday('now') - julianday(p.last_activity, 'unixepoch')) * 0.01
    ) AS decayed_trust_score,
    
    -- Trust based on consistency over time
    (
        SELECT AVG(reputation_change_stability)
        FROM (
            SELECT 
                ABS(rh.new_reputation - rh.old_reputation) AS reputation_change,
                1.0 - MIN(ABS(rh.new_reputation - rh.old_reputation), 1.0) AS stability_score,
                julianday('now') - julianday(rh.updated_at, 'unixepoch') AS days_ago
            FROM reputation_history rh
            WHERE rh.id = (
                SELECT reputation_history FROM participants WHERE id = p.id
            )
        ) consistency
    ) AS consistency_trust_factor,
    
    -- Node-based trust (if participant is also a node)
    CASE 
        WHEN nr.node_id IS NOT NULL THEN
            nr.trust_score * 0.3  -- Node trust contributes to participant trust
        ELSE 0
    END AS node_trust_contribution,
    
    -- Combined trust score with propagation factors
    (
        (p.reputation_score * 0.4) +  -- Base reputation
        (CASE 
            WHEN p.total_judgment + p.total_impact > 0 THEN
                (p.accurate_judgment + p.accurate_impact) * 1.0 / (p.total_judgment + p.total_impact)
            ELSE 0.5
        END * 0.2) +  -- Validation accuracy
        (COALESCE((
            SELECT AVG(neighbor_reputation * connection_strength)
            FROM (
                SELECT 
                    p2.reputation_score AS neighbor_reputation,
                    CASE 
                        WHEN j.event_id IS NOT NULL THEN 0.8
                        WHEN i.event_id IS NOT NULL THEN 0.6
                        WHEN imp.event_id IS NOT NULL THEN 0.4
                        ELSE 0.1
                    END AS connection_strength
                FROM participants p2
                LEFT JOIN judgment j ON p2.id = j.participant_id
                LEFT JOIN impact i ON p2.id = i.participant_id
                LEFT JOIN impact_predictions imp ON p2.id = (
                    SELECT participant_id 
                    FROM truth_event 
                    WHERE id = (
                        SELECT created_by 
                        FROM event_ci 
                        WHERE id = imp.event_id
                    )
                )
                WHERE j.participant_id = p.id OR i.participant_id = p.id OR imp.event_id = (
                    SELECT id FROM event_ci WHERE created_by = (
                        SELECT id FROM truth_event WHERE participant_id = p.id
                    )
                )
            ) connections
        ), 0.5) * 0.2) +  -- Network trust
        (CASE 
            WHEN nr.node_id IS NOT NULL THEN nr.trust_score * 0.2
            ELSE 0
        END)  -- Node trust
    ) AS propagated_trust_score,
    
    -- Trust centrality measure (how central is this participant in the trust network)
    (
        SELECT COUNT(DISTINCT related_participant_id)
        FROM (
            SELECT participant_id AS related_participant_id
            FROM judgment
            WHERE event_id IN (
                SELECT id FROM truth_event WHERE participant_id = p.id
            )
            
            UNION
            
            SELECT participant_id AS related_participant_id
            FROM impact
            WHERE event_id IN (
                SELECT created_by FROM event_ci WHERE created_by IN (
                    SELECT id FROM truth_event WHERE participant_id = p.id
                )
            )
            
            UNION
            
            SELECT (
                SELECT participant_id 
                FROM truth_event 
                WHERE id = (
                    SELECT created_by 
                    FROM event_ci 
                    WHERE id = ip.event_id
                )
            ) AS related_participant_id
            FROM impact_predictions ip
            WHERE ip.event_id IN (
                SELECT id FROM event_ci WHERE created_by IN (
                    SELECT id FROM truth_event WHERE participant_id = p.id
                )
            )
        ) related
    ) AS trust_centrality,
    
    -- Trust influence radius (how far trust propagates from this participant)
    (
        SELECT AVG(connection_distance)
        FROM (
            SELECT 
                CASE 
                    WHEN j.participant_id IS NOT NULL THEN 1  -- Direct connection
                    WHEN i.participant_id IS NOT NULL THEN 2  -- Second-degree connection
                    WHEN imp.event_id IS NOT NULL THEN 3      -- Third-degree connection
                    ELSE 4
                END AS connection_distance
            FROM judgment j
            JOIN truth_event te ON j.event_id = te.id
            JOIN participants p_related ON te.participant_id = p_related.id
            LEFT JOIN impact i ON i.event_id = te.id
            LEFT JOIN impact_predictions imp ON imp.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
            WHERE p_related.id = p.id
        ) distances
    ) AS trust_influence_radius,
    
    -- Trust stability indicator
    CASE 
        WHEN (
            SELECT COUNT(*)
            FROM reputation_history rh
            WHERE rh.id = p.reputation_history
            AND julianday('now') - julianday(rh.updated_at, 'unixepoch') < 30
        ) >= 5 THEN 1  -- Active participant with recent updates
        ELSE 0
    END AS trust_stability_indicator,
    
    -- Trust diversity (based on variety of contexts participated in)
    (
        SELECT COUNT(DISTINCT category_id) + COUNT(DISTINCT forma_id) + 
               COUNT(DISTINCT cause_id) + COUNT(DISTINCT develop_id) + COUNT(DISTINCT effect_id)
        FROM truth_event
        WHERE participant_id = p.id
    ) AS trust_diversity_score,
    
    -- Anomaly detection in trust patterns
    CASE 
        WHEN p.reputation_score > (
            SELECT AVG(reputation_score) + 2 * SQRT(AVG(POWER(reputation_score, 2)) - POWER(AVG(reputation_score), 2))
            FROM participants
        ) THEN 1  -- Suspiciously high reputation
        ELSE 0
    END AS trust_anomaly_flag,
    
    -- Trust propagation priority
    CASE 
        WHEN p.reputation_score > 0.7 AND trust_centrality > 5 THEN 'HIGH'
        WHEN p.reputation_score > 0.5 AND trust_centrality > 2 THEN 'MEDIUM'
        WHEN p.reputation_score > 0.3 THEN 'LOW'
        ELSE 'MINIMAL'
    END AS trust_propagation_priority,
    
    -- Metadata
    p.created_at AS participant_created_at,
    p.last_activity AS participant_last_activity,
    CURRENT_TIMESTAMP AS calculation_timestamp

FROM participants p
LEFT JOIN node_ratings nr ON nr.node_id = p.public_key;

-- View for trust propagation paths
CREATE VIEW trust_propagation_paths AS
SELECT 
    pt1.participant_id AS source_participant_id,
    pt2.participant_id AS target_participant_id,
    pt1.propagated_trust_score AS source_trust,
    pt2.propagated_trust_score AS target_trust,
    
    -- Trust propagation strength
    pt1.propagated_trust_score * pt2.propagated_trust_score AS combined_trust_strength,
    
    -- Path through shared events
    COUNT(DISTINCT te.id) AS shared_events_count,
    
    -- Path through shared judgments
    (
        SELECT COUNT(*)
        FROM judgment j1
        JOIN judgment j2 ON j1.event_id = j2.event_id
        WHERE j1.participant_id = pt1.participant_id
        AND j2.participant_id = pt2.participant_id
    ) AS shared_judgments_count,
    
    -- Path through shared impacts
    (
        SELECT COUNT(*)
        FROM impact i1
        JOIN impact i2 ON i1.event_id = i2.event_id
        WHERE i1.participant_id = pt1.participant_id
        AND i2.participant_id = pt2.participant_id
    ) AS shared_impacts_count,
    
    -- Path through shared predictions
    (
        SELECT COUNT(*)
        FROM impact_predictions ip1
        JOIN impact_predictions ip2 ON ip1.event_id = ip2.event_id
        WHERE (
            SELECT participant_id FROM truth_event WHERE id = (
                SELECT created_by FROM event_ci WHERE id = ip1.event_id
            )
        ) = pt1.participant_id
        AND (
            SELECT participant_id FROM truth_event WHERE id = (
                SELECT created_by FROM event_ci WHERE id = ip2.event_id
            )
        ) = pt2.participant_id
    ) AS shared_predictions_count,
    
    -- Overall connection strength
    (
        SELECT 
            (shared_judgments_count * 0.4) + 
            (shared_impacts_count * 0.3) + 
            (shared_predictions_count * 0.3)
    ) AS connection_strength,
    
    -- Trust propagation potential
    CASE 
        WHEN combined_trust_strength > 0.5 AND connection_strength > 2 THEN 'STRONG'
        WHEN combined_trust_strength > 0.3 AND connection_strength > 1 THEN 'MODERATE'
        WHEN combined_trust_strength > 0.1 THEN 'WEAK'
        ELSE 'NONE'
    END AS propagation_potential
    
FROM participant_trust_propagation pt1
CROSS JOIN participant_trust_propagation pt2
JOIN truth_event te ON te.participant_id = pt1.participant_id
WHERE pt1.participant_id != pt2.participant_id
AND te.id IN (
    SELECT id FROM truth_event WHERE participant_id = pt2.participant_id
)
GROUP BY pt1.participant_id, pt2.participant_id, pt1.propagated_trust_score, pt2.propagated_trust_score;

-- View for identifying trust hubs in the network
CREATE VIEW trust_hubs_identification AS
SELECT 
    participant_id,
    participant_public_key,
    base_reputation,
    propagated_trust_score,
    trust_centrality,
    
    -- Hub score based on centrality and influence
    trust_centrality * trust_influence_radius * propagated_trust_score AS hub_score,
    
    -- Hub category
    CASE 
        WHEN trust_centrality > 10 AND propagated_trust_score > 0.7 THEN 'MAJOR_HUB'
        WHEN trust_centrality > 5 AND propagated_trust_score > 0.5 THEN 'MINOR_HUB'
        WHEN trust_centrality > 2 AND propagated_trust_score > 0.3 THEN 'EMERGING_HUB'
        ELSE 'PERIPHERAL'
    END AS hub_category,
    
    -- Hub characteristics
    CASE 
        WHEN trust_diversity_score > 8 THEN 'DIVERSE_EXPERT'
        WHEN trust_centrality > 15 THEN 'CONNECTIVITY_HUB'
        WHEN base_reputation > 0.8 THEN 'ACCURACY_LEADER'
        WHEN trust_influence_radius < 2 THEN 'LOCAL_INFLUENCER'
        ELSE 'BALANCED_PARTICIPANT'
    END AS hub_characteristics,
    
    trust_diversity_score,
    trust_stability_indicator,
    trust_anomaly_flag,
    trust_propagation_priority
    
FROM participant_trust_propagation
ORDER BY hub_score DESC;

-- View for monitoring trust propagation anomalies
CREATE VIEW trust_anomaly_monitoring AS
SELECT 
    participant_id,
    participant_public_key,
    base_reputation,
    propagated_trust_score,
    trust_centrality,
    
    -- Anomaly detection based on unexpected trust patterns
    CASE 
        WHEN base_reputation < 0.3 AND trust_centrality > 10 THEN 'HIGH_CENTRALITY_LOW_REPUTATION'
        WHEN base_reputation > 0.8 AND trust_centrality < 2 THEN 'HIGH_REPUTATION_LOW_CENTRALITY'
        WHEN ABS(base_reputation - propagated_trust_score) > 0.3 THEN 'REPUTATION_PROPAGATION_MISMATCH'
        WHEN trust_anomaly_flag = 1 THEN 'REPUTATION_OUTLIER'
        ELSE 'NORMAL'
    END AS anomaly_type,
    
    -- Anomaly severity
    CASE 
        WHEN base_reputation < 0.3 AND trust_centrality > 10 THEN 'HIGH'
        WHEN base_reputation > 0.8 AND trust_centrality < 2 THEN 'MEDIUM'
        WHEN ABS(base_reputation - propagated_trust_score) > 0.3 THEN 'MEDIUM'
        WHEN trust_anomaly_flag = 1 THEN 'HIGH'
        ELSE 'LOW'
    END AS anomaly_severity,
    
    -- Potential manipulation indicator
    CASE 
        WHEN trust_centrality > 20 AND base_reputation > 0.9 THEN 1  -- Potentially manipulated
        WHEN trust_anomaly_flag = 1 AND trust_centrality > 10 THEN 1  -- Suspicious high reputation with high connectivity
        ELSE 0
    END AS potential_manipulation_flag,
    
    -- Recommended action
    CASE 
        WHEN trust_centrality > 20 AND base_reputation > 0.9 THEN 'INVESTIGATE'
        WHEN trust_anomaly_flag = 1 AND trust_centrality > 10 THEN 'REVIEW'
        WHEN base_reputation < 0.3 AND trust_centrality > 10 THEN 'MONITOR'
        ELSE 'NORMAL'
    END AS recommended_action
    
FROM participant_trust_propagation
WHERE 
    (base_reputation < 0.3 AND trust_centrality > 10) OR  -- Suspicious low reputation but high connectivity
    (base_reputation > 0.8 AND trust_centrality < 2) OR   -- Suspicious high reputation but low connectivity
    (ABS(base_reputation - propagated_trust_score) > 0.3) OR  -- Big mismatch between base and propagated trust
    trust_anomaly_flag = 1;  -- Flagged as outlier
```

## Key Features

### Multi-Faceted Trust Propagation
The view calculates trust propagation through multiple channels:
- Direct trust based on judgment weights
- Validation accuracy trust
- Network-based trust from participant relationships
- Temporal decay of trust
- Consistency-based trust factors

### Network Analysis
Analyzes the network structure to determine:
- Trust centrality (how central a participant is in the trust network)
- Trust influence radius (how far trust propagates from a participant)
- Trust diversity (variety of contexts a participant engages with)

### Anomaly Detection
Identifies potential manipulation or unusual trust patterns:
- High centrality with low reputation
- High reputation with low centrality
- Significant mismatches between base and propagated trust scores
- Outlier reputation scores

### Hub Identification
Identifies key participants who serve as trust hubs in the network:
- Major hubs with high connectivity and reputation
- Minor hubs with moderate influence
- Emerging hubs gaining influence

### Trust Propagation Paths
Maps the pathways through which trust propagates between participants via:
- Shared events
- Shared judgments
- Shared impact assessments
- Shared predictions

## Relationship to Model Core
This view implements the trust propagation mechanisms described in the model where:
- Trust propagates through the network based on participant relationships
- Validation accuracy affects trust propagation
- Temporal decay applies to trust weights over time
- The system is designed to resist manipulation attempts
- Network topology influences trust propagation patterns

## Usage Examples

```sql
-- Get trust propagation metrics for a specific participant
SELECT * FROM participant_trust_propagation WHERE participant_id = ?;

-- Find trust hubs in the network
SELECT * FROM trust_hubs_identification WHERE hub_category IN ('MAJOR_HUB', 'MINOR_HUB');

-- Monitor trust propagation anomalies
SELECT * FROM trust_anomaly_monitoring WHERE anomaly_severity = 'HIGH';

-- Find trust propagation paths between participants
SELECT * FROM trust_propagation_paths 
WHERE source_participant_id = ? AND target_participant_id = ?;

-- Identify participants with high influence in the trust network
SELECT * FROM participant_trust_propagation 
WHERE trust_centrality > 5 AND propagated_trust_score > 0.6;
```

## Integration with Other Components
- Works with `participants` table to get base reputation scores
- Integrates with `judgment_weights` for direct trust calculations
- Connects to `reputation_history` for consistency analysis
- Links to `node_ratings` for node-based trust components
- Supports `impact_predictions` and `truth_event` for network analysis
- Used in `judgment_weights` to determine participant weights

## Notes
- The view balances direct reputation with network-based trust propagation
- Anomaly detection helps identify potential manipulation attempts
- Trust propagation follows network connections but is capped to prevent unlimited growth
- Temporal decay ensures that older contributions have diminishing influence
- Diversity measures help identify well-rounded participants