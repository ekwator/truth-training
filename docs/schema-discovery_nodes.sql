CREATE TABLE schema_version (
            version TEXT PRIMARY KEY,
            applied_at INTEGER NOT NULL,
            description TEXT
        );
CREATE TABLE category (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT
);
CREATE TABLE cause (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
    description TEXT
);
CREATE TABLE develop (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL,
    description TEXT
);
CREATE TABLE effect (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL,
    description TEXT
);
CREATE TABLE forma (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL,
    description TEXT
);
CREATE TABLE context (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
category_id INTEGER,
    forma_id    INTEGER,
    cause_id    INTEGER,
    develop_id  INTEGER,
    effect_id   INTEGER,
    description TEXT,
    FOREIGN KEY(category_id) REFERENCES category(id),
    FOREIGN KEY(forma_id)    REFERENCES forma(id),
    FOREIGN KEY(cause_id)    REFERENCES cause(id),
    FOREIGN KEY(develop_id)  REFERENCES develop(id),
    FOREIGN KEY(effect_id)   REFERENCES effect(id)
);
CREATE TABLE impact_type (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
description TEXT
);
CREATE TABLE nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE TABLE sqlite_sequence(name,seq);
CREATE INDEX idx_nodes_address ON nodes(address);
CREATE INDEX idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX idx_nodes_type ON nodes(type);
CREATE INDEX idx_nodes_reachable ON nodes(reachable);
CREATE TABLE truth_events (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    description     TEXT NOT NULL,
    category_id     INTEGER,                -- FK → category.id, nullable
    forma_id        INTEGER,                -- FK → forma.id, nullable
    cause_id        INTEGER,                -- FK → cause.id, nullable
    develop_id      INTEGER,                -- FK → develop.id, nullable
    effect_id       INTEGER,                -- FK → effect.id, nullable
    vector          INTEGER NOT NULL,       -- 0/1 вместо BOOLEAN
    detected        INTEGER,                -- NULL/0/1
    corrected       INTEGER NOT NULL DEFAULT 0,
timestamp_start INTEGER NOT NULL,
    timestamp_end   INTEGER,
    code            INTEGER NOT NULL DEFAULT 1,  -- 8-bit event code
    collective_score REAL, signature TEXT, public_key TEXT,
    FOREIGN KEY(category_id) REFERENCES category(id),
    FOREIGN KEY(forma_id) REFERENCES forma(id),
    FOREIGN KEY(cause_id) REFERENCES cause(id),
    FOREIGN KEY(develop_id) REFERENCES develop(id),
    FOREIGN KEY(effect_id) REFERENCES effect(id)
);
CREATE INDEX idx_truth_events_category ON truth_events(category_id);
CREATE INDEX idx_truth_events_forma ON truth_events(forma_id);
CREATE INDEX idx_truth_events_cause ON truth_events(cause_id);
CREATE INDEX idx_truth_events_develop ON truth_events(develop_id);
CREATE INDEX idx_truth_events_effect ON truth_events(effect_id);
CREATE TABLE statements (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id        INTEGER NOT NULL,
    text            TEXT NOT NULL,
    context         TEXT,
    truth_score     REAL,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL, signature TEXT, public_key TEXT,
    FOREIGN KEY(event_id) REFERENCES truth_events(id)
);
CREATE TABLE impact (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_id INTEGER NOT NULL,
            type_id INTEGER NOT NULL,
            value INTEGER NOT NULL,      -- SQLite bool (0/1)
            notes TEXT,
            created_at INTEGER NOT NULL, signature TEXT, public_key TEXT,
            FOREIGN KEY(event_id) REFERENCES truth_events(id),
            FOREIGN KEY(type_id) REFERENCES impact_type(id)
);
CREATE TABLE progress_metrics (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp                    INTEGER NOT NULL,
    total_events                 INTEGER NOT NULL,
    total_events_group           INTEGER NOT NULL,
    total_positive_impact        REAL    NOT NULL,
    total_positive_impact_group  REAL    NOT NULL,
    total_negative_impact        REAL    NOT NULL,
    total_negative_impact_group  REAL    NOT NULL,
    trend                        REAL    NOT NULL,
    trend_group                  REAL    NOT NULL
);
CREATE TABLE node_ratings (
    node_id                TEXT PRIMARY KEY,
    events_true            INTEGER NOT NULL DEFAULT 0,
    events_false           INTEGER NOT NULL DEFAULT 0,
    validations            INTEGER NOT NULL DEFAULT 0,
    reused_events          INTEGER NOT NULL DEFAULT 0,
    trust_score            REAL    NOT NULL DEFAULT 0.0,
    propagation_priority   REAL    NOT NULL DEFAULT 0.0,
    last_updated           INTEGER NOT NULL
);
CREATE TABLE group_ratings (
    group_id      TEXT PRIMARY KEY,
    members       TEXT    NOT NULL,
    avg_score     REAL    NOT NULL,
    coherence     REAL    NOT NULL,
    last_updated  INTEGER NOT NULL
);
CREATE TABLE users (
    pubkey        TEXT PRIMARY KEY,
    role          TEXT NOT NULL DEFAULT 'observer',
    trust_score   REAL NOT NULL DEFAULT 0.0,
    last_updated  INTEGER NOT NULL,
    display_name  TEXT
);
CREATE TABLE roles (
    role          TEXT PRIMARY KEY,
    level         INTEGER NOT NULL,
    description   TEXT
);
CREATE TABLE participants (
    id TEXT PRIMARY KEY,
    public_key TEXT UNIQUE NOT NULL,
    reputation_score REAL NOT NULL DEFAULT 0.5,
    total_judgments INTEGER NOT NULL DEFAULT 0,
    accurate_judgments INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    last_activity INTEGER
);
CREATE TABLE events_ci (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    event_type TEXT NOT NULL,
    created_by TEXT NOT NULL REFERENCES participants(id),
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    resolution_data TEXT
);
CREATE TABLE judgments (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL REFERENCES participants(id),
    event_id TEXT NOT NULL REFERENCES events_ci(id),
    assessment TEXT NOT NULL,
    confidence_level REAL NOT NULL,
    reasoning TEXT,
    submitted_at INTEGER NOT NULL,
    signature TEXT NOT NULL,
    UNIQUE(participant_id, event_id)
);
CREATE TABLE consensus_ci (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL REFERENCES events_ci(id),
    consensus_value TEXT NOT NULL,
    confidence_score REAL NOT NULL,
    participant_count INTEGER NOT NULL,
    calculated_at INTEGER NOT NULL,
    algorithm_version TEXT NOT NULL
);
CREATE TABLE reputation_history (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL REFERENCES participants(id),
    old_reputation REAL NOT NULL,
    new_reputation REAL NOT NULL,
    change_reason TEXT NOT NULL,
    event_id TEXT,
    updated_at INTEGER NOT NULL
);
CREATE TABLE sync_log (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            op           TEXT NOT NULL,
            table_name   TEXT NOT NULL,
            record_id    TEXT NOT NULL,
            signature    TEXT,
            public_key   TEXT,
            created_at   INTEGER NOT NULL
        );
CREATE TABLE sync_logs (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp  INTEGER NOT NULL,
            peer_url   TEXT NOT NULL,
            mode       TEXT NOT NULL,
            status     TEXT NOT NULL,
            details    TEXT NOT NULL
        );
CREATE TABLE node_metrics (
            pubkey                  TEXT PRIMARY KEY,
            last_seen               INTEGER NOT NULL,
            relay_success_rate      REAL    NOT NULL DEFAULT 0.0,
            quality_index           REAL    NOT NULL DEFAULT 0.0,
            propagation_priority    REAL    NOT NULL DEFAULT 0.0
        );
CREATE TABLE active_tokens (
            public_key    TEXT    NOT NULL,
            refresh_token TEXT    NOT NULL UNIQUE,
            expires_at    INTEGER NOT NULL
        );
CREATE INDEX idx_active_tokens_pub ON active_tokens(public_key);
CREATE TABLE app_config (
            key   TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );
CREATE TABLE peer_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            peer_url TEXT NOT NULL,
            last_sync INTEGER,
            success_count INTEGER DEFAULT 0,
            fail_count INTEGER DEFAULT 0,
            last_quality_index REAL DEFAULT 0.0,
            last_trust_score REAL DEFAULT 0.0
        );
