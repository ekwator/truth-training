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

        let mut conn = Connection::open(&db_path).map_err(|e| e.to_string())?;

        if !db_exists {
            // New database: initialize schema
            conn.execute_batch(truth_storage::export_schema_sql())
                .map_err(|e| e.to_string())?;
            conn.execute_batch("PRAGMA journal_mode=WAL;")
                .map_err(|e| e.to_string())?;

            // Run migrations
            Self::run_migrations(&conn)?;

            // Try to get locale from config, fallback to "en"
            let locale = Self::get_locale_from_config().unwrap_or_else(|_| {
                log::warn!("Failed to read locale from config during DB init, using default 'en'");
                "en".to_string()
            });

            log::info!("Initializing new database with locale: {}", locale);

            // Seed knowledge base with locale from config
            Self::seed_knowledge_base(&mut conn, &locale)?;
        } else {
            // Existing database: ensure schema is up to date
            conn.execute_batch("PRAGMA journal_mode=WAL;")
                .map_err(|e| e.to_string())?;
            Self::run_migrations(&conn)?;
        }

        Ok(Db(Mutex::new(conn)))
    }

    fn get_locale_from_config() -> Result<String, String> {
        // Read config file directly (sync approach)
        use dirs;
        use serde_json;
        use std::fs;

        let home_dir = dirs::home_dir().ok_or("Failed to get home directory")?;
        let config_path = home_dir.join(".truth-training").join("config.json");

        if !config_path.exists() {
            log::info!("Config file does not exist, using default locale 'en'");
            return Ok("en".to_string());
        }

        let content = fs::read_to_string(&config_path)
            .map_err(|e| format!("Failed to read config: {}", e))?;

        let config: serde_json::Value =
            serde_json::from_str(&content).map_err(|e| format!("Failed to parse config: {}", e))?;

        let locale = config
            .get("locale")
            .and_then(|v| v.as_str())
            .unwrap_or("en")
            .to_string();

        // Validate locale
        if !["en", "ru"].contains(&locale.as_str()) {
            log::warn!("Invalid locale '{}' in config, using default 'en'", locale);
            return Ok("en".to_string());
        }

        log::info!("Read locale '{}' from config file", locale);
        Ok(locale)
    }

    fn run_migrations(conn: &Connection) -> Result<(), String> {
        // Check if migration from v0.2.0 to v1.0.0 is needed
        let has_old_events = conn
            .query_row("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='events' AND sql LIKE '%context_id%'", [], |row| row.get::<_, i64>(0))
            .unwrap_or(0) > 0;

        if has_old_events {
            // Migration will be handled by application logic when needed
            // For now, we keep both schemas for compatibility
        }
        Ok(())
    }

    fn seed_knowledge_base(conn: &mut Connection, locale: &str) -> Result<(), String> {
        // Use locale-aware seeding from core library
        truth_storage::seed_knowledge_base(conn, locale)
            .map_err(|e| format!("Failed to seed knowledge base: {}", e))?;
        Ok(())
    }

    pub fn insert_impact(
        &self,
        id: &str,
        event_id: &str,
        impact_level: i32,
        notes: Option<&str>,
        created_at: &str,
    ) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute(
            "INSERT INTO impacts (id, event_id, impact_level, notes, created_at) VALUES (?, ?, ?, ?, ?)",
            params![id, event_id, impact_level, notes, created_at],
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


    pub fn get_overall_metrics(&self) -> Result<(i64, f64, Option<String>), String> {
        let conn = self.0.lock();
        let total_events: i64 = conn
            .query_row("SELECT COUNT(1) FROM truth_events", [], |r| r.get(0))
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
                   SELECT datetime(MAX(timestamp_start), 'unixepoch') as ts FROM truth_events
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

    pub fn list_event_summaries(
        &self,
    ) -> Result<Vec<(String, String, Option<f64>, String)>, String> {
        let conn = self.0.lock();
        let mut stmt = conn
            .prepare(
                "SELECT e.description, COALESCE(e.description,''),
                        (SELECT AVG(confidence_level) FROM judgments j WHERE j.event_id = CAST(e.id AS TEXT)),
                        datetime(e.timestamp_start, 'unixepoch')
                 FROM truth_events e
                 ORDER BY e.timestamp_start DESC",
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

    pub fn insert_truth_event(
        &self,
        description: &str,
        category_id: Option<i64>,
        forma_id: Option<i64>,
        cause_id: Option<i64>,
        develop_id: Option<i64>,
        effect_id: Option<i64>,
        vector: bool,
        timestamp_start: i64,
    ) -> Result<i64, String> {
        let conn = self.0.lock();
        conn.execute(
            r#"INSERT INTO truth_events (description, category_id, forma_id, cause_id, develop_id, effect_id, vector, detected, corrected, timestamp_start, timestamp_end, code, collective_score)
               VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, NULL, 0, ?8, NULL, 1, NULL)"#,
            params![
                description,
                category_id,
                forma_id,
                cause_id,
                develop_id,
                effect_id,
                if vector { 1 } else { 0 },
                timestamp_start,
            ],
        )
        .map_err(|e| e.to_string())?;
        Ok(conn.last_insert_rowid())
    }

    pub fn get_truth_event_with_names(
        &self,
        event_id: i64,
    ) -> Option<crate::commands::events::Event> {
        use crate::commands::events::Event;
        let conn = self.0.lock();
        let mut stmt = conn.prepare(
            r#"SELECT 
                e.id, e.description, e.category_id, e.forma_id, e.cause_id, e.develop_id, e.effect_id,
                e.vector, e.detected, e.corrected, e.timestamp_start, e.timestamp_end, e.code, e.collective_score,
                cat.name, f.name, c.name, d.name, eff.name
               FROM truth_events e
               LEFT JOIN category cat ON e.category_id = cat.id
               LEFT JOIN forma f ON e.forma_id = f.id
               LEFT JOIN cause c ON e.cause_id = c.id
               LEFT JOIN develop d ON e.develop_id = d.id
               LEFT JOIN effect eff ON e.effect_id = eff.id
               WHERE e.id = ?1"#
        ).ok()?;

        let mut rows = stmt.query(params![event_id]).ok()?;
        if let Some(row) = rows.next().ok()? {
            Some(Event {
                id: row.get(0).ok()?,
                description: row.get(1).ok()?,
                category_id: row.get(2).ok()?,
                forma_id: row.get(3).ok()?,
                cause_id: row.get(4).ok()?,
                develop_id: row.get(5).ok()?,
                effect_id: row.get(6).ok()?,
                vector: row.get::<_, i64>(7).ok()? != 0,
                detected: row.get::<_, Option<i64>>(8).ok()?.map(|v| v != 0),
                corrected: row.get::<_, i64>(9).ok()? != 0,
                timestamp_start: row.get(10).ok()?,
                timestamp_end: row.get(11).ok()?,
                code: row.get::<_, i64>(12).ok()? as u8,
                collective_score: row.get(13).ok()?,
                category_name: row.get(14).ok()?,
                forma_name: row.get(15).ok()?,
                cause_name: row.get(16).ok()?,
                develop_name: row.get(17).ok()?,
                effect_name: row.get(18).ok()?,
            })
        } else {
            None
        }
    }

    pub fn list_truth_events_with_names(
        &self,
        page: u32,
        per_page: u32,
    ) -> Result<crate::commands::events::ListEventsResponse, String> {
        use crate::commands::events::{Event, ListEventsResponse};
        let conn = self.0.lock();

        let total: i64 = conn
            .query_row("SELECT COUNT(1) FROM truth_events", [], |row| row.get(0))
            .map_err(|e| e.to_string())?;

        let offset = (page.saturating_sub(1) as i64) * (per_page as i64);
        let mut stmt = conn.prepare(
            r#"SELECT 
                e.id, e.description, e.category_id, e.forma_id, e.cause_id, e.develop_id, e.effect_id,
                e.vector, e.detected, e.corrected, e.timestamp_start, e.timestamp_end, e.code, e.collective_score,
                cat.name, f.name, c.name, d.name, eff.name
               FROM truth_events e
               LEFT JOIN category cat ON e.category_id = cat.id
               LEFT JOIN forma f ON e.forma_id = f.id
               LEFT JOIN cause c ON e.cause_id = c.id
               LEFT JOIN develop d ON e.develop_id = d.id
               LEFT JOIN effect eff ON e.effect_id = eff.id
               ORDER BY e.timestamp_start DESC LIMIT ?1 OFFSET ?2"#
        )
        .map_err(|e| e.to_string())?;

        let mut rows = stmt
            .query(params![per_page as i64, offset])
            .map_err(|e| e.to_string())?;
        let mut data: Vec<Event> = Vec::new();
        while let Some(row) = rows.next().map_err(|e| e.to_string())? {
            data.push(Event {
                id: row.get(0).map_err(|e| e.to_string())?,
                description: row.get(1).map_err(|e| e.to_string())?,
                category_id: row.get(2).ok().unwrap_or(None),
                forma_id: row.get(3).ok().unwrap_or(None),
                cause_id: row.get(4).ok().unwrap_or(None),
                develop_id: row.get(5).ok().unwrap_or(None),
                effect_id: row.get(6).ok().unwrap_or(None),
                vector: row.get::<_, i64>(7).map_err(|e| e.to_string())? != 0,
                detected: row
                    .get::<_, Option<i64>>(8)
                    .map_err(|e| e.to_string())?
                    .map(|v| v != 0),
                corrected: row.get::<_, i64>(9).map_err(|e| e.to_string())? != 0,
                timestamp_start: row.get(10).map_err(|e| e.to_string())?,
                timestamp_end: row.get(11).ok().unwrap_or(None),
                code: row.get::<_, i64>(12).map_err(|e| e.to_string())? as u8,
                collective_score: row.get(13).ok().unwrap_or(None),
                category_name: row.get(14).ok().unwrap_or(None),
                forma_name: row.get(15).ok().unwrap_or(None),
                cause_name: row.get(16).ok().unwrap_or(None),
                develop_name: row.get(17).ok().unwrap_or(None),
                effect_name: row.get(18).ok().unwrap_or(None),
            });
        }

        Ok(ListEventsResponse { data, total })
    }
}
