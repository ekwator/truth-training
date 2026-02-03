**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

-- SQL Implementation for Participant Group Parameters and Correlation
-- Updated stability calculations with quality-focused thresholds to improve fairness
--
-- AUTHORITATIVE IMPLEMENTATION: This is the authoritative implementation for convergence detection.
-- All convergence detection logic should be implemented in SQL views and triggers.
-- Rust code should NOT duplicate or replace this database logic.

-- View for manual group correlations
```sql
CREATE VIEW manual_group_correlations AS
SELECT 
    pg.id as group_id,
    pg.description,
    pgp.parameter_id,
    pgp.priority_value,
    tc.correlation_result
FROM participants_groups pg
JOIN participants_groups_parameters pgp ON pg.id = pgp.group_id
JOIN group_correlations tc ON tc.group_id = pg.id
WHERE pg.type = 'manual';
```
-- View for convergence zones
```sql
CREATE VIEW convergence_zones AS
SELECT 
    gc1.group_id as group1_id,
    gc2.group_id as group2_id,
    gc1.description as group1_desc,
    gc2.description as group2_desc,
    ABS(gc1.correlation_result - gc2.correlation_result) as result_difference
FROM group_correlations gc1
CROSS JOIN group_correlations gc2
WHERE gc1.group_id < gc2.group_id  -- Compare different groups
  AND gc1.correlation_result IS NOT NULL
  AND gc2.correlation_result IS NOT NULL;
```
-- Updated view for auto group stability with quality-focused thresholds
```sql
CREATE VIEW auto_group_stability AS
SELECT 
    ag.id as auto_group_id,
    ag.description,
    COUNT(cz.group1_id) as convergence_instances,
    AVG(cz.result_difference) as avg_difference,
    MIN(cz.result_difference) as min_difference,
    MAX(cz.result_difference) as max_difference,
    CASE 
        WHEN AVG(cz.result_difference) < 0.05 AND MIN(cz.result_difference) < 0.02 THEN 'STABLE' -- High quality convergence
        WHEN AVG(cz.result_difference) < 0.10 AND COUNT(cz.group1_id) >= 2 THEN 'MODERATELY_STABLE' -- Require quality + minimum instances  
        WHEN AVG(cz.result_difference) < 0.20 AND COUNT(cz.group1_id) >= 1 THEN 'MINIMALLY_STABLE' -- Accept lower quality with at least one instance
        ELSE 'UNSTABLE'
    END as stability_status
FROM participants_groups ag
LEFT JOIN convergence_zones cz ON ag.description LIKE '%' || cz.group1_desc || '%'
WHERE ag.type = 'auto'
GROUP BY ag.id, ag.description;
```
-- View for group evaluation metrics
```sql
CREATE VIEW group_evaluation_metrics AS
SELECT 
    pg.id,
    pg.description,
    pg.type,
    -- Count of participants in the group (for reporting only, not weighting)
    (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg.id AND pgm.left_at IS NULL) as participant_count,
    -- Manual group scoring: weighted average of correlation results
    CASE 
        WHEN pg.type = 'manual' THEN 
            (SELECT AVG(correlation_result) * 0.7 FROM group_correlations WHERE group_id = pg.id) +
            (SELECT COUNT(*) * 0.3 FROM convergence_zones WHERE group1_id = pg.id OR group2_id = pg.id)
        ELSE 0
    END as manual_score,
    -- Auto group scoring: based on convergence quality
    CASE 
        WHEN pg.type = 'auto' THEN 
            (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg.id OR group2_id = pg.id)
        ELSE 0
    END as auto_score
FROM participants_groups pg;
```
-- View for fairness verification - checks correlation between size and scores
```sql
CREATE VIEW fairness_verification_size_impact AS
SELECT 
    pg.id,
    pg.description,
    pg.type,
    (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg.id AND pgm.left_at IS NULL) as participant_count,
    CASE 
        WHEN pg.type = 'manual' THEN 
            (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg.id)
        ELSE
            (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg.id OR group2_id = pg.id)
    END as score,
    -- Correlation coefficient between participant count and score (should be close to 0 for fairness)
    (SELECT 
        (COUNT(*) * SUM(pc_s.product) - SUM(pc_s.count) * SUM(pc_s.score)) / 
        (SQRT(COUNT(*) * SUM(pc_s.count_sq) - SUM(pc_s.count) * SUM(pc_s.count)) * 
         SQRT(COUNT(*) * SUM(pc_s.score_sq) - SUM(pc_s.score) * SUM(pc_s.score)))
     FROM (
         SELECT 
             (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) as count,
             CASE 
                 WHEN pg_inner.type = 'manual' THEN 
                     (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg_inner.id)
                 ELSE
                     (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg_inner.id OR group2_id = pg_inner.id)
             END as score,
             (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) *
             CASE 
                 WHEN pg_inner.type = 'manual' THEN 
                     (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg_inner.id)
                 ELSE
                     (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg_inner.id OR group2_id = pg_inner.id)
             END as product,
             ((SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) * 
              (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL)) as count_sq,
             (CASE 
                 WHEN pg_inner.type = 'manual' THEN 
                     (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg_inner.id)
                 ELSE
                     (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg_inner.id OR group2_id = pg_inner.id)
             END *
             CASE 
                 WHEN pg_inner.type = 'manual' THEN 
                     (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg_inner.id)
                 ELSE
                     (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg_inner.id OR group2_id = pg_inner.id)
             END) as score_sq
         FROM participants_groups pg_inner
         WHERE pg_inner.type = pg.type
     ) as pc_s
    ) as size_score_correlation
FROM participants_groups pg;
```
-- View for stability independence check - verifies that stability doesn't correlate with group size
```sql
CREATE VIEW stability_independence_check AS
SELECT 
    ags.auto_group_id,
    ags.description,
    ags.participant_count,
    ags.stability_status,
    -- Calculate Pearson correlation between participant count and stability score
    -- Stability scores: STABLE=3, MODERATELY_STABLE=2, MINIMALLY_STABLE=1, UNSTABLE=0
    (SELECT 
        (COUNT(*) * SUM(pc_ss.product) - SUM(pc_ss.count) * SUM(pc_ss.stability_score)) / 
        (SQRT(COUNT(*) * SUM(pc_ss.count_sq) - SUM(pc_ss.count) * SUM(pc_ss.count)) * 
         SQRT(COUNT(*) * SUM(pc_ss.score_sq) - SUM(pc_ss.stability_score) * SUM(pc_ss.stability_score)))
     FROM (
         SELECT 
             (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) as count,
             CASE ags_inner.stability_status
                 WHEN 'STABLE' THEN 3
                 WHEN 'MODERATELY_STABLE' THEN 2
                 WHEN 'MINIMALLY_STABLE' THEN 1
                 ELSE 0
             END as stability_score,
             (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) *
             CASE ags_inner.stability_status
                 WHEN 'STABLE' THEN 3
                 WHEN 'MODERATELY_STABLE' THEN 2
                 WHEN 'MINIMALLY_STABLE' THEN 1
                 ELSE 0
             END as product,
             ((SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL) * 
              (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg_inner.id AND pgm.left_at IS NULL)) as count_sq,
             (CASE ags_inner.stability_status
                 WHEN 'STABLE' THEN 3
                 WHEN 'MODERATELY_STABLE' THEN 2
                 WHEN 'MINIMALLY_STABLE' THEN 1
                 ELSE 0
             END *
             CASE ags_inner.stability_status
                 WHEN 'STABLE' THEN 3
                 WHEN 'MODERATELY_STABLE' THEN 2
                 WHEN 'MINIMALLY_STABLE' THEN 1
                 ELSE 0
             END) as score_sq
         FROM auto_group_stability ags_inner
         JOIN participants_groups pg_inner ON ags_inner.auto_group_id = pg_inner.id
     ) as pc_ss
    ) as size_stability_correlation
FROM auto_group_stability ags
JOIN participants_groups pg ON ags.auto_group_id = pg.id;
```
-- View for group correlation summary - orders by stability rather than size
```sql
CREATE VIEW group_correlation_summary AS
SELECT 
    pg.id,
    pg.description,
    pg.type,
    (SELECT COUNT(*) FROM participants_groups_members pgm WHERE pgm.group_id = pg.id AND pgm.left_at IS NULL) as participant_count,
    CASE 
        WHEN pg.type = 'manual' THEN 
            (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg.id)
        ELSE
            (SELECT AVG(2 - result_difference) FROM convergence_zones WHERE group1_id = pg.id OR group2_id = pg.id)
    END as correlation_score,
    CASE 
        WHEN pg.type = 'auto' THEN 
            (SELECT stability_status FROM auto_group_stability WHERE auto_group_id = pg.id)
        ELSE 'N/A'
    END as stability_status
FROM participants_groups pg
ORDER BY 
    -- Rank groups by stability rather than size (addresses fairness concern)
    CASE 
        WHEN pg.type = 'auto' THEN 
            CASE (SELECT stability_status FROM auto_group_stability WHERE auto_group_id = pg.id)
                WHEN 'STABLE' THEN 4
                WHEN 'MODERATELY_STABLE' THEN 3
                WHEN 'MINIMALLY_STABLE' THEN 2
                ELSE 1
            END
        ELSE 
            CASE 
                WHEN pg.type = 'manual' THEN 
                    (SELECT AVG(correlation_result) FROM group_correlations WHERE group_id = pg.id)
                ELSE 0
            END
    END DESC,
    pg.id;
```