# Anti-Manipulation Triggers

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This document describes the SQL triggers that implement the anti-manipulation mechanisms for the Truth Training system. These triggers detect and respond to various forms of manipulation attempts in the network.

## Purpose
The anti-manipulation triggers ensure the integrity of the collective intelligence system by detecting suspicious patterns, limiting influence of potentially malicious actors, and maintaining fair distribution of trust and influence across the network.

## Trigger Definitions

### 1. detect_sybil_attack_patterns
This trigger detects potential Sybil attack patterns by monitoring for unusual clustering of participant activity.

```sql
CREATE TRIGGER detect_sybil_attack_patterns
AFTER INSERT ON participants
FOR EACH ROW
BEGIN
    -- Check for IP-based clustering (if IP information is available)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.public_key,
        'IP_CLUSTERING',
        CASE 
            WHEN COUNT(*) > 10 THEN 5  -- Critical threshold
            WHEN COUNT(*) > 5 THEN 3   -- Moderate threshold
            ELSE 1                     -- Low threshold
        END AS severity,
        'Detected ' || COUNT(*) || ' participants from similar IP range',
        CURRENT_TIMESTAMP
    FROM participants p2
    WHERE p2.ip_address LIKE SUBSTR(NEW.ip_address, 1, LENGTH(NEW.ip_address) - 3) || '%'
    AND p2.id != NEW.id
    AND julianday('now') - julianday(p2.created_at, 'unixepoch') < 7  -- Created in last 7 days
    GROUP BY SUBSTR(p2.ip_address, 1, LENGTH(p2.ip_address) - 3)
    HAVING COUNT(*) > 3;
    
    -- Check for timing-based clustering (participants created in short time window)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.public_key,
        'TEMPORAL_CLUSTERING',
        CASE 
            WHEN COUNT(*) > 20 THEN 5
            WHEN COUNT(*) > 10 THEN 4
            WHEN COUNT(*) > 5 THEN 2
            ELSE 1
        END AS severity,
        'Detected ' || COUNT(*) || ' participants created within 1 hour',
        CURRENT_TIMESTAMP
    FROM participants p3
    WHERE julianday('now') - julianday(p3.created_at, 'unixepoch') < 1.0/24.0  -- Within 1 hour
    AND p3.id != NEW.id
    AND ABS(julianday(p3.created_at, 'unixepoch') - julianday(NEW.created_at, 'unixepoch')) < 1.0/24.0
    GROUP BY CAST(julianday(p3.created_at, 'unixepoch') * 24 AS INTEGER)  -- Group by hour
    HAVING COUNT(*) > 3;
    
    -- Check for behavioral similarity clustering
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.public_key,
        'BEHAVIORAL_SIMILARITY',
        CASE 
            WHEN similarity_score > 0.9 THEN 5  -- Very similar
            WHEN similarity_score > 0.7 THEN 4  -- Highly similar
            WHEN similarity_score > 0.5 THEN 2  -- Moderately similar
            ELSE 1
        END AS severity,
        'Participant behavior ' || ROUND(similarity_score, 2) || ' similar to other participants',
        CURRENT_TIMESTAMP
    FROM (
        SELECT 
            p1.public_key,
            AVG(
                CASE 
                    WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.1 THEN 1.0
                    WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.3 THEN 0.7
                    WHEN ABS(p1.reputation_score - p2.reputation_score) < 0.5 THEN 0.4
                    ELSE 0.1
                END
            ) AS similarity_score
        FROM participants p1
        JOIN participants p2 ON p1.id != p2.id
        WHERE julianday('now') - julianday(p1.created_at, 'unixepoch') < 30  -- New participants
        AND julianday('now') - julianday(p2.created_at, 'unixepoch') < 30
        GROUP BY p1.public_key
    ) similarity_calc
    WHERE similarity_calc.public_key = NEW.public_key
    AND similarity_calc.similarity_score > 0.6;
END;
```

### 2. detect_coordinated_assessment_patterns
This trigger identifies coordinated assessment patterns that may indicate manipulation attempts.

```sql
CREATE TRIGGER detect_coordinated_assessment_patterns
AFTER INSERT ON judgment
FOR EACH ROW
BEGIN
    -- Detect coordinated assessments (similar judgments in short time window)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'COORDINATED_JUDGMENT',
        CASE 
            WHEN COUNT(*) > 10 THEN 5  -- Critical
            WHEN COUNT(*) > 5 THEN 4   -- High
            WHEN COUNT(*) > 3 THEN 2   -- Medium
            ELSE 1                     -- Low
        END AS severity,
        'Detected ' || COUNT(*) || ' similar judgments in 5-minute window',
        CURRENT_TIMESTAMP
    FROM judgment j2
    WHERE j2.event_id = NEW.event_id
    AND j2.assessment = NEW.assessment
    AND ABS((julianday('now', 'subsec') - julianday(j2.created_at, 'unixepoch', 'subsec')) * 86400) < 300  -- 5 minutes
    AND j2.participant_id != NEW.participant_id
    GROUP BY j2.event_id
    HAVING COUNT(*) > 2;
    
    -- Detect synchronized impact assessments
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'SYNCHRONIZED_IMPACT',
        CASE 
            WHEN COUNT(*) > 8 THEN 5
            WHEN COUNT(*) > 4 THEN 3
            ELSE 1
        END AS severity,
        'Detected ' || COUNT(*) || ' synchronized impact assessments',
        CURRENT_TIMESTAMP
    FROM impact i
    WHERE i.event_id = (
        SELECT event_id FROM judgment WHERE id = NEW.id
    )
    AND i.value = (
        SELECT CASE 
            WHEN NEW.assessment = 'true' THEN 1
            WHEN NEW.assessment = 'false' THEN 0
            ELSE 0.5
        END
    )
    AND ABS((julianday('now', 'subsec') - julianday(i.created_at, 'unixepoch', 'subsec')) * 86400) < 60  -- 1 minute
    GROUP BY i.event_id
    HAVING COUNT(*) > 2;
    
    -- Update participant trust based on detected patterns
    UPDATE participants
    SET 
        reputation_score = reputation_score * CASE 
            WHEN (SELECT COUNT(*) FROM manipulation_indicators WHERE node_id = NEW.participant_id AND severity >= 4) > 0 THEN 0.8
            WHEN (SELECT COUNT(*) FROM manipulation_indicators WHERE node_id = NEW.participant_id AND severity >= 2) > 0 THEN 0.9
            ELSE 1.0
        END,
        last_reviewed = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
END;
```

### 3. detect_anomalous_reputation_changes
This trigger detects unusual reputation changes that might indicate manipulation.

```sql
CREATE TRIGGER detect_anomalous_reputation_changes
AFTER UPDATE ON participants
FOR EACH ROW
WHEN OLD.reputation_score != NEW.reputation_score
BEGIN
    -- Check for rapid reputation changes
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.public_key,
        'RAPID_REPUTATION_CHANGE',
        CASE 
            WHEN ABS(NEW.reputation_score - OLD.reputation_score) > 0.3 THEN 5  -- Large change
            WHEN ABS(NEW.reputation_score - OLD.reputation_score) > 0.2 THEN 4  -- Significant change
            WHEN ABS(NEW.reputation_score - OLD.reputation_score) > 0.1 THEN 2  -- Moderate change
            ELSE 1
        END AS severity,
        'Reputation changed from ' || OLD.reputation_score || ' to ' || NEW.reputation_score || 
        ' (' || ROUND(ABS(NEW.reputation_score - OLD.reputation_score) * 100, 2) || '% change)',
        CURRENT_TIMESTAMP
    WHERE ABS(NEW.reputation_score - OLD.reputation_score) > 0.1;  -- Only significant changes
    
    -- Check for reputation convergence anomalies (too similar to other participants)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.public_key,
        'REPUTATION_CONVERGENCE',
        CASE 
            WHEN AVG(ABS(NEW.reputation_score - p.reputation_score)) < 0.05 THEN 5
            WHEN AVG(ABS(NEW.reputation_score - p.reputation_score)) < 0.1 THEN 3
            ELSE 1
        END AS severity,
        'Reputation too similar to other participants (avg diff: ' || 
        ROUND(AVG(ABS(NEW.reputation_score - p.reputation_score)), 3) || ')',
        CURRENT_TIMESTAMP
    FROM participants p
    WHERE p.id != NEW.id
    AND julianday('now') - julianday(p.created_at, 'unixepoch') < 30  -- Only check with recent participants
    AND ABS(NEW.reputation_score - p.reputation_score) < 0.1
    GROUP BY NEW.public_key
    HAVING COUNT(*) > 5;
    
    -- Log reputation change for audit
    INSERT INTO reputation_history (
        participant_id,
        old_reputation,
        new_reputation,
        change_reason,
        updated_at
    )
    VALUES (
        NEW.id,
        OLD.reputation_score,
        NEW.reputation_score,
        CASE 
            WHEN ABS(NEW.reputation_score - OLD.reputation_score) > 0.2 THEN 'significant_change_detected'
            WHEN NEW.reputation_score > OLD.reputation_score THEN 'positive_assessment'
            ELSE 'negative_assessment'
        END,
        CURRENT_TIMESTAMP
    );
END;
```

### 4. update_participant_trust_on_anomaly_detection
This trigger adjusts participant trust based on detected anomalies.

```sql
CREATE TRIGGER update_participant_trust_on_anomaly_detection
AFTER INSERT ON manipulation_indicators
FOR EACH ROW
WHEN NEW.severity >= 3
BEGIN
    -- Reduce trust score based on anomaly severity
    UPDATE participants
    SET 
        reputation_score = reputation_score * CASE 
            WHEN NEW.severity >= 5 THEN 0.5  -- Critical anomaly - 50% reduction
            WHEN NEW.severity >= 4 THEN 0.7  -- High anomaly - 30% reduction
            WHEN NEW.severity >= 3 THEN 0.85 -- Medium anomaly - 15% reduction
            ELSE reputation_score
        END,
        reputation_score = CASE 
            WHEN reputation_score < 0.1 THEN 0.1  -- Minimum reputation threshold
            ELSE reputation_score
        END,
        last_reviewed = CURRENT_TIMESTAMP
    WHERE public_key = NEW.node_id;
    
    -- Update trust propagation limits for anomalous participants
    INSERT OR REPLACE INTO node_trust_limits (
        node_id,
        max_weight,
        decay_factor,
        small_constants,
        last_adjusted_at
    )
    SELECT 
        NEW.node_id,
        CASE 
            WHEN NEW.severity >= 5 THEN 0.1   -- Severely limited for critical anomalies
            WHEN NEW.severity >= 4 THEN 0.3   -- Limited for high anomalies
            WHEN NEW.severity >= 3 THEN 0.5   -- Moderately limited for medium anomalies
            ELSE 0.8  -- Normal trust for low anomalies
        END AS max_weight,
        CASE 
            WHEN NEW.severity >= 4 THEN 0.8   -- Faster decay for high anomalies
            WHEN NEW.severity >= 3 THEN 0.9   -- Normal decay for medium anomalies
            ELSE 0.95  -- Slow decay for low anomalies
        END AS decay_factor,
        CASE 
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END AS small_constants,
        CURRENT_TIMESTAMP
    WHERE NEW.node_id IN (SELECT public_key FROM participants);
    
    -- Add to watchlist if severity is high
    INSERT OR IGNORE INTO manipulation_watchlist (
        participant_id,
        watch_reason,
        added_at,
        review_priority
    )
    SELECT 
        p.id,
        NEW.indicator_type || ': ' || NEW.details,
        CURRENT_TIMESTAMP,
        CASE 
            WHEN NEW.severity >= 5 THEN 'critical'
            WHEN NEW.severity >= 4 THEN 'high'
            WHEN NEW.severity >= 3 THEN 'medium'
            ELSE 'low'
        END AS priority
    FROM participants p
    WHERE p.public_key = NEW.node_id;
END;
```

### 5. detect_group_formation_manipulation
This trigger detects attempts to manipulate group formation or artificially inflate group metrics.

```sql
CREATE TRIGGER detect_group_formation_manipulation
AFTER INSERT ON participants_groups_members
FOR EACH ROW
BEGIN
    -- Detect rapid group joining patterns
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'RAPID_GROUP_JOINING',
        CASE 
            WHEN COUNT(*) > 10 THEN 5  -- Joining many groups rapidly
            WHEN COUNT(*) > 5 THEN 3   -- Joining several groups rapidly
            ELSE 1
        END AS severity,
        'Joined ' || COUNT(*) || ' groups in 24 hours',
        CURRENT_TIMESTAMP
    FROM participants_groups_members pjm
    WHERE pjm.participant_id = NEW.participant_id
    AND julianday('now') - julianday(pjm.joined_at, 'unixepoch') < 1  -- In last 24 hours
    GROUP BY pjm.participant_id
    HAVING COUNT(*) > 3;
    
    -- Detect artificial group inflation (creating many groups with few members)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'ARTIFICIAL_GROUP_CREATION',
        CASE 
            WHEN (
                SELECT AVG(member_count) < 2  -- Average less than 2 members per group
                FROM (
                    SELECT pg.id, COUNT(pgm.participant_id) as member_count
                    FROM participants_groups pg
                    LEFT JOIN participants_groups_members pgm ON pg.id = pgm.group_id
                    WHERE pg.created_by = NEW.participant_id
                    GROUP BY pg.id
                )
            ) AND (
                SELECT COUNT(*) > 5  -- Created more than 5 groups
                FROM participants_groups
                WHERE created_by = NEW.participant_id
            ) THEN 4
            ELSE 1
        END AS severity,
        'Created ' || (
            SELECT COUNT(*) 
            FROM participants_groups 
            WHERE created_by = NEW.participant_id
        ) || ' groups with low average membership',
        CURRENT_TIMESTAMP
    WHERE NEW.participant_id IN (
        SELECT created_by
        FROM participants_groups
        GROUP BY created_by
        HAVING COUNT(*) > 3
    );
    
    -- Update participant reputation based on group-related anomalies
    UPDATE participants
    SET 
        reputation_score = reputation_score * CASE 
            WHEN (SELECT COUNT(*) FROM manipulation_indicators WHERE node_id = (
                SELECT public_key FROM participants WHERE id = NEW.participant_id
            ) AND indicator_type LIKE '%GROUP%') > 0 THEN 0.9
            ELSE 1.0
        END,
        last_reviewed = CURRENT_TIMESTAMP
    WHERE id = NEW.participant_id;
END;
```

### 6. detect_temporal_manipulation_patterns
This trigger detects temporal patterns that might indicate manipulation attempts.

```sql
CREATE TRIGGER detect_temporal_manipulation_patterns
AFTER INSERT ON truth_event
FOR EACH ROW
BEGIN
    -- Detect burst activity patterns (many events in short time)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'BURST_ACTIVITY',
        CASE 
            WHEN COUNT(*) > 20 THEN 5  -- Very high activity
            WHEN COUNT(*) > 10 THEN 4  -- High activity
            WHEN COUNT(*) > 5 THEN 2   -- Moderate activity
            ELSE 1
        END AS severity,
        'Created ' || COUNT(*) || ' events in 1 hour',
        CURRENT_TIMESTAMP
    FROM truth_event te
    WHERE te.participant_id = NEW.participant_id
    AND julianday('now', 'subsec') - julianday(te.created_at, 'unixepoch', 'subsec') < 1.0/24.0  -- Within 1 hour
    GROUP BY te.participant_id
    HAVING COUNT(*) > 3;
    
    -- Detect coordinated temporal patterns across participants
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        NEW.participant_id,
        'COORDINATED_TIMING',
        CASE 
            WHEN COUNT(*) > 15 THEN 5  -- High coordination
            WHEN COUNT(*) > 8 THEN 4   -- Significant coordination
            WHEN COUNT(*) > 4 THEN 2   -- Some coordination
            ELSE 1
        END AS severity,
        'Detected ' || COUNT(*) || ' participants with events in same 5-minute window',
        CURRENT_TIMESTAMP
    FROM truth_event te
    WHERE ABS((julianday('now', 'subsec') - julianday(te.created_at, 'unixepoch', 'subsec')) * 86400) < 300  -- 5 minutes
    AND te.id != NEW.id
    AND julianday('now') - julianday(te.created_at, 'unixepoch') < 1  -- In last 24 hours
    GROUP BY CAST((julianday(te.created_at, 'unixepoch') * 86400 / 300) AS INTEGER)  -- Group by 5-minute intervals
    HAVING COUNT(*) > 5;
    
    -- Update participant metrics based on temporal patterns
    INSERT OR REPLACE INTO participant_behavior_patterns (
        participant_id,
        pattern_signature,
        stability_score,
        anomaly_score,
        updated_at
    )
    SELECT 
        NEW.participant_id,
        'temporal_activity_' || CAST((julianday(NEW.created_at, 'unixepoch') * 86400 / 3600) AS INTEGER),  -- Hourly pattern
        CASE 
            WHEN (
                SELECT AVG(activity_variance) < 0.1
                FROM (
                    SELECT 
                        CAST((julianday(created_at, 'unixepoch') * 86400 / 3600) AS INTEGER) AS hour,
                        COUNT(*) AS hourly_count
                    FROM truth_event
                    WHERE participant_id = NEW.participant_id
                    AND julianday('now') - julianday(created_at, 'unixepoch') < 7  -- Last 7 days
                    GROUP BY CAST((julianday(created_at, 'unixepoch') * 86400 / 3600) AS INTEGER)
                ) activity_by_hour
            ) THEN 0.8  -- Regular pattern
            ELSE 0.4  -- Irregular pattern
        END AS stability_score,
        CASE 
            WHEN (SELECT COUNT(*) FROM manipulation_indicators WHERE node_id = (
                SELECT public_key FROM participants WHERE id = NEW.participant_id
            ) AND indicator_type LIKE '%BURST%') > 0 THEN 0.7
            ELSE 0.2
        END AS anomaly_score,
        CURRENT_TIMESTAMP
    FROM participants
    WHERE id = NEW.participant_id;
END;
```

### 7. update_node_trust_on_manipulation_detection
This trigger reduces node trust when manipulation is detected.

```sql
CREATE TRIGGER update_node_trust_on_manipulation_detection
AFTER INSERT ON manipulation_indicators
FOR EACH ROW
WHEN NEW.severity >= 2
BEGIN
    -- Reduce trust score of affected nodes
    UPDATE node_ratings
    SET 
        trust_score = trust_score * CASE 
            WHEN NEW.severity >= 5 THEN 0.3  -- Critical: 70% reduction
            WHEN NEW.severity >= 4 THEN 0.5  -- High: 50% reduction
            WHEN NEW.severity >= 3 THEN 0.7  -- Medium: 30% reduction
            WHEN NEW.severity >= 2 THEN 0.9  -- Low: 10% reduction
            ELSE 1.0
        END,
        trust_score = CASE 
            WHEN trust_score < 0.05 THEN 0.05  -- Minimum trust threshold
            ELSE trust_score
        END,
        propagation_priority = propagation_priority * CASE 
            WHEN NEW.severity >= 4 THEN 0.5  -- Severely limit propagation for high severity
            WHEN NEW.severity >= 3 THEN 0.7  -- Limit propagation for medium severity
            WHEN NEW.severity >= 2 THEN 0.9  -- Slightly limit propagation for low severity
            ELSE 1.0
        END,
        last_updated = CURRENT_TIMESTAMP
    WHERE node_id = NEW.node_id;
    
    -- Update node performance metrics
    UPDATE node_performance
    SET 
        quality_index = quality_index * CASE 
            WHEN NEW.severity >= 4 THEN 0.6
            WHEN NEW.severity >= 3 THEN 0.8
            ELSE 0.95
        END,
        propagation_priority = propagation_priority * CASE 
            WHEN NEW.severity >= 4 THEN 0.4
            WHEN NEW.severity >= 3 THEN 0.7
            ELSE 0.9
        END
    WHERE pubkey = NEW.node_id;
    
    -- Add to node watchlist if severity is high
    INSERT OR IGNORE INTO manipulation_watchlist (
        participant_id,
        watch_reason,
        added_at,
        review_priority
    )
    SELECT 
        p.id,
        'Node manipulation: ' || NEW.indicator_type || ' (' || NEW.severity || ')',
        CURRENT_TIMESTAMP,
        CASE 
            WHEN NEW.severity >= 5 THEN 'critical'
            WHEN NEW.severity >= 4 THEN 'high'
            WHEN NEW.severity >= 3 THEN 'medium'
            ELSE 'low'
        END
    FROM participants p
    WHERE p.public_key = NEW.node_id;
    
    -- Log the trust adjustment
    INSERT INTO trust_adjustment_log (
        node_id,
        old_trust,
        new_trust,
        adjustment_reason,
        severity,
        adjusted_at
    )
    SELECT 
        NEW.node_id,
        (SELECT trust_score FROM node_ratings WHERE node_id = NEW.node_id),
        (SELECT trust_score FROM node_ratings WHERE node_id = NEW.node_id) * CASE 
            WHEN NEW.severity >= 5 THEN 0.3
            WHEN NEW.severity >= 4 THEN 0.5
            WHEN NEW.severity >= 3 THEN 0.7
            WHEN NEW.severity >= 2 THEN 0.9
            ELSE 1.0
        END,
        NEW.indicator_type,
        NEW.severity,
        CURRENT_TIMESTAMP;
END;
```

### 8. detect_convergence_manipulation
This trigger detects attempts to manipulate the convergence of truth and impact axes.

```sql
CREATE TRIGGER detect_convergence_manipulation
AFTER UPDATE ON event_ci
FOR EACH ROW
WHEN OLD.status != NEW.status AND NEW.status IN ('resolved', 'archived')
BEGIN
    -- Check if convergence happened unnaturally fast
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        ec.created_by,
        'RUSHED_CONVERGENCE',
        CASE 
            WHEN julianday(ec.created_at, 'unixepoch') - julianday(ec.created_at_original, 'unixepoch') < 1 THEN 5  -- Converged in less than 1 day
            WHEN julianday(ec.created_at, 'unixepoch') - julianday(ec.created_at_original, 'unixepoch') < 3 THEN 3  -- Converged in less than 3 days
            ELSE 1
        END AS severity,
        'Event converged abnormally fast (' || 
        ROUND(julianday(ec.created_at, 'unixepoch') - julianday(ec.created_at_original, 'unixepoch'), 2) || ' days)',
        CURRENT_TIMESTAMP
    FROM event_ci ec
    WHERE ec.id = NEW.id
    AND julianday('now') - julianday(ec.created_at_original, 'unixepoch') < 7;  -- Only check recent events
    
    -- Check for coordinated convergence (multiple participants converging simultaneously)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        ec.created_by,
        'COORDINATED_CONVERGENCE',
        CASE 
            WHEN COUNT(DISTINCT te.participant_id) > 10 THEN 5  -- Many participants coordinating
            WHEN COUNT(DISTINCT te.participant_id) > 5 THEN 3   -- Several participants coordinating
            ELSE 1
        END AS severity,
        'Detected ' || COUNT(DISTINCT te.participant_id) || ' participants converging simultaneously',
        CURRENT_TIMESTAMP
    FROM event_ci ec
    JOIN truth_event te ON ec.created_by = te.id
    WHERE ec.id = NEW.id
    AND julianday('now', 'subsec') - julianday(te.created_at, 'unixepoch', 'subsec') < 3600  -- Within 1 hour
    GROUP BY ec.id
    HAVING COUNT(DISTINCT te.participant_id) > 3;
    
    -- Update event stability metrics
    INSERT OR REPLACE INTO event_stability (
        event_id,
        truth_stable,
        impact_stable,
        stabilized_at
    )
    SELECT 
        ec.id,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5 AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-24 hours')
            ) THEN 1
            ELSE 0
        END AS truth_stable,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5 AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-24 hours')
            ) THEN 1
            ELSE 0
        END AS impact_stable,
        CASE 
            WHEN (
                SELECT COUNT(*) >= 5 AND ABS(AVG(truth_score) - MEDIAN(truth_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-24 hours')
            ) AND (
                SELECT COUNT(*) >= 5 AND ABS(AVG(impact_score) - MEDIAN(impact_score)) < 0.05
                FROM event_state_history
                WHERE event_id = ec.id
                AND recorded_at > datetime('now', '-24 hours')
            ) THEN CURRENT_TIMESTAMP
            ELSE NULL
        END AS stabilized_at
    FROM event_ci ec
    WHERE ec.id = NEW.id;
END;
```

### 9. update_participant_influence_limits_on_manipulation
This trigger adjusts influence limits when manipulation is detected.

```sql
CREATE TRIGGER update_participant_influence_limits_on_manipulation
AFTER INSERT ON manipulation_indicators
FOR EACH ROW
WHEN NEW.severity >= 3
BEGIN
    -- Adjust participant influence limits based on manipulation severity
    INSERT OR REPLACE INTO participant_influence_limits (
        participant_id,
        max_daily_assessments,
        max_event_assessments,
        trust_decay_rate,
        calculated_at
    )
    SELECT 
        p.id,
        CASE 
            WHEN NEW.severity >= 5 THEN 5    -- Very limited for critical violations
            WHEN NEW.severity >= 4 THEN 10   -- Limited for high violations
            WHEN NEW.severity >= 3 THEN 20   -- Moderately limited for medium violations
            ELSE 50  -- Normal limit for low violations
        END AS max_daily_assessments,
        CASE 
            WHEN NEW.severity >= 5 THEN 1    -- Only 1 assessment per event for critical
            WHEN NEW.severity >= 4 THEN 2    -- Up to 2 assessments per event for high
            WHEN NEW.severity >= 3 THEN 3    -- Up to 3 assessments per event for medium
            ELSE 5  -- Normal for low
        END AS max_event_assessments,
        CASE 
            WHEN NEW.severity >= 4 THEN 0.1  -- Fast decay for high severity
            WHEN NEW.severity >= 3 THEN 0.05 -- Medium decay for medium severity
            ELSE 0.01  -- Slow decay for low severity
        END AS trust_decay_rate,
        CURRENT_TIMESTAMP
    FROM participants p
    WHERE p.public_key = NEW.node_id;
    
    -- Reduce participant's weight in consensus calculations
    UPDATE judgment_weights
    SET 
        weight = weight * CASE 
            WHEN NEW.severity >= 5 THEN 0.3
            WHEN NEW.severity >= 4 THEN 0.5
            WHEN NEW.severity >= 3 THEN 0.7
            ELSE 0.9
        END,
        calculated_at = CURRENT_TIMESTAMP
    WHERE participant_id = (
        SELECT id FROM participants WHERE public_key = NEW.node_id
    );
    
    -- Update consensus calculations to reflect reduced weights
    INSERT OR REPLACE INTO consensus_ci (
        event_id,
        consensus_value,
        confidence_score,
        participant_count,
        calculated_at,
        algorithm_version
    )
    SELECT 
        j.event_id,
        AVG(j.assessment_value * jw.weight) AS consensus_value,
        AVG(j.confidence_level * jw.weight) AS confidence_score,
        COUNT(DISTINCT j.participant_id) AS participant_count,
        CURRENT_TIMESTAMP,
        1 AS algorithm_version
    FROM judgment j
    JOIN judgment_weights jw ON j.participant_id = jw.participant_id AND j.event_id = jw.event_id
    WHERE j.event_id IN (
        SELECT event_id FROM judgment WHERE participant_id = (
            SELECT id FROM participants WHERE public_key = NEW.node_id
        )
    )
    GROUP BY j.event_id;
END;
```

### 10. detect_prediction_manipulation
This trigger detects manipulation in prediction accuracy.

```sql
CREATE TRIGGER detect_prediction_manipulation
AFTER UPDATE ON impact_predictions
FOR EACH ROW
WHEN OLD.probability != NEW.probability OR OLD.horizon != NEW.horizon
BEGIN
    -- Detect if predictions are being manipulated to appear more accurate
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        details,
        detected_at
    )
    SELECT 
        te.participant_id,
        'PREDICTION_MANIPULATION',
        CASE 
            WHEN ABS(NEW.probability - (
                SELECT AVG(ABS(expected_strength - collective_score))
                FROM impact_predictions ip2
                JOIN truth_event te2 ON ip2.event_id = (
                    SELECT id FROM event_ci WHERE created_by = te2.id
                )
                WHERE te2.participant_id = te.participant_id
            )) > 0.5 THEN 4  -- Large deviation from historical accuracy
            WHEN NEW.horizon < 1 AND NEW.probability > 0.9 THEN 3  -- Very short horizon with high confidence
            ELSE 1
        END AS severity,
        'Suspicious prediction pattern: prob=' || NEW.probability || ', horizon=' || NEW.horizon,
        CURRENT_TIMESTAMP
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
    
    -- Update participant's prediction reputation
    UPDATE participants
    SET 
        reputation_score = reputation_score * CASE 
            WHEN (SELECT COUNT(*) FROM manipulation_indicators WHERE node_id = (
                SELECT public_key FROM participants WHERE id = te.participant_id
            ) AND indicator_type = 'PREDICTION_MANIPULATION') > 0 THEN 0.95
            ELSE 1.0
        END,
        last_reviewed = CURRENT_TIMESTAMP
    FROM truth_event te
    JOIN event_ci ec ON te.id = ec.created_by
    WHERE ec.id = NEW.event_id;
END;
```

## Additional Utility Triggers

### reset_manipulation_flags_after_review
Resets manipulation indicators after manual review.

```sql
CREATE TRIGGER reset_manipulation_flags_after_review
AFTER UPDATE ON manipulation_indicators
FOR EACH ROW
WHEN NEW.reviewed = 1 AND OLD.reviewed = 0
BEGIN
    -- If review indicates false positive, restore normal trust levels
    UPDATE participants
    SET 
        reputation_score = reputation_score / CASE 
            WHEN NEW.severity >= 5 THEN 0.5  -- Restore 50% reduction
            WHEN NEW.severity >= 4 THEN 0.7  -- Restore 30% reduction
            WHEN NEW.severity >= 3 THEN 0.85 -- Restore 15% reduction
            ELSE 1.0
        END,
        reputation_score = CASE 
            WHEN reputation_score > 1.0 THEN 1.0
            ELSE reputation_score
        END
    WHERE public_key = NEW.node_id
    AND NEW.review_result = 'false_positive';
    
    -- Update node trust if manipulation was false alarm
    UPDATE node_ratings
    SET 
        trust_score = trust_score / CASE 
            WHEN NEW.severity >= 5 THEN 0.3
            WHEN NEW.severity >= 4 THEN 0.5
            WHEN NEW.severity >= 3 THEN 0.7
            WHEN NEW.severity >= 2 THEN 0.9
            ELSE 1.0
        END,
        trust_score = CASE 
            WHEN trust_score > 1.0 THEN 1.0
            ELSE trust_score
        END
    WHERE node_id = NEW.node_id
    AND NEW.review_result = 'false_positive';
END;
```

## Notes

- All triggers maintain system integrity by detecting and responding to potential manipulation
- Trust scores are automatically adjusted based on detected anomalies
- The system maintains detailed logs for audit and review purposes
- Manipulation indicators are categorized by severity to prioritize response
- False positives can be corrected through manual review processes
- The system implements gradual trust restoration after false positives are identified
- All manipulation detection is based on pattern recognition, not content analysis
- Temporal patterns are a key indicator of coordinated manipulation attempts