use std::sync::Arc;

use rusqlite::Connection;
use tokio::sync::Mutex;

pub type SharedConn = Arc<Mutex<Connection>>;

const NODES_SCHEMA: &str = r#"
CREATE TABLE IF NOT EXISTS nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_nodes_address ON nodes(address);
CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_reachable ON nodes(reachable);
"#;

pub fn setup_nodes_db(statements: &[&str]) -> SharedConn {
    let conn = core_lib::storage::open_db(":memory:").expect("in-memory db");
    conn.execute_batch(NODES_SCHEMA)
        .expect("create nodes schema");
    for stmt in statements {
        conn.execute(stmt, []).expect("seed node");
    }
    Arc::new(Mutex::new(conn))
}

pub fn insert_node_sql(
    address: &str,
    node_type: &str,
    reachable: bool,
    ttl: i64,
    last_seen: i64,
    source: &str,
    node_id: &str,
) -> String {
    format!(
        "INSERT INTO nodes (address, type, reachable, last_seen, ttl, source, node_id, created_at, updated_at)
         VALUES ('{address}', '{node_type}', {reachable}, {last_seen}, {ttl}, '{source}', '{node_id}', {last_seen}, {last_seen});",
        reachable = if reachable { 1 } else { 0 },
        address = address,
        node_type = node_type,
        last_seen = last_seen,
        ttl = ttl,
        source = source,
        node_id = node_id
    )
}
