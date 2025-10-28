use directories::ProjectDirs;
use parking_lot::Mutex;
use rusqlite::{params, Connection, OptionalExtension};
use std::path::PathBuf;

pub struct Db(pub Mutex<Connection>);

impl Db {
    pub fn initialize() -> Result<Self, String> {
        let proj = ProjectDirs::from("com", "truth-training", "TruthTraining")
            .ok_or_else(|| "cannot resolve data dir".to_string())?;
        let mut db_path: PathBuf = proj.data_dir().to_path_buf();
        std::fs::create_dir_all(&db_path).map_err(|e| e.to_string())?;
        db_path.push("truth_training.sqlite");

        let conn = Connection::open(db_path).map_err(|e| e.to_string())?;
        conn.execute_batch(
            r#"
            PRAGMA journal_mode=WAL;
            CREATE TABLE IF NOT EXISTS events (
              id TEXT PRIMARY KEY,
              title TEXT NOT NULL,
              description TEXT,
              created_at TEXT NOT NULL,
              status TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS judgments (
              id TEXT PRIMARY KEY,
              event_id TEXT NOT NULL,
              assessment TEXT NOT NULL,
              confidence_level REAL NOT NULL,
              reasoning TEXT,
              submitted_at TEXT NOT NULL,
              FOREIGN KEY(event_id) REFERENCES events(id)
            );
            CREATE TABLE IF NOT EXISTS logs (
              id TEXT PRIMARY KEY,
              timestamp TEXT NOT NULL,
              source TEXT NOT NULL,
              level TEXT NOT NULL,
              message TEXT NOT NULL
            );
            "#,
        )
        .map_err(|e| e.to_string())?;

        Ok(Db(Mutex::new(conn)))
    }

    pub fn insert_event(&self, id: &str, title: &str, description: Option<&str>, created_at: &str, status: &str) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute(
            "INSERT INTO events (id, title, description, created_at, status) VALUES (?, ?, ?, ?, ?)",
            params![id, title, description, created_at, status],
        )
        .map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn insert_judgment(
        &self,
        id: &str,
        event_id: &str,
        assessment: &str,
        confidence_level: f64,
        reasoning: Option<&str>,
        submitted_at: &str,
    ) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute(
            "INSERT INTO judgments (id, event_id, assessment, confidence_level, reasoning, submitted_at) VALUES (?, ?, ?, ?, ?, ?)",
            params![id, event_id, assessment, confidence_level, reasoning, submitted_at],
        )
        .map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn list_judgments_for_event(&self, event_id: &str, limit: i64, offset: i64) -> Result<(Vec<(String,String,String,f64,Option<String>,String)>, i64), String> {
        let conn = self.0.lock();
        let total: i64 = conn
            .query_row("SELECT COUNT(1) FROM judgments WHERE event_id = ?1", [event_id], |row| row.get(0))
            .map_err(|e| e.to_string())?;

        let mut stmt = conn
            .prepare("SELECT id, event_id, assessment, confidence_level, reasoning, submitted_at FROM judgments WHERE event_id = ?1 ORDER BY datetime(submitted_at) DESC LIMIT ?2 OFFSET ?3")
            .map_err(|e| e.to_string())?;
        let mut rows = stmt
            .query(params![event_id, limit, offset])
            .map_err(|e| e.to_string())?;
        let mut items = Vec::new();
        while let Some(row) = rows.next().map_err(|e| e.to_string())? {
            items.push((
                row.get(0).map_err(|e| e.to_string())?,
                row.get(1).map_err(|e| e.to_string())?,
                row.get(2).map_err(|e| e.to_string())?,
                row.get(3).map_err(|e| e.to_string())?,
                row.get(4).ok().unwrap_or(None),
                row.get(5).map_err(|e| e.to_string())?,
            ));
        }
        Ok((items, total))
    }

    pub fn get_judgment_stats(&self, event_id: &str) -> Result<(i64,i64,i64,f64,Option<String>), String> {
        let conn = self.0.lock();
        let (t_true, t_false, t_uncertain): (i64,i64,i64) = {
            let true_c: i64 = conn.query_row("SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='true'", [event_id], |r| r.get(0)).unwrap_or(0);
            let false_c: i64 = conn.query_row("SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='false'", [event_id], |r| r.get(0)).unwrap_or(0);
            let uncertain_c: i64 = conn.query_row("SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='uncertain'", [event_id], |r| r.get(0)).unwrap_or(0);
            (true_c,false_c,uncertain_c)
        };
        let avg_conf: f64 = conn.query_row("SELECT COALESCE(AVG(confidence_level),0.0) FROM judgments WHERE event_id=?1", [event_id], |r| r.get(0)).unwrap_or(0.0);
        let last_submitted: Option<String> = conn.query_row("SELECT submitted_at FROM judgments WHERE event_id=?1 ORDER BY datetime(submitted_at) DESC LIMIT 1", [event_id], |r| r.get(0)).optional().unwrap_or(None);
        Ok((t_true,t_false,t_uncertain,avg_conf,last_submitted))
    }

    pub fn list_logs(&self, page: i64, page_size: i64) -> Result<(Vec<(String,String,String,String,String)>, i64), String> {
        let conn = self.0.lock();
        let total: i64 = conn
            .query_row("SELECT COUNT(1) FROM logs", [], |row| row.get(0))
            .map_err(|e| e.to_string())?;
        let offset = (page.max(1) - 1) * page_size.max(1);
        let mut stmt = conn
            .prepare("SELECT id, timestamp, source, level, message FROM logs ORDER BY datetime(timestamp) DESC LIMIT ?1 OFFSET ?2")
            .map_err(|e| e.to_string())?;
        let mut rows = stmt.query(params![page_size, offset]).map_err(|e| e.to_string())?;
        let mut items = Vec::new();
        while let Some(row) = rows.next().map_err(|e| e.to_string())? {
            items.push((
                row.get(0).map_err(|e| e.to_string())?,
                row.get(1).map_err(|e| e.to_string())?,
                row.get(2).map_err(|e| e.to_string())?,
                row.get(3).map_err(|e| e.to_string())?,
                row.get(4).map_err(|e| e.to_string())?,
            ));
        }
        Ok((items, total))
    }

    pub fn clear_logs(&self) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute("DELETE FROM logs", []).map_err(|e| e.to_string())?;
        Ok(())
    }

    pub fn get_overall_metrics(&self) -> Result<(i64, f64, Option<String>), String> {
        let conn = self.0.lock();
        let total_events: i64 = conn
            .query_row("SELECT COUNT(1) FROM events", [], |r| r.get(0))
            .unwrap_or(0);
        let avg_impact: f64 = conn
            .query_row(
                "SELECT COALESCE(AVG(confidence_level),0.0) FROM judgments",
                [],
                |r| r.get(0),
            )
            .unwrap_or(0.0);
        let last_updated: Option<String> = conn
            .query_row(
                "SELECT ts FROM (
                   SELECT MAX(datetime(created_at)) as ts FROM events
                   UNION ALL
                   SELECT MAX(datetime(submitted_at)) as ts FROM judgments
                 ) ORDER BY datetime(ts) DESC LIMIT 1",
                [],
                |r| r.get(0),
            )
            .optional()
            .unwrap_or(None);
        Ok((total_events, avg_impact, last_updated))
    }

    pub fn list_event_summaries(&self) -> Result<Vec<(String, String, Option<f64>, String)>, String> {
        let conn = self.0.lock();
        let mut stmt = conn
            .prepare(
                "SELECT e.title, COALESCE(e.description,''),
                        (SELECT AVG(confidence_level) FROM judgments j WHERE j.event_id = e.id),
                        e.created_at
                 FROM events e
                 ORDER BY datetime(e.created_at) DESC",
            )
            .map_err(|e| e.to_string())?;
        let mut rows = stmt.query([]).map_err(|e| e.to_string())?;
        let mut out = Vec::new();
        while let Some(row) = rows.next().map_err(|e| e.to_string())? {
            out.push((
                row.get(0).map_err(|e| e.to_string())?,
                row.get(1).map_err(|e| e.to_string())?,
                row.get(2).ok().unwrap_or(None),
                row.get(3).map_err(|e| e.to_string())?,
            ));
        }
        Ok(out)
    }
}


