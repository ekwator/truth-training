# Truth Event Impact Correlation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view calculates correlation between truth_event collective_score and impact assessments to identify patterns in how truth assessments relate to impact observations, implementing the relationship analysis described in section 2.5.

## Purpose
The `truth_event_impact_correlation` view analyzes the relationship between truth assessments (collective_score) and impact assessments to identify patterns, correlations, and potential misalignments between these two orthogonal axes of evaluation. This helps understand how truth and consequence assessments relate to each other in the system.

## SQL Implementation

```sql
-- View to calculate correlation between truth_event collective_score and impact assessments
CREATE VIEW truth_event_impact_correlation AS
SELECT 
    te.id AS event_id,
    te.description AS event_description,
    te.global_id AS event_global_id,
    te.participant_id AS event_creator_id,
    p.public_key AS event_creator_public_key,
    
    -- Truth metrics
    te.collective_score AS truth_score,
    te.impact_score AS impact_score_from_event,
    te.judgment_score AS judgment_score_from_event,
    
    -- Impact metrics
    im.total_magnitude AS impact_magnitude,
    im.positive_ratio AS positive_impact_ratio,
    im.negative_ratio AS negative_impact_ratio,
    im.uncertainty AS uncertain_impact_ratio,
    
    -- Direct correlation calculation
    (
        SELECT 
            CASE 
                WHEN COUNT(*) > 1 THEN
                    (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                    SQRT(
                        (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                        (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                    )
                ELSE 0
            END
        FROM impact i
        WHERE i.event_id = (
            SELECT event_ci.id
            FROM event_ci
            WHERE event_ci.created_by = te.id
        )
    ) AS truth_impact_correlation_coefficient,
    
    -- Average impact value for this event
    (
        SELECT AVG(value)
        FROM impact
        WHERE event_id = (
            SELECT event_ci.id
            FROM event_ci
            WHERE event_ci.created_by = te.id
        )
    ) AS avg_impact_value,
    
    -- Standard deviation of impact values
    (
        SELECT SQRT(AVG(POWER(value - avg_val, 2)))
        FROM (
            SELECT value, 
            (SELECT AVG(value) FROM impact WHERE event_id = (
                SELECT event_ci.id FROM event_ci WHERE event_ci.created_by = te.id
            )) AS avg_val
            FROM impact
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        )
    ) AS impact_std_deviation,
    
    -- Number of impact assessments for this event
    (
        SELECT COUNT(*)
        FROM impact
        WHERE event_id = (
            SELECT event_ci.id
            FROM event_ci
            WHERE event_ci.created_by = te.id
        )
    ) AS impact_assessment_count,
    
    -- Number of judgment assessments for this event
    (
        SELECT COUNT(*)
        FROM judgment
        WHERE event_id = (
            SELECT event_ci.id
            FROM event_ci
            WHERE event_ci.created_by = te.id
        )
    ) AS judgment_assessment_count,
    
    -- Consensus confidence for this event
    (
        SELECT confidence_score
        FROM consensus_ci
        WHERE event_id = (
            SELECT event_ci.id
            FROM event_ci
            WHERE event_ci.created_by = te.id
        )
    ) AS consensus_confidence,
    
    -- Event classification based on correlation
    CASE 
        WHEN ABS(
            (SELECT 
                CASE 
                    WHEN COUNT(*) > 1 THEN
                        (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                        SQRT(
                            (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                            (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                        )
                    ELSE 0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            ))
        ) > 0.7 THEN 'STRONG_CORRELATION'
        WHEN ABS(
            (SELECT 
                CASE 
                    WHEN COUNT(*) > 1 THEN
                        (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                        SQRT(
                            (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                            (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                        )
                    ELSE 0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            ))
        ) > 0.3 THEN 'MODERATE_CORRELATION'
        WHEN ABS(
            (SELECT 
                CASE 
                    WHEN COUNT(*) > 1 THEN
                        (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                        SQRT(
                            (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                            (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                        )
                    ELSE 0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            ))
        ) > 0.1 THEN 'WEAK_CORRELATION'
        ELSE 'NO_CORRELATION'
    END AS correlation_strength,
    
    -- Direction of correlation
    CASE 
        WHEN (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 1 THEN
                        (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                        SQRT(
                            (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                            (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                        )
                    ELSE 0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) > 0 THEN 'POSITIVE'
        WHEN (
            SELECT 
                CASE 
                    WHEN COUNT(*) > 1 THEN
                        (COUNT(*) * SUM(te.collective_score * i.value) - SUM(te.collective_score) * SUM(i.value)) /
                        SQRT(
                            (COUNT(*) * SUM(POWER(te.collective_score, 2)) - POWER(SUM(te.collective_score), 2)) *
                            (COUNT(*) * SUM(POWER(i.value, 2)) - POWER(SUM(i.value), 2))
                        )
                    ELSE 0
                END
            FROM impact i
            WHERE i.event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) < 0 THEN 'NEGATIVE'
        ELSE 'NONE'
    END AS correlation_direction,
    
    -- Misalignment detection (high truth score with low impact or vice versa)
    CASE 
        WHEN te.collective_score > 0.7 AND (
            SELECT AVG(value) 
            FROM impact 
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) < 0.3 THEN 'TRUTH_HIGH_IMPACT_LOW'
        WHEN te.collective_score < 0.3 AND (
            SELECT AVG(value) 
            FROM impact 
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) > 0.7 THEN 'TRUTH_LOW_IMPACT_HIGH'
        ELSE 'ALIGNED'
    END AS alignment_status,
    
    -- Confidence in correlation (based on number of assessments)
    CASE 
        WHEN (
            SELECT COUNT(*)
            FROM impact
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 10 AND (
            SELECT COUNT(*)
            FROM judgment
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 10 THEN 'HIGH'
        WHEN (
            SELECT COUNT(*)
            FROM impact
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 5 AND (
            SELECT COUNT(*)
            FROM judgment
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 5 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS correlation_confidence,
    
    -- Context information
    cat.name AS category_name,
    f.name AS forma_name,
    ca.name AS cause_name,
    d.name AS develop_name,
    e.name AS effect_name,
    
    -- Timeline information
    et.time_axis_id AS event_time_axis,
    et.t_start AS event_start_time,
    et.t_end AS event_end_time,
    
    -- Participant reputation at time of event creation
    p.reputation_score AS creator_reputation,
    
    -- Temporal distance metrics
    julianday('now') - julianday(te.created_at, 'unixepoch') AS days_since_event_creation,
    
    -- Quadrant classification
    CASE 
        WHEN te.collective_score >= 0.5 AND (
            SELECT AVG(value) 
            FROM impact 
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 0 THEN 'Q1'  -- High truth, High impact
        WHEN te.collective_score >= 0.5 AND (
            SELECT AVG(value) 
            FROM impact 
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) < 0 THEN 'Q2'   -- High truth, Low impact
        WHEN te.collective_score < 0.5 AND (
            SELECT AVG(value) 
            FROM impact 
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) >= 0 THEN 'Q3'   -- Low truth, High impact
        ELSE 'Q4'                                                      -- Low truth, Low impact
    END AS event_quadrant,
    
    -- Impact trend analysis
    (
        SELECT 
            CASE 
                WHEN COUNT(*) > 1 THEN
                    (COUNT(*) * SUM(row_num * value) - SUM(row_num) * SUM(value)) /
                    (COUNT(*) * SUM(POWER(row_num, 2)) - POWER(SUM(row_num), 2))
                ELSE 0
            END
        FROM (
            SELECT 
                value,
                ROW_NUMBER() OVER (ORDER BY created_at) AS row_num
            FROM impact
            WHERE event_id = (
                SELECT event_ci.id
                FROM event_ci
                WHERE event_ci.created_by = te.id
            )
        ) ranked_impacts
    ) AS impact_trend_slope,
    
    -- Timestamp of correlation calculation
    CURRENT_TIMESTAMP AS correlation_calculated_at

FROM truth_event te
JOIN participants p ON te.participant_id = p.id
LEFT JOIN category cat ON te.category_id = cat.id
LEFT JOIN forma f ON te.forma_id = f.id
LEFT JOIN cause ca ON te.cause_id = ca.id
LEFT JOIN develop d ON te.develop_id = d.id
LEFT JOIN effect e ON te.effect_id = e.id
LEFT JOIN event_timeline et ON te.timeline_id = et.id
LEFT JOIN impact_metrics im ON im.event_id = (
    SELECT event_ci.id
    FROM event_ci
    WHERE event_ci.created_by = te.id
);

-- View for correlation analysis across all events
CREATE VIEW truth_impact_correlation_analysis AS
SELECT 
    -- Overall correlation statistics
    AVG(truth_impact_correlation_coefficient) AS average_correlation,
    MIN(truth_impact_correlation_coefficient) AS min_correlation,
    MAX(truth_impact_correlation_coefficient) AS max_correlation,
    SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) AS correlation_std_deviation,
    (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr,
    
    -- Distribution of correlation strengths
    SUM(CASE WHEN correlation_strength = 'STRONG_CORRELATION' THEN 1 ELSE 0 END) AS strong_correlation_events,
    SUM(CASE WHEN correlation_strength = 'MODERATE_CORRELATION' THEN 1 ELSE 0 END) AS moderate_correlation_events,
    SUM(CASE WHEN correlation_strength = 'WEAK_CORRELATION' THEN 1 ELSE 0 END) AS weak_correlation_events,
    SUM(CASE WHEN correlation_strength = 'NO_CORRELATION' THEN 1 ELSE 0 END) AS no_correlation_events,
    
    -- Distribution of alignment statuses
    SUM(CASE WHEN alignment_status = 'TRUTH_HIGH_IMPACT_LOW' THEN 1 ELSE 0 END) AS truth_high_impact_low_events,
    SUM(CASE WHEN alignment_status = 'TRUTH_LOW_IMPACT_HIGH' THEN 1 ELSE 0 END) AS truth_low_impact_high_events,
    SUM(CASE WHEN alignment_status = 'ALIGNED' THEN 1 ELSE 0 END) AS aligned_events,
    
    -- Direction distribution
    SUM(CASE WHEN correlation_direction = 'POSITIVE' THEN 1 ELSE 0 END) AS positive_correlation_events,
    SUM(CASE WHEN correlation_direction = 'NEGATIVE' THEN 1 ELSE 0 END) AS negative_correlation_events,
    SUM(CASE WHEN correlation_direction = 'NONE' THEN 1 ELSE 0 END) AS neutral_correlation_events,
    
    -- Quadrant distribution
    SUM(CASE WHEN event_quadrant = 'Q1' THEN 1 ELSE 0 END) AS q1_events,
    SUM(CASE WHEN event_quadrant = 'Q2' THEN 1 ELSE 0 END) AS q2_events,
    SUM(CASE WHEN event_quadrant = 'Q3' THEN 1 ELSE 0 END) AS q3_events,
    SUM(CASE WHEN event_quadrant = 'Q4' THEN 1 ELSE 0 END) AS q4_events,
    
    -- Confidence distribution
    SUM(CASE WHEN correlation_confidence = 'HIGH' THEN 1 ELSE 0 END) AS high_confidence_correlations,
    SUM(CASE WHEN correlation_confidence = 'MEDIUM' THEN 1 ELSE 0 END) AS medium_confidence_correlations,
    SUM(CASE WHEN correlation_confidence = 'LOW' THEN 1 ELSE 0 END) AS low_confidence_correlations,
    
    -- Count of events analyzed
    COUNT(*) AS total_events_analyzed,
    
    -- Percentage calculations
    SUM(CASE WHEN correlation_strength = 'STRONG_CORRELATION' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS strong_correlation_percentage,
    SUM(CASE WHEN correlation_strength = 'MODERATE_CORRELATION' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS moderate_correlation_percentage,
    SUM(CASE WHEN correlation_strength = 'WEAK_CORRELATION' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS weak_correlation_percentage,
    SUM(CASE WHEN correlation_strength = 'NO_CORRELATION' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS no_correlation_percentage,
    
    -- Misalignment percentage
    (SUM(CASE WHEN alignment_status != 'ALIGNED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) AS misalignment_percentage,
    
    -- Average metrics
    AVG(truth_score) AS average_truth_score,
    AVG(impact_score_from_event) AS average_impact_score,
    AVG(avg_impact_value) AS average_overall_impact_value,
    AVG(impact_assessment_count) AS average_impact_assessments_per_event,
    AVG(judgment_assessment_count) AS average_judgment_assessments_per_event,
    
    -- Creator reputation metrics
    AVG(creator_reputation) AS average_creator_reputation,
    MIN(creator_reputation) AS min_creator_reputation,
    MAX(creator_reputation) AS max_creator_reputation,
    
    -- Temporal metrics
    AVG(days_since_event_creation) AS average_days_since_creation,
    MIN(days_since_event_creation) AS min_days_since_creation,
    MAX(days_since_event_creation) AS max_days_since_creation,
    
    -- Trend metrics
    AVG(impact_trend_slope) AS average_impact_trend_slope,
    MIN(impact_trend_slope) AS min_impact_trend_slope,
    MAX(impact_trend_slope) AS max_impact_trend_slope,
    
    -- Timestamp of analysis
    CURRENT_TIMESTAMP AS analysis_calculated_at

FROM truth_event_impact_correlation;

-- View for identifying outliers and anomalies in truth-impact correlation
CREATE VIEW truth_impact_correlation_outliers AS
SELECT 
    event_id,
    event_description,
    truth_score,
    avg_impact_value,
    truth_impact_correlation_coefficient,
    correlation_strength,
    alignment_status,
    correlation_confidence,
    
    -- Z-score for correlation coefficient (to identify extreme values)
    (truth_impact_correlation_coefficient - (
        SELECT AVG(truth_impact_correlation_coefficient) 
        FROM truth_event_impact_correlation
    )) / (
        SELECT SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) 
        FROM (
            SELECT truth_impact_correlation_coefficient, 
            (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr
            FROM truth_event_impact_correlation
        )
    ) AS correlation_z_score,
    
    -- Identify outliers based on z-score
    CASE 
        WHEN ABS(
            (truth_impact_correlation_coefficient - (
                SELECT AVG(truth_impact_correlation_coefficient) 
                FROM truth_event_impact_correlation
            )) / (
                SELECT SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) 
                FROM (
                    SELECT truth_impact_correlation_coefficient, 
                    (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr
                    FROM truth_event_impact_correlation
                )
            )
        ) > 2 THEN 'EXTREME_OUTLIER'
        WHEN ABS(
            (truth_impact_correlation_coefficient - (
                SELECT AVG(truth_impact_correlation_coefficient) 
                FROM truth_event_impact_correlation
            )) / (
                SELECT SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) 
                FROM (
                    SELECT truth_impact_correlation_coefficient, 
                    (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr
                    FROM truth_event_impact_correlation
                )
            )
        ) > 1.5 THEN 'MODERATE_OUTLIER'
        ELSE 'NORMAL'
    END AS outlier_classification,
    
    -- Significance of outlier (based on confidence and extremity)
    CASE 
        WHEN ABS(truth_impact_correlation_coefficient) > 0.8 AND correlation_confidence = 'HIGH' THEN 'HIGH_SIGNIFICANCE'
        WHEN ABS(truth_impact_correlation_coefficient) > 0.6 AND correlation_confidence IN ('HIGH', 'MEDIUM') THEN 'MEDIUM_SIGNIFICANCE'
        WHEN ABS(truth_impact_correlation_coefficient) > 0.4 THEN 'LOW_SIGNIFICANCE'
        ELSE 'INSIGNIFICANT'
    END AS outlier_significance,
    
    -- Potential cause of misalignment
    CASE 
        WHEN alignment_status = 'TRUTH_HIGH_IMPACT_LOW' AND event_quadrant = 'Q2' THEN 'HISTORICAL_FACT_WITH_LOW_IMPACT'
        WHEN alignment_status = 'TRUTH_LOW_IMPACT_HIGH' AND event_quadrant = 'Q3' THEN 'DANGEROUS_MISINFORMATION'
        WHEN correlation_strength = 'NO_CORRELATION' AND impact_assessment_count > 5 THEN 'COMPLEX_EVENT_WITH_MULTIPLE_INTERPRETATIONS'
        WHEN correlation_strength = 'NO_CORRELATION' AND judgment_assessment_count > 5 THEN 'CONTROVERSIAL_TOPIC_WITH_DIVERSE_VIEWS'
        ELSE 'NEEDS_INVESTIGATION'
    END AS potential_cause,
    
    -- Priority for review based on outlier significance and potential impact
    CASE 
        WHEN outlier_classification = 'EXTREME_OUTLIER' AND outlier_significance = 'HIGH_SIGNIFICANCE' THEN 'CRITICAL'
        WHEN outlier_classification = 'MODERATE_OUTLIER' AND outlier_significance = 'HIGH_SIGNIFICANCE' THEN 'HIGH'
        WHEN outlier_significance = 'MEDIUM_SIGNIFICANCE' THEN 'MEDIUM'
        WHEN outlier_significance = 'LOW_SIGNIFICANCE' THEN 'LOW'
        ELSE 'MONITOR'
    END AS review_priority,
    
    event_creator_public_key,
    event_quadrant,
    impact_assessment_count,
    judgment_assessment_count,
    creator_reputation,
    
    correlation_calculated_at

FROM truth_event_impact_correlation
WHERE 
    ABS(
        (truth_impact_correlation_coefficient - (
            SELECT AVG(truth_impact_correlation_coefficient) 
            FROM truth_event_impact_correlation
        )) / (
            SELECT SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) 
            FROM (
                SELECT truth_impact_correlation_coefficient, 
                (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr
                FROM truth_event_impact_correlation
            )
        )
    ) > 1.5  -- Focus on moderate to extreme outliers
ORDER BY ABS(truth_impact_correlation_coefficient) DESC;

-- View for correlation trend analysis over time
CREATE VIEW truth_impact_correlation_trends AS
SELECT 
    -- Rolling correlation averages
    (SELECT AVG(truth_impact_correlation_coefficient) 
     FROM truth_event_impact_correlation 
     WHERE days_since_event_creation <= 1) AS correlation_last_day,
     
    (SELECT AVG(truth_impact_correlation_coefficient) 
     FROM truth_event_impact_correlation 
     WHERE days_since_event_creation BETWEEN 1 AND 7) AS correlation_last_week,
     
    (SELECT AVG(truth_impact_correlation_coefficient) 
     FROM truth_event_impact_correlation 
     WHERE days_since_event_creation BETWEEN 7 AND 30) AS correlation_last_month,
     
    (SELECT AVG(truth_impact_correlation_coefficient) 
     FROM truth_event_impact_correlation 
     WHERE days_since_event_creation BETWEEN 30 AND 90) AS correlation_last_quarter,
    
    -- Trend direction
    CASE 
        WHEN (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation <= 1) > 
             (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 1 AND 7) THEN 'IMPROVING'
        WHEN (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation <= 1) < 
             (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 1 AND 7) THEN 'DECLINING'
        ELSE 'STABLE'
    END AS correlation_trend_short_term,
    
    CASE 
        WHEN (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 1 AND 7) > 
             (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 7 AND 30) THEN 'IMPROVING'
        WHEN (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 1 AND 7) < 
             (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 7 AND 30) THEN 'DECLINING'
        ELSE 'STABLE'
    END AS correlation_trend_medium_term,
    
    -- Count of events in each time period
    (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation <= 1) AS events_last_day,
    (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 1 AND 7) AS events_last_week,
    (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 7 AND 30) AS events_last_month,
    (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation BETWEEN 30 AND 90) AS events_last_quarter,
    
    -- Stability of correlation over time
    SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) AS correlation_volatility,
    (SELECT SQRT(AVG(POWER(truth_impact_correlation_coefficient - avg_corr, 2))) 
     FROM (
         SELECT truth_impact_correlation_coefficient, 
         (SELECT AVG(truth_impact_correlation_coefficient) FROM truth_event_impact_correlation) AS avg_corr
         FROM truth_event_impact_correlation
     )) AS avg_corr,
    
    -- Confidence in trend based on number of events
    CASE 
        WHEN (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation <= 7) >= 20 THEN 'HIGH'
        WHEN (SELECT COUNT(*) FROM truth_event_impact_correlation WHERE days_since_event_creation <= 7) >= 10 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS trend_confidence,
    
    -- Overall system maturity indicator
    CASE 
        WHEN (SELECT COUNT(*) FROM truth_event_impact_correlation) >= 1000 THEN 'MATURING'
        WHEN (SELECT COUNT(*) FROM truth_event_impact_correlation) >= 500 THEN 'GROWING'
        WHEN (SELECT COUNT(*) FROM truth_event_impact_correlation) >= 100 THEN 'ESTABLISHING'
        ELSE 'INITIAL'
    END AS system_maturity_stage,
    
    -- Timestamp of trend analysis
    CURRENT_TIMESTAMP AS trends_calculated_at

FROM truth_event_impact_correlation;
```

## Key Features

### Correlation Analysis
Calculates Pearson correlation coefficient between truth scores and impact values to quantify the relationship between these two axes.

### Strength Classification
Classifies correlations into strength categories (Strong, Moderate, Weak, None) based on the correlation coefficient magnitude.

### Alignment Detection
Identifies events where truth and impact assessments are misaligned (high truth score with low impact or vice versa).

### Outlier Identification
Uses z-scores to identify events with extreme correlation values that may require special attention.

### Trend Analysis
Analyzes how correlations change over time to identify patterns and trends in the relationship between truth and impact axes.

### Confidence Assessment
Evaluates the confidence level of correlation calculations based on the number of assessments available.

### Quadrant Integration
Connects correlation analysis with the quadrant classification system to provide context for correlation patterns.

## Relationship to Model Core
This view implements the truth-impact relationship analysis described in the model where:
- Truth and impact axes are orthogonal but may show correlation in specific contexts
- Misalignments between axes indicate interesting cases for investigation
- The system tracks how truth and impact assessments relate to each other over time
- Correlation analysis supports the identification of patterns and anomalies in assessment behavior
- Understanding the relationship between truth and impact axes helps optimize the collective intelligence process

## Usage Examples

```sql
-- Get correlation analysis for a specific event
SELECT * FROM truth_event_impact_correlation WHERE event_id = ?;

-- Get overall correlation statistics
SELECT * FROM truth_impact_correlation_analysis;

-- Find outlier events with unusual truth-impact relationships
SELECT * FROM truth_impact_correlation_outliers 
WHERE outlier_classification = 'EXTREME_OUTLIER' 
ORDER BY ABS(truth_impact_correlation_coefficient) DESC;

-- Check correlation trends over time
SELECT * FROM truth_impact_correlation_trends;

-- Find events with high truth but low impact (potential historical facts)
SELECT * FROM truth_event_impact_correlation 
WHERE alignment_status = 'TRUTH_HIGH_IMPACT_LOW' 
AND event_quadrant = 'Q2';

-- Find events with low truth but high impact (potential dangerous misinformation)
SELECT * FROM truth_event_impact_correlation 
WHERE alignment_status = 'TRUTH_LOW_IMPACT_HIGH' 
AND event_quadrant = 'Q3';
```

## Integration with Other Components
- Works with `truth_event` and `impact` tables to get assessment data
- Connects to `consensus_ci` for confidence scoring
- Integrates with `event_projection` through quadrant classification
- Supports `event_stability` analysis by identifying stable correlation patterns
- Feeds into `group_ratings` for analytical group formation based on correlation patterns
- Used in `participant_reputation` analysis to identify assessors with unusual patterns

## Notes
- The correlation calculation uses the standard Pearson product-moment correlation coefficient
- Confidence in correlation is based on the number of assessments available
- Outlier detection helps identify events that may need special review
- The view provides both individual event analysis and aggregate system statistics
- Temporal analysis helps identify trends in the truth-impact relationship over time