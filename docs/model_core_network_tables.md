-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Triggers for Node Discovery and Network Tables  

-- Trigger to update trust score and propagation priority when node ratings are updated  
-- Automatically recalculates trust score and propagation priority based on new event counts  
```sql
CREATE TRIGGER update_trust_score_after_rating_change
AFTER UPDATE ON node_ratings
BEGIN
    UPDATE node_ratings
    SET
        trust_score = (
            CASE
                WHEN (NEW.events_true + NEW.events_false) = 0 THEN 0.0
                ELSE (NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)
            END
        ),
        propagation_priority = (
            CASE
                WHEN NEW.validations > 0 THEN
                    (CASE
                        WHEN (NEW.events_true + NEW.events_false) = 0 THEN 0.0
                        ELSE (NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)
                    END * 0.5) +
                    (NEW.validations * 1.0 / (NEW.validations + 10)) * 0.3 +
                    (NEW.reused_events * 1.0 / (NEW.validations + 1)) * 0.2
                ELSE
                    (CASE
                        WHEN (NEW.events_true + NEW.events_false) = 0 THEN 0.0
                        ELSE (NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)
                    END * 0.5)
            END
        ),
        last_updated = (SELECT strftime('%s', 'now'))
    WHERE node_id = NEW.node_id;
END;
```
-- Trigger to clean up expired tokens automatically
-- Removes tokens that have exceeded their expiration time
```sql
CREATE TRIGGER cleanup_expired_tokens
AFTER INSERT ON active_tokens
BEGIN
    DELETE FROM active_tokens
    WHERE expires_at < (SELECT strftime('%s', 'now'));
END;
```
-- Trigger to update peer history when sync occurs  
-- Automatically updates peer history with new synchronization information  
```sql
CREATE TRIGGER update_peer_synchronization_after_sync
AFTER INSERT ON sync_attempts
BEGIN
    INSERT OR REPLACE INTO peer_synchronization (
        peer_url,
        mode,
        status,
        details,
        last_sync,
        success_count,
        fail_count,
        last_quality_index,
        last_trust_score
    )
    SELECT
        NEW.peer_url,
        NEW.mode,
        NEW.status,
        NEW.details,
        NEW.timestamp,
        CASE
            WHEN NEW.status = 'success' THEN
                COALESCE((SELECT success_count FROM peer_synchronization WHERE peer_url = NEW.peer_url), 0) + 1
            ELSE
                COALESCE((SELECT success_count FROM peer_synchronization WHERE peer_url = NEW.peer_url), 0)
        END,
        CASE
            WHEN NEW.status != 'success' THEN
                COALESCE((SELECT fail_count FROM peer_synchronization WHERE peer_url = NEW.peer_url), 0) + 1
            ELSE
                COALESCE((SELECT fail_count FROM peer_synchronization WHERE peer_url = NEW.peer_url), 0)
        END,
        COALESCE((SELECT quality_index FROM node_performance WHERE pubkey = (
            SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
        )), 0.0),
        COALESCE((SELECT trust_score FROM node_ratings WHERE node_id = (
            SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
        )), 0.0)
    WHERE NOT EXISTS (
        SELECT 1 FROM peer_synchronization WHERE peer_url = NEW.peer_url
    );
    
    UPDATE peer_synchronization
    SET
        mode = NEW.mode,
        status = NEW.status,
        details = NEW.details,
        last_sync = NEW.timestamp,
        success_count = CASE
            WHEN NEW.status = 'success' THEN success_count + 1
            ELSE success_count
        END,
        fail_count = CASE
            WHEN NEW.status != 'success' THEN fail_count + 1
            ELSE fail_count
        END,
        last_quality_index = COALESCE((SELECT quality_index FROM node_performance WHERE pubkey = (
            SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
        )), 0.0),
        last_trust_score = COALESCE((SELECT trust_score FROM node_ratings WHERE node_id = (
            SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
        )), 0.0)
    WHERE peer_url = NEW.peer_url;
END;
```
-- Trigger to update node metrics when sync occurs  
-- Updates performance metrics when synchronization events are logged  
```sql
CREATE TRIGGER update_node_performance_after_sync
AFTER INSERT ON sync_attempts
BEGIN
    INSERT OR REPLACE INTO node_performance (
        pubkey,
        last_seen,
        relay_success_rate,
        quality_index,
        propagation_priority
    )
    SELECT
        n.node_id,
        NEW.timestamp,
        COALESCE(nm.relay_success_rate, 0.0),
        COALESCE(nm.quality_index, 0.0),
        COALESCE(nr.propagation_priority, 0.0)
    FROM discovery_nodes n
    LEFT JOIN node_performance nm ON n.node_id = nm.pubkey
    LEFT JOIN node_ratings nr ON n.node_id = nr.node_id
    WHERE n.address = NEW.peer_url
    AND NOT EXISTS (SELECT 1 FROM node_performance WHERE pubkey = n.node_id);
    
    UPDATE node_performance
    SET
        last_seen = NEW.timestamp,
        relay_success_rate = (
            SELECT AVG(CASE WHEN sl.status = 'success' THEN 1.0 ELSE 0.0 END)
            FROM sync_attempts sl
            JOIN discovery_nodes n2 ON sl.peer_url = n2.address
            WHERE n2.node_id = node_performance.pubkey
        )
    WHERE pubkey = (
        SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
    );
END;
```

-- Trigger to update participant reputation based on sync accuracy
-- Updates participant reputation based on the accuracy of synced events
-- Implements the relationship: sync_operations.public_key → discovery_nodes.node_id → participants.public_key
```sql
CREATE TRIGGER update_participant_reputation_on_sync
AFTER INSERT ON sync_operations
BEGIN
    -- Update participant's reputation based on sync success/failure
    UPDATE participants
    SET
        total_judgment = total_judgment + 1,
        accurate_judgment = accurate_judgment + CASE
            WHEN NEW.op = 'insert' OR NEW.op = 'update' THEN 1  -- Consider successful sync operations as accurate
            ELSE 0
        END
    WHERE public_key = (
        SELECT node_id FROM discovery_nodes WHERE node_id = NEW.public_key
    );
    
    -- Update reputation score based on combined accuracy of both impact and judgment assessments
    UPDATE participants
    SET reputation_score = CASE
        WHEN (total_impact + total_judgment) > 0 THEN
            (accurate_impact + accurate_judgment) * 1.0 / (total_impact + total_judgment)
        ELSE 0.5
    END
    WHERE public_key = (
        SELECT node_id FROM discovery_nodes WHERE node_id = NEW.public_key
    );
END;
```

-- Trigger to update discovery history when new node is discovered
-- Implements the relationship: discovery_history.node_id → discovery_nodes.id
```sql
CREATE TRIGGER update_discovery_history_on_node_discovery
AFTER INSERT ON discovery_nodes
BEGIN
    INSERT INTO discovery_history (
        node_id,
        discovery_type,
        discovered_at,
        ttl,
        status,
        source
    )
    VALUES (
        NEW.id,
        'beacon',  -- Default discovery type
        NEW.created_at,
        NEW.ttl,
        'active',
        'automatic'
    );
END;
```

-- Trigger to update node trust limits based on sync performance
-- Implements the relationship: node_trust_limits.node_id → discovery_nodes.node_id
```sql
CREATE TRIGGER update_node_trust_limits_based_on_sync_performance
AFTER UPDATE ON sync_attempts
BEGIN
    INSERT OR REPLACE INTO node_trust_limits (
        node_id,
        max_weight,
        decay_factor,
        small_constants,
        last_adjusted_at
    )
    SELECT
        dn.node_id,
        CASE
            WHEN sa.status = 'success' THEN
                CASE
                    WHEN ntl.max_weight < 1.0 THEN ntl.max_weight + 0.1
                    ELSE 1.0
                END
            ELSE
                CASE
                    WHEN ntl.max_weight > 0.1 THEN ntl.max_weight - 0.1
                    ELSE 0.1
                END
        END as max_weight,
        CASE
            WHEN sa.status = 'success' THEN
                CASE
                    WHEN ntl.decay_factor < 1.0 THEN ntl.decay_factor + 0.05
                    ELSE 1.0
                END
            ELSE
                CASE
                    WHEN ntl.decay_factor > 0.01 THEN ntl.decay_factor - 0.05
                    ELSE 0.01
                END
        END as decay_factor,
        (CASE
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END) as small_constants,
        (SELECT strftime('%s', 'now')) as last_adjusted_at
    FROM discovery_nodes dn
    JOIN sync_attempts sa ON dn.address = sa.peer_url
    LEFT JOIN node_trust_limits ntl ON dn.node_id = ntl.node_id
    WHERE dn.node_id = (
        SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
    );
END;
```

-- Trigger to update node behavior patterns based on sync patterns
-- Implements the relationship: node_behavior_patterns.node_id → discovery_nodes.node_id
```sql
CREATE TRIGGER update_node_behavior_patterns_on_sync
AFTER INSERT ON sync_attempts
BEGIN
    INSERT OR REPLACE INTO node_behavior_patterns (
        node_id,
        pattern_signature,
        stability_score,
        anomaly_score,
        updated_at
    )
    SELECT
        dn.node_id,
        'SYNC_PATTERN_' || sa.status || '_' || sa.mode as pattern_signature,
        CASE
            WHEN sa.status = 'success' THEN
                COALESCE((SELECT stability_score FROM node_behavior_patterns WHERE node_id = dn.node_id), 0.5) + 0.1
            ELSE
                COALESCE((SELECT stability_score FROM node_behavior_patterns WHERE node_id = dn.node_id), 0.5) - 0.1
        END as stability_score,
        CASE
            WHEN sa.status = 'success' THEN
                COALESCE((SELECT anomaly_score FROM node_behavior_patterns WHERE node_id = dn.node_id), 0.5) - 0.1
            ELSE
                COALESCE((SELECT anomaly_score FROM node_behavior_patterns WHERE node_id = dn.node_id), 0.5) + 0.1
        END as anomaly_score,
        (SELECT strftime('%s', 'now')) as updated_at
    FROM discovery_nodes dn
    JOIN sync_attempts sa ON dn.address = sa.peer_url
    WHERE dn.node_id = (
        SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
    );
END;
```

-- Trigger to update manipulation indicators based on suspicious sync patterns
-- Implements the relationship: manipulation_indicators.node_id → discovery_nodes.node_id
```sql
CREATE TRIGGER update_manipulation_indicators_on_suspicious_sync
AFTER INSERT ON sync_attempts
BEGIN
    -- Check if there are suspicious patterns (too frequent sync attempts, etc.)
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        detected_at
    )
    SELECT
        dn.node_id,
        'HIGH_SYNC_FREQUENCY' as indicator_type,
        CASE
            WHEN sync_count > 100 THEN 3  -- High severity
            WHEN sync_count > 50 THEN 2   -- Medium severity
            ELSE 1                        -- Low severity
        END as severity,
        (SELECT strftime('%s', 'now')) as detected_at
    FROM discovery_nodes dn
    JOIN (
        SELECT
            sa.peer_url,
            COUNT(*) as sync_count
        FROM sync_attempts sa
        WHERE sa.timestamp > (SELECT strftime('%s', 'now') - 3600)  -- Count syncs in last hour
        GROUP BY sa.peer_url
        HAVING COUNT(*) > 10  -- Threshold for considering high frequency
    ) sync_freq ON dn.address = sync_freq.peer_url
    WHERE dn.node_id = (
        SELECT node_id FROM discovery_nodes WHERE address = NEW.peer_url
    );
END;
```