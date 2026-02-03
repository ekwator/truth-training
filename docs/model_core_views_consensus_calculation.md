# Consensus Calculation View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view calculates consensus values based on participant judgments and impact assessments, implementing the aggregation function that computes collective agreement on event truth and impact values as described in section 2.6.4.

## Purpose
The `consensus_calculation` view computes the collective intelligence state by aggregating individual participant contributions into a unified consensus value that represents the collective agreement on truth and impact values for events.

## SQL Implementation

```sql
-- View to calculate consensus values based on participant judgments and impact assessments
CREATE VIEW consensus_calculation AS
SELECT 
    event_ci.id AS event_id,
    -- Calculate consensus value based on participant judgments and impact assessments
    (
        SELECT ROUND(AVG(j.assessment_value * jw.weight))
        FROM judgment j
        JOIN judgment_weights jw ON j.participant_id = jw.participant_id
        WHERE j.event_id = event_ci.id
    ) AS consensus_value,
    
    -- Calculate confidence score in the consensus
    (
        SELECT AVG(j.confidence_level * jw.weight)
        FROM judgment j
        JOIN judgment_weights jw ON j.participant_id = jw.participant_id
        WHERE j.event_id = event_ci.id
    ) AS confidence_score,
    
    -- Count of participants involved in consensus
    (
        SELECT COUNT(DISTINCT participant_id)
        FROM judgment
        WHERE event_id = event_ci.id
    ) AS participant_count,
    
    -- Calculate impact-based consensus
    (
        SELECT AVG(i.value * p.reputation_score)
        FROM impact i
        JOIN truth_event te ON i.event_id = te.id
        JOIN participants p ON te.participant_id = p.id
        WHERE i.event_id = (
            SELECT created_by FROM event_ci WHERE id = event_ci.id
        )
    ) AS impact_consensus,
    
    -- Overall consensus combining both axes
    (
        SELECT 
            COALESCE(cc.confidence_score, 0) * 0.5 + 
            COALESCE(ic.impact_score, 0) * 0.5
        FROM consensus_ci cc
        LEFT JOIN (
            SELECT 
                te.id as event_id,
                AVG(i.value * pr.reputation_score) as impact_score
            FROM truth_event te
            JOIN impact i ON te.id = i.event_id
            JOIN participants pr ON te.participant_id = pr.id
            GROUP BY te.id
        ) ic ON cc.event_id = ic.event_id
        WHERE cc.event_id = event_ci.id
    ) AS combined_consensus,
    
    CURRENT_TIMESTAMP AS calculated_at
FROM event_ci;

-- Alternative view for real-time consensus calculation
CREATE VIEW real_time_consensus AS
SELECT 
    ec.event_id,
    ec.consensus_value,
    ec.confidence_score,
    ec.participant_count,
    CASE 
        WHEN ec.confidence_score > 0.7 AND ec.participant_count >= 3 THEN 'HIGH'
        WHEN ec.confidence_score > 0.5 AND ec.participant_count >= 2 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS consensus_quality,
    ec.calculated_at
FROM consensus_calculation ec;
```

## Key Features

### Consensus Value Calculation
The view calculates the consensus value by taking the weighted average of all judgments, where weights are determined by participant reputation scores.

### Confidence Scoring
Confidence in the consensus is calculated based on both the confidence levels expressed by participants and their reputation weights.

### Impact Integration
The view also incorporates impact assessments to provide a more holistic consensus that considers both truth and consequence axes.

### Quality Assessment
The view includes a quality assessment that categorizes the reliability of the consensus based on confidence scores and participant count.

## Relationship to Model Core
This view implements the core consensus mechanism described in the model, where collective truth emerges from the aggregation of individual assessments. The view ensures that the consensus calculation aligns with the principles of collective intelligence and distributed truth assessment.

## Usage Examples

```sql
-- Get current consensus for a specific event
SELECT * FROM consensus_calculation WHERE event_id = ?;

-- Get events with high-confidence consensus
SELECT * FROM real_time_consensus WHERE consensus_quality = 'HIGH';

-- Monitor consensus evolution over time
SELECT 
    event_id,
    confidence_score,
    participant_count,
    calculated_at
FROM consensus_calculation
ORDER BY calculated_at DESC;
```

## Integration with Other Components
- Works with `judgment_weights` to apply appropriate participant reputation weights
- Integrates with `impact` assessments to provide comprehensive consensus
- Feeds into `event_ci` state management for event classification
- Supports the calculation of `truth_state` values

## Notes
- The view is designed to be refreshed regularly to reflect new participant contributions
- Weights are based on participant reputation scores from the `participants` table
- The consensus calculation supports both individual event analysis and cross-event comparisons
