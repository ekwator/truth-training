use core_lib::storage as truth_storage;
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

        // Check if database already exists
        let db_exists = db_path.exists();

        // Use core function to open database
        let mut conn = truth_storage::open_db(db_path.to_str().ok_or_else(|| "invalid db path".to_string())?)
            .map_err(|e| format!("Failed to open database: {}", e))?;

        // Set WAL mode for better concurrency
        conn.execute_batch("PRAGMA journal_mode=WAL;")
            .map_err(|e| e.to_string())?;

        if !db_exists {
            // New database: core::init_db() already initialized schema and ran migrations
            // Try to get locale from config, fallback to "en"
            let locale = Self::get_locale_from_config().unwrap_or_else(|_| {
                log::warn!("Failed to read locale from config during DB init, using default 'en'");
                "en".to_string()
            });

            log::info!("Initializing new database with locale: {}", locale);

            // Seed knowledge base with locale from config using core function
            truth_storage::seed_knowledge_base(&mut conn, &locale)
                .map_err(|e| format!("Failed to seed knowledge base: {}", e))?;
        } else {
            // Existing database: core::init_db() already ran migrations
            // No additional action needed
        }

        Ok(Db(Mutex::new(conn)))
    }

    // Simplified locale function - Desktop UI is English-only
    fn get_locale_from_config() -> Result<String, String> {
        // Desktop UI is English-only, always return "en"
        Ok("en".to_string())
    }

    // Note: run_migrations() is now handled by core::storage::init_db()
    // which is called automatically by core::storage::open_db()
    // This function is kept for backward compatibility but is no longer needed
    #[allow(dead_code)]
    fn run_migrations(_conn: &Connection) -> Result<(), String> {
        // Migrations are now handled by core::storage::run_migrations()
        // which is called automatically during core::storage::init_db()
        Ok(())
    }

    // Note: insert_impact() removed - use core::storage::add_impact() instead
    
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

    pub fn list_judgments_for_event(
        &self,
        event_id: &str,
        limit: i64,
        offset: i64,
    ) -> Result<
        (
            Vec<(String, String, String, f64, Option<String>, String)>,
            i64,
        ),
        String,
    > {
        let conn = self.0.lock();
        let total: i64 = conn
            .query_row(
                "SELECT COUNT(1) FROM judgments WHERE event_id = ?1",
                [event_id],
                |row| row.get(0),
            )
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

    pub fn get_judgment_stats(
        &self,
        event_id: &str,
    ) -> Result<(i64, i64, i64, f64, Option<String>), String> {
        let conn = self.0.lock();
        let (t_true, t_false, t_uncertain): (i64, i64, i64) = {
            let true_c: i64 = conn
                .query_row(
                    "SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='true'",
                    [event_id],
                    |r| r.get(0),
                )
                .unwrap_or(0);
            let false_c: i64 = conn
                .query_row(
                    "SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='false'",
                    [event_id],
                    |r| r.get(0),
                )
                .unwrap_or(0);
            let uncertain_c: i64 = conn
                .query_row(
                    "SELECT COUNT(1) FROM judgments WHERE event_id=?1 AND assessment='uncertain'",
                    [event_id],
                    |r| r.get(0),
                )
                .unwrap_or(0);
            (true_c, false_c, uncertain_c)
        };
        let avg_conf: f64 = conn
            .query_row(
                "SELECT COALESCE(AVG(confidence_level),0.0) FROM judgments WHERE event_id=?1",
                [event_id],
                |r| r.get(0),
            )
            .unwrap_or(0.0);
        let last_submitted: Option<String> = conn.query_row("SELECT submitted_at FROM judgments WHERE event_id=?1 ORDER BY datetime(submitted_at) DESC LIMIT 1", [event_id], |r| r.get(0)).optional().unwrap_or(None);
        Ok((t_true, t_false, t_uncertain, avg_conf, last_submitted))
    }


    // Note: get_overall_metrics() removed - use core::storage::load_metrics() instead
    // Note: list_event_summaries() removed - use core::storage::load_truth_events() instead
    // Note: insert_truth_event() removed - use core::storage::add_truth_event() instead
    // Note: get_truth_event_with_names() removed - use core::storage::get_truth_event() + entity name resolution instead
    // Note: list_truth_events_with_names() removed - use core::storage::load_truth_events() + entity name resolution instead
}
