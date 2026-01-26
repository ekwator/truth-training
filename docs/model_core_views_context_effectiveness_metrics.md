-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Measuring Effectiveness of Different Context Classifications  

-- Measures the effectiveness of different context classifications in predicting accurate impact and judgment outcomes
-- Links: truth_event.category_id → category.id, truth_event.forma_id → forma.id, truth_event.cause_id → cause.id, truth_event.develop_id → develop.id, truth_event.effect_id → effect.id
```sql
CREATE VIEW context_effectiveness_metrics AS
SELECT
    c.id as category_id,
    c.name as category_name,
    c.description as category_description,
    -- Count events in this category
    (SELECT COUNT(*) FROM truth_event te WHERE te.category_id = c.id) as events_in_category,
    -- Average truth accuracy for this category
    (SELECT AVG(ABS(te.collective_score - 0.5)) FROM truth_event te WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as avg_truth_certainty,
    -- Average impact accuracy for this category
    (SELECT AVG(ABS(te.impact_score)) FROM truth_event te WHERE te.category_id = c.id AND te.impact_score IS NOT NULL) as avg_impact_magnitude,
    -- Correlation between context and truth outcomes
    (SELECT AVG(CASE 
        WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN forma f ON te.forma_id = f.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as forma_truth_alignment,
    -- Effectiveness of cause context
    (SELECT AVG(CASE 
        WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN cause ca ON te.cause_id = ca.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as cause_truth_alignment,
    -- Effectiveness of development context
    (SELECT AVG(CASE 
        WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN develop d ON te.develop_id = d.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as develop_truth_alignment,
    -- Effectiveness of effect context
    (SELECT AVG(CASE 
        WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN effect e ON te.effect_id = e.id
    WHERE te.category_id = c.id AND te.impact_score IS NOT NULL) as effect_impact_alignment,
    -- Overall context effectiveness score
    ((SELECT AVG(CASE 
        WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN forma f ON te.forma_id = f.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
    (SELECT AVG(CASE 
        WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN cause ca ON te.cause_id = ca.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
    (SELECT AVG(CASE 
        WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN develop d ON te.develop_id = d.id
    WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
    (SELECT AVG(CASE 
        WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    JOIN effect e ON te.effect_id = e.id
    WHERE te.category_id = c.id AND te.impact_score IS NOT NULL)) / 4.0 as overall_context_effectiveness,
    -- Variance in effectiveness across different context dimensions
    (SELECT AVG(POWER(individual_effectiveness - overall_avg, 2))
     FROM (
         SELECT 
             (SELECT AVG(CASE 
                 WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN forma f ON te.forma_id = f.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as forma_effectiveness,
             (SELECT AVG(CASE 
                 WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN cause ca ON te.cause_id = ca.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as cause_effectiveness,
             (SELECT AVG(CASE 
                 WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN develop d ON te.develop_id = d.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as develop_effectiveness,
             (SELECT AVG(CASE 
                 WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0
                 WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0
                 WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN effect e ON te.effect_id = e.id
             WHERE te.category_id = c.id AND te.impact_score IS NOT NULL) as effect_effectiveness,
             ((SELECT AVG(CASE 
                 WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN forma f ON te.forma_id = f.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
             (SELECT AVG(CASE 
                 WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN cause ca ON te.cause_id = ca.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
             (SELECT AVG(CASE 
                 WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0
                 WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0
                 WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN develop d ON te.develop_id = d.id
             WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) +
             (SELECT AVG(CASE 
                 WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0
                 WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0
                 WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                 ELSE 0.5
             END)
             FROM truth_event te
             JOIN effect e ON te.effect_id = e.id
             WHERE te.category_id = c.id AND te.impact_score IS NOT NULL)) / 4.0 as overall_avg
         FROM (SELECT 1) dummy
     ) unpivot_effectiveness
     CROSS JOIN (SELECT forma_effectiveness as individual_effectiveness FROM (SELECT (SELECT AVG(CASE WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0 WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0 WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0 ELSE 0.5 END) FROM truth_event te JOIN forma f ON te.forma_id = f.id WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as forma_effectiveness) 
     UNION ALL SELECT cause_effectiveness as individual_effectiveness FROM (SELECT (SELECT AVG(CASE WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0 WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0 WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0 ELSE 0.5 END) FROM truth_event te JOIN cause ca ON te.cause_id = ca.id WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as cause_effectiveness)
     UNION ALL SELECT develop_effectiveness as individual_effectiveness FROM (SELECT (SELECT AVG(CASE WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0 WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0 WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0 ELSE 0.5 END) FROM truth_event te JOIN develop d ON te.develop_id = d.id WHERE te.category_id = c.id AND te.collective_score IS NOT NULL) as develop_effectiveness)
     UNION ALL SELECT effect_effectiveness as individual_effectiveness FROM (SELECT (SELECT AVG(CASE WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0 WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0 WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0 ELSE 0.5 END) FROM truth_event te JOIN effect e ON te.effect_id = e.id WHERE te.category_id = c.id AND te.impact_score IS NOT NULL) as effect_effectiveness)
    ) as context_consistency_variance,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM category c;
```

-- View for analyzing effectiveness by context dimension
-- This view provides detailed analysis of each context dimension's effectiveness
```sql
CREATE VIEW context_dimension_effectiveness AS
SELECT
    'forma' as context_dimension,
    f.id as dimension_id,
    f.name as dimension_name,
    f.description as dimension_description,
    f.quality as semantic_valence,
    -- Count events with this forma
    (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = f.id) as events_with_forma,
    -- Effectiveness in predicting truth
    (SELECT AVG(CASE 
        WHEN f.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN f.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN f.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.forma_id = f.id AND te.collective_score IS NOT NULL) as truth_prediction_effectiveness,
    -- Effectiveness in predicting impact
    (SELECT AVG(CASE 
        WHEN f.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN f.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN f.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.forma_id = f.id AND te.impact_score IS NOT NULL) as impact_prediction_effectiveness,
    -- Confidence in effectiveness measure (based on sample size)
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = f.id) >= 20 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = f.id) >= 10 THEN 'MEDIUM'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = f.id) >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as confidence_level
    
FROM forma f

UNION ALL

SELECT
    'cause' as context_dimension,
    ca.id as dimension_id,
    ca.name as dimension_name,
    ca.description as dimension_description,
    ca.quality as semantic_valence,
    (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = ca.id) as events_with_cause,
    (SELECT AVG(CASE 
        WHEN ca.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN ca.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN ca.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.cause_id = ca.id AND te.collective_score IS NOT NULL) as truth_prediction_effectiveness,
    (SELECT AVG(CASE 
        WHEN ca.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN ca.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN ca.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.cause_id = ca.id AND te.impact_score IS NOT NULL) as impact_prediction_effectiveness,
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = ca.id) >= 20 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = ca.id) >= 10 THEN 'MEDIUM'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = ca.id) >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as confidence_level
FROM cause ca

UNION ALL

SELECT
    'develop' as context_dimension,
    d.id as dimension_id,
    d.name as dimension_name,
    d.description as dimension_description,
    d.quality as semantic_valence,
    (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = d.id) as events_with_develop,
    (SELECT AVG(CASE 
        WHEN d.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN d.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN d.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.develop_id = d.id AND te.collective_score IS NOT NULL) as truth_prediction_effectiveness,
    (SELECT AVG(CASE 
        WHEN d.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN d.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN d.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.develop_id = d.id AND te.impact_score IS NOT NULL) as impact_prediction_effectiveness,
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = d.id) >= 20 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = d.id) >= 10 THEN 'MEDIUM'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = d.id) >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as confidence_level
FROM develop d

UNION ALL

SELECT
    'effect' as context_dimension,
    e.id as dimension_id,
    e.name as dimension_name,
    e.description as dimension_description,
    e.quality as semantic_valence,
    (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = e.id) as events_with_effect,
    (SELECT AVG(CASE 
        WHEN e.quality = 1 AND te.collective_score > 0.6 THEN 1.0
        WHEN e.quality = 0 AND te.collective_score < 0.4 THEN 1.0
        WHEN e.quality != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.effect_id = e.id AND te.collective_score IS NOT NULL) as truth_prediction_effectiveness,
    (SELECT AVG(CASE 
        WHEN e.quality = 1 AND te.impact_score > 0 THEN 1.0
        WHEN e.quality = 0 AND te.impact_score < 0 THEN 1.0
        WHEN e.quality != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
        ELSE 0.5
    END)
    FROM truth_event te
    WHERE te.effect_id = e.id AND te.impact_score IS NOT NULL) as impact_prediction_effectiveness,
    CASE
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = e.id) >= 20 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = e.id) >= 10 THEN 'MEDIUM'
        WHEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = e.id) >= 5 THEN 'LOW'
        ELSE 'VERY_LOW'
    END as confidence_level
FROM effect e;
```

-- View for ranking context elements by effectiveness
-- This view ranks context elements based on their predictive power
```sql
CREATE VIEW context_effectiveness_rankings AS
SELECT
    cde.context_dimension,
    cde.dimension_id,
    cde.dimension_name,
    cde.semantic_valence,
    cde.events_with_dimension,
    cde.truth_prediction_effectiveness,
    cde.impact_prediction_effectiveness,
    -- Combined effectiveness score
    (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 as combined_effectiveness,
    cde.confidence_level,
    -- Effectiveness rank within dimension
    ROW_NUMBER() OVER (
        PARTITION BY cde.context_dimension 
        ORDER BY (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 DESC
    ) as effectiveness_rank_in_dimension,
    -- Overall effectiveness rank
    ROW_NUMBER() OVER (
        ORDER BY (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 DESC
    ) as overall_effectiveness_rank,
    -- Effectiveness category
    CASE
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.8 THEN 'HIGHLY_EFFECTIVE'
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.6 THEN 'EFFECTIVE'
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.4 THEN 'MODERATELY_EFFECTIVE'
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.2 THEN 'SLIGHTLY_EFFECTIVE'
        ELSE 'INEFFECTIVE'
    END as effectiveness_category,
    -- Recommendation based on effectiveness
    CASE
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.8 AND cde.events_with_dimension >= 10 THEN 'USE_FREQUENTLY'
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 >= 0.6 AND cde.events_with_dimension >= 5 THEN 'USE_SELECTIVELY'
        WHEN (COALESCE(cde.truth_prediction_effectiveness, 0.5) + COALESCE(cde.impact_prediction_effectiveness, 0.5)) / 2.0 <= 0.3 AND cde.events_with_dimension >= 10 THEN 'REVIEW_CONTEXT_DEFINITION'
        WHEN cde.events_with_dimension < 5 THEN 'INSUFFICIENT_DATA_FOR_RECOMMENDATION'
        ELSE 'USE_CAUTIOUSLY'
    END as usage_recommendation
FROM (
    SELECT
        context_dimension,
        dimension_id,
        dimension_name,
        CASE context_dimension
            WHEN 'forma' THEN (SELECT name FROM forma WHERE id = dimension_id)
            WHEN 'cause' THEN (SELECT name FROM cause WHERE id = dimension_id)
            WHEN 'develop' THEN (SELECT name FROM develop WHERE id = dimension_id)
            WHEN 'effect' THEN (SELECT name FROM effect WHERE id = dimension_id)
        END as dimension_name,
        CASE context_dimension
            WHEN 'forma' THEN (SELECT quality FROM forma WHERE id = dimension_id)
            WHEN 'cause' THEN (SELECT quality FROM cause WHERE id = dimension_id)
            WHEN 'develop' THEN (SELECT quality FROM develop WHERE id = dimension_id)
            WHEN 'effect' THEN (SELECT quality FROM effect WHERE id = dimension_id)
        END as semantic_valence,
        CASE context_dimension
            WHEN 'forma' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = dimension_id)
            WHEN 'cause' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = dimension_id)
            WHEN 'develop' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = dimension_id)
            WHEN 'effect' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = dimension_id)
        END as events_with_dimension,
        CASE context_dimension
            WHEN 'forma' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) = 1 AND te.collective_score > 0.6 THEN 1.0
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) = 0 AND te.collective_score < 0.4 THEN 1.0
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.forma_id = dimension_id AND te.collective_score IS NOT NULL
            )
            WHEN 'cause' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) = 1 AND te.collective_score > 0.6 THEN 1.0
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) = 0 AND te.collective_score < 0.4 THEN 1.0
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.cause_id = dimension_id AND te.collective_score IS NOT NULL
            )
            WHEN 'develop' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) = 1 AND te.collective_score > 0.6 THEN 1.0
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) = 0 AND te.collective_score < 0.4 THEN 1.0
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) != COALESCE(CASE WHEN te.collective_score > 0.5 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.develop_id = dimension_id AND te.collective_score IS NOT NULL
            )
            WHEN 'effect' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) = 1 AND te.impact_score > 0 THEN 1.0
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) = 0 AND te.impact_score < 0 THEN 1.0
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.effect_id = dimension_id AND te.impact_score IS NOT NULL
            )
        END as truth_prediction_effectiveness,
        CASE context_dimension
            WHEN 'forma' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) = 1 AND te.impact_score > 0 THEN 1.0
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) = 0 AND te.impact_score < 0 THEN 1.0
                    WHEN (SELECT quality FROM forma WHERE id = dimension_id) != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.forma_id = dimension_id AND te.impact_score IS NOT NULL
            )
            WHEN 'cause' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) = 1 AND te.impact_score > 0 THEN 1.0
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) = 0 AND te.impact_score < 0 THEN 1.0
                    WHEN (SELECT quality FROM cause WHERE id = dimension_id) != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.cause_id = dimension_id AND te.impact_score IS NOT NULL
            )
            WHEN 'develop' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) = 1 AND te.impact_score > 0 THEN 1.0
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) = 0 AND te.impact_score < 0 THEN 1.0
                    WHEN (SELECT quality FROM develop WHERE id = dimension_id) != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.develop_id = dimension_id AND te.impact_score IS NOT NULL
            )
            WHEN 'effect' THEN (
                SELECT AVG(CASE 
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) = 1 AND te.impact_score > 0 THEN 1.0
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) = 0 AND te.impact_score < 0 THEN 1.0
                    WHEN (SELECT quality FROM effect WHERE id = dimension_id) != COALESCE(CASE WHEN te.impact_score > 0 THEN 1 ELSE 0 END, -1) THEN 0.0
                    ELSE 0.5
                END)
                FROM truth_event te
                WHERE te.effect_id = dimension_id AND te.impact_score IS NOT NULL
            )
        END as impact_prediction_effectiveness,
        CASE
            WHEN (CASE context_dimension
                WHEN 'forma' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = dimension_id)
                WHEN 'cause' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = dimension_id)
                WHEN 'develop' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = dimension_id)
                WHEN 'effect' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = dimension_id)
            END) >= 20 THEN 'HIGH'
            WHEN (CASE context_dimension
                WHEN 'forma' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = dimension_id)
                WHEN 'cause' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = dimension_id)
                WHEN 'develop' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = dimension_id)
                WHEN 'effect' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = dimension_id)
            END) >= 10 THEN 'MEDIUM'
            WHEN (CASE context_dimension
                WHEN 'forma' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.forma_id = dimension_id)
                WHEN 'cause' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.cause_id = dimension_id)
                WHEN 'develop' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.develop_id = dimension_id)
                WHEN 'effect' THEN (SELECT COUNT(*) FROM truth_event te WHERE te.effect_id = dimension_id)
            END) >= 5 THEN 'LOW'
            ELSE 'VERY_LOW'
        END as confidence_level
    FROM (
        SELECT 'forma' as context_dimension, id as dimension_id FROM forma
        UNION ALL SELECT 'cause' as context_dimension, id as dimension_id FROM cause
        UNION ALL SELECT 'develop' as context_dimension, id as dimension_id FROM develop
        UNION ALL SELECT 'effect' as context_dimension, id as dimension_id FROM effect
    ) all_dimensions
) cde
ORDER BY overall_effectiveness_rank;
```

-- View for identifying ineffective contexts
-- This view highlights context elements that are not predictive of outcomes
```sql
CREATE VIEW ineffective_context_identification AS
SELECT
    cer.context_dimension,
    cer.dimension_id,
    cer.dimension_name,
    cer.semantic_valence,
    cer.events_with_dimension,
    cer.truth_prediction_effectiveness,
    cer.impact_prediction_effectiveness,
    cer.combined_effectiveness,
    cer.effectiveness_category,
    cer.usage_recommendation,
    -- Reason for ineffectiveness
    CASE
        WHEN cer.combined_effectiveness < 0.3 AND cer.events_with_dimension >= 10 THEN 'POOR_PREDICTIVE_POWER'
        WHEN cer.confidence_level = 'VERY_LOW' AND cer.events_with_dimension < 5 THEN 'INSUFFICIENT_SAMPLE_SIZE'
        WHEN ABS(cer.truth_prediction_effectiveness - cer.impact_prediction_effectiveness) > 0.4 THEN 'DIMENSION_INCONSISTENCY'
        ELSE 'MISALIGNED_WITH_OUTCOMES'
    END as ineffectiveness_reason,
    -- Suggested action
    CASE
        WHEN cer.combined_effectiveness < 0.2 AND cer.events_with_dimension >= 10 THEN 'CONSIDER_REMOVING_OR_REDEFINING'
        WHEN cer.confidence_level = 'VERY_LOW' THEN 'COLLECT_MORE_DATA_BEFORE_ASSESSING'
        WHEN ABS(cer.truth_prediction_effectiveness - cer.impact_prediction_effectiveness) > 0.4 THEN 'INVESTIGATE_DIMENSION_SPLITTING'
        WHEN cer.semantic_valence = 1 AND cer.truth_prediction_effectiveness < 0.4 THEN 'REVIEW_POSITIVE_QUALITY_ASSIGNMENT'
        WHEN cer.semantic_valence = 0 AND cer.truth_prediction_effectiveness < 0.4 THEN 'REVIEW_NEGATIVE_QUALITY_ASSIGNMENT'
        ELSE 'MONITOR_CONTINUOUSLY'
    END as suggested_action,
    -- Impact on system
    CASE
        WHEN cer.combined_effectiveness < 0.2 THEN 'HIGH_NEGATIVE_IMPACT'
        WHEN cer.combined_effectiveness < 0.4 THEN 'MODERATE_NEGATIVE_IMPACT'
        ELSE 'LOW_NEGATIVE_IMPACT'
    END as system_impact_level
FROM context_effectiveness_rankings cer
WHERE cer.combined_effectiveness < 0.4  -- Below moderate effectiveness
ORDER BY cer.combined_effectiveness ASC;