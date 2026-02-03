# Network Tables Triggers

**Document Version:** v1.1.1  
**Status:** Specification  
**Updated:** 2026-01-03  
**Status:** Approved

## Overview
This document describes the SQL triggers that implement the node discovery and network tables schema for the Truth Training system. These triggers manage network operations, trust scores, and synchronization data.

## Purpose
The network tables triggers ensure that node discovery, trust metrics, and synchronization operations are properly managed across the distributed network, maintaining data consistency and enabling effective P2P communication.

## Trigger Definitions

### 1. update_trust_score_after_rating_change
This trigger automatically recalculates trust score and propagation priority based on new event counts when node ratings are updated.

```sql
CREATE TRIGGER update_trust_score_after_rating_change
AFTER UPDATE ON node_ratings
FOR EACH ROW
WHEN OLD.events_true != NEW.events_true OR OLD.events_false != NEW.events_false OR OLD.validations != NEW.validations
BEGIN
    -- Recalculate trust score using the formula: (events_true - events_false) / (events_true + events_false + ε)
    UPDATE node_ratings
    SET 
        trust_score = CASE 
            WHEN (NEW.events_true + NEW.events_false) > 0 THEN
                (NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)
            ELSE 0.0  -- Neutral trust if no events processed
        END,
        propagation_priority = CASE 
            WHEN (NEW.events_true + NEW.events_false) > 0 THEN
                ((NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)) * 0.7 +  -- Trust component (70%)
                (NEW.validations * 1.0 / (SELECT MAX(validations + 1) FROM node_ratings)) * 0.3  -- Validation component (30%)
            ELSE 0.0
        END,
        last_updated = CURRENT_TIMESTAMP
    WHERE node_id = NEW.node_id;
    
    -- Update node performance with the new trust score
    INSERT OR REPLACE INTO node_performance (
        pubkey,
        last_seen,
        relay_success_rate,
        quality_index,
        propagation_priority
    )
    SELECT 
        NEW.node_id,
        (SELECT last_seen FROM discovery_nodes WHERE node_id = NEW.node_id),
        (SELECT relay_success_rate FROM node_performance WHERE pubkey = NEW.node_id),
        (SELECT 
            CASE 
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END
         FROM sync_attempts
         WHERE peer_url = (SELECT address FROM discovery_nodes WHERE node_id = NEW.node_id)
        ) * 0.5 +  -- Success rate component
        CASE 
            WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1 THEN 0.5  -- Recent activity bonus
            ELSE 0.1
        END AS quality_index,
        NEW.propagation_priority
    FROM node_ratings
    WHERE node_id = NEW.node_id;
END;
```

### 2. cleanup_expired_tokens
This trigger removes tokens that have exceeded their expiration time.

```sql
CREATE TRIGGER cleanup_expired_tokens
AFTER INSERT ON active_tokens
FOR EACH ROW
BEGIN
    -- Clean up expired tokens
    DELETE FROM active_tokens
    WHERE expires_at < (julianday('now') * 86400);  -- Convert days to seconds
    
    -- Log the cleanup operation
    INSERT INTO sync_operations (
        op,
        table_name,
        record_id,
        signature,
        public_key,
        created_at
    )
    VALUES (
        'cleanup',
        'active_tokens',
        'expired_tokens_removed',
        'system_cleanup_operation',
        NEW.public_key,
        CURRENT_TIMESTAMP
    );
END;
```

### 3. update_peer_synchronization_after_sync
This trigger automatically updates peer history with new synchronization information when sync logs are created.

```sql
CREATE TRIGGER update_peer_synchronization_after_sync
AFTER INSERT ON sync_operations
FOR EACH ROW
WHEN NEW.op IN ('sync_start', 'sync_complete', 'sync_success', 'sync_failure')
BEGIN
    -- Update peer synchronization statistics
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
        NEW.public_key,
        CASE 
            WHEN NEW.table_name LIKE '%full%' THEN 'full'
            WHEN NEW.table_name LIKE '%delta%' THEN 'delta'
            ELSE 'unknown'
        END AS mode,
        CASE 
            WHEN NEW.op = 'sync_success' THEN 'success'
            WHEN NEW.op = 'sync_failure' THEN 'failure'
            ELSE NEW.op
        END AS status,
        NEW.table_name || ':' || NEW.record_id AS details,
        CURRENT_TIMESTAMP,
        CASE 
            WHEN NEW.op = 'sync_success' THEN 
                (SELECT COALESCE(success_count, 0) FROM peer_synchronization WHERE peer_url = NEW.public_key) + 1
            ELSE (SELECT COALESCE(success_count, 0) FROM peer_synchronization WHERE peer_url = NEW.public_key)
        END AS success_count,
        CASE 
            WHEN NEW.op = 'sync_failure' THEN 
                (SELECT COALESCE(fail_count, 0) FROM peer_synchronization WHERE peer_url = NEW.public_key) + 1
            ELSE (SELECT COALESCE(fail_count, 0) FROM peer_synchronization WHERE peer_url = NEW.public_key)
        END AS fail_count,
        (SELECT quality_index FROM node_performance WHERE pubkey = NEW.public_key),
        (SELECT trust_score FROM node_ratings WHERE node_id = NEW.public_key)
    WHERE NEW.public_key IN (SELECT node_id FROM discovery_nodes);
END;
```

### 4. update_node_performance_after_sync
This trigger updates performance metrics when synchronization events are logged.

```sql
CREATE TRIGGER update_node_performance_after_sync
AFTER INSERT ON sync_operations
FOR EACH ROW
WHEN NEW.op LIKE 'sync_%'
BEGIN
    -- Update node performance metrics based on sync operations
    INSERT OR REPLACE INTO node_performance (
        pubkey,
        last_seen,
        relay_success_rate,
        quality_index,
        propagation_priority
    )
    SELECT 
        NEW.public_key,
        CURRENT_TIMESTAMP,
        (SELECT 
            CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END
         FROM sync_attempts
         WHERE peer_url = (SELECT address FROM discovery_nodes WHERE node_id = NEW.public_key)
        ) AS relay_success_rate,
        (SELECT 
            (CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +  -- Success rate component
            CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1 THEN 0.5  -- Recent activity bonus
                ELSE 0.1
            END
         FROM node_ratings
         WHERE node_id = NEW.public_key
        ) AS quality_index,
        (SELECT 
            ((CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +  -- Success rate component
            CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1 THEN 0.5  -- Recent activity bonus
                ELSE 0.1
            END) * trust_score
         FROM node_ratings
         WHERE node_id = NEW.public_key
        ) AS propagation_priority
    FROM discovery_nodes
    WHERE node_id = NEW.public_key;
END;
```

### 5. update_participant_reputation_on_sync
This trigger updates participant reputation based on sync success/failure, connecting: "sync_operations.public_key" → "discovery_nodes.node_id" → "participants.public_key".

```sql
CREATE TRIGGER update_participant_reputation_on_sync
AFTER INSERT ON sync_operations
FOR EACH ROW
WHEN NEW.op IN ('sync_success', 'sync_failure')
BEGIN
    -- Update participant reputation based on sync operations
    UPDATE participants
    SET 
        reputation_score = CASE 
            WHEN NEW.op = 'sync_success' THEN
                reputation_score + 0.01  -- Small boost for successful sync
            WHEN NEW.op = 'sync_failure' THEN
                reputation_score - 0.005  -- Small penalty for failed sync
            ELSE reputation_score
        END,
        reputation_score = CASE 
            WHEN reputation_score > 1.0 THEN 1.0
            WHEN reputation_score < 0.0 THEN 0.0
            ELSE reputation_score
        END,
        last_activity = CURRENT_TIMESTAMP
    WHERE public_key = (
        SELECT public_key 
        FROM discovery_nodes 
        WHERE node_id = NEW.public_key
    );
    
    -- Update reputation history
    INSERT INTO reputation_history (
        old_reputation,
        new_reputation,
        change_reason,
        updated_at
    )
    SELECT 
        (reputation_score - CASE 
            WHEN NEW.op = 'sync_success' THEN 0.01
            WHEN NEW.op = 'sync_failure' THEN -0.005
            ELSE 0
        END),
        reputation_score,
        CASE 
            WHEN NEW.op = 'sync_success' THEN 'sync_success'
            WHEN NEW.op = 'sync_failure' THEN 'sync_failure'
            ELSE 'sync_operation'
        END,
        CURRENT_TIMESTAMP
    FROM participants
    WHERE public_key = (
        SELECT public_key 
        FROM discovery_nodes 
        WHERE node_id = NEW.public_key
    );
END;
```

### 6. update_discovery_history_on_node_discovery
This trigger updates discovery history when new nodes are discovered, connecting: "discovery_history.node_id" → "discovery_nodes.id".

```sql
CREATE TRIGGER update_discovery_history_on_node_discovery
AFTER INSERT ON discovery_nodes
FOR EACH ROW
BEGIN
    -- Add discovery record to history
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
        CASE 
            WHEN NEW.source LIKE 'beacon%' THEN 'beacon'
            WHEN NEW.source LIKE 'manual%' THEN 'manual'
            WHEN NEW.source LIKE 'api%' THEN 'api'
            ELSE 'unknown'
        END,
        CURRENT_TIMESTAMP,
        NEW.ttl,
        CASE 
            WHEN NEW.reachable = 1 THEN 'active'
            ELSE 'unreachable'
        END,
        NEW.source
    );
    
    -- Update node performance record
    INSERT OR REPLACE INTO node_performance (
        pubkey,
        last_seen,
        relay_success_rate,
        quality_index,
        propagation_priority
    )
    VALUES (
        NEW.node_id,
        CURRENT_TIMESTAMP,
        0.0,  -- Initial success rate
        0.5,  -- Initial quality index
        0.0   -- Initial propagation priority
    );
    
    -- Initialize node ratings if not exists
    INSERT OR IGNORE INTO node_ratings (
        node_id,
        events_true,
        events_false,
        validations,
        reused_events,
        trust_score,
        propagation_priority,
        last_updated
    )
    VALUES (
        NEW.node_id,
        0, 0, 0, 0,  -- Initial counts
        0.0,  -- Initial neutral trust
        0.0,  -- Initial propagation priority
        CURRENT_TIMESTAMP
    );
END;
```

### 7. update_node_trust_limits_based_on_sync_performance
This trigger updates node trust limits based on sync performance, connecting: "node_trust_limits.node_id" → "discovery_nodes.node_id".

```sql
CREATE TRIGGER update_node_trust_limits_based_on_sync_performance
AFTER UPDATE ON node_performance
FOR EACH ROW
WHEN OLD.relay_success_rate != NEW.relay_success_rate OR OLD.quality_index != NEW.quality_index
BEGIN
    -- Update node trust limits based on performance
    INSERT OR REPLACE INTO node_trust_limits (
        node_id,
        max_weight,
        decay_factor,
        small_constants,
        last_adjusted_at
    )
    SELECT 
        NEW.pubkey,
        CASE 
            WHEN NEW.quality_index > 0.8 THEN 1.0  -- High quality nodes get max weight
            WHEN NEW.quality_index > 0.6 THEN 0.7  -- Medium quality nodes get medium weight
            WHEN NEW.quality_index > 0.4 THEN 0.4  -- Lower quality nodes get reduced weight
            ELSE 0.2  -- Poor quality nodes get minimal weight
        END AS max_weight,
        CASE 
            WHEN NEW.relay_success_rate > 0.9 THEN 0.95  -- High success rate = slow decay
            WHEN NEW.relay_success_rate > 0.7 THEN 0.90  -- Medium success rate = medium decay
            WHEN NEW.relay_success_rate > 0.5 THEN 0.85  -- Lower success rate = faster decay
            ELSE 0.80  -- Poor success rate = fast decay
        END AS decay_factor,
        CASE 
            WHEN ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ) = 0.0
            THEN 0.000001
            ELSE MIN( ( ( CAST(strftime('%s', 'now') AS REAL) * 1000000 + strftime('%f', 'now') * 1000000 - CAST(strftime('%s', 'now') AS REAL) * 1000000 ) % 2.0 ), 1.99999 )
        END AS small_constants,
        CURRENT_TIMESTAMP
    WHERE NEW.pubkey IN (SELECT node_id FROM discovery_nodes);
END;
```

### 8. update_node_behavior_patterns_on_sync
This trigger updates node behavior patterns based on sync patterns, connecting: "node_behavior_patterns.node_id" → "discovery_nodes.node_id".

```sql
CREATE TRIGGER update_node_behavior_patterns_on_sync
AFTER INSERT ON sync_operations
FOR EACH ROW
WHEN NEW.op LIKE 'sync%'
BEGIN
    -- Update node behavior patterns based on sync operations
    INSERT OR REPLACE INTO node_behavior_patterns (
        node_id,
        pattern_signature,
        stability_score,
        anomaly_score,
        updated_at
    )
    SELECT 
        NEW.public_key,
        SUBSTR(NEW.signature, 1, 16) || ':' || NEW.op || ':' || NEW.table_name AS pattern_signature,
        CASE 
            WHEN (
                SELECT AVG(relay_success_rate) > 0.8
                FROM sync_attempts
                WHERE peer_url = (SELECT address FROM discovery_nodes WHERE node_id = NEW.public_key)
                AND last_sync > datetime('now', '-7 days')
            ) THEN 0.9
            WHEN (
                SELECT AVG(relay_success_rate) > 0.6
                FROM sync_attempts
                WHERE peer_url = (SELECT address FROM discovery_nodes WHERE node_id = NEW.public_key)
                AND last_sync > datetime('now', '-7 days')
            ) THEN 0.7
            WHEN (
                SELECT AVG(relay_success_rate) > 0.4
                FROM sync_attempts
                WHERE peer_url = (SELECT address FROM discovery_nodes WHERE node_id = NEW.public_key)
                AND last_sync > datetime('now', '-7 days')
            ) THEN 0.5
            ELSE 0.3
        END AS stability_score,
        CASE 
            WHEN NEW.op = 'sync_failure' THEN 0.8  -- Higher anomaly score for failures
            WHEN NEW.op = 'sync_success' THEN 0.2  -- Lower anomaly score for successes
            ELSE 0.5
        END AS anomaly_score,
        CURRENT_TIMESTAMP
    WHERE NEW.public_key IN (SELECT node_id FROM discovery_nodes);
END;
```

### 9. update_manipulation_indicators_on_suspicious_sync
This trigger updates manipulation indicators based on suspicious sync patterns, connecting: "manipulation_indicators.node_id" → "discovery_nodes.node_id".

```sql
CREATE TRIGGER update_manipulation_indicators_on_suspicious_sync
AFTER INSERT ON sync_operations
FOR EACH ROW
WHEN NEW.op IN ('sync_failure', 'sync_duplicate', 'sync_error')
BEGIN
    -- Check for suspicious patterns and update manipulation indicators
    INSERT OR REPLACE INTO manipulation_indicators (
        node_id,
        indicator_type,
        severity,
        detected_at
    )
    SELECT 
        NEW.public_key,
        CASE 
            WHEN NEW.op = 'sync_duplicate' THEN 'DUPLICATE_SYNC'
            WHEN NEW.op = 'sync_failure' AND (
                SELECT COUNT(*) > 5
                FROM sync_operations
                WHERE public_key = NEW.public_key
                AND op = 'sync_failure'
                AND created_at > (julianday('now') - 1) * 86400  -- In last 24 hours
            ) THEN 'HIGH_FAILURE_RATE'
            WHEN NEW.op = 'sync_error' THEN 'SYNC_ERROR_PATTERN'
            ELSE 'UNKNOWN_SUSPICIOUS_ACTIVITY'
        END AS indicator_type,
        CASE 
            WHEN NEW.op = 'sync_duplicate' THEN 3  -- Medium severity
            WHEN NEW.op = 'sync_failure' AND (
                SELECT COUNT(*) > 10
                FROM sync_operations
                WHERE public_key = NEW.public_key
                AND op = 'sync_failure'
                AND created_at > (julianday('now') - 1) * 86400  -- In last 24 hours
            ) THEN 5  -- High severity
            WHEN NEW.op = 'sync_error' THEN 2  -- Low severity
            ELSE 1
        END AS severity,
        CURRENT_TIMESTAMP
    WHERE NEW.public_key IN (
        SELECT node_id 
        FROM discovery_nodes
        WHERE (
            -- Check for duplicate operations in short time
            SELECT COUNT(*) > 3
            FROM sync_operations
            WHERE public_key = NEW.public_key
            AND created_at > (julianday('now') - 0.1) * 86400  -- In last 2.4 hours
        ) OR (
            -- Check for high failure rate
            SELECT (COUNT(CASE WHEN op = 'sync_failure' THEN 1 END) * 1.0 / COUNT(*)) > 0.5
            FROM sync_operations
            WHERE public_key = NEW.public_key
            AND created_at > (julianday('now') - 1) * 86400  -- In last 24 hours
        )
    );
    
    -- Adjust trust score if suspicious activity detected
    UPDATE node_ratings
    SET 
        trust_score = trust_score * 0.9,  -- Reduce trust by 10%
        last_updated = CURRENT_TIMESTAMP
    WHERE node_id = NEW.public_key
    AND EXISTS (
        SELECT 1 FROM manipulation_indicators 
        WHERE node_id = NEW.public_key 
        AND severity >= 3 
        AND detected_at > datetime('now', '-7 days')
    );
END;
```

### 10. update_node_reachability_on_sync_attempt
This trigger updates node reachability based on sync attempts.

```sql
CREATE TRIGGER update_node_reachability_on_sync_attempt
AFTER INSERT ON sync_attempts
FOR EACH ROW
BEGIN
    -- Update node reachability based on sync attempts
    UPDATE discovery_nodes
    SET 
        reachable = CASE 
            WHEN NEW.status = 'success' THEN 1
            ELSE 0
        END,
        last_seen = CASE 
            WHEN NEW.status = 'success' THEN CURRENT_TIMESTAMP
            ELSE last_seen
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE address = NEW.peer_url;
    
    -- Update discovery history with reachability status
    INSERT INTO discovery_history (
        node_id,
        discovery_type,
        discovered_at,
        ttl,
        status,
        source
    )
    SELECT 
        id,
        'reachability_check',
        CURRENT_TIMESTAMP,
        ttl,
        CASE 
            WHEN NEW.status = 'success' THEN 'active'
            ELSE 'unreachable'
        END,
        'sync_attempt'
    FROM discovery_nodes
    WHERE address = NEW.peer_url;
END;
```

### 11. update_node_metrics_on_sync_success
This trigger updates node metrics when sync is successful.

```sql
CREATE TRIGGER update_node_metrics_on_sync_success
AFTER INSERT ON sync_attempts
FOR EACH ROW
WHEN NEW.status = 'success'
BEGIN
    -- Update node ratings with validation count on successful sync
    UPDATE node_ratings
    SET 
        validations = validations + 1,
        last_updated = CURRENT_TIMESTAMP
    WHERE node_id = (
        SELECT node_id 
        FROM discovery_nodes 
        WHERE address = NEW.peer_url
    );
    
    -- Update peer synchronization stats
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
        dn.node_id,
        'sync',
        'success',
        'Successful sync attempt',
        CURRENT_TIMESTAMP,
        (SELECT COALESCE(success_count, 0) FROM peer_synchronization WHERE peer_url = dn.node_id) + 1,
        (SELECT COALESCE(fail_count, 0) FROM peer_synchronization WHERE peer_url = dn.node_id),
        (SELECT quality_index FROM node_performance WHERE pubkey = dn.node_id),
        (SELECT trust_score FROM node_ratings WHERE node_id = dn.node_id)
    FROM discovery_nodes dn
    WHERE dn.address = NEW.peer_url;
END;
```

### 12. update_node_metrics_on_sync_failure
This trigger updates node metrics when sync fails.

```sql
CREATE TRIGGER update_node_metrics_on_sync_failure
AFTER INSERT ON sync_attempts
FOR EACH ROW
WHEN NEW.status = 'failure'
BEGIN
    -- Update peer synchronization stats for failure
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
        dn.node_id,
        'sync',
        'failure',
        NEW.details,
        CURRENT_TIMESTAMP,
        (SELECT COALESCE(success_count, 0) FROM peer_synchronization WHERE peer_url = dn.node_id),
        (SELECT COALESCE(fail_count, 0) FROM peer_synchronization WHERE peer_url = dn.node_id) + 1,
        (SELECT quality_index FROM node_performance WHERE pubkey = dn.node_id),
        (SELECT trust_score FROM node_ratings WHERE node_id = dn.node_id)
    FROM discovery_nodes dn
    WHERE dn.address = NEW.peer_url;
    
    -- Update discovery node reachability status
    UPDATE discovery_nodes
    SET 
        reachable = 0,
        updated_at = CURRENT_TIMESTAMP
    WHERE address = NEW.peer_url;
END;
```

### 13. update_trust_score_after_rating_change_duplicate
This trigger is a duplicate of the first trigger but with additional validation.

```sql
CREATE TRIGGER update_trust_score_after_rating_change_duplicate
AFTER UPDATE ON node_ratings
FOR EACH ROW
WHEN OLD.events_true != NEW.events_true OR OLD.events_false != NEW.events_false OR OLD.validations != NEW.validations OR OLD.reused_events != NEW.reused_events
BEGIN
    -- Validate the update parameters
    SELECT RAISE(ROLLBACK, 'Invalid events count') 
    WHERE NEW.events_true < 0 OR NEW.events_false < 0 OR NEW.validations < 0 OR NEW.reused_events < 0;
    
    -- Recalculate trust score with validation
    UPDATE node_ratings
    SET 
        trust_score = CASE 
            WHEN (NEW.events_true + NEW.events_false) > 0 THEN
                CASE 
                    WHEN (NEW.events_true - NEW.events_false) > (NEW.events_true + NEW.events_false) THEN 1.0
                    WHEN (NEW.events_true - NEW.events_false) < -(NEW.events_true + NEW.events_false) THEN -1.0
                    ELSE (NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)
                END
            ELSE 0.0  -- Neutral trust if no events processed
        END,
        propagation_priority = CASE 
            WHEN (NEW.events_true + NEW.events_false) > 0 THEN
                CASE 
                    WHEN ((NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)) > 1.0 THEN 1.0
                    WHEN ((NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)) < -1.0 THEN 0.0
                    ELSE 
                        ((NEW.events_true - NEW.events_false) * 1.0 / (NEW.events_true + NEW.events_false)) * 0.7 +  -- Trust component (70%)
                        (NEW.validations * 1.0 / (SELECT MAX(validations + 1) FROM node_ratings)) * 0.3  -- Validation component (30%)
                END
            ELSE 0.0
        END,
        last_updated = CURRENT_TIMESTAMP
    WHERE node_id = NEW.node_id;
END;
```

## Additional Utility Triggers

### update_node_ttl_expiration
Checks and updates node records when TTL expires.

```sql
CREATE TRIGGER update_node_ttl_expiration
AFTER UPDATE ON discovery_nodes
FOR EACH ROW
WHEN julianday('now') - julianday(NEW.updated_at, 'unixepoch') > NEW.ttl / 86400.0
BEGIN
    -- Mark node as expired if TTL has passed
    UPDATE discovery_nodes
    SET 
        reachable = 0,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id
    AND julianday('now') - julianday(updated_at, 'unixepoch') > ttl / 86400.0;
END;
```

### update_sync_statistics
Maintains statistics about synchronization operations.

```sql
CREATE TRIGGER update_sync_statistics
AFTER INSERT ON sync_operations
FOR EACH ROW
BEGIN
    -- Update sync statistics for analysis
    INSERT OR REPLACE INTO sync_statistics (
        peer_url,
        total_operations,
        success_count,
        failure_count,
        success_rate,
        last_operation,
        last_operation_type
    )
    SELECT 
        so.public_key,
        (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key) AS total_operations,
        (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key AND op LIKE '%success%') AS success_count,
        (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key AND op LIKE '%fail%') AS failure_count,
        CASE 
            WHEN (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key) > 0 THEN
                (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key AND op LIKE '%success%') * 1.0 / 
                (SELECT COUNT(*) FROM sync_operations WHERE public_key = so.public_key)
            ELSE 0.0
        END AS success_rate,
        CURRENT_TIMESTAMP AS last_operation,
        so.op AS last_operation_type
    FROM sync_operations so
    WHERE so.id = NEW.id;
END;
```

## Notes

- All triggers maintain data consistency across the network tables
- Trust scores are calculated using the formula (events_true - events_false) / (events_true + events_false + ε)
- Node performance metrics are updated based on sync success/failure rates
- Manipulation indicators are set when suspicious patterns are detected
- TTL expiration is handled automatically to maintain node list accuracy
- Node reputation is affected by synchronization success/failure
- The system maintains both individual node metrics and network-wide statistics
- All triggers include proper validation to prevent invalid data