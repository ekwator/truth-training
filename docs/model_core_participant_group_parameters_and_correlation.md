# SQL Triggers for Participant Group Parameters and Correlation

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview

This document defines the SQL triggers that implement the calculation of parameters for participant groups and the group correlation code. These triggers address the architectural issue of fairness and representative balance between participants by implementing a group correlation algorithm that correlates information at the group level without allowing larger or more active groups to dominate aggregated results simply due to their volume.

## Trigger Definitions

### 1. Trigger for Updating Manual Group Parameters

```sql
-- Trigger to update manual group parameters when participants create or modify priority configurations
CREATE TRIGGER update_manual_group_parameters
AFTER INSERT ON participants_groups
FOR EACH ROW
WHEN NEW.type = 'manual'
BEGIN
    -- Initialize default parameter priorities for new manual groups
    INSERT INTO participants_groups_parameters (group_id, parameter_id, priority_value, updated_at)
    SELECT 
        NEW.id as group_id,
        pp.id as parameter_id,
        0.5 as priority_value,  -- Default neutral priority
        CURRENT_TIMESTAMP as updated_at
    FROM group_parameters pp
    WHERE pp.active = 1;
END;
```

### 2. Trigger for Handling Parameter Priority Updates

```sql
-- Trigger to handle updates to parameter priorities in manual groups
CREATE TRIGGER handle_parameter_priority_update
AFTER UPDATE ON participants_groups_parameters
FOR EACH ROW
BEGIN
    -- Update the group correlation cache when priorities change
    INSERT OR REPLACE INTO group_correlation_cache (
        group_id,
        correlation_result,
        last_calculated
    )
    VALUES (
        NEW.group_id,
        NULL, -- Will be calculated by the correlation algorithm
        CURRENT_TIMESTAMP
    );
    
    -- Mark the group as needing recalculation
    UPDATE participants_groups 
    SET last_updated = CURRENT_TIMESTAMP 
    WHERE id = NEW.group_id;
END;
```

### 3. Trigger for Automatic Group Formation

```sql
-- Trigger to create automatic groups when multiple manual groups show convergence
CREATE TRIGGER create_auto_group_on_convergence
AFTER UPDATE ON group_correlation_cache
FOR EACH ROW
WHEN NEW.correlation_result IS NOT NULL
BEGIN
    -- Check if multiple manual groups have converged (similar correlation results)
    INSERT INTO participants_groups (type, created_at, description)
    SELECT 
        'auto' as type,
        CURRENT_TIMESTAMP as created_at,
        'Auto-group formed from convergence of manual groups with similar correlation patterns' as description
    WHERE (
        SELECT COUNT(DISTINCT gc1.group_id)
        FROM group_correlation_cache gc1
        JOIN group_correlation_cache gc2 ON ABS(gc1.correlation_result - NEW.correlation_result) < 0.05
        WHERE gc1.group_id != NEW.group_id
    ) >= 2  -- At least 2 other groups with similar correlation
    AND NOT EXISTS (
        SELECT 1 
        FROM participants_groups pg 
        WHERE pg.type = 'auto' 
        AND pg.created_at > datetime('now', '-1 hour')  -- Avoid frequent auto-group creation
    );
    
    -- If auto-group was created, link to the converging manual groups
    INSERT INTO auto_group_convergence (
        auto_group_id,
        manual_group_id,
        convergence_strength,
        detected_at
    )
    SELECT 
        (SELECT id FROM participants_groups WHERE type = 'auto' ORDER BY created_at DESC LIMIT 1) as auto_group_id,
        NEW.group_id as manual_group_id,
        1.0 - ABS(gc.correlation_result - NEW.correlation_result) as convergence_strength,
        CURRENT_TIMESTAMP as detected_at
    FROM group_correlation_cache gc
    WHERE gc.group_id = NEW.group_id
    AND ABS(gc.correlation_result - NEW.correlation_result) < 0.05;
END;
```

### 4. Trigger for Updating Group Ratings

```sql
-- Trigger to update group ratings based on correlation results and convergence
CREATE TRIGGER update_group_ratings
AFTER INSERT ON auto_group_convergence
FOR EACH ROW
BEGIN
    -- Calculate and update the rating for the auto group based on convergence data
    INSERT OR REPLACE INTO group_ratings (
        group_id,
        avg_score,
        coherence,
        last_updated
    )
    SELECT 
        NEW.auto_group_id as group_id,
        AVG(1.0 - ABS(gc.correlation_result - gc2.correlation_result)) as avg_score,
        1 - (SUM(ABS(gc.correlation_result - avg_corr.avg_corr_val)) / (COUNT(*) * 1.0)) as coherence,
        CURRENT_TIMESTAMP as last_updated
    FROM auto_group_convergence agc
    JOIN group_correlation_cache gc ON agc.manual_group_id = gc.group_id
    JOIN group_correlation_cache gc2 ON gc2.group_id = NEW.manual_group_id
    JOIN (
        SELECT AVG(correlation_result) as avg_corr_val
        FROM group_correlation_cache gc_inner
        JOIN auto_group_convergence agc_inner ON agc_inner.manual_group_id = gc_inner.group_id
        WHERE agc_inner.auto_group_id = NEW.auto_group_id
    ) avg_corr ON 1=1
    WHERE agc.auto_group_id = NEW.auto_group_id
    GROUP BY agc.auto_group_id;
END;
```

### 5. Trigger for Periodic Correlation Recalculation

```sql
-- Trigger to periodically recalculate correlations when new data is available
CREATE TRIGGER recalculate_group_correlations
AFTER INSERT ON truth_event
FOR EACH ROW
BEGIN
    -- Mark all group correlations as needing recalculation when new event data is available
    UPDATE group_correlation_cache
    SET correlation_result = NULL,
        last_calculated = CURRENT_TIMESTAMP
    WHERE group_id IN (
        SELECT DISTINCT group_id 
        FROM participants_groups_parameters 
        WHERE group_id IN (
            SELECT id FROM participants_groups WHERE type = 'manual'
        )
    );
    
    -- Update all manual groups to trigger recalculation
    UPDATE participants_groups
    SET last_updated = CURRENT_TIMESTAMP
    WHERE type = 'manual';
END;
```

### 6. Trigger for Impact Assessment Correlation

```sql
-- Trigger to calculate impact correlation for groups when new impact data is added
CREATE TRIGGER calculate_impact_correlation_for_groups
AFTER INSERT ON impact
FOR EACH ROW
BEGIN
    -- Update correlation cache for groups that use impact-related parameters
    INSERT OR REPLACE INTO group_correlation_cache (
        group_id,
        correlation_result,
        last_calculated
    )
    SELECT 
        pgp.group_id,
        -- Calculate correlation based on the impact data and group's parameter priorities
        (
            SELECT AVG(
                CASE 
                    WHEN NEW.value IS NOT NULL 
                    THEN NEW.value * pgp.priority_value
                    ELSE 0.5 * pgp.priority_value  -- Neutral value for NULL
                END
            )
            FROM participants_groups_parameters pgp2
            JOIN group_parameters gp ON pgp2.parameter_id = gp.id
            WHERE pgp2.group_id = pgp.group_id
            AND gp.parameter_name LIKE '%impact%'
        ) as correlation_result,
        CURRENT_TIMESTAMP as last_calculated
    FROM participants_groups_parameters pgp
    JOIN group_parameters gp ON pgp.parameter_id = gp.id
    WHERE gp.parameter_name LIKE '%impact%'
    AND pgp.group_id IN (
        SELECT id FROM participants_groups WHERE type = 'manual'
    );
END;
```

### 7. Trigger for Judgment Assessment Correlation

```sql
-- Trigger to calculate judgment correlation for groups when new judgment data is added
CREATE TRIGGER calculate_judgment_correlation_for_groups
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Update correlation cache for groups that use judgment-related parameters
    INSERT OR REPLACE INTO group_correlation_cache (
        group_id,
        correlation_result,
        last_calculated
    )
    SELECT 
        pgp.group_id,
        -- Calculate correlation based on the judgment data and group's parameter priorities
        (
            SELECT AVG(
                CASE 
                    WHEN NEW.assessment IS NOT NULL 
                    THEN ABS(NEW.assessment) * pgp.priority_value  -- Normalize assessment to 0-1 range
                    ELSE 0.5 * pgp.priority_value  -- Neutral value for NULL
                END
            )
            FROM participants_groups_parameters pgp2
            JOIN group_parameters gp ON pgp2.parameter_id = gp.id
            WHERE pgp2.group_id = pgp.group_id
            AND gp.parameter_name LIKE '%judgment%'
        ) as correlation_result,
        CURRENT_TIMESTAMP as last_calculated
    FROM participants_groups_parameters pgp
    JOIN group_parameters gp ON pgp.parameter_id = gp.id
    WHERE gp.parameter_name LIKE '%judgment%'
    AND pgp.group_id IN (
        SELECT id FROM participants_groups WHERE type = 'manual'
    );
END;
```

### 8. Trigger for Event Timeline Correlation

```sql
-- Trigger to calculate timeline correlation for groups when new timeline data is added
CREATE TRIGGER calculate_timeline_correlation_for_groups
AFTER INSERT ON event_timeline
FOR EACH ROW
BEGIN
    -- Update correlation cache for groups that use temporal synchronization parameters
    INSERT OR REPLACE INTO group_correlation_cache (
        group_id,
        correlation_result,
        last_calculated
    )
    SELECT 
        pgp.group_id,
        -- Calculate correlation based on the timeline data and group's parameter priorities
        (
            SELECT AVG(
                CASE 
                    WHEN NEW.t_start IS NOT NULL 
                    THEN (NEW.t_start % 86400) / 86400.0 * pgp.priority_value  -- Normalize to 0-1 based on time of day
                    ELSE 0.5 * pgp.priority_value  -- Neutral value for NULL
                END
            )
            FROM participants_groups_parameters pgp2
            JOIN group_parameters gp ON pgp2.parameter_id = gp.id
            WHERE pgp2.group_id = pgp.group_id
            AND gp.parameter_name LIKE '%temporal%'
        ) as correlation_result,
        CURRENT_TIMESTAMP as last_calculated
    FROM participants_groups_parameters pgp
    JOIN group_parameters gp ON pgp.parameter_id = gp.id
    WHERE gp.parameter_name LIKE '%temporal%'
    AND pgp.group_id IN (
        SELECT id FROM participants_groups WHERE type = 'manual'
    );
END;
```

### 9. Trigger for Convergence Detection Across Groups

```sql
-- Trigger to detect convergence between different group correlation results
CREATE TRIGGER detect_group_convergence
AFTER UPDATE ON group_correlation_cache
FOR EACH ROW
WHEN NEW.correlation_result IS NOT NULL AND OLD.correlation_result IS NULL
BEGIN
    -- Check for convergence with other groups that have been recently calculated
    INSERT INTO convergence_zones (
        group1_id,
        group2_id,
        result_difference,
        convergence_probability,
        detected_at
    )
    SELECT 
        NEW.group_id as group1_id,
        gcc.group_id as group2_id,
        ABS(NEW.correlation_result - gcc.correlation_result) as result_difference,
        CASE 
            WHEN ABS(NEW.correlation_result - gcc.correlation_result) < 0.05 THEN 0.9
            WHEN ABS(NEW.correlation_result - gcc.correlation_result) < 0.10 THEN 0.7
            WHEN ABS(NEW.correlation_result - gcc.correlation_result) < 0.20 THEN 0.5
            ELSE 0.1
        END as convergence_probability,
        CURRENT_TIMESTAMP as detected_at
    FROM group_correlation_cache gcc
    WHERE gcc.group_id != NEW.group_id
    AND gcc.correlation_result IS NOT NULL
    AND gcc.last_calculated > datetime('now', '-1 hour')  -- Only compare recent calculations
    AND ABS(NEW.correlation_result - gcc.correlation_result) < 0.20;  -- Only insert if somewhat similar
END;
```

### 10. Trigger for Auto-Group Stability Evaluation

```sql
-- Trigger to evaluate stability of auto-groups based on convergence patterns
CREATE TRIGGER evaluate_auto_group_stability
AFTER INSERT ON convergence_zones
FOR EACH ROW
BEGIN
    -- Update stability metrics for auto-groups based on new convergence data
    INSERT OR REPLACE INTO auto_group_stability (
        auto_group_id,
        stability_metric,
        convergence_instances,
        avg_difference,
        min_difference,
        max_difference,
        stability_status,
        last_evaluated
    )
    SELECT 
        ag.id as auto_group_id,
        -- Calculate stability as inverse of average difference (lower differences = higher stability)
        1.0 - AVG(cz.result_difference) as stability_metric,
        COUNT(cz.group1_id) as convergence_instances,
        AVG(cz.result_difference) as avg_difference,
        MIN(cz.result_difference) as min_difference,
        MAX(cz.result_difference) as max_difference,
        CASE 
            WHEN AVG(cz.result_difference) < 0.05 AND COUNT(cz.group1_id) >= 3 THEN 'STABLE'
            WHEN AVG(cz.result_difference) < 0.10 AND COUNT(cz.group1_id) >= 2 THEN 'MODERATELY_STABLE'
            WHEN AVG(cz.result_difference) < 0.20 AND COUNT(cz.group1_id) >= 1 THEN 'MINIMALLY_STABLE'
            ELSE 'UNSTABLE'
        END as stability_status,
        CURRENT_TIMESTAMP as last_evaluated
    FROM participants_groups ag
    LEFT JOIN auto_group_convergence agc ON ag.id = agc.auto_group_id
    LEFT JOIN convergence_zones cz ON (cz.group1_id = agc.manual_group_id OR cz.group2_id = agc.manual_group_id)
    WHERE ag.type = 'auto'
    AND ag.id IN (
        SELECT DISTINCT agc2.auto_group_id
        FROM auto_group_convergence agc2
        WHERE agc2.manual_group_id IN (NEW.group1_id, NEW.group2_id)
    )
    GROUP BY ag.id;
END;
```

## Fairness and Balance Guarantees

These triggers implement the following fairness mechanisms to address the architectural issue mentioned:

1. **Volume Independence**: Group size and activity volume are irrelevant to the correlation process - majority voting is not used.

2. **Priority-Based Correlation**: Correlation is performed based on priority configurations over fixed parameters rather than participant clustering.

3. **Convergence Over Volume**: Truth is evaluated as stability under variation of priorities, not as consensus, authority, or dominance.

4. **Automatic Group Formation**: Auto-groups emerge when different manual priority configurations produce convergent correlation outcomes, not based on participant count.

5. **Stability Requirements**: Auto-groups require persistent convergence across multiple evaluation cycles to maintain validity, preventing temporary fluctuations from causing immediate dissolution.

## Parameter Space Definition

The system operates within a fixed parameter space with the following parameters:

- Geographic proximity parameters (detection of network address patterns, node types classification)
- Interaction density parameters (synchronization frequency, success rates, peer interaction history)
- Temporal synchronization parameters (timing alignment of event, impact, and judgment assessments)
- Reputation similarity parameters (participant reputation score alignment)
- Context similarity parameters (shared event context usage)
- Judgment similarity parameters (alignment in event assessment patterns)
- Impact similarity parameters (alignment in consequence assessment patterns)
- Event chain similarity parameters (graph structure alignment)
- Judgment chain similarity parameters (logical relationship alignment)
- Impact chain similarity parameters (consequence relationship alignment)

## Implementation Notes

- All triggers operate on the principle that groups are analytical constructs, not authorities
- Correlation operates on results, not participants or group membership overlap
- The algorithm identifies when structurally different groups (with different priority vectors) arrive at similar correlation conclusions, indicating robustness of the findings
- Automatic groups are NOT permanent and undergo continuous re-evaluation