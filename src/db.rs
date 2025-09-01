use crate::models::Statement;
use rusqlite::{params, Connection, OptionalExtension};

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
        CREATE INDEX IF NOT EXISTS idx_statements_updated_at
            ON statements(updated_at);
        "#,
    )
    .expect("migrate");
    conn
}

pub fn upsert_statement(conn: &Connection, s: &Statement) -> rusqlite::Result<()> {
    // если есть запись новее — не перетирать
    let existing: Option<i64> = conn
        .query_row(
            "SELECT updated_at FROM statements WHERE id=?1",
            params![s.id.to_string()],
            |r| r.get(0),
        )
        .optional()?;

    if let Some(u) = existing {
        if u >= s.updated_at {
            return Ok(()); // локальная запись новее или равна
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
