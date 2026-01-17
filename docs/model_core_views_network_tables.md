-- **Document Version:** v1.1.0  
-- **Status:** Specification  
-- **Updated:** 2025-12-28  
-- **Status:** Approved  
-- SQL Views for Node Discovery and Network Tables  

-- Function to calculate trust score based on events and validations  
-- Trust(n) = (events_true - events_false) / (events_true + events_false + ε)  
-- When no events have been processed, trust score defaults to 0.0 (neutral trust)  
```
CREATE VIEW trust_score_calculation AS
SELECT
    node_id,
    CASE
        WHEN (events_true + events_false) = 0 THEN 0.0
        ELSE (events_true - events_false) * 1.0 / (events_true + events_false)
    END as calculated_trust_score
FROM node_ratings;
```
-- Function to calculate propagation priority based on trust and activity metrics  
-- Priority(n) = f(trust_score, validation_count, reuse_frequency)  
-- Combines trust score (50%), validation frequency (30%), and reuse frequency (20%)  
```
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
```
-- Function to calculate relay success rate  
-- Success rate = successful_operations / total_operations  
-- This would require additional tracking tables for successful/total operations  
```
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
    FROM node_performance nm
    -- This would require additional tracking tables for successful/total operations
);
```
-- Function to calculate quality index based on multiple factors  
-- Q(n) = α * recent_performance + β * historical_consistency + γ * trust_factor  
-- Where α=0.4, β=0.4, γ=0.2 (these weights can be adjusted as needed)  
```
CREATE VIEW quality_index_calculation AS
SELECT
    nm.pubkey,
    (0.4 * COALESCE(recent_performance, 0.0)) +
    (0.4 * COALESCE(historical_consistency, 0.0)) +
    (0.2 * COALESCE(nr.trust_score, 0.0)) as calculated_quality_index
FROM node_performance nm
LEFT JOIN node_ratings nr ON nm.pubkey = nr.node_id;
```
-- Function to check token expiration status  
-- Returns tokens that have expired based on current timestamp  
```
CREATE VIEW expired_tokens AS
SELECT
    public_key,
    refresh_token,
    expires_at
FROM active_tokens
WHERE expires_at < (SELECT strftime('%s', 'now'));
```
-- Function to calculate success rate from peer history  
-- Success rate = success_count / (success_count + fail_count)  
```
CREATE VIEW peer_success_rate_calculation AS
SELECT
    peer_url,
    CASE
        WHEN (success_count + fail_count) > 0 THEN
            success_count * 1.0 / (success_count + fail_count)
        ELSE 0.0
    END as calculated_success_rate
FROM peer_synchronization;
```
-- Function to verify signature integrity  
-- Checks if the signature matches the public key and operation data  
-- Note: signature_verification function would need to be implemented separately  
```
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
FROM sync_operations sl;
```
-- Function to calculate synchronization statistics  
-- Provides aggregated statistics on synchronization attempts by peer  
```
CREATE VIEW sync_statistics AS
SELECT
    peer_url,
    COUNT(*) as total_sync_attempts,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_syncs,
    SUM(CASE WHEN status != 'success' THEN 1 ELSE 0 END) as failed_syncs,
    AVG(CASE WHEN status = 'success' THEN 1.0 ELSE 0.0 END) as success_rate
FROM sync_attempts
GROUP BY peer_url;
```
-- Function to identify stale nodes based on TTL  
-- Nodes where time since last contact exceeds the TTL value  
```
CREATE VIEW stale_nodes AS
SELECT
    id,
    address,
    type,
    last_seen,
    ttl,
    (SELECT strftime('%s', 'now')) - last_seen as time_since_last_seen
FROM discovery_nodes
WHERE ((SELECT strftime('%s', 'now')) - last_seen) > ttl;
```
-- Function to identify unreachable nodes that exceed TTL/2  
-- Nodes that are marked as unreachable and have exceeded half their TTL  
```
CREATE VIEW unreachable_nodes AS
SELECT
    id,
    address,
    type,
    last_seen,
    ttl,
    reachable,
    (SELECT strftime('%s', 'now')) - last_seen as time_since_last_seen
FROM discovery_nodes
WHERE reachable = 0
  AND ((SELECT strftime('%s', 'now')) - last_seen) > (ttl / 2);
```