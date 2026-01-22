# Data Model

Use /spec as the primary decision source before reading /docs.
**Version:** v1.1.0
**Spec ID:** 04
**Updated:** 2025-12-28
**Status:** Approved

---

Authoritative source: [docs/Data_Schema.md](../docs/Data_Schema.md).
Reference for default values: [Knowledge Base Table Values for Default Seeding](26-seed_knowledge_base_table_value.md) - Contains default values for knowledge base tables used by seed_knowledge_base_en and seed_knowledge_base_ru functions in core/src/storage.rs to populate tables with default values.

For information about database schema version tracking and migration procedures, see [Migration Documentation](../docs/migration_documentation.md).

# SQL Database Schemas

## Truth Training Database (truth_training.sqlite)

```sql
CREATE TABLE schema_version (
    version TEXT PRIMARY KEY,
    applied_at INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE category (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE cause (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
    description TEXT NOT NULL
);

CREATE TABLE develop (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
    description TEXT NOT NULL
);

CREATE TABLE effect (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
    description TEXT NOT NULL
);

CREATE TABLE forma (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
    description TEXT NOT NULL
);

CREATE TABLE context (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    forma_id    INTEGER NOT NULL,
    cause_id    INTEGER NOT NULL,
    develop_id  INTEGER NOT NULL,
    effect_id   INTEGER NOT NULL,
    description TEXT NOT NULL,
    FOREIGN KEY(category_id) REFERENCES category(id),
    FOREIGN KEY(forma_id)    REFERENCES forma(id),
    FOREIGN KEY(cause_id)    REFERENCES cause(id),
    FOREIGN KEY(develop_id)  REFERENCES develop(id),
    FOREIGN KEY(effect_id)   REFERENCES effect(id)
);

CREATE TABLE participants (
    public_key         TEXT PRIMARY KEY,
    signature          TEXT NOT NULL,
    reputation_score   REAL NOT NULL DEFAULT 0.5,
    reputation_history INTEGER NOT NULL,
    total_judgment     INTEGER NOT NULL DEFAULT 0,
    accurate_judgment INTEGER NOT NULL DEFAULT 0,
    total_impact       INTEGER NOT NULL DEFAULT 0,
    accurate_impact    INTEGER NOT NULL DEFAULT 0,
    created_at         INTEGER NOT NULL,
    last_activity      INTEGER,
    FOREIGN KEY(reputation_history) REFERENCES reputation_history(id)
);

CREATE TABLE reputation_history (
    id             INTEGER PRIMARY KEY,
    old_reputation REAL NOT NULL,
    new_reputation REAL NOT NULL,
    change_reason  TEXT NOT NULL,
    updated_at     INTEGER NOT NULL
);

CREATE TABLE truth_event (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    description      TEXT NOT NULL,
    global_id        TEXT NOT NULL UNIQUE,
    participant_id   TEXT NOT NULL,
    signature        TEXT NOT NULL,
    category_id      INTEGER NOT NULL,
    forma_id         INTEGER NOT NULL,
    cause_id         INTEGER NOT NULL,
    develop_id       INTEGER NOT NULL,
    effect_id        INTEGER NOT NULL,
    vector           INTEGER NOT NULL,
    detected         INTEGER,
    corrected        INTEGER NOT NULL DEFAULT 0,
    timeline_id      INTEGER NOT NULL,
    code             INTEGER NOT NULL DEFAULT 1,
    collective_score REAL NOT NULL,
    impact_score     REAL NOT NULL,
    judgment_score   REAL,
    FOREIGN KEY (participant_id) REFERENCES participants(public_key),
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (forma_id) REFERENCES forma(id),
    FOREIGN KEY (cause_id) REFERENCES cause(id),
    FOREIGN KEY (develop_id) REFERENCES develop(id),
    FOREIGN KEY (effect_id) REFERENCES effect(id),
    FOREIGN KEY (timeline_id) REFERENCES event_timeline(id)
);

CREATE TABLE event_ci (
    id              INTEGER PRIMARY KEY,
    created_by      INTEGER NOT NULL,
    event_type      TEXT NOT NULL DEFAULT 'judgment',
    status          TEXT NOT NULL DEFAULT 'active',
    old_status      TEXT NOT NULL DEFAULT 'active',
    resolution_data TEXT NOT NULL DEFAULT 'unstable',
    created_at      INTEGER NOT NULL,
    FOREIGN KEY (created_by) REFERENCES truth_event(id)
);

CREATE TABLE impact (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id           INTEGER NOT NULL,
    type_id            INTEGER NOT NULL,
    trend              INTEGER NOT NULL,
    value              INTEGER,
    notes              TEXT,
    impact_metrics     INTEGER NOT NULL,
    impact_predictions INTEGER NOT NULL,
    signature          TEXT NOT NULL,
    timeline_id        INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES truth_event(id),
    FOREIGN KEY (type_id) REFERENCES effect(id),
    FOREIGN KEY (impact_metrics) REFERENCES impact_metrics(id),
    FOREIGN KEY (impact_predictions) REFERENCES impact_predictions(id),
    FOREIGN KEY (timeline_id) REFERENCES impact_timeline(id)
);

CREATE TABLE impact_metrics (
    id              INTEGER PRIMARY KEY,
    event_id        INTEGER NOT NULL,
    total_magnitude INTEGER,
    positive_ratio  INTEGER,
    negative_ratio  INTEGER,
    uncertainty     INTEGER,
    calculated_at   INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE impact_predictions (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id              INTEGER NOT NULL,
    predicted_impact_type INTEGER NOT NULL,
    expected_strength     REAL NOT NULL,
    probability           REAL NOT NULL,
    horizon               REAL NOT NULL,
    created_at            INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (predicted_impact_type) REFERENCES effect(id)
);

CREATE TABLE impact_links (
    source_impact_id INTEGER NOT NULL,
    target_impact_id INTEGER NOT NULL,
    relation_type    TEXT NOT NULL,
    signature        TEXT NOT NULL,
    created_at       INTEGER NOT NULL,
    FOREIGN KEY (source_impact_id) REFERENCES impact(id),
    FOREIGN KEY (target_impact_id) REFERENCES impact(id)
);

CREATE TABLE judgment (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    participant_id   INTEGER NOT NULL,
    event_id         INTEGER NOT NULL,
    assessment       REAL,
    confidence_level REAL,
    reasoning        TEXT,
    consensus_ci     INTEGER NOT NULL,
    judgment_weights INTEGER NOT NULL,
    signature        TEXT NOT NULL,
    timeline_id      INTEGER NOT NULL,
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (consensus_ci) REFERENCES consensus_ci(id),
    FOREIGN KEY (judgment_weights) REFERENCES judgment_weights(id),
    FOREIGN KEY (timeline_id) REFERENCES judgment_timeline(id),
    UNIQUE(participant_id, event_id)
);

CREATE TABLE judgment_links (
    source_judgment_id INTEGER NOT NULL,
    target_judgment_id INTEGER NOT NULL,
    relation_type      TEXT NOT NULL,
    signature          TEXT NOT NULL,
    created_at         INTEGER NOT NULL,
    FOREIGN KEY (source_judgment_id) REFERENCES judgment(id),
    FOREIGN KEY (target_judgment_id) REFERENCES judgment(id)
);

CREATE TABLE consensus_ci (
    id                INTEGER PRIMARY KEY,
    event_id          INTEGER NOT NULL,
    consensus_value   INTEGER NOT NULL,
    confidence_score  REAL NOT NULL,
    participant_count INTEGER NOT NULL,
    calculated_at     INTEGER NOT NULL,
    algorithm_version INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE judgment_weights (
    id             INTEGER PRIMARY KEY,
    participant_id INTEGER NOT NULL,
    event_id       INTEGER NOT NULL,
    weight         REAL,
    calculated_at  INTEGER NOT NULL,
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE statements (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id    INTEGER NOT NULL,
    truth_score REAL,
    created_at INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES truth_event(id)
);

CREATE TABLE progress_metrics (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    total_events                 INTEGER NOT NULL,
    total_events_group           INTEGER NOT NULL,
    total_positive_impacts       REAL NOT NULL,
    total_positive_impacts_group REAL NOT NULL,
    total_negative_impacts       REAL NOT NULL,
    total_negative_impact_group  REAL NOT NULL,
    trend                        REAL NOT NULL,
    trend_group                  REAL NOT NULL,
    last_updated                 INTEGER NOT NULL
);

CREATE TABLE event_projection (
    event_id      INTEGER NOT NULL,
    truth_score   REAL NOT NULL,
    impact_score  REAL NOT NULL,
    quadrant      TEXT,
    calculated_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE event_links (
    source_impact_id INTEGER NOT NULL,
    target_impact_id INTEGER NOT NULL,
    relation_type    TEXT NOT NULL,
    signature        TEXT NOT NULL,
    created_at       INTEGER NOT NULL,
    FOREIGN KEY (source_impact_id) REFERENCES truth_event(id),
    FOREIGN KEY (target_impact_id) REFERENCES truth_event(id)
);

CREATE TABLE time_axes (
    id          INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    time_type   TEXT NOT NULL,
    created_at  INTEGER NOT NULL
);

CREATE TABLE event_timeline (
    id           INTEGER PRIMARY KEY,
    time_axis_id INTEGER NOT NULL,
    signature    TEXT NOT NULL,
    t_start      INTEGER NOT NULL,
    t_end        INTEGER,
    FOREIGN KEY (time_axis_id) REFERENCES time_axes(id)
);

CREATE TABLE impact_timeline (
    id           INTEGER PRIMARY KEY,
    time_axis_id INTEGER NOT NULL,
    signature    TEXT NOT NULL,
    t_start      INTEGER NOT NULL,
    t_end        INTEGER,
    FOREIGN KEY (time_axis_id) REFERENCES time_axes(id)
);

CREATE TABLE judgment_timeline (
    id           INTEGER PRIMARY KEY,
    time_axis_id INTEGER NOT NULL,
    signature    TEXT NOT NULL,
    t_start      INTEGER NOT NULL,
    t_end        INTEGER,
    FOREIGN KEY (time_axis_id) REFERENCES time_axes(id)
);

CREATE TABLE truth_state (
    id            INTEGER PRIMARY KEY,
    event_id      INTEGER NOT NULL,
    time_axis_id INTEGER NOT NULL,
    truth_state   TEXT NOT NULL,
    truth_score   REAL NOT NULL,
    dispersion    REAL NOT NULL,
    confidence    REAL NOT NULL,
    calculated_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id),
    FOREIGN KEY (time_axis_id) REFERENCES time_axes(id)
);

CREATE TABLE event_state_history (
    id             INTEGER PRIMARY KEY,
    event_id       INTEGER NOT NULL,
    judgment_count INTEGER NOT NULL,
    truth_score    REAL NOT NULL,
    impact_count   INTEGER NOT NULL,
    impact_score   REAL NOT NULL,
    recorded_at    INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE event_stability (
    id            INTEGER PRIMARY KEY,
    event_id      INTEGER NOT NULL,
    truth_stable INTEGER,
    impact_stable INTEGER,
    stabilized_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event_ci(id)
);

CREATE TABLE judgment_heuristics (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    judgment_id  INTEGER NOT NULL,
    heuristic_id INTEGER NOT NULL,
    influence    REAL NOT NULL,
    created_at   INTEGER NOT NULL,
    FOREIGN KEY (judgment_id) REFERENCES judgment(id),
    FOREIGN KEY (heuristic_id) REFERENCES expert_heuristics(id)
);

CREATE TABLE expert_heuristics (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,
    description     TEXT NOT NULL,
    domain          TEXT NOT NULL,
    weight          REAL NOT NULL,
    confidence      REAL NOT NULL,
    proven_accuracy REAL NOT NULL,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

-- Indexes for performance optimization
CREATE INDEX idx_truth_event_global_id ON truth_event(global_id);
CREATE INDEX idx_truth_event_participant_id ON truth_event(participant_id);
CREATE INDEX idx_impact_event_id ON impact(event_id);
CREATE INDEX idx_judgment_event_id ON judgment(event_id);
CREATE INDEX idx_judgment_participant_id ON judgment(participant_id);
CREATE INDEX idx_statements_event_id ON statements(event_id);
CREATE INDEX idx_event_ci_created_by ON event_ci(created_by);
CREATE INDEX idx_participants_public_key ON participants(public_key);
```

## Discovery Nodes Database (discovery_nodes.sqlite)

```sql
CREATE TABLE schema_version (
    version TEXT PRIMARY KEY,
    applied_at INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE discovery_nodes (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    address    TEXT NOT NULL UNIQUE,
    type       TEXT NOT NULL,
    reachable  INTEGER NOT NULL,
    last_seen  INTEGER NOT NULL,
    ttl        INTEGER NOT NULL,
    source     TEXT,
    node_id    TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES participants(public_key)
);

CREATE TABLE discovery_history (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    node_id        INTEGER NOT NULL,
    discovery_type TEXT NOT NULL,
    discovered_at  INTEGER NOT NULL,
    ttl            INTEGER NOT NULL,
    status         TEXT NOT NULL,
    source         TEXT NOT NULL,
    FOREIGN KEY (node_id) REFERENCES discovery_nodes(id)
);

CREATE TABLE node_ratings (
    node_id              TEXT PRIMARY KEY,
    events_true          INTEGER NOT NULL DEFAULT 0,
    events_false         INTEGER NOT NULL DEFAULT 0,
    validations          INTEGER NOT NULL DEFAULT 0,
    reused_events        INTEGER NOT NULL DEFAULT 0,
    trust_score          REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    last_updated         INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES discovery_nodes(node_id)
);

CREATE TABLE node_performance (
    pubkey               TEXT PRIMARY KEY,
    last_seen            INTEGER NOT NULL,
    relay_success_rate   REAL NOT NULL DEFAULT 0.0,
    quality_index        REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (pubkey) REFERENCES discovery_nodes(node_id)
);

CREATE TABLE active_tokens (
    public_key    TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    expires_at    INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES discovery_nodes(node_id)
);

CREATE TABLE peer_synchronization (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    peer_url           TEXT NOT NULL,
    mode               TEXT NOT NULL,
    status             TEXT NOT NULL,
    details            TEXT NOT NULL,
    last_sync          INTEGER,
    success_count      INTEGER DEFAULT 0,
    fail_count         INTEGER DEFAULT 0,
    last_quality_index REAL DEFAULT 0.0,
    last_trust_score   REAL DEFAULT 0.0,
    FOREIGN KEY (peer_url) REFERENCES discovery_nodes(address)
);

CREATE TABLE sync_operations (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    op         TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT NOT NULL,
    signature  TEXT,
    public_key TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES discovery_nodes(node_id)
);

CREATE TABLE sync_attempts (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    peer_url  TEXT NOT NULL,
    mode      TEXT NOT NULL,
    status    TEXT NOT NULL,
    details   TEXT NOT NULL
);

CREATE TABLE node_trust_limits (
    node_id          TEXT NOT NULL PRIMARY KEY,
    max_weight       REAL NOT NULL,
    decay_factor     REAL NOT NULL,
    small_constants  REAL NOT NULL,
    last_adjusted_at INTEGER NOT NULL
);

CREATE TABLE node_behavior_signatures (
    node_id         TEXT NOT NULL,
    signature       TEXT NOT NULL,
    stability_score REAL NOT NULL,
    anomaly_score   REAL NOT NULL,
    updated_at      INTEGER NOT NULL
);

CREATE TABLE manipulation_flags (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    node_id     TEXT NOT NULL,
    flag_type   TEXT,
    severity    INTEGER NOT NULL,
    detected_at INTEGER NOT NULL
);

-- Indexes for performance optimization
CREATE INDEX idx_discovery_nodes_address ON discovery_nodes(address);
CREATE INDEX idx_discovery_nodes_last_seen ON discovery_nodes(last_seen);
CREATE INDEX idx_discovery_nodes_type ON discovery_nodes(type);
CREATE INDEX idx_discovery_nodes_reachable ON discovery_nodes(reachable);
CREATE INDEX idx_discovery_nodes_node_id ON discovery_nodes(node_id);
CREATE INDEX idx_discovery_history_node_id ON discovery_history(node_id);
CREATE INDEX idx_node_ratings_node_id ON node_ratings(node_id);
CREATE INDEX idx_node_performance_pubkey ON node_performance(pubkey);
CREATE INDEX idx_active_tokens_public_key ON active_tokens(public_key);
CREATE INDEX idx_active_tokens_expires_at ON active_tokens(expires_at);
CREATE INDEX idx_peer_synchronization_peer_url ON peer_synchronization(peer_url);
CREATE INDEX idx_sync_operations_public_key ON sync_operations(public_key);
CREATE INDEX idx_sync_attempts_peer_url ON sync_attempts(peer_url);
CREATE INDEX idx_sync_attempts_timestamp ON sync_attempts(timestamp);
```

## Summary

The schemas for the two databases (truth_training.sqlite and discovery_nodes.sqlite) are now properly separated with distinct purposes:

- **truth_training.sqlite** - Core application database containing tables for events, impacts, judgments, and user data
- **discovery_nodes.sqlite** - Network discovery database containing tables for node discovery, performance monitoring, and synchronization logs
