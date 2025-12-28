-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved
-- SQL Model for Node Discovery and Network Tables
-- Based on docs/model_core.md:3276-3648
--
-- This file defines the SQL schema and calculation logic for the network layer tables
-- in the Truth Training system, including node discovery, reputation, performance metrics,
-- authentication, and synchronization tracking.

-- Table for storing information about discovered nodes in Truth Training network
-- Purpose: Tracking peer nodes, their addresses, types, availability and other discovery metadata
-- Fields: id (unique node identifier), address (URL or ip:port of node), type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
--       reachable (availability flag), last_seen (time of last successful contact), ttl (record lifetime)
--       source (source of node discovery), node_id (node's public key), timestamps
CREATE TABLE nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES participants(public_key)
);

-- Indexes for performance optimization on nodes table
CREATE INDEX idx_nodes_address ON nodes(address);
CREATE INDEX idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX idx_nodes_type ON nodes(type);
CREATE INDEX idx_nodes_reachable ON nodes(reachable);
CREATE INDEX idx_nodes_node_id ON nodes(node_id);

-- Table for storing node reputation and trust metrics
-- Purpose: Evaluating node reliability based on their activity and assessment accuracy
-- Fields: node_id (unique node identifier), events_true/false (number of true/false events)
--       validations (number of confirmations), reused_events (number of reused events)
--       trust_score (overall trust rating -1.0 to 1.0), propagation_priority (distribution priority)
--       last_updated (timestamp of last update)
CREATE TABLE node_ratings (
    node_id TEXT PRIMARY KEY,
    events_true INTEGER NOT NULL DEFAULT 0,
    events_false INTEGER NOT NULL DEFAULT 0,
    validations INTEGER NOT NULL DEFAULT 0,
    reused_events INTEGER NOT NULL DEFAULT 0,
    trust_score REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    last_updated INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);

-- Function to calculate trust score based on events and validations
-- Trust(n) = (events_true - events_false) / (events_true + events_false + ε)
-- When no events have been processed, trust score defaults to 0.0 (neutral trust)
CREATE VIEW trust_score_calculation AS
SELECT
    node_id,
    CASE
        WHEN (events_true + events_false) = 0 THEN 0.0
        ELSE (events_true - events_false) * 1.0 / (events_true + events_false)
    END as calculated_trust_score
FROM node_ratings;

-- Function to calculate propagation priority based on trust and activity metrics
-- Priority(n) = f(trust_score, validation_count, reuse_frequency)
-- Combines trust score (50%), validation frequency (30%), and reuse frequency (20%)
CREATE VIEW propagation_priority_calculation AS
SELECT
    nr.node_id,
    CASE
        WHEN nr.validations > 0 THEN
            (nr.trust_score * 0.5) +
            (nr.validations * 1.0 / (nr.validations + 10)) * 0.3 +
            (nr.reused_events * 1.0 / (nr.validations + 1)) * 0.2
        ELSE nr.trust_score * 0.5
    END as calculated_priority
FROM node_ratings nr;

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

-- Table for monitoring node performance and status
-- Purpose: Tracking node performance metrics for synchronization optimization
-- Fields: pubkey (unique node identifier), last_seen (time of last contact)
--       relay_success_rate (percentage of successful transfers), quality_index (continuity of trust indicator)
--       propagation_priority (distribution priority)
CREATE TABLE node_metrics (
    pubkey TEXT PRIMARY KEY,
    last_seen INTEGER NOT NULL,
    relay_success_rate REAL NOT NULL DEFAULT 0.0,
    quality_index REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (pubkey) REFERENCES nodes(node_id)
);

-- Function to calculate relay success rate
-- Success rate = successful_operations / total_operations
-- This would require additional tracking tables for successful/total operations
CREATE VIEW relay_success_rate_calculation AS
SELECT
    pubkey,
    CASE
        WHEN total_operations > 0 THEN successful_operations * 1.0 / total_operations
        ELSE 0.0
    END as calculated_success_rate
FROM (
    SELECT
        nm.pubkey,
        nm.successful_operations,
        nm.total_operations
    FROM node_metrics nm
    -- This would require additional tracking tables for successful/total operations
);

-- Function to calculate quality index based on multiple factors
-- Q(n) = α * recent_performance + β * historical_consistency + γ * trust_factor
-- Where α=0.4, β=0.4, γ=0.2 (these weights can be adjusted as needed)
CREATE VIEW quality_index_calculation AS
SELECT
    nm.pubkey,
    (0.4 * COALESCE(recent_performance, 0.0)) +
    (0.4 * COALESCE(historical_consistency, 0.0)) +
    (0.2 * COALESCE(nr.trust_score, 0.0)) as calculated_quality_index
FROM node_metrics nm
LEFT JOIN node_ratings nr ON nm.pubkey = nr.node_id;

-- Table for managing authentication sessions based on JWT tokens
-- Purpose: Storing active refresh tokens allowing access token renewal without re-authentication
-- Fields: public_key (node public key), refresh_token (refresh token value), expires_at (expiration timestamp)
CREATE TABLE active_tokens (
    public_key TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES nodes(node_id)
);

-- Function to check token expiration status
-- Returns tokens that have expired based on current timestamp
CREATE VIEW expired_tokens AS
SELECT
    public_key,
    refresh_token,
    expires_at
FROM active_tokens
WHERE expires_at < (SELECT strftime('%s', 'now'));

-- Trigger to clean up expired tokens automatically
-- Removes tokens that have exceeded their expiration time
CREATE TRIGGER cleanup_expired_tokens
AFTER SELECT ON expired_tokens
BEGIN
    DELETE FROM active_tokens
    WHERE expires_at < (SELECT strftime('%s', 'now'));
END;

-- Table for storing peer synchronization history
-- Purpose: Tracking interaction history with each node for diagnostics and reliability analysis
-- Fields: id (unique history record), peer_url (peer node URL), mode (synchronization mode)
--       status (synchronization status), details (synchronization details), last_sync (time of last sync)
--       success/fail counts, quality and trust metrics at time of synchronization
CREATE TABLE peer_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    peer_url TEXT NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL,
    last_sync INTEGER,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    last_quality_index REAL DEFAULT 0.0,
    last_trust_score REAL DEFAULT 0.0,
    FOREIGN KEY (peer_url) REFERENCES nodes(address)
);

-- Function to calculate success rate from peer history
-- Success rate = success_count / (success_count + fail_count)
CREATE VIEW peer_success_rate_calculation AS
SELECT
    peer_url,
    CASE
        WHEN (success_count + fail_count) > 0 THEN
            success_count * 1.0 / (success_count + fail_count)
        ELSE 0.0
    END as calculated_success_rate
FROM peer_history;

-- Trigger to update peer history when sync occurs
-- Automatically updates peer history with new synchronization information
CREATE TRIGGER update_peer_history_after_sync
AFTER INSERT ON sync_logs
BEGIN
    INSERT OR REPLACE INTO peer_history (
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
                COALESCE((SELECT success_count FROM peer_history WHERE peer_url = NEW.peer_url), 0) + 1
            ELSE
                COALESCE((SELECT success_count FROM peer_history WHERE peer_url = NEW.peer_url), 0)
        END,
        CASE
            WHEN NEW.status != 'success' THEN
                COALESCE((SELECT fail_count FROM peer_history WHERE peer_url = NEW.peer_url), 0) + 1
            ELSE
                COALESCE((SELECT fail_count FROM peer_history WHERE peer_url = NEW.peer_url), 0)
        END,
        COALESCE((SELECT quality_index FROM node_metrics WHERE pubkey = (
            SELECT node_id FROM nodes WHERE address = NEW.peer_url
        )), 0.0),
        COALESCE((SELECT trust_score FROM node_ratings WHERE node_id = (
            SELECT node_id FROM nodes WHERE address = NEW.peer_url
        )), 0.0)
    WHERE NOT EXISTS (
        SELECT 1 FROM peer_history WHERE peer_url = NEW.peer_url
    );
    
    UPDATE peer_history
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
        last_quality_index = COALESCE((SELECT quality_index FROM node_metrics WHERE pubkey = (
            SELECT node_id FROM nodes WHERE address = NEW.peer_url
        )), 0.0),
        last_trust_score = COALESCE((SELECT trust_score FROM node_ratings WHERE node_id = (
            SELECT node_id FROM nodes WHERE address = NEW.peer_url
        )), 0.0)
    WHERE peer_url = NEW.peer_url;
END;

-- Table for tracking low-level synchronization operations
-- Purpose: Tracking changes at individual record level, auditing and debugging synchronization
-- Fields: id (log record identifier), op (operation type: insert/update/delete)
--       table_name (name of affected table), record_id (identifier of affected record)
--       signature (signature of synchronization participant), public_key (participant public key)
--       created_at (timestamp of operation)
CREATE TABLE sync_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    op TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT NOT NULL,
    signature TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES nodes(node_id)
);

-- Function to verify signature integrity
-- Checks if the signature matches the public key and operation data
-- Note: signature_verification function would need to be implemented separately
CREATE VIEW sync_integrity_check AS
SELECT
    sl.id,
    sl.op,
    sl.table_name,
    sl.record_id,
    sl.signature,
    sl.public_key,
    CASE
        WHEN signature_verification(sl.public_key, sl.signature,
            sl.op || sl.table_name || sl.record_id || sl.created_at) = TRUE
        THEN 'VALID'
        ELSE 'INVALID'
    END as integrity_status
FROM sync_log sl;

-- Table for recording high-level synchronization events between nodes
-- Purpose: Recording synchronization events between nodes for monitoring network-wide operations
-- Fields: id (log record identifier), timestamp (when sync occurred), peer_url (peer node URL)
--       mode (sync mode or protocol), status (result status), details (additional info)
CREATE TABLE sync_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    peer_url TEXT NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL
);

-- Function to calculate synchronization statistics
-- Provides aggregated statistics on synchronization attempts by peer
CREATE VIEW sync_statistics AS
SELECT
    peer_url,
    COUNT(*) as total_sync_attempts,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_syncs,
    SUM(CASE WHEN status != 'success' THEN 1 ELSE 0 END) as failed_syncs,
    AVG(CASE WHEN status = 'success' THEN 1.0 ELSE 0.0 END) as success_rate
FROM sync_logs
GROUP BY peer_url;

-- Function to identify stale nodes based on TTL
-- Nodes where time since last contact exceeds the TTL value
CREATE VIEW stale_nodes AS
SELECT
    id,
    address,
    type,
    last_seen,
    ttl,
    (SELECT strftime('%s', 'now')) - last_seen as time_since_last_seen
FROM nodes
WHERE ((SELECT strftime('%s', 'now')) - last_seen) > ttl;

-- Function to identify unreachable nodes that exceed TTL/2
-- Nodes that are marked as unreachable and have exceeded half their TTL
CREATE VIEW unreachable_nodes AS
SELECT
    id,
    address,
    type,
    last_seen,
    ttl,
    reachable,
    (SELECT strftime('%s', 'now')) - last_seen as time_since_last_seen
FROM nodes
WHERE reachable = 0
  AND ((SELECT strftime('%s', 'now')) - last_seen) > (ttl / 2);

-- Trigger to update node metrics when sync occurs
-- Updates performance metrics when synchronization events are logged
CREATE TRIGGER update_node_metrics_after_sync
AFTER INSERT ON sync_logs
BEGIN
    INSERT OR REPLACE INTO node_metrics (
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
    FROM nodes n
    LEFT JOIN node_metrics nm ON n.node_id = nm.pubkey
    LEFT JOIN node_ratings nr ON n.node_id = nr.node_id
    WHERE n.address = NEW.peer_url
    AND NOT EXISTS (SELECT 1 FROM node_metrics WHERE pubkey = n.node_id);
    
    UPDATE node_metrics
    SET
        last_seen = NEW.timestamp,
        relay_success_rate = (
            SELECT AVG(CASE WHEN sl.status = 'success' THEN 1.0 ELSE 0.0 END)
            FROM sync_logs sl
            JOIN nodes n2 ON sl.peer_url = n2.address
            WHERE n2.node_id = node_metrics.pubkey
        )
    WHERE pubkey = (
        SELECT node_id FROM nodes WHERE address = NEW.peer_url
    );
END;

-- Indexes for performance optimization
CREATE INDEX idx_node_ratings_node_id ON node_ratings(node_id);
CREATE INDEX idx_node_metrics_pubkey ON node_metrics(pubkey);
CREATE INDEX idx_active_tokens_public_key ON active_tokens(public_key);
CREATE INDEX idx_active_tokens_expires_at ON active_tokens(expires_at);
CREATE INDEX idx_peer_history_peer_url ON peer_history(peer_url);
CREATE INDEX idx_sync_log_public_key ON sync_log(public_key);
CREATE INDEX idx_sync_logs_peer_url ON sync_logs(peer_url);
CREATE INDEX idx_sync_logs_timestamp ON sync_logs(timestamp);