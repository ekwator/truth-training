-- **Document Version:** v1.1.0
-- **Status:** Specification
-- **Updated:** 2025-12-28
-- **Status:** Approved
-- SQL Triggers for Node Discovery and Network Tables

-- Trigger to update trust score and propagation priority when node ratings are updated
-- Automatically recalculates trust score and propagation priority based on new event counts
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

-- Trigger to clean up expired tokens automatically
-- Removes tokens that have exceeded their expiration time
CREATE TRIGGER cleanup_expired_tokens
AFTER SELECT ON expired_tokens
BEGIN
    DELETE FROM active_tokens
    WHERE expires_at < (SELECT strftime('%s', 'now'));
END;

-- Trigger to update peer history when sync occurs
-- Automatically updates peer history with new synchronization information
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

-- Trigger to update node metrics when sync occurs
-- Updates performance metrics when synchronization events are logged
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