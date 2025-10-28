use directories::ProjectDirs;
use parking_lot::Mutex;
use rusqlite::{params, Connection};
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
}


