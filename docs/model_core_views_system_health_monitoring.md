# System Health Monitoring View

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This view provides system-wide health metrics and performance indicators, aggregating data from various system tables to assess overall system stability and performance as described in the model.

## Purpose
The `system_health_monitoring` view provides comprehensive monitoring of the Truth Training system's health, performance, and stability. It aggregates data from multiple system tables to offer insights into system operation, identify potential issues, and track the overall health of the distributed collective intelligence network.

## SQL Implementation

```sql
-- View to provide system-wide health metrics and performance indicators
CREATE VIEW system_health_monitoring AS
SELECT 
    -- System-wide statistics
    (SELECT COUNT(*) FROM participants) AS total_participants,
    (SELECT COUNT(*) FROM truth_event) AS total_events,
    (SELECT COUNT(*) FROM impact) AS total_impacts,
    (SELECT COUNT(*) FROM judgment) AS total_judgments,
    (SELECT COUNT(*) FROM discovery_nodes) AS total_nodes,
    
    -- Active participants (those with activity in last 30 days)
    (
        SELECT COUNT(*) 
        FROM participants 
        WHERE last_activity > (julianday('now') - 30) * 86400
    ) AS active_participants,
    
    -- Recent activity metrics
    (
        SELECT COUNT(*) 
        FROM truth_event 
        WHERE created_at > (julianday('now') - 1) * 86400
    ) AS events_last_24h,
    
    (
        SELECT COUNT(*) 
        FROM impact 
        WHERE timeline_id > (julianday('now') - 1) * 86400
    ) AS impacts_last_24h,
    
    (
        SELECT COUNT(*) 
        FROM judgment 
        WHERE timeline_id > (julianday('now') - 1) * 86400
    ) AS judgments_last_24h,
    
    -- Average reputation score across all participants
    (SELECT AVG(reputation_score) FROM participants) AS average_participant_reputation,
    
    -- Reputation distribution metrics
    (SELECT COUNT(*) FROM participants WHERE reputation_score > 0.7) AS high_reputation_participants,
    (SELECT COUNT(*) FROM participants WHERE reputation_score BETWEEN 0.3 AND 0.7) AS medium_reputation_participants,
    (SELECT COUNT(*) FROM participants WHERE reputation_score < 0.3) AS low_reputation_participants,
    
    -- Event classification distribution
    (
        SELECT COUNT(*) 
        FROM event_projection ep
        JOIN (
            SELECT 
                event_id,
                CASE 
                    WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
                    WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
                    WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
                    ELSE 'Q4'
                END AS quadrant
            FROM event_projection
        ) q ON ep.event_id = q.event_id
        WHERE q.quadrant = 'Q1'
    ) AS q1_events,
    
    (
        SELECT COUNT(*) 
        FROM event_projection ep
        JOIN (
            SELECT 
                event_id,
                CASE 
                    WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
                    WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
                    WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
                    ELSE 'Q4'
                END AS quadrant
            FROM event_projection
        ) q ON ep.event_id = q.event_id
        WHERE q.quadrant = 'Q2'
    ) AS q2_events,
    
    (
        SELECT COUNT(*) 
        FROM event_projection ep
        JOIN (
            SELECT 
                event_id,
                CASE 
                    WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
                    WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
                    WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
                    ELSE 'Q4'
                END AS quadrant
            FROM event_projection
        ) q ON ep.event_id = q.event_id
        WHERE q.quadrant = 'Q3'
    ) AS q3_events,
    
    (
        SELECT COUNT(*) 
        FROM event_projection ep
        JOIN (
            SELECT 
                event_id,
                CASE 
                    WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
                    WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
                    WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
                    ELSE 'Q4'
                END AS quadrant
            FROM event_projection
        ) q ON ep.event_id = q.event_id
        WHERE q.quadrant = 'Q4'
    ) AS q4_events,
    
    -- Network health metrics
    (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS reachable_nodes,
    (SELECT AVG(trust_score) FROM node_ratings) AS average_node_trust,
    (SELECT AVG(relay_success_rate) FROM node_performance) AS average_relay_success_rate,
    
    -- Node distribution by type
    (SELECT COUNT(*) FROM discovery_nodes WHERE type = 'LAN') AS lan_nodes,
    (SELECT COUNT(*) FROM discovery_nodes WHERE type = 'WIFI') AS wifi_nodes,
    (SELECT COUNT(*) FROM discovery_nodes WHERE type = 'GLOBAL') AS global_nodes,
    (SELECT COUNT(*) FROM discovery_nodes WHERE type = 'RELAY') AS relay_nodes,
    (SELECT COUNT(*) FROM discovery_nodes WHERE type = 'CLIENT') AS client_nodes,
    
    -- Data freshness metrics
    (
        SELECT AVG(julianday('now') - julianday(last_activity, 'unixepoch'))
        FROM participants
    ) AS avg_days_since_participant_activity,
    
    (
        SELECT MIN(julianday('now') - julianday(created_at, 'unixepoch'))
        FROM participants
    ) AS days_since_first_participant,
    
    -- Temporal metrics
    (
        SELECT AVG(julianday('now') - julianday(calculated_at, 'unixepoch'))
        FROM event_state_history
    ) AS avg_age_of_event_history,
    
    -- Stability metrics
    (
        SELECT COUNT(*) 
        FROM event_stability 
        WHERE truth_stable = 1 OR impact_stable = 1
    ) AS stable_events_count,
    
    (
        SELECT COUNT(*) 
        FROM event_stability 
        WHERE truth_stable = 0 AND impact_stable = 0
    ) AS unstable_events_count,
    
    -- Prediction accuracy metrics
    (
        SELECT AVG(accuracy_score) 
        FROM (
            SELECT 
                CASE 
                    WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                         (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                    ELSE 0.0
                END AS accuracy_score
            FROM impact_predictions ip
            JOIN truth_event te ON ip.event_id = (
                SELECT id FROM event_ci WHERE created_by = te.id
            )
        ) acc
    ) AS average_prediction_accuracy,
    
    -- Consensus metrics
    (
        SELECT AVG(confidence_score) 
        FROM consensus_ci
    ) AS average_consensus_confidence,
    
    -- Data consistency metrics
    (
        SELECT COUNT(*) 
        FROM truth_event 
        WHERE collective_score IS NULL OR collective_score < 0 OR collective_score > 1
    ) AS events_with_invalid_collective_score,
    
    (
        SELECT COUNT(*) 
        FROM impact 
        WHERE value NOT IN (0, 1) AND value IS NOT NULL
    ) AS impacts_with_invalid_values,
    
    -- Synchronization health
    (
        SELECT AVG(success_rate) 
        FROM (
            SELECT 
                (success_count * 100.0) / (success_count + fail_count) AS success_rate
            FROM peer_synchronization
            WHERE (success_count + fail_count) > 0
        ) sync_rates
    ) AS average_sync_success_rate,
    
    -- Trigger efficiency metrics (estimated)
    (
        SELECT COUNT(*) 
        FROM sync_operations 
        WHERE created_at > (julianday('now') - 1) * 86400
    ) AS sync_operations_last_24h,
    
    -- System health score (composite metric)
    (
        SELECT 
            (active_participants * 0.2) +
            (average_participant_reputation * 0.15) +
            (reachable_nodes * 0.1) +
            (average_node_trust * 0.1) +
            (average_prediction_accuracy * 0.1) +
            (average_consensus_confidence * 0.1) +
            (CASE WHEN events_last_24h > 10 THEN 0.1 ELSE events_last_24h * 0.01 END) +
            (CASE WHEN (q1_events + q2_events) * 1.0 / NULLIF(total_events, 0) > 0.5 THEN 0.15 ELSE 0 END) +
            (CASE WHEN (unstable_events_count * 1.0 / NULLIF(total_events, 0)) < 0.3 THEN 0.1 ELSE 0 END)
        FROM (
            SELECT 
                (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 30) * 86400) AS active_participants,
                (SELECT AVG(reputation_score) FROM participants) AS average_participant_reputation,
                (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS reachable_nodes,
                (SELECT AVG(trust_score) FROM node_ratings) AS average_node_trust,
                (SELECT AVG(accuracy_score) FROM (
                    SELECT 
                        CASE 
                            WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                                 (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                            ELSE 0.0
                        END AS accuracy_score
                    FROM impact_predictions ip
                    JOIN truth_event te ON ip.event_id = (
                        SELECT id FROM event_ci WHERE created_by = te.id
                    )
                ) acc) AS average_prediction_accuracy,
                (SELECT AVG(confidence_score) FROM consensus_ci) AS average_consensus_confidence,
                (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) AS events_last_24h,
                (SELECT COUNT(*) FROM participants) AS total_participants,
                (SELECT COUNT(*) FROM truth_event) AS total_events,
                (SELECT COUNT(*) FROM event_projection ep JOIN (
                    SELECT 
                        event_id,
                        CASE 
                            WHEN truth_score >= 0.5 AND impact_score >= 0 THEN 'Q1'
                            WHEN truth_score >= 0.5 AND impact_score < 0 THEN 'Q2'
                            WHEN truth_score < 0.5 AND impact_score >= 0 THEN 'Q3'
                            ELSE 'Q4'
                        END AS quadrant
                    FROM event_projection
                ) q ON ep.event_id = q.event_id WHERE q.quadrant IN ('Q1', 'Q2')) AS valid_events,
                (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) AS unstable_events_count
        ) metrics
    ) AS system_health_score,
    
    -- Health status based on composite score
    CASE 
        WHEN (
            SELECT 
                (active_participants * 0.2) +
                (average_participant_reputation * 0.15) +
                (reachable_nodes * 0.1) +
                (average_node_trust * 0.1) +
                (average_prediction_accuracy * 0.1) +
                (average_consensus_confidence * 0.1) +
                (CASE WHEN events_last_24h > 10 THEN 0.1 ELSE events_last_24h * 0.01 END) +
                (CASE WHEN (q1_events + q2_events) * 1.0 / NULLIF(total_events, 0) > 0.5 THEN 0.15 ELSE 0 END) +
                (CASE WHEN (unstable_events_count * 1.0 / NULLIF(total_events, 0)) < 0.3 THEN 0.1 ELSE 0 END)
            FROM (
                SELECT 
                    (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 30) * 86400) AS active_participants,
                    (SELECT AVG(reputation_score) FROM participants) AS average_participant_reputation,
                    (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS reachable_nodes,
                    (SELECT AVG(trust_score) FROM node_ratings) AS average_node_trust,
                    (SELECT AVG(accuracy_score) FROM (
                        SELECT 
                            CASE 
                                WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                                     (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                                ELSE 0.0
                            END AS accuracy_score
                        FROM impact_predictions ip
                        JOIN truth_event te ON ip.event_id = (
                            SELECT id FROM event_ci WHERE created_by = te.id
                        )
                    ) acc) AS average_prediction_accuracy,
                    (SELECT AVG(confidence_score) FROM consensus_ci) AS average_consensus_confidence,
                    (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) AS events_last_24h,
                    (SELECT COUNT(*) FROM truth_event) AS total_events,
                    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) AS unstable_events_count
            ) metrics
        ) >= 0.8 THEN 'EXCELLENT'
        WHEN (
            SELECT 
                (active_participants * 0.2) +
                (average_participant_reputation * 0.15) +
                (reachable_nodes * 0.1) +
                (average_node_trust * 0.1) +
                (average_prediction_accuracy * 0.1) +
                (average_consensus_confidence * 0.1) +
                (CASE WHEN events_last_24h > 10 THEN 0.1 ELSE events_last_24h * 0.01 END) +
                (CASE WHEN (q1_events + q2_events) * 1.0 / NULLIF(total_events, 0) > 0.5 THEN 0.15 ELSE 0 END) +
                (CASE WHEN (unstable_events_count * 1.0 / NULLIF(total_events, 0)) < 0.3 THEN 0.1 ELSE 0 END)
            FROM (
                SELECT 
                    (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 30) * 86400) AS active_participants,
                    (SELECT AVG(reputation_score) FROM participants) AS average_participant_reputation,
                    (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS reachable_nodes,
                    (SELECT AVG(trust_score) FROM node_ratings) AS average_node_trust,
                    (SELECT AVG(accuracy_score) FROM (
                        SELECT 
                            CASE 
                                WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                                     (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                                ELSE 0.0
                            END AS accuracy_score
                        FROM impact_predictions ip
                        JOIN truth_event te ON ip.event_id = (
                            SELECT id FROM event_ci WHERE created_by = te.id
                        )
                    ) acc) AS average_prediction_accuracy,
                    (SELECT AVG(confidence_score) FROM consensus_ci) AS average_consensus_confidence,
                    (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) AS events_last_24h,
                    (SELECT COUNT(*) FROM truth_event) AS total_events,
                    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) AS unstable_events_count
            ) metrics
        ) >= 0.6 THEN 'GOOD'
        WHEN (
            SELECT 
                (active_participants * 0.2) +
                (average_participant_reputation * 0.15) +
                (reachable_nodes * 0.1) +
                (average_node_trust * 0.1) +
                (average_prediction_accuracy * 0.1) +
                (average_consensus_confidence * 0.1) +
                (CASE WHEN events_last_24h > 10 THEN 0.1 ELSE events_last_24h * 0.01 END) +
                (CASE WHEN (q1_events + q2_events) * 1.0 / NULLIF(total_events, 0) > 0.5 THEN 0.15 ELSE 0 END) +
                (CASE WHEN (unstable_events_count * 1.0 / NULLIF(total_events, 0)) < 0.3 THEN 0.1 ELSE 0 END)
            FROM (
                SELECT 
                    (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 30) * 86400) AS active_participants,
                    (SELECT AVG(reputation_score) FROM participants) AS average_participant_reputation,
                    (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS reachable_nodes,
                    (SELECT AVG(trust_score) FROM node_ratings) AS average_node_trust,
                    (SELECT AVG(accuracy_score) FROM (
                        SELECT 
                            CASE 
                                WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                                     (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                                ELSE 0.0
                            END AS accuracy_score
                        FROM impact_predictions ip
                        JOIN truth_event te ON ip.event_id = (
                            SELECT id FROM event_ci WHERE created_by = te.id
                        )
                    ) acc) AS average_prediction_accuracy,
                    (SELECT AVG(confidence_score) FROM consensus_ci) AS average_consensus_confidence,
                    (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) AS events_last_24h,
                    (SELECT COUNT(*) FROM truth_event) AS total_events,
                    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) AS unstable_events_count
            ) metrics
        ) >= 0.4 THEN 'FAIR'
        ELSE 'POOR'
    END AS system_health_status,
    
    -- Warning flags
    CASE 
        WHEN (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 7) * 86400) < 5 THEN 1
        ELSE 0
    END AS low_recent_activity_warning,
    
    CASE 
        WHEN (SELECT AVG(reputation_score) FROM participants) < 0.4 THEN 1
        ELSE 0
    END AS low_average_reputation_warning,
    
    CASE 
        WHEN (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) * 1.0 / NULLIF((SELECT COUNT(*) FROM discovery_nodes), 0) < 0.5 THEN 1
        ELSE 0
    END AS network_connectivity_warning,
    
    CASE 
        WHEN (SELECT AVG(confidence_score) FROM consensus_ci) < 0.3 THEN 1
        ELSE 0
    END AS low_consensus_confidence_warning,
    
    -- Timestamp of calculation
    CURRENT_TIMESTAMP AS health_metrics_calculated_at;

-- View for health trend analysis
CREATE VIEW system_health_trends AS
SELECT 
    -- Rolling averages for trend analysis
    (SELECT AVG(events_count) FROM (
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 7) * 86400 AND (julianday('now') - 6) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 6) * 86400 AND (julianday('now') - 5) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 5) * 86400 AND (julianday('now') - 4) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 4) * 86400 AND (julianday('now') - 3) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 3) * 86400 AND (julianday('now') - 2) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 2) * 86400 AND (julianday('now') - 1) * 86400
        UNION ALL
        SELECT COUNT(*) AS events_count FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 1) * 86400 AND julianday('now') * 86400
    )) AS weekly_avg_events,
    
    (SELECT AVG(daily_events) FROM (
        SELECT COUNT(*) AS daily_events FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 1) * 86400 AND julianday('now') * 86400
        UNION ALL
        SELECT COUNT(*) AS daily_events FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 2) * 86400 AND (julianday('now') - 1) * 86400
        UNION ALL
        SELECT COUNT(*) AS daily_events FROM truth_event 
        WHERE created_at BETWEEN (julianday('now') - 3) * 86400 AND (julianday('now') - 2) * 86400
    )) AS 3day_avg_events,
    
    -- Growth rates
    (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) AS events_today,
    (SELECT COUNT(*) FROM truth_event WHERE created_at BETWEEN (julianday('now') - 2) * 86400 AND (julianday('now') - 1) * 86400) AS events_yesterday,
    (SELECT COUNT(*) FROM truth_event WHERE created_at BETWEEN (julianday('now') - 7) * 86400 AND julianday('now') * 86400) AS events_this_week,
    (SELECT COUNT(*) FROM truth_event WHERE created_at BETWEEN (julianday('now') - 14) * 86400 AND (julianday('now') - 7) * 86400) AS events_last_week,
    
    -- Trend indicators
    CASE 
        WHEN (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) > 
             (SELECT AVG(daily_events) FROM (
                 SELECT COUNT(*) AS daily_events FROM truth_event 
                 WHERE created_at BETWEEN (julianday('now') - 7) * 86400 AND (julianday('now') - 1) * 86400
                 UNION ALL
                 SELECT COUNT(*) AS daily_events FROM truth_event 
                 WHERE created_at BETWEEN (julianday('now') - 14) * 86400 AND (julianday('now') - 7) * 86400
             )) THEN 'INCREASING'
        WHEN (SELECT COUNT(*) FROM truth_event WHERE created_at > (julianday('now') - 1) * 86400) < 
             (SELECT AVG(daily_events) FROM (
                 SELECT COUNT(*) AS daily_events FROM truth_event 
                 WHERE created_at BETWEEN (julianday('now') - 7) * 86400 AND (julianday('now') - 1) * 86400
                 UNION ALL
                 SELECT COUNT(*) AS daily_events FROM truth_event 
                 WHERE created_at BETWEEN (julianday('now') - 14) * 86400 AND (julianday('now') - 7) * 86400
             )) THEN 'DECREASING'
        ELSE 'STABLE'
    END AS event_activity_trend,
    
    -- Participant engagement trends
    (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 1) * 86400) AS active_today,
    (SELECT COUNT(*) FROM participants WHERE last_activity BETWEEN (julianday('now') - 7) * 86400 AND julianday('now') * 86400) AS active_this_week,
    (SELECT COUNT(*) FROM participants WHERE last_activity BETWEEN (julianday('now') - 30) * 86400 AND julianday('now') * 86400) AS active_this_month,
    
    -- Reputation trends
    (SELECT AVG(reputation_score) FROM participants WHERE last_activity > (julianday('now') - 7) * 86400) AS avg_reputation_recent_active,
    (SELECT AVG(reputation_score) FROM participants WHERE last_activity BETWEEN (julianday('now') - 30) * 86400 AND (julianday('now') - 7) * 86400) AS avg_reputation_prev_period,
    
    -- Stability trends
    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 1 OR impact_stable = 1) AS stable_events,
    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) AS unstable_events,
    (SELECT COUNT(*) FROM event_stability WHERE 
        (julianday('now') - julianday(stabilized_at, 'unixepoch')) < 7) AS newly_stabilized_events,
    
    -- Prediction accuracy trends
    (SELECT AVG(accuracy_score) FROM (
        SELECT 
            CASE 
                WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                     (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                ELSE 0.0
            END AS accuracy_score
        FROM impact_predictions ip
        JOIN truth_event te ON ip.event_id = (
            SELECT id FROM event_ci WHERE created_by = te.id
        )
        WHERE ip.created_at > (julianday('now') - 7) * 86400
    )) AS recent_prediction_accuracy,
    
    (SELECT AVG(accuracy_score) FROM (
        SELECT 
            CASE 
                WHEN ABS(expected_strength - COALESCE(collective_score, 0)) <= 
                     (0.2 * ABS(expected_strength) + 0.1) THEN 1.0
                ELSE 0.0
            END AS accuracy_score
        FROM impact_predictions ip
        JOIN truth_event te ON ip.event_id = (
            SELECT id FROM event_ci WHERE created_by = te.id
        )
        WHERE ip.created_at BETWEEN (julianday('now') - 14) * 86400 AND (julianday('now') - 7) * 86400
    )) AS prev_period_prediction_accuracy,
    
    CURRENT_TIMESTAMP AS trend_analysis_calculated_at;

-- View for system alerts and warnings
CREATE VIEW system_alerts AS
SELECT 
    'LOW_PARTICIPANT_ACTIVITY' AS alert_type,
    'Recent participant activity is below threshold' AS alert_message,
    (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 7) * 86400) AS current_value,
    5 AS threshold_value,
    CASE 
        WHEN (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 7) * 86400) < 5 THEN 'HIGH'
        ELSE 'LOW'
    END AS alert_severity
FROM dual
WHERE (SELECT COUNT(*) FROM participants WHERE last_activity > (julianday('now') - 7) * 86400) < 5

UNION ALL

SELECT 
    'LOW_NETWORK_CONNECTIVITY' AS alert_type,
    'Network connectivity is below acceptable threshold' AS alert_message,
    (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) AS current_value,
    (SELECT COUNT(*) FROM discovery_nodes) * 0.5 AS threshold_value,
    CASE 
        WHEN (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) * 1.0 / NULLIF((SELECT COUNT(*) FROM discovery_nodes), 0) < 0.5 THEN 'HIGH'
        ELSE 'MEDIUM'
    END AS alert_severity
FROM dual
WHERE (SELECT COUNT(*) FROM discovery_nodes WHERE reachable = 1) * 1.0 / NULLIF((SELECT COUNT(*) FROM discovery_nodes), 0) < 0.5

UNION ALL

SELECT 
    'HIGH_INVALID_DATA_COUNT' AS alert_type,
    'High number of events with invalid collective scores detected' AS alert_message,
    (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NULL OR collective_score < 0 OR collective_score > 1) AS current_value,
    5 AS threshold_value,
    CASE 
        WHEN (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NULL OR collective_score < 0 OR collective_score > 1) > 5 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS alert_severity
FROM dual
WHERE (SELECT COUNT(*) FROM truth_event WHERE collective_score IS NULL OR collective_score < 0 OR collective_score > 1) > 5

UNION ALL

SELECT 
    'LOW_CONSENSUS_CONFIDENCE' AS alert_type,
    'Average consensus confidence is below acceptable threshold' AS alert_message,
    (SELECT AVG(confidence_score) FROM consensus_ci) AS current_value,
    0.3 AS threshold_value,
    'MEDIUM' AS alert_severity
FROM dual
WHERE (SELECT AVG(confidence_score) FROM consensus_ci) < 0.3

UNION ALL

SELECT 
    'HIGH_UNSTABLE_EVENT_RATIO' AS alert_type,
    'Ratio of unstable events is above acceptable threshold' AS alert_message,
    (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) * 1.0 / 
    NULLIF((SELECT COUNT(*) FROM event_stability), 0) AS current_value,
    0.5 AS threshold_value,
    CASE 
        WHEN (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) * 1.0 / 
             NULLIF((SELECT COUNT(*) FROM event_stability), 0) > 0.5 THEN 'HIGH'
        ELSE 'MEDIUM'
    END AS alert_severity
FROM dual
WHERE (SELECT COUNT(*) FROM event_stability WHERE truth_stable = 0 AND impact_stable = 0) * 1.0 / 
      NULLIF((SELECT COUNT(*) FROM event_stability), 0) > 0.5;

-- View for operational dashboard metrics
CREATE VIEW operational_dashboard_metrics AS
SELECT 
    shm.total_participants,
    shm.active_participants,
    shm.total_events,
    shm.events_last_24h,
    shm.total_impacts,
    shm.impacts_last_24h,
    shm.total_judgments,
    shm.judgments_last_24h,
    shm.average_participant_reputation,
    shm.high_reputation_participants,
    shm.q1_events,
    shm.q3_events,  -- Disinformation events that need attention
    shm.reachable_nodes,
    shm.average_node_trust,
    shm.average_relay_success_rate,
    shm.stable_events_count,
    shm.unstable_events_count,
    shm.average_prediction_accuracy,
    shm.average_consensus_confidence,
    shm.system_health_score,
    shm.system_health_status,
    shm.low_recent_activity_warning,
    shm.low_average_reputation_warning,
    shm.network_connectivity_warning,
    shm.low_consensus_confidence_warning,
    
    -- Ratios and percentages
    shm.active_participants * 100.0 / NULLIF(shm.total_participants, 0) AS participant_engagement_rate,
    shm.events_last_24h * 100.0 / NULLIF((SELECT COUNT(*) FROM truth_event), 0) AS daily_event_rate,
    shm.q3_events * 100.0 / NULLIF(shm.total_events, 0) AS disinformation_percentage,
    shm.stable_events_count * 100.0 / NULLIF(shm.total_events, 0) AS event_stability_rate,
    
    -- Operational capacity
    CASE 
        WHEN shm.total_participants < 10 THEN 'LIMITED_CAPACITY'
        WHEN shm.total_participants < 100 THEN 'MODERATE_CAPACITY'
        WHEN shm.total_participants < 1000 THEN 'GOOD_CAPACITY'
        ELSE 'HIGH_CAPACITY'
    END AS system_capacity_status,
    
    -- Performance indicators
    CASE 
        WHEN shm.average_consensus_confidence > 0.7 THEN 'HIGH_PERFORMANCE'
        WHEN shm.average_consensus_confidence > 0.5 THEN 'MEDIUM_PERFORMANCE'
        ELSE 'LOW_PERFORMANCE'
    END AS consensus_performance_status,
    
    CASE 
        WHEN shm.average_prediction_accuracy > 0.7 THEN 'HIGH_ACCURACY'
        WHEN shm.average_prediction_accuracy > 0.5 THEN 'MEDIUM_ACCURACY'
        ELSE 'LOW_ACCURACY'
    END AS prediction_accuracy_status,
    
    CURRENT_TIMESTAMP AS dashboard_metrics_calculated_at

FROM system_health_monitoring shm;
```

## Key Features

### Comprehensive Health Metrics
The view provides a comprehensive set of health metrics covering:
- Participant activity and reputation distribution
- Event counts and classification distribution
- Network connectivity and node health
- Data consistency and validity checks
- Prediction accuracy and consensus confidence

### Composite Health Scoring
Calculates a composite health score that combines multiple metrics to provide an overall system health assessment with clear status categories (Excellent, Good, Fair, Poor).

### Trend Analysis
Provides trend analysis for key metrics to identify patterns and potential issues before they become critical problems.

### Alert System
Includes an alert system that identifies when key metrics fall below acceptable thresholds, allowing for proactive system management.

### Operational Dashboard
Creates a simplified view suitable for operational dashboards that focuses on key performance indicators and actionable metrics.

### Data Freshness Tracking
Monitors the age and freshness of data across the system to identify stale information that might affect system performance.

## Relationship to Model Core
This view implements the system health monitoring aspects of the model where:
- System-wide metrics are collected and analyzed
- Stability and performance indicators are tracked
- The system monitors its own health and performance
- Alerts are generated when metrics fall outside acceptable ranges
- Trends are analyzed to predict potential issues

## Usage Examples

```sql
-- Get overall system health
SELECT * FROM system_health_monitoring;

-- Check system trends
SELECT * FROM system_health_trends;

-- Review system alerts
SELECT * FROM system_alerts;

-- Get operational dashboard metrics
SELECT * FROM operational_dashboard_metrics;

-- Check for specific health concerns
SELECT 
    system_health_status,
    system_health_score,
    low_recent_activity_warning,
    low_average_reputation_warning,
    network_connectivity_warning
FROM system_health_monitoring;

-- Monitor prediction accuracy trends
SELECT 
    recent_prediction_accuracy,
    prev_period_prediction_accuracy,
    CASE 
        WHEN recent_prediction_accuracy > prev_period_prediction_accuracy THEN 'IMPROVING'
        WHEN recent_prediction_accuracy < prev_period_prediction_accuracy THEN 'DECLINING'
        ELSE 'STABLE'
    END AS prediction_trend
FROM system_health_trends;
```

## Integration with Other Components
- Aggregates data from all major system tables
- Supports monitoring and alerting systems
- Provides metrics for system administration and maintenance
- Feeds into operational dashboards and reporting
- Used for capacity planning and performance optimization

## Notes
- The view provides a snapshot of system health at the time of query
- Health scores are normalized and weighted to provide meaningful composite values
- Alert thresholds are configurable based on system requirements
- The view is designed to be efficient and avoid expensive calculations where possible
- Trend analysis uses rolling windows to identify patterns over time