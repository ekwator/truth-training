-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Measuring Stability of Consensus Across the Network Over Time  

-- Measures the stability of consensus across the network over time
-- Identifies potential manipulation or disagreement patterns
-- Links: consensus_ci.event_id → event_ci.id → event_ci.created_by → truth_event.id → truth_event.participant_id → participants.id
```sql
CREATE VIEW network_consensus_stability AS
SELECT
    cc.id as consensus_id,
    cc.event_id,
    cc.consensus_value,
    cc.confidence_score,
    cc.participant_count,
    cc.calculated_at as consensus_calculated_at,
    -- Get the event information
    te.description as event_description,
    te.collective_score as local_collective_score,
    te.impact_score as local_impact_score,
    te.judgment_score as local_judgment_score,
    -- Compare local scores with consensus scores
    ABS(COALESCE(cc.confidence_score, 0) - COALESCE(te.collective_score, 0.5)) as consensus_local_delta,
    -- Stability measure: how much the consensus has changed over time
    (SELECT COUNT(*) 
     FROM consensus_ci cc2 
     WHERE cc2.event_id = cc.event_id 
       AND cc2.calculated_at < cc.calculated_at) as prior_consensus_count,
    -- Agreement level between participants
    CASE
        WHEN cc.participant_count > 1 THEN
            -- Calculate agreement based on confidence scores
            (SELECT AVG(ABS(cc3.confidence_score - cc.confidence_score))
             FROM consensus_ci cc3
             WHERE cc3.event_id = cc.event_id
               AND cc3.calculated_at BETWEEN cc.calculated_at - 86400 AND cc.calculated_at)  -- Within last 24 hours
        ELSE 1.0  -- No comparison possible
    END as inter_consensus_agreement,
    -- Consensus volatility (how much it changes over time)
    (SELECT AVG(ABS(cc2.confidence_score - LAG(cc2.confidence_score, 1, cc2.confidence_score) OVER (ORDER BY cc2.calculated_at)))
     FROM consensus_ci cc2
     WHERE cc2.event_id = cc.event_id
       AND cc2.calculated_at <= cc.calculated_at) as consensus_volatility,
    -- Consensus maturity (how long it has been stable)
    (SELECT MIN(cc2.calculated_at)
     FROM consensus_ci cc2
     WHERE cc2.event_id = cc.event_id
       AND ABS(cc2.confidence_score - cc.confidence_score) < 0.05  -- Close to current value
       AND cc2.calculated_at < cc.calculated_at) as stability_start_time,
    -- Network-wide agreement level
    CASE
        WHEN cc.confidence_score > 0.8 THEN 'STRONG_AGREEMENT'
        WHEN cc.confidence_score > 0.6 THEN 'MODERATE_AGREEMENT'
        WHEN cc.confidence_score > 0.4 THEN 'WEAK_AGREEMENT'
        WHEN cc.confidence_score > 0.2 THEN 'MINIMAL_AGREEMENT'
        ELSE 'NO_AGREEMENT'
    END as agreement_level,
    -- Participant diversity measure
    cc.participant_count as participant_diversity,
    -- Potential manipulation indicator
    CASE
        WHEN cc.participant_count = 1 THEN 'LOW_DIVERSITY'
        WHEN cc.participant_count < 3 AND cc.confidence_score > 0.8 THEN 'HIGH_CONFIDENCE_LOW_DIVERSE'
        WHEN (SELECT AVG(ABS(cc2.confidence_score - cc.confidence_score))
              FROM consensus_ci cc2
              WHERE cc2.event_id = cc.event_id
                AND cc2.calculated_at BETWEEN cc.calculated_at - 86400 AND cc.calculated_at) > 0.3 THEN 'HIGH_VOLATILITY'
        ELSE 'STABLE'
    END as stability_classification,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM consensus_ci cc
JOIN event_ci ec ON cc.event_id = ec.id
JOIN truth_event te ON ec.created_by = te.id;
```

-- View for tracking consensus stability over time
-- This view monitors how consensus changes over time for each event
```sql
CREATE VIEW consensus_stability_over_time AS
WITH event_consensus_history AS (
    SELECT 
        cc.event_id,
        cc.confidence_score,
        cc.calculated_at,
        cc.participant_count,
        -- Lag function to compare with previous consensus
        LAG(cc.confidence_score, 1, cc.confidence_score) OVER (
            PARTITION BY cc.event_id 
            ORDER BY cc.calculated_at
        ) as previous_confidence_score,
        LAG(cc.calculated_at, 1, cc.calculated_at) OVER (
            PARTITION BY cc.event_id 
            ORDER BY cc.calculated_at
        ) as previous_calculation_time,
        LAG(cc.participant_count, 1, cc.participant_count) OVER (
            PARTITION BY cc.event_id 
            ORDER BY cc.calculated_at
        ) as previous_participant_count
    FROM consensus_ci cc
)
SELECT
    ech.event_id,
    ech.confidence_score,
    ech.calculated_at,
    ech.participant_count,
    ech.previous_confidence_score,
    ech.previous_calculation_time,
    ech.previous_participant_count,
    -- Calculate change in confidence
    ABS(ech.confidence_score - ech.previous_confidence_score) as confidence_change,
    -- Calculate time between calculations
    ech.calculated_at - ech.previous_calculation_time as time_between_calculations,
    -- Calculate change in participant count
    ech.participant_count - ech.previous_participant_count as participant_change,
    -- Stability indicator (lower values indicate more stability)
    CASE
        WHEN ABS(ech.confidence_score - ech.previous_confidence_score) < 0.05 AND 
             (ech.participant_count - ech.previous_participant_count) = 0 THEN 'STABLE'
        WHEN ABS(ech.confidence_score - ech.previous_confidence_score) < 0.1 THEN 'MOSTLY_STABLE'
        WHEN ABS(ech.confidence_score - ech.previous_confidence_score) < 0.2 THEN 'SOME_FLUX'
        WHEN ABS(ech.confidence_score - ech.previous_confidence_score) < 0.3 THEN 'MODERATE_CHANGE'
        ELSE 'HIGH_FLUX'
    END as stability_indicator,
    -- Consensus velocity (rate of change)
    CASE
        WHEN (ech.calculated_at - ech.previous_calculation_time) > 0 THEN
            ABS(ech.confidence_score - ech.previous_confidence_score) / (ech.calculated_at - ech.previous_calculation_time) * 86400  -- Per day
        ELSE 0
    END as consensus_velocity,
    -- Cumulative stability score
    AVG(ABS(ech.confidence_score - ech.previous_confidence_score)) OVER (
        PARTITION BY ech.event_id
        ORDER BY ech.calculated_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) as cumulative_stability_score,
    -- Moving average of stability over last 5 calculations
    AVG(ABS(ech.confidence_score - ech.previous_confidence_score)) OVER (
        PARTITION BY ech.event_id
        ORDER BY ech.calculated_at
        ROWS BETWEEN 4 PRECEDING AND CURRENT ROW
    ) as moving_avg_stability
FROM event_consensus_history ech
WHERE ech.previous_confidence_score IS NOT NULL  -- Exclude first entry
ORDER BY ech.event_id, ech.calculated_at;
```

-- View for identifying potential manipulation patterns
-- This view looks for signs of manipulation or abnormal consensus behavior
```sql
CREATE VIEW manipulation_pattern_identification AS
SELECT
    ncst.event_id,
    ncst.confidence_score,
    ncst.calculated_at,
    ncst.participant_count,
    ncst.confidence_change,
    ncst.stability_indicator,
    ncst.consensus_velocity,
    ncst.cumulative_stability_score,
    -- Detect rapid changes in consensus
    CASE
        WHEN ncst.confidence_change > 0.3 THEN 'RAPID_CHANGE'
        WHEN ncst.confidence_change > 0.2 THEN 'MODERATE_CHANGE'
        WHEN ncst.confidence_change > 0.1 THEN 'SMALL_CHANGE'
        ELSE 'STABLE'
    END as change_intensity,
    -- Detect unusual participant count changes
    CASE
        WHEN ABS(ncst.participant_change) > 5 THEN 'MASS_JOIN_LEAVE_EVENT'
        WHEN ABS(ncst.participant_change) > 2 THEN 'SIGNIFICANT_PARTICIPANT_CHANGE'
        WHEN ABS(ncst.participant_change) = ncst.participant_count AND ncst.participant_count > 0 THEN 'ALL_NEW_PARTICIPANTS'
        ELSE 'NORMAL_PARTICIPANT_FLOW'
    END as participant_change_pattern,
    -- Velocity-based manipulation indicator
    CASE
        WHEN ncst.consensus_velocity > 0.5 THEN 'HIGH_VELOCITY_CONCERN'
        WHEN ncst.consensus_velocity > 0.2 THEN 'MODERATE_VELOCITY_CONCERN'
        ELSE 'NORMAL_VELOCITY'
    END as velocity_concern_level,
    -- Overall risk score
    (ncst.confidence_change * 0.4 + 
     CASE 
         WHEN ABS(ncst.participant_change) > 5 THEN 0.3
         WHEN ABS(ncst.participant_change) > 2 THEN 0.2
         ELSE 0.0
     END +
     CASE 
         WHEN ncst.consensus_velocity > 0.5 THEN 0.3
         WHEN ncst.consensus_velocity > 0.2 THEN 0.2
         ELSE 0.0
     END) as manipulation_risk_score,
    -- Risk category
    CASE
        WHEN (ncst.confidence_change * 0.4 + 
              CASE 
                  WHEN ABS(ncst.participant_change) > 5 THEN 0.3
                  WHEN ABS(ncst.participant_change) > 2 THEN 0.2
                  ELSE 0.0
              END +
              CASE 
                  WHEN ncst.consensus_velocity > 0.5 THEN 0.3
                  WHEN ncst.consensus_velocity > 0.2 THEN 0.2
                  ELSE 0.0
              END) >= 0.6 THEN 'HIGH_RISK'
        WHEN (ncst.confidence_change * 0.4 + 
              CASE 
                  WHEN ABS(ncst.participant_change) > 5 THEN 0.3
                  WHEN ABS(ncst.participant_change) > 2 THEN 0.2
                  ELSE 0.0
              END +
              CASE 
                  WHEN ncst.consensus_velocity > 0.5 THEN 0.3
                  WHEN ncst.consensus_velocity > 0.2 THEN 0.2
                  ELSE 0.0
              END) >= 0.3 THEN 'MEDIUM_RISK'
        WHEN (ncst.confidence_change * 0.4 + 
              CASE 
                  WHEN ABS(ncst.participant_change) > 5 THEN 0.3
                  WHEN ABS(ncst.participant_change) > 2 THEN 0.2
                  ELSE 0.0
              END +
              CASE 
                  WHEN ncst.consensus_velocity > 0.5 THEN 0.3
                  WHEN ncst.consensus_velocity > 0.2 THEN 0.2
                  ELSE 0.0
              END) >= 0.1 THEN 'LOW_RISK'
        ELSE 'NO_CONCERN'
    END as risk_category,
    -- Pattern classification
    CASE
        WHEN ncst.confidence_change > 0.3 AND ABS(ncst.participant_change) > 3 THEN 'COORDINATED_MANIPULATION'
        WHEN ncst.confidence_change > 0.3 AND ncst.participant_count < 3 THEN 'MINORITY_OVERRULE'
        WHEN ncst.confidence_change < 0.05 AND ABS(ncst.participant_change) > 5 THEN 'SYNTHETIC_ACTIVITY'
        WHEN ncst.consensus_velocity > 0.5 THEN 'RAPID_CONSENSUS_SHIFT'
        ELSE 'NORMAL_CONSENSUS_EVOLUTION'
    END as pattern_classification,
    -- Recommended action
    CASE
        WHEN (ncst.confidence_change * 0.4 + 
              CASE 
                  WHEN ABS(ncst.participant_change) > 5 THEN 0.3
                  WHEN ABS(ncst.participant_change) > 2 THEN 0.2
                  ELSE 0.0
              END +
              CASE 
                  WHEN ncst.consensus_velocity > 0.5 THEN 0.3
                  WHEN ncst.consensus_velocity > 0.2 THEN 0.2
                  ELSE 0.0
              END) >= 0.6 THEN 'INVESTIGATE_IMMEDIATELY'
        WHEN (ncst.confidence_change * 0.4 + 
              CASE 
                  WHEN ABS(ncst.participant_change) > 5 THEN 0.3
                  WHEN ABS(ncst.participant_change) > 2 THEN 0.2
                  ELSE 0.0
              END +
              CASE 
                  WHEN ncst.consensus_velocity > 0.5 THEN 0.3
                  WHEN ncst.consensus_velocity > 0.2 THEN 0.2
                  ELSE 0.0
              END) >= 0.3 THEN 'MONITOR_CLOSELY'
        ELSE 'NORMAL_MONITORING'
    END as recommended_action
FROM consensus_stability_over_time ncst
ORDER BY ncst.manipulation_risk_score DESC;
```

-- View for network-wide consensus stability metrics
-- This view provides aggregate metrics about network consensus stability
```sql
CREATE VIEW network_wide_consensus_metrics AS
SELECT
    -- Overall network stability
    COUNT(DISTINCT event_id) as total_events_monitored,
    AVG(confidence_score) as average_network_confidence,
    STDDEV(confidence_score) as network_confidence_std_dev,
    MIN(confidence_score) as lowest_confidence,
    MAX(confidence_score) as highest_confidence,
    -- Stability metrics
    AVG(confidence_change) as average_confidence_volatility,
    AVG(consensus_velocity) as average_consensus_velocity,
    AVG(cumulative_stability_score) as average_cumulative_stability,
    -- Participant metrics
    AVG(participant_count) as average_participants_per_event,
    MIN(participant_count) as min_participants_per_event,
    MAX(participant_count) as max_participants_per_event,
    -- Risk metrics
    SUM(CASE WHEN risk_category = 'HIGH_RISK' THEN 1 ELSE 0 END) as high_risk_incidents,
    SUM(CASE WHEN risk_category = 'MEDIUM_RISK' THEN 1 ELSE 0 END) as medium_risk_incidents,
    SUM(CASE WHEN risk_category = 'LOW_RISK' THEN 1 ELSE 0 END) as low_risk_incidents,
    SUM(CASE WHEN risk_category = 'NO_CONCERN' THEN 1 ELSE 0 END) as no_concern_incidents,
    -- Pattern metrics
    SUM(CASE WHEN pattern_classification = 'COORDINATED_MANIPULATION' THEN 1 ELSE 0 END) as coordinated_manipulation_signs,
    SUM(CASE WHEN pattern_classification = 'MINORITY_OVERRULE' THEN 1 ELSE 0 END) as minority_overrule_signs,
    SUM(CASE WHEN pattern_classification = 'SYNTHETIC_ACTIVITY' THEN 1 ELSE 0 END) as synthetic_activity_signs,
    SUM(CASE WHEN pattern_classification = 'RAPID_CONSENSUS_SHIFT' THEN 1 ELSE 0 END) as rapid_shift_signs,
    -- Network health score (0-100 scale)
    100 - (
        (AVG(confidence_change) * 100) * 0.3 +  -- Volatility factor
        (AVG(consensus_velocity) * 100) * 0.2 +  -- Velocity factor
        (SUM(CASE WHEN risk_category = 'HIGH_RISK' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) * 0.5  -- Risk factor
    ) as network_health_score,
    -- Timestamp
    (SELECT strftime('%s', 'now')) as analyzed_at
FROM manipulation_pattern_identification mpi;