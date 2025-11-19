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
            PRAGMA foreign_keys = ON;
            PRAGMA journal_mode=WAL;

            -- knowledge_base tables
            CREATE TABLE IF NOT EXISTS category (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                description TEXT
            );

            CREATE TABLE IF NOT EXISTS cause (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                quality     INTEGER NOT NULL,
                description TEXT
            );

            CREATE TABLE IF NOT EXISTS develop (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                quality     INTEGER NOT NULL,
                description TEXT
            );

            CREATE TABLE IF NOT EXISTS effect (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                quality     INTEGER NOT NULL,
                description TEXT
            );

            CREATE TABLE IF NOT EXISTS forma (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                quality     INTEGER NOT NULL,
                description TEXT
            );

            CREATE TABLE IF NOT EXISTS context (
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

            CREATE TABLE IF NOT EXISTS impact_type (
                id          INTEGER PRIMARY KEY,
                name        TEXT NOT NULL,
                description TEXT
            );

            -- base tables (v1.0.0 schema with embedded context fields)
            CREATE TABLE IF NOT EXISTS truth_events (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                description     TEXT NOT NULL,
                category_id     INTEGER,
                forma_id        INTEGER,
                cause_id        INTEGER,
                develop_id      INTEGER,
                effect_id       INTEGER,
                vector          INTEGER NOT NULL,
                detected        INTEGER,
                corrected       INTEGER NOT NULL DEFAULT 0,
                timestamp_start INTEGER NOT NULL,
                timestamp_end   INTEGER,
                code            INTEGER NOT NULL DEFAULT 1,
                collective_score REAL,
                FOREIGN KEY(category_id) REFERENCES category(id),
                FOREIGN KEY(forma_id) REFERENCES forma(id),
                FOREIGN KEY(cause_id) REFERENCES cause(id),
                FOREIGN KEY(develop_id) REFERENCES develop(id),
                FOREIGN KEY(effect_id) REFERENCES effect(id)
            );

            CREATE INDEX IF NOT EXISTS idx_truth_events_category ON truth_events(category_id);
            CREATE INDEX IF NOT EXISTS idx_truth_events_forma ON truth_events(forma_id);
            CREATE INDEX IF NOT EXISTS idx_truth_events_cause ON truth_events(cause_id);
            CREATE INDEX IF NOT EXISTS idx_truth_events_develop ON truth_events(develop_id);
            CREATE INDEX IF NOT EXISTS idx_truth_events_effect ON truth_events(effect_id);

            CREATE TABLE IF NOT EXISTS impact (
                id          TEXT PRIMARY KEY,
                event_id    INTEGER NOT NULL,
                type_id     INTEGER NOT NULL,
                value       INTEGER NOT NULL,
                notes       TEXT,
                created_at  INTEGER NOT NULL,
                FOREIGN KEY(event_id) REFERENCES truth_events(id),
                FOREIGN KEY(type_id) REFERENCES impact_type(id)
            );

            CREATE TABLE IF NOT EXISTS progress_metrics (
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

            -- Legacy tables for backward compatibility (will be migrated)
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
            "#,
        )
        .map_err(|e| e.to_string())?;

        // Run migrations
        Self::run_migrations(&conn)?;

        // Seed knowledge base if empty
        Self::seed_knowledge_base(&conn)?;

        Ok(Db(Mutex::new(conn)))
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

    fn seed_knowledge_base(conn: &Connection) -> Result<(), String> {
        // Seed categories
        let categories: &[(i64, &str, &str)] = &[
            (1, "Social", "Communication, reputation, trust"),
            (2, "Financial", "Money, property, contracts"),
            (3, "Political", "State, treaties, international relations"),
            (4, "Legal", "Law, compliance, courts"),
            (5, "Personal", "Self-assessment, inner decisions"),
            (6, "Organizational", "Teams, companies, processes"),
            (7, "Media", "Information, press, platforms"),
            (8, "Technological", "IT systems, data, security"),
        ];
        for (id, name, desc) in categories {
            conn.execute(
                "INSERT OR IGNORE INTO category (id, name, description) VALUES (?1, ?2, ?3)",
                params![id, name, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed causes
        let causes: &[(i64, &str, i64, &str)] = &[
            (1, "Fear", 0, "Avoidance of punishment or blame"),
            (2, "Benefit", 0, "Material/personal interest"),
            (3, "Mercy", 1, "Compassion, care for others"),
            (4, "Ignorance", 0, "Lack of knowledge, mistakes"),
            (5, "Duty", 1, "Obligation, responsibility"),
            (6, "Curiosity", 1, "Search for truth, inquiry"),
            (7, "Pressure", 0, "Coercion, conformism"),
            (8, "Care", 1, "Protecting another's good"),
        ];
        for (id, name, q, desc) in causes {
            conn.execute(
                "INSERT OR IGNORE INTO cause (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
                params![id, name, q, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed develops
        let develops: &[(i64, &str, i64, &str)] = &[
            (1, "Concealment", 0, "Intentional omission/withholding"),
            (2, "Manipulation", 0, "Distortion, pressure, context switch"),
            (3, "Transparency", 1, "Openness, factual availability"),
            (4, "Verification", 1, "Cross-checking sources"),
            (5, "Exaggeration", 0, "Overstatement, false salience"),
            (6, "Confession", 1, "Owning mistakes, remediation"),
        ];
        for (id, name, q, desc) in develops {
            conn.execute(
                "INSERT OR IGNORE INTO develop (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
                params![id, name, q, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed effects
        let effects: &[(i64, &str, i64, &str)] = &[
            (1, "Distrust", 0, "Erodes trust and ties"),
            (2, "Trust", 1, "Strengthens cooperation"),
            (3, "Conflict", 0, "Escalation, confrontation"),
            (4, "Reconciliation", 1, "Reduced tension, alignment"),
            (5, "Sanctions", 0, "Legal/reputational penalties"),
            (6, "Learning", 1, "Competence growth, insights"),
            (7, "Reputation Loss", 0, "Status decrease"),
            (8, "Reputation Gain", 1, "Status increase"),
        ];
        for (id, name, q, desc) in effects {
            conn.execute(
                "INSERT OR IGNORE INTO effect (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
                params![id, name, q, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed formas
        let formas: &[(i64, &str, i64, &str)] = &[
            (1, "Deception", 0, "Conscious distortion of reality"),
            (2, "Truth", 1, "Conformance to facts and checks"),
            (3, "Self-deception", 0, "Distortion to reassure oneself"),
            (4, "Half-truth", 0, "Partial truth with distortions"),
            (5, "Silence", 0, "Withholding significant info"),
            (6, "Openness", 1, "Proactive disclosure of facts"),
        ];
        for (id, name, q, desc) in formas {
            conn.execute(
                "INSERT OR IGNORE INTO forma (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
                params![id, name, q, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed impact_types
        let impact_types: &[(i64, &str, &str)] = &[
            (1, "Reputation", "Social capital, trust"),
            (2, "Finance", "Money, assets, liabilities"),
            (3, "Emotions", "Stress, confidence, motivation"),
            (4, "Law", "Legal risks, sanctions"),
            (5, "Health", "Physical/mental condition"),
            (6, "Time", "Time losses/gains"),
        ];
        for (id, name, desc) in impact_types {
            conn.execute(
                "INSERT OR IGNORE INTO impact_type (id, name, description) VALUES (?1, ?2, ?3)",
                params![id, name, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        // Seed context templates
        let contexts: &[(i64, &str, i64, i64, i64, i64, i64, &str)] = &[
            (
                1,
                "Interpersonal: openness",
                1,
                2,
                5,
                3,
                2,
                "Honest dialogue, strengthening trust",
            ),
            (
                2,
                "Interpersonal: concealment",
                1,
                1,
                1,
                1,
                1,
                "Withholding a significant fact, trust erosion",
            ),
            (
                3,
                "Finance: fraud",
                2,
                1,
                2,
                2,
                5,
                "Deception for profit, legal consequences",
            ),
            (
                4,
                "Finance: transparent reporting",
                2,
                2,
                5,
                4,
                8,
                "Verifiable facts, reputation growth",
            ),
            (
                5,
                "Politics: treaty breach",
                3,
                1,
                2,
                1,
                1,
                "Hidden violations, loss of trust",
            ),
            (
                6,
                "Politics: treaty compliance",
                3,
                2,
                5,
                4,
                2,
                "Confirmed execution of obligations",
            ),
            (
                7,
                "Organization: admitting a mistake",
                6,
                2,
                5,
                6,
                6,
                "Admission and correction improve learning",
            ),
            (
                8,
                "Media: disinformation",
                7,
                1,
                7,
                2,
                3,
                "Manipulations leading to conflict",
            ),
        ];
        for (id, name, cat, forma, cause, develop, effect, desc) in contexts {
            conn.execute(
                "INSERT OR IGNORE INTO context (id, name, category_id, forma_id, cause_id, develop_id, effect_id, description) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
                params![id, name, cat, forma, cause, develop, effect, desc],
            )
            .map_err(|e| e.to_string())?;
        }

        Ok(())
    }

    #[allow(dead_code)] // Public API method - may be called by external code
    pub fn insert_event(
        &self,
        id: &str,
        title: &str,
        description: Option<&str>,
        context_id: &str,
        start_date: Option<&str>,
        end_date: Option<&str>,
        created_at: &str,
        status: &str,
    ) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute(
            "INSERT INTO events (id, title, description, context_id, start_date, end_date, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            params![id, title, description, context_id, start_date, end_date, created_at, status],
        )
        .map_err(|e| e.to_string())?;
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

    #[allow(dead_code)]
    pub fn insert_or_update_summary(
        &self,
        id: &str,
        event_id: &str,
        summary_text: Option<&str>,
        recommendations: Option<&str>,
        updated_at: &str,
    ) -> Result<(), String> {
        let conn = self.0.lock();
        conn.execute(
            "INSERT OR REPLACE INTO summaries (id, event_id, summary_text, recommendations, updated_at) VALUES (?, ?, ?, ?, ?)",
            params![id, event_id, summary_text, recommendations, updated_at],
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

    pub fn list_logs(
        &self,
        page: i64,
        page_size: i64,
    ) -> Result<(Vec<(String, String, String, String, String)>, i64), String> {
        let conn = self.0.lock();
        let total: i64 = conn
            .query_row("SELECT COUNT(1) FROM logs", [], |row| row.get(0))
            .map_err(|e| e.to_string())?;
        let offset = (page.max(1) - 1) * page_size.max(1);
        let mut stmt = conn
            .prepare("SELECT id, timestamp, source, level, message FROM logs ORDER BY datetime(timestamp) DESC LIMIT ?1 OFFSET ?2")
            .map_err(|e| e.to_string())?;
        let mut rows = stmt
            .query(params![page_size, offset])
            .map_err(|e| e.to_string())?;
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
        conn.execute("DELETE FROM logs", [])
            .map_err(|e| e.to_string())?;
        Ok(())
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
