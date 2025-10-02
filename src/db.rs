use rusqlite::{params, Connection, OptionalExtension};
use crate::models::{Statement, TruthEvent, Impact};
use chrono::Utc;

/// Инициализация схемы (вызывается один раз)
pub fn init_db(path: &str) -> Connection {
    let conn = Connection::open(path).expect("open sqlite");
    conn.execute_batch(
        r#"
        PRAGMA journal_mode = WAL;

        CREATE TABLE IF NOT EXISTS statements (
            id TEXT PRIMARY KEY,
            text TEXT NOT NULL,
            truth_score REAL NULL,
            context TEXT NULL,
            updated_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS events (
            id TEXT PRIMARY KEY,
            description TEXT NOT NULL,
            context_id INTEGER NOT NULL,
            vector INTEGER NOT NULL,
            detected INTEGER,
            corrected INTEGER NOT NULL DEFAULT 0,
            timestamp_start INTEGER NOT NULL,
            timestamp_end INTEGER,
            created_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS impacts (
            id TEXT PRIMARY KEY,
            event_id TEXT NOT NULL,
            type_id INTEGER NOT NULL,
            positive INTEGER NOT NULL,
            notes TEXT,
            created_at INTEGER NOT NULL,
            code TEXT NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_statements_updated_at ON statements(updated_at);
        CREATE INDEX IF NOT EXISTS idx_events_timestamp_start ON events(timestamp_start);
        "#,
    )
    .expect("migrate");
    conn
}

/* ---------------- Statements ---------------- */

pub fn insert_new_statement(
    conn: &Connection,
    text: &str,
    context: Option<String>,
    truth_score: Option<f32>,
) -> rusqlite::Result<Statement> {
    let s = Statement::new(text, context, truth_score);
    upsert_statement(conn, &s)?;
    Ok(s)
}

pub fn upsert_statement(conn: &Connection, s: &Statement) -> rusqlite::Result<()> {
    // проверим наличие и время обновления
    let existing: Option<i64> = conn
        .query_row("SELECT updated_at FROM statements WHERE id=?1", params![s.id.to_string()], |r| r.get(0))
        .optional()?;

    if let Some(u) = existing {
        if u >= s.updated_at {
            return Ok(());
        }
    }

    conn.execute(
        r#"
        INSERT INTO statements (id, text, truth_score, context, updated_at)
        VALUES (?1, ?2, ?3, ?4, ?5)
        ON CONFLICT(id) DO UPDATE SET
            text=excluded.text,
            truth_score=excluded.truth_score,
            context=excluded.context,
            updated_at=excluded.updated_at
        "#,
        params![
            s.id.to_string(),
            s.text,
            s.truth_score,
            s.context,
            s.updated_at
        ],
    )?;
    Ok(())
}

pub fn get_all(conn: &Connection) -> rusqlite::Result<Vec<Statement>> {
    let mut st = conn.prepare(
        "SELECT id, text, truth_score, context, updated_at
         FROM statements
         ORDER BY updated_at DESC",
    )?;
    let rows = st.query_map([], |r| {
        Ok(Statement {
            id: r.get::<_, String>(0)?.parse().unwrap(),
            text: r.get(1)?,
            truth_score: r.get(2)?,
            context: r.get(3)?,
            updated_at: r.get(4)?,
        })
    })?;
    Ok(rows.filter_map(Result::ok).collect())
}

/* ---------------- Events ---------------- */

pub fn insert_event(conn: &Connection, e: &TruthEvent) -> rusqlite::Result<()> {
    conn.execute(
        r#"
        INSERT INTO events (id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, created_at)
        VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
        ON CONFLICT(id) DO UPDATE SET
            description=excluded.description,
            context_id=excluded.context_id,
            vector=excluded.vector,
            detected=excluded.detected,
            corrected=excluded.corrected,
            timestamp_start=excluded.timestamp_start,
            timestamp_end=excluded.timestamp_end,
            created_at=excluded.created_at
        "#,
        params![
            e.id.to_string(),
            e.description,
            e.context_id,
            if e.vector { 1 } else { 0 },
            e.detected.map(|b| if b { 1 } else { 0 }), // Option<i32>
            if e.corrected { 1 } else { 0 },
            e.timestamp_start,
            e.timestamp_end,
            e.created_at
        ],
    )?;
    Ok(())
}

pub fn get_all_events(conn: &Connection) -> rusqlite::Result<Vec<TruthEvent>> {
    let mut st = conn.prepare(
        "SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, created_at
         FROM events
         ORDER BY timestamp_start DESC",
    )?;
    let rows = st.query_map([], |r| {
        let detected_opt: Option<i32> = r.get(4).ok();
        Ok(TruthEvent {
            id: r.get::<_, String>(0)?.parse().unwrap(),
            description: r.get(1)?,
            context_id: r.get(2)?,
            vector: r.get::<_, i32>(3)? == 1,
            detected: detected_opt.map(|v| v == 1),
            corrected: r.get::<_, i32>(5)? == 1,
            timestamp_start: r.get(6)?,
            timestamp_end: r.get::<_, Option<i64>>(7)?,
            created_at: r.get(8)?,
            code: r.get(9)?,
        })
    })?;
    Ok(rows.filter_map(Result::ok).collect())
}

/* ---------------- Impacts ---------------- */

pub fn insert_impact(conn: &Connection, im: &Impact) -> rusqlite::Result<()> {
    conn.execute(
        r#"
        INSERT INTO impacts (id, event_id, type_id, positive, notes, created_at)
        VALUES (?1, ?2, ?3, ?4, ?5, ?6)
        ON CONFLICT(id) DO UPDATE SET
            event_id=excluded.event_id,
            type_id=excluded.type_id,
            positive=excluded.positive,
            notes=excluded.notes,
            created_at=excluded.created_at
        "#,
        params![
            im.id.to_string(),
            im.event_id.to_string(),
            im.type_id,
            if im.positive { 1 } else { 0 },
            im.notes,
            Utc::now().timestamp()
        ],
    )?;
    Ok(())
}
