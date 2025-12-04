#![allow(dead_code)]

use core_lib::storage;
use rusqlite::{Connection, Result};

const LEGACY_SCHEMA: &str = r#"
CREATE TABLE IF NOT EXISTS events (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    context_id TEXT,
    start_date TEXT,
    end_date TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS impacts (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    impact_level INTEGER NOT NULL CHECK(impact_level >= 1 AND impact_level <= 5),
    notes TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS summaries (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL UNIQUE,
    summary_text TEXT,
    recommendations TEXT,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS judgments (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    assessment TEXT NOT NULL,
    confidence_level REAL NOT NULL,
    reasoning TEXT,
    submitted_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS logs (
    id TEXT PRIMARY KEY,
    timestamp TEXT NOT NULL,
    source TEXT NOT NULL,
    level TEXT NOT NULL,
    message TEXT NOT NULL
);
"#;

pub fn memory_conn() -> Connection {
    let conn = Connection::open_in_memory().expect("open in-memory sqlite");
    storage::init_truth_schema(&conn).expect("init truth schema");
    conn
}

pub fn memory_conn_with_legacy() -> Connection {
    let conn = memory_conn();
    seed_legacy_tables(&conn).expect("seed legacy tables");
    conn
}

pub fn seed_legacy_tables(conn: &Connection) -> Result<()> {
    conn.execute_batch(LEGACY_SCHEMA)
}

pub fn table_exists(conn: &Connection, table: &str) -> bool {
    conn.query_row(
        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?1",
        [table],
        |row| row.get::<_, i64>(0),
    )
    .unwrap_or(0)
        > 0
}

pub fn legacy_tables_absent(conn: &Connection) -> bool {
    ["events", "impacts", "summaries", "logs"]
        .iter()
        .all(|name| !table_exists(conn, name))
}
