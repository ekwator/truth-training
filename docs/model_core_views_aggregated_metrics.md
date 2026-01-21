-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Aggregated System Metrics and Expert Functions  

-- Function to calculate system trend  
-- Trend = (Σ P - Σ N) / total_events  
-- Where P — positive impacts, N — negative impacts  
```sql
CREATE VIEW system_trend_calculation AS
SELECT 
    total_events,
    total_positive_impacts,
    total_negative_impacts,
    CASE 
        WHEN total_events > 0 
        THEN (total_positive_impacts - total_negative_impacts) / total_events
        ELSE 0.0
    END as calculated_trend,
    total_positive_impacts - total_negative_impacts as impact_balance
FROM progress_metrics;
```
-- Function to calculate group vs individual comparison  
```sql
CREATE VIEW group_efficiency_calculation AS
SELECT 
    total_events,
    total_events_group,
    CASE 
        WHEN total_events > 0 AND (total_events_group * 1.0 / total_events) > 0.5
        THEN 'HIGH'  -- group collaboration effective
        ELSE 'LOW'   -- individual assessment dominant
    END as system_efficiency,
    (total_events_group * 1.0 / total_events) as group_to_total_ratio
FROM progress_metrics;
```
-- Function to calculate heuristic influence on judgments  
```sql
CREATE VIEW heuristic_influence_calculation AS
SELECT 
    jh.judgment_id,
    jh.heuristic_id,
    eh.name as heuristic_name,
    eh.domain as heuristic_domain,
    jh.influence,
    -- Calculate cumulative influence of all heuristics on a judgment
    SUM(jh.influence) OVER (PARTITION BY jh.judgment_id) as total_influence_per_judgment
FROM judgment_heuristics jh
JOIN expert_heuristics eh ON jh.heuristic_id = eh.id;
```
-- Function to apply heuristics based on confidence threshold  
```sql
CREATE VIEW applicable_heuristics AS
SELECT 
    eh.id,
    eh.name,
    eh.domain,
    eh.weight,
    eh.confidence,
    eh.proven_accuracy,
    CASE 
        WHEN eh.confidence > 0.7 THEN eh.weight
        ELSE eh.weight * 0.5  -- reduced weight
    END as effective_weight
FROM expert_heuristics eh;
```
-- Function to detect conflicting heuristics  
```sql
CREATE VIEW conflicting_heuristics AS
SELECT 
    jh1.judgment_id,
    jh1.heuristic_id as heuristic1_id,
    jh2.heuristic_id as heuristic2_id,
    eh1.name as heuristic1_name,
    eh2.name as heuristic2_name,
    'CONFLICT_DETECTED' as conflict_status
FROM judgment_heuristics jh1
JOIN judgment_heuristics jh2 ON jh1.judgment_id = jh2.judgment_id
JOIN expert_heuristics eh1 ON jh1.heuristic_id = eh1.id
JOIN expert_heuristics eh2 ON jh2.heuristic_id = eh2.id
WHERE jh1.heuristic_id != jh2.heuristic_id
  AND ((eh1.domain = eh2.domain AND eh1.name != eh2.name) 
       OR (eh1.confidence > 0.8 AND eh2.confidence > 0.8 AND eh1.weight > 0.8 AND eh2.weight > 0.8));
```
-- Function to calculate final event assessment as aggregated function  
-- Final event assessment is aggregated function of all applied heuristics and judgment  
```sql
CREATE VIEW final_event_assessment AS
SELECT 
    ec.id as event_id,
    -- Combine judgment confidence with heuristic influences
    COALESCE(cc.confidence_score, 0) as base_confidence,
    COALESCE(SUM(jh.influence), 0) as total_heuristic_influence,
    (COALESCE(cc.confidence_score, 0) + COALESCE(SUM(jh.influence), 0)) / 2 as combined_assessment
FROM event_ci ec
LEFT JOIN consensus_ci cc ON ec.id = cc.event_id
LEFT JOIN judgment j ON ec.id = j.event_id
LEFT JOIN judgment_heuristics jh ON j.id = jh.judgment_id
GROUP BY ec.id, cc.confidence_score;
```
-- Domain classification for heuristics  
-- "logic" — formal logic rules  
-- "statistical" — statistical inference rules  
-- "empirical" — experience-based rules  
-- "contextual" — context-sensitive rules  
-- "domain_specific" — specialized knowledge rules  
```sql
CREATE VIEW domain_classification_stats AS
SELECT 
    domain,
    COUNT(*) as heuristic_count,
    AVG(weight) as avg_weight,
    AVG(confidence) as avg_confidence,
    AVG(proven_accuracy) as avg_accuracy
FROM expert_heuristics
GROUP BY domain;
```
-- Function to calculate relay success rate  
-- Success rate = successful_operations / total_operations  
```sql
CREATE VIEW relay_success_rate_calculation AS
SELECT
    pubkey,
    CASE
        WHEN total_operations > 0 THEN successful_operations * 1.0 / total_operations
        ELSE 0.0
    END as calculated_success_rate
FROM (
    SELECT
        nm.pubkey,
        nm.successful_operations,
        nm.total_operations
    FROM node_performance nm
    -- This would require additional tracking tables for successful/total operations
);
```
-- Function to calculate quality index based on multiple factors  
-- Q(n) = α * recent_performance + β * historical_consistency + γ * trust_factor  
-- Where α=0.4, β=0.4, γ=0.2 (these weights can be adjusted as needed)  
```sql
CREATE VIEW quality_index_calculation AS
SELECT
    nm.pubkey,
    (0.4 * COALESCE(recent_performance, 0.0)) +
    (0.4 * COALESCE(historical_consistency, 0.0)) +
    (0.2 * COALESCE(nr.trust_score, 0.0)) as calculated_quality_index
FROM node_performance nm
LEFT JOIN node_ratings nr ON nm.pubkey = nr.node_id;
```
-- Function to calculate success rate from peer history  
-- Success rate = success_count / (success_count + fail_count)  
```sql
CREATE VIEW peer_success_rate_calculation AS
SELECT
    peer_url,
    CASE
        WHEN (success_count + fail_count) > 0 THEN
            success_count * 1.0 / (success_count + fail_count)
        ELSE 0.0
    END as calculated_success_rate
FROM peer_synchronization;
```
-- Function to calculate synchronization statistics
-- Provides aggregated statistics on synchronization attempts by peer
```sql
CREATE VIEW sync_statistics AS
SELECT
    peer_url,
    COUNT(*) as total_sync_attempts,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_syncs,
    SUM(CASE WHEN status != 'success' THEN 1 ELSE 0 END) as failed_syncs,
    AVG(CASE WHEN status = 'success' THEN 1.0 ELSE 0.0 END) as success_rate
FROM sync_attempts
GROUP BY peer_url;
```

-- Function to calculate event classification based on convergence of impact and judgment axes
-- Handles event classification updates and manages event classification based on the convergence of impact and judgment axes
```sql
CREATE VIEW event_classification_calculation AS
SELECT
    ec.id as event_id,
    ec.event_type,
    ec.status,
    CASE
        WHEN NOT (
            EXISTS (
                SELECT 1 FROM impact_metrics
                WHERE impact_metrics.event_id = ec.id
                AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
            ) OR
            EXISTS (
                SELECT 1 FROM judgment_weights
                WHERE judgment_weights.event_id = ec.id
                AND judgment_weights.weight IS NOT NULL
            )
        ) THEN 'unstable'
        WHEN (
            EXISTS (
                SELECT 1 FROM impact_metrics
                WHERE impact_metrics.event_id = ec.id
                AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
            ) XOR
            EXISTS (
                SELECT 1 FROM judgment_weights
                WHERE judgment_weights.event_id = ec.id
                AND judgment_weights.weight IS NOT NULL
            )
        ) THEN 'suppose'
        WHEN (
            EXISTS (
                SELECT 1 FROM impact_metrics
                WHERE impact_metrics.event_id = ec.id
                AND (positive_ratio IS NOT NULL OR negative_ratio IS NOT NULL OR uncertainty IS NOT NULL)
            ) AND
            EXISTS (
                SELECT 1 FROM judgment_weights
                WHERE judgment_weights.event_id = ec.id
                AND judgment_weights.weight IS NOT NULL
            ) AND
            ec.event_type = 'both' AND
            (ec.status = 'resolved' OR ec.status = 'archived')
        ) THEN 'consent'
        ELSE 'unstable'
    END as calculated_resolution_data
FROM event_ci ec;
```

-- Function to calculate event state history tracking for temporal analysis
-- This view supports tracking how event assessments evolve over time as mentioned in section 3.6
```sql
CREATE VIEW event_state_history_tracking AS
SELECT
    esh.id,
    esh.event_id,
    esh.judgment_count,
    esh.truth_score,
    esh.impact_count,
    esh.impact_score,
    esh.recorded_at,
    -- Calculate the rate of change of truth assessment over time
    (esh.truth_score - LAG(esh.truth_score, 1, esh.truth_score) OVER (
        PARTITION BY esh.event_id ORDER BY esh.recorded_at
    )) /
    (esh.recorded_at - LAG(esh.recorded_at, 1, esh.recorded_at) OVER (
        PARTITION BY esh.event_id ORDER BY esh.recorded_at
    ) + (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)) as truth_change_rate,
    -- Calculate the rate of change of impact assessment over time
    (esh.impact_score - LAG(esh.impact_score, 1, esh.impact_score) OVER (
        PARTITION BY esh.event_id ORDER BY esh.recorded_at
    )) /
    (esh.recorded_at - LAG(esh.recorded_at, 1, esh.recorded_at) OVER (
        PARTITION BY esh.event_id ORDER BY esh.recorded_at
    ) + (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)) as impact_change_rate
FROM event_state_history esh;
```

-- Function to calculate event stability detection based on temporal dynamics
-- This view supports the stability detection logic mentioned in section 3.7
```sql
CREATE VIEW event_stability_detection AS
SELECT
    es.id,
    es.event_id,
    es.truth_stable,
    es.impact_stable,
    es.stabilized_at,
    -- Calculate if truth is stabilized based on threshold (small_constants)
    CASE
        WHEN ABS(esht.truth_change_rate) < (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)
        AND (SELECT confidence FROM truth_state WHERE event_id = es.event_id LIMIT 1) > 0.7
        THEN 1
        ELSE 0
    END as calculated_truth_stable,
    -- Calculate if impact is stabilized based on threshold (small_constants)
    CASE
        WHEN ABS(esht.impact_change_rate) < (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END)
        AND (SELECT impact_significance FROM truth_state WHERE event_id = es.event_id LIMIT 1) > 0.5
        THEN 1
        ELSE 0
    END as calculated_impact_stable
FROM event_stability es
LEFT JOIN event_state_history_tracking esht ON es.event_id = esht.event_id;
```