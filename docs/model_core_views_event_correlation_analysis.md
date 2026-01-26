-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Correlation Analysis Between Related Events in the Event Graph  

-- Analyzes correlations between related events in the event graph
-- Implements the relationship analysis described in section 2.5
-- Links: event_links.source_impact_id → truth_event.id → event_ci.created_by → truth_event.id → participants.id
```sql
CREATE VIEW event_correlation_analysis AS
SELECT
    el.source_impact_id as source_event_id,
    el.target_impact_id as target_event_id,
    el.relation_type,
    -- Get source event data
    te1.description as source_event_description,
    te1.collective_score as source_truth_score,
    te1.impact_score as source_impact_score,
    te1.judgment_score as source_judgment_score,
    -- Get target event data
    te2.description as target_event_description,
    te2.collective_score as target_truth_score,
    te2.impact_score as target_impact_score,
    te2.judgment_score as target_judgment_score,
    -- Calculate correlation between truth scores
    (te1.collective_score - (SELECT AVG(collective_score) FROM truth_event)) * 
    (te2.collective_score - (SELECT AVG(collective_score) FROM truth_event)) as truth_correlation_component,
    -- Calculate correlation between impact scores
    (te1.impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL)) * 
    (te2.impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL)) as impact_correlation_component,
    -- Calculate correlation between judgment scores
    (COALESCE(te1.judgment_score, 0.5) - (SELECT AVG(COALESCE(judgment_score, 0.5)) FROM truth_event)) * 
    (COALESCE(te2.judgment_score, 0.5) - (SELECT AVG(COALESCE(judgment_score, 0.5)) FROM truth_event)) as judgment_correlation_component,
    -- Calculate overall correlation score
    ((te1.collective_score - (SELECT AVG(collective_score) FROM truth_event)) * 
     (te2.collective_score - (SELECT AVG(collective_score) FROM truth_event)) +
     (te1.impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL)) * 
     (te2.impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL))) / 2.0 as combined_correlation_score,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM event_links el
JOIN truth_event te1 ON el.source_impact_id = te1.id
JOIN truth_event te2 ON el.target_impact_id = te2.id;
```

-- View for calculating Pearson correlation coefficient between events
-- This view implements a more mathematically rigorous correlation calculation
```sql
CREATE VIEW event_correlation_pearson_coefficient AS
SELECT
    eca.source_event_id,
    eca.target_event_id,
    eca.relation_type,
    -- Calculate numerator for Pearson correlation
    SUM(eca.truth_correlation_component) as sum_truth_cross_products,
    SUM(eca.impact_correlation_component) as sum_impact_cross_products,
    -- Calculate denominators (standard deviations)
    SQRT(SUM(POWER(eca.source_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)) * 
         SUM(POWER(eca.target_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2))) as truth_denominator,
    SQRT(SUM(POWER(eca.source_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)) * 
         SUM(POWER(eca.target_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2))) as impact_denominator,
    -- Calculate Pearson correlation coefficients
    CASE
        WHEN SQRT(SUM(POWER(eca.source_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)) * 
                  SUM(POWER(eca.target_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2))) = 0
        THEN 0
        ELSE SUM(eca.truth_correlation_component) / 
             SQRT(SUM(POWER(eca.source_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)) * 
                  SUM(POWER(eca.target_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)))
    END as truth_correlation_coefficient,
    CASE
        WHEN SQRT(SUM(POWER(eca.source_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)) * 
                  SUM(POWER(eca.target_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2))) = 0
        THEN 0
        ELSE SUM(eca.impact_correlation_component) / 
             SQRT(SUM(POWER(eca.source_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)) * 
                  SUM(POWER(eca.target_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)))
    END as impact_correlation_coefficient,
    -- Combined correlation (average of truth and impact correlations)
    (CASE
        WHEN SQRT(SUM(POWER(eca.source_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)) * 
                  SUM(POWER(eca.target_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2))) = 0
        THEN 0
        ELSE SUM(eca.truth_correlation_component) / 
             SQRT(SUM(POWER(eca.source_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)) * 
                  SUM(POWER(eca.target_truth_score - (SELECT AVG(collective_score) FROM truth_event), 2)))
    END +
    CASE
        WHEN SQRT(SUM(POWER(eca.source_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)) * 
                  SUM(POWER(eca.target_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2))) = 0
        THEN 0
        ELSE SUM(eca.impact_correlation_component) / 
             SQRT(SUM(POWER(eca.source_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)) * 
                  SUM(POWER(eca.target_impact_score - (SELECT AVG(impact_score) FROM truth_event WHERE impact_score IS NOT NULL), 2)))
    END) / 2.0 as combined_correlation_coefficient
FROM event_correlation_analysis eca
GROUP BY eca.source_event_id, eca.target_event_id, eca.relation_type;
```

-- View for identifying strongly correlated events
-- This view finds pairs of events with high correlation coefficients
```sql
CREATE VIEW strongly_correlated_events AS
SELECT
    ecp.source_event_id,
    ecp.target_event_id,
    ecp.relation_type,
    ecp.truth_correlation_coefficient,
    ecp.impact_correlation_coefficient,
    ecp.combined_correlation_coefficient,
    te1.description as source_event_desc,
    te2.description as target_event_desc,
    -- Categorize correlation strength
    CASE
        WHEN ABS(ecp.combined_correlation_coefficient) >= 0.7 THEN 'STRONG'
        WHEN ABS(ecp.combined_correlation_coefficient) >= 0.3 THEN 'MODERATE'
        WHEN ABS(ecp.combined_correlation_coefficient) >= 0.1 THEN 'WEAK'
        ELSE 'VERY_WEAK'
    END as correlation_strength,
    -- Direction of correlation
    CASE
        WHEN ecp.combined_correlation_coefficient > 0 THEN 'POSITIVE'
        WHEN ecp.combined_correlation_coefficient < 0 THEN 'NEGATIVE'
        ELSE 'NONE'
    END as correlation_direction,
    -- Statistical significance (simplified)
    CASE
        WHEN ABS(ecp.combined_correlation_coefficient) >= 0.5 THEN 1
        ELSE 0
    END as is_statistically_significant
FROM event_correlation_pearson_coefficient ecp
JOIN truth_event te1 ON ecp.source_event_id = te1.id
JOIN truth_event te2 ON ecp.target_event_id = te2.id
WHERE ABS(ecp.combined_correlation_coefficient) >= 0.3  -- Only moderately to strongly correlated events
ORDER BY ABS(ecp.combined_correlation_coefficient) DESC;
```

-- View for calculating event cluster analysis
-- This view groups events that are highly correlated with each other
```sql
CREATE VIEW event_cluster_analysis AS
WITH RECURSIVE event_graph_traversal(source_id, target_id, path, depth) AS (
    -- Base case: direct connections
    SELECT 
        source_event_id, 
        target_event_id, 
        CAST(source_event_id AS TEXT) || ',' || CAST(target_event_id AS TEXT) as path,
        1 as depth
    FROM strongly_correlated_events
    WHERE combined_correlation_coefficient >= 0.5
    
    UNION ALL
    
    -- Recursive case: extend paths
    SELECT 
        egt.source_id,
        sce.target_event_id,
        egt.path || ',' || CAST(sce.target_event_id AS TEXT) as path,
        egt.depth + 1
    FROM event_graph_traversal egt
    JOIN strongly_correlated_events sce ON egt.target_id = sce.source_event_id
    WHERE strpos(egt.path, CAST(sce.target_event_id AS TEXT)) = 0  -- Avoid cycles
    AND egt.depth < 5  -- Limit depth
)
SELECT 
    source_id as cluster_seed_event,
    COUNT(DISTINCT target_id) as cluster_size,
    AVG(sc.combined_correlation_coefficient) as avg_cluster_correlation,
    MIN(sc.combined_correlation_coefficient) as min_cluster_correlation,
    MAX(sc.combined_correlation_coefficient) as max_cluster_correlation,
    GROUP_CONCAT(DISTINCT target_id) as cluster_members
FROM event_graph_traversal egt
JOIN strongly_correlated_events sc ON egt.target_id = sc.target_event_id AND egt.source_id = sc.source_event_id
GROUP BY source_id
HAVING COUNT(DISTINCT target_id) >= 2  -- Only clusters with at least 2 members
ORDER BY cluster_size DESC;