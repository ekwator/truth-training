use crate::{
    CoreError,
    Impact,
    NewTruthEvent,
    ProgressMetrics,
    TruthEvent,
    Statement,
    NewStatement,
    NodeRating,
    GroupRating,
    GraphData,
    GraphNode,
    GraphLink,
};
use rusqlite::{Connection, OptionalExtension, params};
use serde::{Deserialize, Serialize};
// serde_json используется через полные пути
use uuid::Uuid;
use chrono::Utc;
use std::fs;
use std::collections::{HashMap, HashSet, VecDeque};
use crate::models::SyncLog;
use crate::trust_propagation::{apply_time_decay, propagate_from_remote};
use crate::models::RbacUser;

/// Создать соединение с базой данных и инициализировать схему
pub fn create_db_connection(db_path: &str) -> Result<Connection, CoreError> {
    let conn = Connection::open(db_path)?;
    init_db(&conn)?;
    Ok(conn)
}

/// SQL-схема (knowledge_base + base)
const SCHEMA_SQL: &str = r#"
PRAGMA foreign_keys = ON;

-- knowledge_base
CREATE TABLE IF NOT EXISTS category (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS cause (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    quality     INTEGER NOT NULL, -- 0/1
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

-- base
CREATE TABLE IF NOT EXISTS truth_events (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    description     TEXT NOT NULL,
    context_id      INTEGER NOT NULL,
    vector          INTEGER NOT NULL,       -- 0/1 вместо BOOLEAN
    detected        INTEGER,                -- NULL/0/1
    corrected       INTEGER NOT NULL DEFAULT 0,
timestamp_start INTEGER NOT NULL,
    timestamp_end   INTEGER,
    code            INTEGER NOT NULL DEFAULT 1,  -- 8-bit event code
FOREIGN KEY(context_id) REFERENCES context(id)
);

CREATE TABLE IF NOT EXISTS statements (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id        INTEGER NOT NULL,
    text            TEXT NOT NULL,
    context         TEXT,
    truth_score     REAL,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL,
    FOREIGN KEY(event_id) REFERENCES truth_events(id)
);

CREATE TABLE IF NOT EXISTS impact (
            id TEXT PRIMARY KEY,
            event_id TEXT NOT NULL,
type_id INTEGER NOT NULL,
            value INTEGER NOT NULL,      -- SQLite bool (0/1)
notes TEXT,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(event_id) REFERENCES truth_events(id)
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

-- node and group ratings
CREATE TABLE IF NOT EXISTS node_ratings (
    node_id        TEXT PRIMARY KEY,
    events_true    INTEGER NOT NULL DEFAULT 0,
    events_false   INTEGER NOT NULL DEFAULT 0,
    validations    INTEGER NOT NULL DEFAULT 0,
    reused_events  INTEGER NOT NULL DEFAULT 0,
    trust_score    REAL    NOT NULL DEFAULT 0.0,
    last_updated   INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS group_ratings (
    group_id      TEXT PRIMARY KEY,
    members       TEXT    NOT NULL,
    avg_score     REAL    NOT NULL,
    coherence     REAL    NOT NULL,
    last_updated  INTEGER NOT NULL
);

-- RBAC: users and roles
CREATE TABLE IF NOT EXISTS users (
    pubkey        TEXT PRIMARY KEY,
    role          TEXT NOT NULL DEFAULT 'observer',
    trust_score   REAL NOT NULL DEFAULT 0.0,
    last_updated  INTEGER NOT NULL,
    display_name  TEXT
);

-- Optional roles reference for future extension
CREATE TABLE IF NOT EXISTS roles (
    role          TEXT PRIMARY KEY,
    level         INTEGER NOT NULL,
    description   TEXT
);
INSERT OR IGNORE INTO roles(role, level, description) VALUES
    ('observer', 1, 'Read-only observer'),
    ('node',     2, 'Authenticated node with delegation rights'),
    ('admin',    3, 'Administrator');
"#;

/// Открыть/инициализировать БД по пути
pub fn open_db(path: &str) -> Result<Connection, CoreError> {
    let conn = Connection::open(path)?;
    init_db(&conn)?;
    Ok(conn)
}

/// Инициализация базы: создаёт таблицы, если их нет
pub fn init_db(conn: &Connection) -> Result<(), CoreError> {
    conn.execute_batch(SCHEMA_SQL)?;
    run_migrations(conn)?;
    Ok(())
}

/// Выполнить миграции: добавить недостающие колонки и служебные таблицы
pub fn run_migrations(conn: &Connection) -> Result<(), CoreError> {
    // Проверка наличия колонки в таблице
    fn has_column(conn: &Connection, table: &str, column: &str) -> Result<bool, rusqlite::Error> {
        let mut stmt = conn.prepare(&format!("PRAGMA table_info('{}')", table))?;
        let mut rows = stmt.query([])?;
        while let Some(row) = rows.next()? {
            let name: String = row.get(1)?; // 1 = name
            if name == column {
                return Ok(true);
            }
        }
        Ok(false)
    }

    // Добавить подписи/ключи для truth_events
    if !has_column(conn, "truth_events", "signature")? {
        conn.execute("ALTER TABLE truth_events ADD COLUMN signature TEXT", [])?;
    }
    if !has_column(conn, "truth_events", "public_key")? {
        conn.execute("ALTER TABLE truth_events ADD COLUMN public_key TEXT", [])?;
    }

    // Добавить подписи/ключи для statements
    if !has_column(conn, "statements", "signature")? {
        conn.execute("ALTER TABLE statements ADD COLUMN signature TEXT", [])?;
    }
    if !has_column(conn, "statements", "public_key")? {
        conn.execute("ALTER TABLE statements ADD COLUMN public_key TEXT", [])?;
    }

    // Добавить подписи/ключи для impact
    if !has_column(conn, "impact", "signature")? {
        conn.execute("ALTER TABLE impact ADD COLUMN signature TEXT", [])?;
    }
    if !has_column(conn, "impact", "public_key")? {
        conn.execute("ALTER TABLE impact ADD COLUMN public_key TEXT", [])?;
    }

    // Таблица журнала синхронизации
    conn.execute_batch(
        r#"
        CREATE TABLE IF NOT EXISTS sync_log (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            op           TEXT NOT NULL,
            table_name   TEXT NOT NULL,
            record_id    TEXT NOT NULL,
            signature    TEXT,
            public_key   TEXT,
            created_at   INTEGER NOT NULL
        );

        -- high-level synchronization logs
        CREATE TABLE IF NOT EXISTS sync_logs (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp  INTEGER NOT NULL,
            peer_url   TEXT NOT NULL,
            mode       TEXT NOT NULL,
            status     TEXT NOT NULL,
            details    TEXT NOT NULL
        );
        "#,
    )?;

    // Ensure ratings tables exist (idempotent)
    conn.execute_batch(
        r#"
        CREATE TABLE IF NOT EXISTS node_ratings (
            node_id        TEXT PRIMARY KEY,
            events_true    INTEGER NOT NULL DEFAULT 0,
            events_false   INTEGER NOT NULL DEFAULT 0,
            validations    INTEGER NOT NULL DEFAULT 0,
            reused_events  INTEGER NOT NULL DEFAULT 0,
            trust_score    REAL    NOT NULL DEFAULT 0.0,
            last_updated   INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS group_ratings (
            group_id      TEXT PRIMARY KEY,
            members       TEXT    NOT NULL,
            avg_score     REAL    NOT NULL,
            coherence     REAL    NOT NULL,
            last_updated  INTEGER NOT NULL
        );

        -- active JWT refresh tokens (per public key)
        CREATE TABLE IF NOT EXISTS active_tokens (
            public_key    TEXT    NOT NULL,
            refresh_token TEXT    NOT NULL UNIQUE,
            expires_at    INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_active_tokens_pub ON active_tokens(public_key);
        "#,
    )?;

    // RBAC migrations: ensure users table has required columns and seed roles
    conn.execute_batch(
        r#"
        CREATE TABLE IF NOT EXISTS users (
            pubkey        TEXT PRIMARY KEY,
            role          TEXT NOT NULL DEFAULT 'observer',
            trust_score   REAL NOT NULL DEFAULT 0.0,
            last_updated  INTEGER NOT NULL,
            display_name  TEXT
        );
        CREATE TABLE IF NOT EXISTS roles (
            role          TEXT PRIMARY KEY,
            level         INTEGER NOT NULL,
            description   TEXT
        );
        INSERT OR IGNORE INTO roles(role, level, description) VALUES
            ('observer', 1, 'Read-only observer'),
            ('node',     2, 'Authenticated node with delegation rights'),
            ('admin',    3, 'Administrator');
        "#,
    )?;

    Ok(())
}

/// Register a refresh token for a given public key with expiration timestamp (unix seconds)
pub fn register_refresh_token(
    conn: &Connection,
    public_key: &str,
    refresh_token: &str,
    expires_at: i64,
) -> Result<(), CoreError> {
    conn.execute(
        r#"INSERT INTO active_tokens (public_key, refresh_token, expires_at)
           VALUES (?1, ?2, ?3)"#,
        params![public_key, refresh_token, expires_at],
    )?;
    Ok(())
}

/// Find a session by refresh token, returning (public_key, expires_at)
pub fn find_session_by_refresh(
    conn: &Connection,
    refresh_token: &str,
) -> Result<Option<(String, i64)>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT public_key, expires_at FROM active_tokens WHERE refresh_token = ?1"#,
    )?;
    let row = stmt
        .query_row(params![refresh_token], |r| Ok((r.get(0)?, r.get(1)?)))
        .optional()?;
    Ok(row)
}

/// Delete a refresh token (on rotation or logout)
pub fn delete_refresh_token(conn: &Connection, refresh_token: &str) -> Result<(), CoreError> {
    conn.execute(
        r#"DELETE FROM active_tokens WHERE refresh_token = ?1"#,
        params![refresh_token],
    )?;
    Ok(())
}

/// Cleanup expired tokens; returns number of rows removed
pub fn cleanup_expired_tokens(conn: &Connection, now_ts: i64) -> Result<usize, CoreError> {
    let affected = conn.execute(
        r#"DELETE FROM active_tokens WHERE expires_at <= ?1"#,
        params![now_ts],
    )?;
    Ok(affected)
}

/* =========================
SEED: knowledge_base
========================= */
type ContextRow<'a> = (i64, &'a str, i64, i64, i64, i64, i64, &'a str);

pub fn seed_knowledge_base(conn: &mut Connection, locale: &str) -> Result<(), CoreError> {
    match locale {
        "ru" => seed_knowledge_base_ru(conn),
        "en" => seed_knowledge_base_en(conn),
        other => Err(CoreError::InvalidArg(format!(
            "unsupported locale: {}",
            other
        ))),
    }
}

fn seed_knowledge_base_ru(conn: &mut Connection) -> Result<(), CoreError> {
    let tx = conn.transaction()?;

    // category
    let categories: &[(i64, &str, &str)] = &[
        (1, "Социальный", "Общение, репутация, доверие"),
        (2, "Финансовый", "Деньги, собственность, договоры"),
        (
            3,
            "Политический",
            "Государство, договоры, международные отношения",
        ),
        (4, "Правовой", "Закон, соблюдение норм, суд"),
        (5, "Личный", "Самооценка, внутренние решения"),
        (6, "Организационный", "Команды, компании, процессы"),
        (7, "Медиа", "Информация, СМИ, платформы"),
        (8, "Технологический", "ИТ-системы, данные, безопасность"),
    ];
    for (id, name, desc) in categories {
        tx.execute(
            "INSERT OR IGNORE INTO category (id, name, description) VALUES (?1, ?2, ?3)",
            params![id, name, desc],
        )?;
    }

    // cause
    let causes: &[(i64, &str, i64, &str)] = &[
        (1, "Страх", 0, "Избежание наказания, осуждения"),
        (2, "Выгода", 0, "Материальный/личный интерес"),
        (3, "Милосердие", 1, "Сострадание, забота о другом"),
        (4, "Неведение", 0, "Отсутствие знаний, ошибки"),
        (5, "Долг", 1, "Обязанность, ответственность"),
        (6, "Любопытство", 1, "Поиск истины, исследование"),
        (7, "Давление", 0, "Принуждение, конформизм"),
        (8, "Забота", 1, "Охрана блага другого"),
    ];
    for (id, name, q, desc) in causes {
        tx.execute(
            "INSERT OR IGNORE INTO cause (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    // develop
    let develops: &[(i64, &str, i64, &str)] = &[
        (1, "Сокрытие", 0, "Умышленное недосказание/умолчание"),
        (
            2,
            "Манипуляция",
            0,
            "Искажение, давление, подмена контекста",
        ),
        (3, "Прозрачность", 1, "Открытость, доступность фактов"),
        (4, "Проверка", 1, "Верификация, сопоставление источников"),
        (5, "Преувеличение", 0, "Гипербола, ложная значимость"),
        (6, "Признание", 1, "Принятие ответственности, исправление"),
    ];
    for (id, name, q, desc) in develops {
        tx.execute(
            "INSERT OR IGNORE INTO develop (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    // effect
    let effects: &[(i64, &str, i64, &str)] = &[
        (1, "Недоверие", 0, "Подрыв доверия, разрыв связей"),
        (2, "Доверие", 1, "Укрепление отношений, кооперация"),
        (3, "Конфликт", 0, "Эскалация, противостояние"),
        (4, "Примирение", 1, "Снижение напряжения, согласие"),
        (5, "Санкции", 0, "Юридические/репутационные последствия"),
        (6, "Обучение", 1, "Рост компетентности, выводы"),
        (7, "Потеря репутации", 0, "Снижение статуса"),
        (8, "Рост репутации", 1, "Укрепление статуса"),
    ];
    for (id, name, q, desc) in effects {
        tx.execute(
            "INSERT OR IGNORE INTO effect (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    // forma
    let formas: &[(i64, &str, i64, &str)] = &[
        (1, "Обман", 0, "Сознательное искажение реальности"),
        (2, "Правда", 1, "Соответствие фактам и проверкам"),
        (3, "Самообман", 0, "Искажение для успокоения себя"),
        (
            4,
            "Полуправда",
            0,
            "Частичное искажение с верными фрагментами",
        ),
        (5, "Умолчание", 0, "Сокрытие значимой информации"),
        (6, "Открытость", 1, "Проактивное раскрытие фактов"),
    ];
    for (id, name, q, desc) in formas {
        tx.execute(
            "INSERT OR IGNORE INTO forma (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    // impact_type
    let impact_types: &[(i64, &str, &str)] = &[
        (1, "Репутация", "Социальный капитал, доверие"),
        (2, "Финансы", "Деньги, активы, обязательства"),
        (3, "Эмоции", "Стресс, уверенность, мотивация"),
        (4, "Право", "Юридические риски, санкции"),
        (5, "Здоровье", "Физическое/психическое состояние"),
        (6, "Время", "Потери/выигрыш времени"),
    ];
    for (id, name, desc) in impact_types {
        tx.execute(
            "INSERT OR IGNORE INTO impact_type (id, name, description) VALUES (?1, ?2, ?3)",
            params![id, name, desc],
        )?;
    }

    // context (типовые связки)
    let contexts: &[ContextRow] = &[
        // id, name, category, forma, cause, develop, effect, desc
        (
            1,
            "Межличностные отношения: открытость",
            1,
            2,
            5,
            3,
            2,
            "Честный диалог, укрепление доверия",
        ),
        (
            2,
            "Межличностные отношения: сокрытие",
            1,
            1,
            1,
            1,
            1,
            "Умолчание значимого факта, эрозия доверия",
        ),
        (
            3,
            "Финансы: мошенничество",
            2,
            1,
            2,
            2,
            5,
            "Обман с целью выгоды, юридические последствия",
        ),
        (
            4,
            "Финансы: прозрачная отчётность",
            2,
            2,
            5,
            4,
            8,
            "Проверяемость фактов, рост репутации",
        ),
        (
            5,
            "Политика: нарушение договора",
            3,
            1,
            2,
            1,
            1,
            "Сокрытие нарушений, падение доверия",
        ),
        (
            6,
            "Политика: соблюдение договора",
            3,
            2,
            5,
            4,
            2,
            "Подтверждённое выполнение обязательств",
        ),
        (
            7,
            "Организация: признание ошибки",
            6,
            2,
            5,
            6,
            6,
            "Признание и исправление повышают обучаемость",
        ),
        (
            8,
            "Медиа: дезинформация",
            7,
            1,
            7,
            2,
            3,
            "Манипуляции, приводящие к конфликтам",
        ),
    ];
    for (id, name, cat, forma, cause, develop, effect, desc) in contexts {
        tx.execute(
            "INSERT OR IGNORE INTO context
             (id, name, category_id, forma_id, cause_id, develop_id, effect_id, description)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![id, name, cat, forma, cause, develop, effect, desc],
        )?;
    }

    tx.commit()?;
    Ok(())
}

fn seed_knowledge_base_en(conn: &mut Connection) -> Result<(), CoreError> {
    let tx = conn.transaction()?;

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
        tx.execute(
            "INSERT OR IGNORE INTO category (id, name, description) VALUES (?1, ?2, ?3)",
            params![id, name, desc],
        )?;
    }

    let causes: &[(i64, &str, i64, &str)] = &[
        (1, "Fear", 0, "Avoidance of punishment or blame"),
        (2, "Benefit", 0, "Material/personal interest"),
        (3, "Mercy", 1, "Compassion, care for others"),
        (4, "Ignorance", 0, "Lack of knowledge, mistakes"),
        (5, "Duty", 1, "Obligation, responsibility"),
        (6, "Curiosity", 1, "Search for truth, inquiry"),
        (7, "Pressure", 0, "Coercion, conformism"),
        (8, "Care", 1, "Protecting another’s good"),
    ];
    for (id, name, q, desc) in causes {
        tx.execute(
            "INSERT OR IGNORE INTO cause (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    let develops: &[(i64, &str, i64, &str)] = &[
        (1, "Concealment", 0, "Intentional omission/withholding"),
        (2, "Manipulation", 0, "Distortion, pressure, context switch"),
        (3, "Transparency", 1, "Openness, factual availability"),
        (4, "Verification", 1, "Cross-checking sources"),
        (5, "Exaggeration", 0, "Overstatement, false salience"),
        (6, "Confession", 1, "Owning mistakes, remediation"),
    ];
    for (id, name, q, desc) in develops {
        tx.execute(
            "INSERT OR IGNORE INTO develop (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

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
        tx.execute(
            "INSERT OR IGNORE INTO effect (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    let formas: &[(i64, &str, i64, &str)] = &[
        (1, "Deception", 0, "Conscious distortion of reality"),
        (2, "Truth", 1, "Conformance to facts and checks"),
        (3, "Self-deception", 0, "Distortion to reassure oneself"),
        (4, "Half-truth", 0, "Partial truth with distortions"),
        (5, "Silence", 0, "Withholding significant info"),
        (6, "Openness", 1, "Proactive disclosure of facts"),
    ];
    for (id, name, q, desc) in formas {
        tx.execute(
            "INSERT OR IGNORE INTO forma (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        )?;
    }

    let impact_types: &[(i64, &str, &str)] = &[
        (1, "Reputation", "Social capital, trust"),
        (2, "Finance", "Money, assets, liabilities"),
        (3, "Emotions", "Stress, confidence, motivation"),
        (4, "Law", "Legal risks, sanctions"),
        (5, "Health", "Physical/mental condition"),
        (6, "Time", "Time losses/gains"),
    ];
    for (id, name, desc) in impact_types {
        tx.execute(
            "INSERT OR IGNORE INTO impact_type (id, name, description) VALUES (?1, ?2, ?3)",
            params![id, name, desc],
        )?;
    }

    let contexts: &[ContextRow] = &[
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
        tx.execute(
            "INSERT OR IGNORE INTO context
             (id, name, category_id, forma_id, cause_id, develop_id, effect_id, description)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![id, name, cat, forma, cause, develop, effect, desc],
        )?;
    }

    tx.commit()?;
    Ok(())
}

/* =========================
Базовые операции
========================= */

/// Добавить событие (detected = NULL; corrected = false; timestamp_end = NULL)
pub fn add_truth_event(conn: &Connection, new_ev: NewTruthEvent) -> Result<i64, CoreError> {
    if new_ev.description.trim().is_empty() {
        return Err(CoreError::InvalidArg("description is empty".into()));
    }

    conn.execute(
        r#"INSERT INTO truth_events
            (description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code)
          VALUES
            (?1, ?2, ?3, NULL, 0, ?4, NULL, ?5)"#,
        params![
            new_ev.description,
            new_ev.context_id,
            if new_ev.vector { 1 } else { 0 },
            new_ev.timestamp_start,
            new_ev.code,
        ],
    )?;

    Ok(conn.last_insert_rowid())
}

/// Получить событие по id
pub fn get_truth_event(conn: &Connection, id: i64) -> Result<Option<TruthEvent>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code, signature, public_key
           FROM truth_events WHERE id = ?1"#,
    )?;

    let row_opt = stmt
        .query_row(params![id], |row| {
        Ok(TruthEvent {
            id: row.get(0)?,
            description: row.get(1)?,
            context_id: row.get(2)?,
                vector: row.get::<_, i64>(3)? != 0,
            detected: row.get::<_, Option<i64>>(4)?.map(|v| v != 0),
            corrected: row.get::<_, i64>(5)? != 0,
            timestamp_start: row.get(6)?,
            timestamp_end: row.get::<_, Option<i64>>(7)?,
                code: row.get(8)?,
                signature: row.get(9)?,
                public_key: row.get(10)?,
            })
        })
        .optional()?;

    Ok(row_opt)
}

/// Отметить событие как распознанное (detected = true/false), опционально проставить окончание и corrected
pub fn set_event_detected(
    conn: &Connection,
    id: i64,
    detected: bool,
    timestamp_end: Option<i64>,
    corrected: bool,
) -> Result<(), CoreError> {
    conn.execute(
        r#"UPDATE truth_events
           SET detected = ?2, timestamp_end = COALESCE(?3, timestamp_end), corrected = ?4
         WHERE id = ?1"#,
        params![
            id,
            if detected { 1 } else { 0 },
            timestamp_end,
            if corrected { 1 } else { 0 }
        ],
    )?;
    Ok(())
}

/// Добавить запись impact
pub fn add_impact(
    conn: &Connection,
    event_id: i64,
    type_id: i64,
    value: bool,
    notes: Option<String>,
) -> Result<String, CoreError> {
    let id = Uuid::new_v4().to_string();
    let created_at = Utc::now().timestamp();

    conn.execute(
        r#"INSERT INTO impact (id, event_id, type_id, value, notes, created_at)
          VALUES (?1, ?2, ?3, ?4, ?5, ?6)"#,
        params![id, event_id, type_id, if value { 1 } else { 0 }, notes, created_at],
    )?;
    Ok(id)
}

/// Пересчёт агрегатов для progress_metrics (MVP-версия)
pub fn recalc_progress_metrics(conn: &Connection, ts: i64) -> Result<i64, CoreError> {
    let total_events: i64 =
        conn.query_row("SELECT COUNT(*) FROM truth_events", [], |r| r.get(0))?;

    let total_positive_impact: f64 = conn.query_row(
        "SELECT COALESCE(SUM(CASE WHEN value=1 THEN 1.0 ELSE 0.0 END),0.0) FROM impact",
        [],
        |r| r.get(0),
    )?;

    let total_negative_impact: f64 = conn.query_row(
        "SELECT COALESCE(SUM(CASE WHEN value=0 THEN 1.0 ELSE 0.0 END),0.0) FROM impact",
        [],
        |r| r.get(0),
    )?;

    // Для MVP примем group = общие значения
    let total_events_group = total_events;
    let total_positive_impact_group = total_positive_impact;
    let total_negative_impact_group = total_negative_impact;

    // Черновая метрика тренда
    let trend = (total_positive_impact - total_negative_impact) / (total_events.max(1) as f64);
    let trend_group = trend;

    conn.execute(
        r#"INSERT INTO progress_metrics (
                timestamp,
                total_events,
                total_events_group,
                total_positive_impact,
                total_positive_impact_group,
                total_negative_impact,
                total_negative_impact_group,
                trend,
                trend_group
            ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)"#,
        params![
            ts,
            total_events,
            total_events_group,
            total_positive_impact,
            total_positive_impact_group,
            total_negative_impact,
            total_negative_impact_group,
            trend,
            trend_group,
        ],
    )?;

    Ok(conn.last_insert_rowid())
}

/// Пересчёт рейтингов узлов и групп на основе текущего состояния БД
pub fn recalc_ratings(conn: &Connection, ts: i64) -> Result<(), CoreError> {
    // 1) Узлы: очищаем и вставляем агрегаты
    conn.execute("DELETE FROM node_ratings", [])?;

    conn.execute(
        r#"
        WITH nodes AS (
            SELECT DISTINCT public_key AS node_id FROM truth_events WHERE public_key IS NOT NULL
            UNION
            SELECT DISTINCT public_key FROM impact WHERE public_key IS NOT NULL
        ),
        stmt_avg AS (
            SELECT event_id, AVG(truth_score) AS avg_score
            FROM statements
            WHERE truth_score IS NOT NULL
            GROUP BY event_id
        ),
        events_true AS (
            SELECT te.public_key AS node_id, COUNT(*) AS cnt
            FROM truth_events te
            LEFT JOIN stmt_avg sa ON sa.event_id = te.id
            WHERE te.public_key IS NOT NULL AND sa.avg_score >= 0.5
            GROUP BY te.public_key
        ),
        events_false AS (
            SELECT te.public_key AS node_id, COUNT(*) AS cnt
            FROM truth_events te
            LEFT JOIN stmt_avg sa ON sa.event_id = te.id
            WHERE te.public_key IS NOT NULL AND sa.avg_score <= -0.5
            GROUP BY te.public_key
        ),
        validations AS (
            SELECT im.public_key AS node_id, COUNT(*) AS cnt
            FROM impact im
            WHERE im.public_key IS NOT NULL
            GROUP BY im.public_key
        ),
        reused AS (
            SELECT te.public_key AS node_id, COUNT(DISTINCT te.id) AS cnt
            FROM truth_events te
            JOIN impact im ON CAST(im.event_id AS INTEGER) = te.id AND im.value = 1
            WHERE te.public_key IS NOT NULL AND im.public_key IS NOT NULL AND im.public_key <> te.public_key
            GROUP BY te.public_key
        )
        INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, last_updated)
        SELECT
            n.node_id,
            COALESCE(et.cnt, 0) AS events_true,
            COALESCE(ef.cnt, 0) AS events_false,
            COALESCE(v.cnt, 0)  AS validations,
            COALESCE(r.cnt, 0)  AS reused_events,
            (
                CASE
                    WHEN (COALESCE(et.cnt,0) + COALESCE(ef.cnt,0)) = 0 THEN 0.0
                    ELSE
                        (
                            CAST(COALESCE(et.cnt,0) - COALESCE(ef.cnt,0) AS REAL) /
                            CAST(COALESCE(et.cnt,0) + COALESCE(ef.cnt,0) AS REAL)
                        )
                        + 0.2 * CAST(COALESCE(r.cnt,0) AS REAL) /
                          CAST(CASE WHEN (COALESCE(et.cnt,0) + COALESCE(ef.cnt,0)) = 0 THEN 1 ELSE (COALESCE(et.cnt,0) + COALESCE(ef.cnt,0)) END AS REAL)
                END
            ) AS trust_score_raw,
            ?1 AS last_updated
        FROM nodes n
        LEFT JOIN events_true et ON et.node_id = n.node_id
        LEFT JOIN events_false ef ON ef.node_id = n.node_id
        LEFT JOIN validations v   ON v.node_id  = n.node_id
        LEFT JOIN reused r        ON r.node_id  = n.node_id
        ;
        "#,
        params![ts],
    )?;

    // 2) Обрезаем trust_score в диапазон [-1,1]
    conn.execute(
        r#"
        UPDATE node_ratings
        SET trust_score = CASE
            WHEN trust_score > 1.0 THEN 1.0
            WHEN trust_score < -1.0 THEN -1.0
            ELSE trust_score
        END
        "#,
        [],
    )?;

    // 3) Группы: пока формируем один глобальный кластер
    // Список участников
    let mut stmt_nodes = conn.prepare("SELECT node_id FROM node_ratings ORDER BY node_id")?;
    let rows = stmt_nodes.query_map([], |row| row.get::<_, String>(0))?;
    let mut members: Vec<String> = Vec::new();
    for r in rows { members.push(r?); }
    let members_json = serde_json::to_string(&members)?;

    // Средний скор
    let avg_score: f64 = conn.query_row(
        "SELECT COALESCE(AVG(trust_score), 0.0) FROM node_ratings",
        [],
        |r| r.get(0),
    )?;

    // Коэффициент согласованности (coherence) валидаторов с агрегированным мнением по событиям
    // total = кол-во оценок impact по событиям, где есть средний балл по statement
    // agree = совпадение знака impact.value с знаком среднего truth_score по событию
    let (total_votes, agree_votes): (i64, i64) = conn.query_row(
        r#"
        WITH stmt_avg AS (
            SELECT event_id, AVG(truth_score) AS avg_score
            FROM statements
            WHERE truth_score IS NOT NULL
            GROUP BY event_id
        )
        SELECT
            COUNT(*) AS total_votes,
            SUM(
                CASE
                    WHEN sa.avg_score IS NULL THEN 0
                    WHEN (sa.avg_score >= 0.0 AND im.value = 1) OR (sa.avg_score < 0.0 AND im.value = 0)
                        THEN 1 ELSE 0
                END
            ) AS agree_votes
        FROM impact im
        JOIN stmt_avg sa ON CAST(im.event_id AS INTEGER) = sa.event_id
        WHERE im.public_key IS NOT NULL
        "#,
        [],
        |r| Ok((r.get(0)?, r.get(1)?)),
    )?;

    let coherence: f64 = if total_votes > 0 {
        (agree_votes as f64) / (total_votes as f64)
    } else { 0.0 };

    // UPSERT глобальной группы
    conn.execute(
        r#"
        INSERT INTO group_ratings (group_id, members, avg_score, coherence, last_updated)
        VALUES ('global', ?1, ?2, ?3, ?4)
        ON CONFLICT(group_id) DO UPDATE SET
            members = excluded.members,
            avg_score = excluded.avg_score,
            coherence = excluded.coherence,
            last_updated = excluded.last_updated
        "#,
        params![members_json, avg_score, coherence, ts],
    )?;

    // RBAC: зеркалим trust_score узлов в таблицу users для JWT и API
    let _ = sync_users_with_node_ratings(conn)?;

    Ok(())
}

/// Загрузить рейтинги узлов
pub fn load_node_ratings(conn: &Connection) -> Result<Vec<NodeRating>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT node_id, events_true, events_false, validations, reused_events, trust_score, last_updated FROM node_ratings ORDER BY trust_score DESC",
    )?;
    let rows = stmt.query_map([], |row| {
        Ok(NodeRating {
            node_id: row.get(0)?,
            events_true: row.get::<_, i64>(1)? as u32,
            events_false: row.get::<_, i64>(2)? as u32,
            validations: row.get::<_, i64>(3)? as u32,
            reused_events: row.get::<_, i64>(4)? as u32,
            trust_score: row.get(5)?,
            last_updated: row.get(6)?,
        })
    })?;
    let mut out = Vec::new();
    for r in rows { out.push(r?); }
    Ok(out)
}

/// Загрузить рейтинги групп
pub fn load_group_ratings(conn: &Connection) -> Result<Vec<GroupRating>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT group_id, members, avg_score, coherence, last_updated FROM group_ratings ORDER BY group_id",
    )?;
    let rows = stmt.query_map([], |row| {
        let members_json: String = row.get(1)?;
        let members: Vec<String> = serde_json::from_str(&members_json).unwrap_or_default();
        Ok(GroupRating {
            group_id: row.get(0)?,
            members,
            avg_score: row.get(2)?,
            coherence: row.get(3)?,
            last_updated: row.get(4)?,
        })
    })?;
    let mut out = Vec::new();
    for r in rows { out.push(r?); }
    Ok(out)
}

/* =========================
RBAC: Users and Roles helpers
========================= */

/// Получить пользователя по публичному ключу
pub fn get_user_by_pubkey(conn: &Connection, pubkey: &str) -> Result<Option<RbacUser>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT pubkey, role, trust_score, last_updated, display_name FROM users WHERE pubkey = ?1"#,
    )?;
    let row = stmt
        .query_row(params![pubkey], |r| {
            Ok(RbacUser {
                pubkey: r.get(0)?,
                role: r.get(1)?,
                trust_score: r.get::<_, f64>(2)? as f32,
                last_updated: r.get(3)?,
                display_name: r.get(4)?,
            })
        })
        .optional()?;
    Ok(row)
}

/// Обновить роль пользователя (создаст при отсутствии)
pub fn update_user_role(conn: &Connection, pubkey: &str, role: &str) -> Result<(), CoreError> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        r#"
        INSERT INTO users(pubkey, role, trust_score, last_updated)
        VALUES (?1, ?2, 0.0, ?3)
        ON CONFLICT(pubkey) DO UPDATE SET role=excluded.role, last_updated=excluded.last_updated
        "#,
        params![pubkey, role, now],
    )?;
    Ok(())
}

/// Список пользователей (сортировка по роли уровню и trust_score)
pub fn list_users(conn: &Connection) -> Result<Vec<RbacUser>, CoreError> {
    let mut stmt = conn.prepare(
        r#"
        SELECT u.pubkey, u.role, u.trust_score, u.last_updated, u.display_name
        FROM users u
        LEFT JOIN roles r ON r.role = u.role
        ORDER BY COALESCE(r.level, 0) DESC, u.trust_score DESC, u.pubkey ASC
        "#,
    )?;
    let rows = stmt.query_map([], |r| {
        Ok(RbacUser {
            pubkey: r.get(0)?,
            role: r.get(1)?,
            trust_score: r.get::<_, f64>(2)? as f32,
            last_updated: r.get(3)?,
            display_name: r.get(4)?,
        })
    })?;
    let mut out = Vec::new();
    for r in rows { out.push(r?); }
    Ok(out)
}

/// Подкорректировать trust_score пользователя, с ограничением [-1,1]
pub fn adjust_trust_score(conn: &Connection, pubkey: &str, delta: f32) -> Result<f32, CoreError> {
    let now = chrono::Utc::now().timestamp();
    // ensure record exists
    conn.execute(
        r#"INSERT OR IGNORE INTO users(pubkey, role, trust_score, last_updated) VALUES (?1, 'observer', 0.0, ?2)"#,
        params![pubkey, now],
    )?;
    // update
    conn.execute(
        r#"
        UPDATE users
        SET trust_score = CASE
            WHEN trust_score + ?2 > 1.0 THEN 1.0
            WHEN trust_score + ?2 < -1.0 THEN -1.0
            ELSE trust_score + ?2
        END,
        last_updated = ?3
        WHERE pubkey = ?1
        "#,
        params![pubkey, delta as f64, now],
    )?;
    // read back
    let updated: f64 = conn.query_row(
        "SELECT trust_score FROM users WHERE pubkey = ?1",
        params![pubkey],
        |r| r.get(0),
    )?;
    Ok(updated as f32)
}

/// Синхронизировать таблицу users с node_ratings (зеркалирование trust_score)
pub fn sync_users_with_node_ratings(conn: &Connection) -> Result<usize, CoreError> {
    let now = chrono::Utc::now().timestamp();
    // Создаём/обновляем записи пользователей из рейтингов
    let affected = conn.execute(
        r#"
        INSERT INTO users(pubkey, role, trust_score, last_updated)
        SELECT nr.node_id, COALESCE(u.role,'observer') as role, nr.trust_score, ?1
        FROM node_ratings nr
        LEFT JOIN users u ON u.pubkey = nr.node_id
        ON CONFLICT(pubkey) DO UPDATE SET
            trust_score = excluded.trust_score,
            last_updated = excluded.last_updated
        "#,
        params![now],
    )?;
    Ok(affected)
}

/// Слияние входящих рейтингов узлов и групп по правилам конфликта
/// - Узлы: берём запись с бОльшим trust_score, при равенстве — с более новым last_updated
/// - Группы: берём запись с более новым last_updated
pub fn merge_ratings(
    conn: &Connection,
    incoming_nodes: &[NodeRating],
    incoming_groups: &[GroupRating],
) -> Result<Vec<(String, f32)>, CoreError> {
    // 0) Перед смешиванием — мягкий временной спад для устаревших записей
    let now_ts = chrono::Utc::now().timestamp();
    let _ = apply_time_decay(conn, now_ts)?;

    // 1) Узлы: распространяем доверие через взвешенное смешивание
    let trust_diffs = propagate_from_remote(conn, incoming_nodes, now_ts)?;

    // 2) Группы: резолвим по last_updated, как и раньше
    let mut stmt_groups = conn.prepare(
        r#"
        INSERT INTO group_ratings (group_id, members, avg_score, coherence, last_updated)
        VALUES (?1, ?2, ?3, ?4, ?5)
        ON CONFLICT(group_id) DO UPDATE SET
            members      = excluded.members,
            avg_score    = excluded.avg_score,
            coherence    = excluded.coherence,
            last_updated = excluded.last_updated
        WHERE excluded.last_updated > group_ratings.last_updated
        "#,
    )?;
    for gr in incoming_groups {
        let members_json = serde_json::to_string(&gr.members)?;
        stmt_groups.execute(rusqlite::params![
            gr.group_id,
            members_json,
            gr.avg_score,
            gr.coherence,
            gr.last_updated,
        ])?;
    }

    // Зеркалим обновлённые trust_score в таблицу users для консистентности JWT/RBAC
    let _ = sync_users_with_node_ratings(conn);

    // Зеркалим обновлённые trust_score в таблицу users для согласованности JWT
    let _ = sync_users_with_node_ratings(conn);

    Ok(trust_diffs)
}

/// Сформировать данные графа доверия
pub fn load_graph(conn: &Connection) -> Result<GraphData, CoreError> {
    // Узлы (id + score)
    let mut stmt_nodes = conn.prepare(
        "SELECT node_id, trust_score FROM node_ratings",
    )?;
    let node_rows = stmt_nodes.query_map([], |row| {
        Ok(GraphNode { id: row.get(0)?, score: row.get(1)? })
    })?;
    let mut nodes: Vec<GraphNode> = Vec::new();
    for r in node_rows { nodes.push(r?); }

    // Рёбра между валидаторами и авторами
    let mut stmt_links = conn.prepare(
        r#"
        SELECT im.public_key AS source,
               te.public_key AS target,
               SUM(CASE WHEN im.value = 1 THEN 1 ELSE 0 END) AS pos,
               SUM(CASE WHEN im.value = 0 THEN 1 ELSE 0 END) AS neg
        FROM impact im
        JOIN truth_events te ON CAST(im.event_id AS INTEGER) = te.id
        WHERE im.public_key IS NOT NULL AND te.public_key IS NOT NULL AND im.public_key <> te.public_key
        GROUP BY source, target
        "#,
    )?;
    let link_rows = stmt_links.query_map([], |row| {
        let source: String = row.get(0)?;
        let target: String = row.get(1)?;
        let pos: i64 = row.get(2)?;
        let neg: i64 = row.get(3)?;
        let total = (pos + neg).max(1) as f32;
        let signed = (pos as f32 - neg as f32) / total; // -1..1
        let weight = (signed + 1.0) / 2.0; // 0..1
        Ok(GraphLink { source, target, weight })
    })?;
    let mut links: Vec<GraphLink> = Vec::new();
    for r in link_rows { links.push(r?); }

    Ok(GraphData { nodes, links })
}

/// Сформировать данные графа доверия с фильтрацией
/// - Фильтр по минимальному скору узла (trust_score >= min_score)
/// - Ограничение числа исходящих связей на узел (max_links)
/// - depth: если задано, ограничить подграф узлами на расстоянии не более depth
///   шагов от узла с максимальным trust_score (по неориентированным рёбрам)
pub fn load_graph_filtered(
    conn: &Connection,
    min_score: f64,
    max_links: usize,
    depth: Option<usize>,
) -> Result<GraphData, CoreError> {
    // Узлы с порогом по trust_score
    let mut stmt_nodes = conn.prepare(
        "SELECT node_id, trust_score FROM node_ratings WHERE trust_score >= ?1 ORDER BY trust_score DESC",
    )?;
    let node_rows = stmt_nodes.query_map(params![min_score], |row| {
        Ok(GraphNode { id: row.get(0)?, score: row.get(1)? })
    })?;
    let mut nodes: Vec<GraphNode> = Vec::new();
    for r in node_rows { nodes.push(r?); }

    // Быстрый выход, если узлов нет
    if nodes.is_empty() {
        return Ok(GraphData { nodes, links: Vec::new() });
    }

    // Множество допустимых узлов для фильтрации рёбер
    let allowed_nodes: HashSet<String> = nodes.iter().map(|n| n.id.clone()).collect();

    // Рёбра между валидаторами и авторами (агрегация положительных/отрицательных влияний)
    let mut stmt_links = conn.prepare(
        r#"
        SELECT im.public_key AS source,
               te.public_key AS target,
               SUM(CASE WHEN im.value = 1 THEN 1 ELSE 0 END) AS pos,
               SUM(CASE WHEN im.value = 0 THEN 1 ELSE 0 END) AS neg
        FROM impact im
        JOIN truth_events te ON CAST(im.event_id AS INTEGER) = te.id
        WHERE im.public_key IS NOT NULL AND te.public_key IS NOT NULL AND im.public_key <> te.public_key
        GROUP BY source, target
        "#,
    )?;
    let link_rows = stmt_links.query_map([], |row| {
        let source: String = row.get(0)?;
        let target: String = row.get(1)?;
        let pos: i64 = row.get(2)?;
        let neg: i64 = row.get(3)?;
        let total = (pos + neg).max(1) as f32;
        let signed = (pos as f32 - neg as f32) / total; // -1..1
        let weight = (signed + 1.0) / 2.0; // 0..1
        Ok(GraphLink { source, target, weight })
    })?;
    let mut all_links: Vec<GraphLink> = Vec::new();
    for r in link_rows { all_links.push(r?); }

    // Фильтруем рёбра по доступным узлам
    let mut filtered_links: Vec<GraphLink> = all_links
        .into_iter()
        .filter(|l| allowed_nodes.contains(&l.source) && allowed_nodes.contains(&l.target))
        .collect();

    // Ограничение числа исходящих рёбер для каждого source по убыванию веса
    let mut by_source: HashMap<String, Vec<GraphLink>> = HashMap::new();
    for link in filtered_links.into_iter() {
        by_source.entry(link.source.clone()).or_default().push(link);
    }
    let mut limited: Vec<GraphLink> = Vec::new();
    for (_src, mut links) in by_source.into_iter() {
        links.sort_by(|a, b| b.weight.partial_cmp(&a.weight).unwrap_or(std::cmp::Ordering::Equal));
        let take_n = links.len().min(max_links);
        limited.extend(links.into_iter().take(take_n));
    }
    filtered_links = limited;

    // Если задана глубина — ограничим подграф вокруг узла с максимальным trust_score
    if let Some(depth_limit) = depth {
        if depth_limit == 0 {
            // Только один узел — с максимальным score
            let center_id = nodes[0].id.clone();
            nodes.retain(|n| n.id == center_id);
            filtered_links.clear();
        } else {
            let center_id = nodes[0].id.clone(); // узел с максимальным score (nodes отсортированы DESC)
            // Построим неориентированную смежность
            let mut adj: HashMap<String, Vec<String>> = HashMap::new();
            for l in &filtered_links {
                adj.entry(l.source.clone()).or_default().push(l.target.clone());
                adj.entry(l.target.clone()).or_default().push(l.source.clone());
            }
            // BFS до depth_limit
            let mut visited: HashSet<String> = HashSet::new();
            let mut q: VecDeque<(String, usize)> = VecDeque::new();
            visited.insert(center_id.clone());
            q.push_back((center_id.clone(), 0));
            while let Some((node, d)) = q.pop_front() {
                if d >= depth_limit { continue; }
                if let Some(nei) = adj.get(&node) {
                    for nxt in nei {
                        if visited.insert(nxt.clone()) {
                            q.push_back((nxt.clone(), d + 1));
                        }
                    }
                }
            }
            // Оставим только посещённые узлы и рёбра между ними
            nodes.retain(|n| visited.contains(&n.id));
            filtered_links.retain(|l| visited.contains(&l.source) && visited.contains(&l.target));
        }
    }

    // Финальная сортировка рёбер по весу убыв.
    filtered_links.sort_by(|a, b| b.weight.partial_cmp(&a.weight).unwrap_or(std::cmp::Ordering::Equal));

    Ok(GraphData { nodes, links: filtered_links })
}

#[derive(Serialize, Deserialize)]
pub struct ExportData {
    pub truth_events: Vec<TruthEvent>,
    pub impacts: Vec<Impact>,
    pub metrics: Vec<ProgressMetrics>,
}

pub fn export_to_json(conn: &Connection, file_path: &str) -> Result<(), CoreError> {
    let truth_events = load_truth_events(conn)?;
    let impacts = load_impacts(conn)?;
    let metrics = load_metrics(conn)?;

    let data = ExportData {
        truth_events,
        impacts,
        metrics,
    };

    let json_data = serde_json::to_string_pretty(&data)?;
    fs::write(file_path, json_data)?;
    Ok(())
}

pub fn import_from_json(conn: &mut Connection, file_path: &str) -> Result<(), CoreError> {
    let json_str = fs::read_to_string(file_path)?;
    let data: ExportData = serde_json::from_str(&json_str)?;

    let tx = conn.transaction()?;

    for event in data.truth_events {
        tx.execute(
            "INSERT INTO truth_events (id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code, signature, public_key)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)",
            rusqlite::params![
                event.id,
                event.description,
                event.context_id,
                if event.vector { 1 } else { 0 },
                event.detected.map(|v| if v { 1 } else { 0 }),
                if event.corrected { 1 } else { 0 },
                event.timestamp_start,
                event.timestamp_end,
                event.code,
                event.signature,
                event.public_key
            ],
        )?;
    }

    for impact in data.impacts {
        tx.execute(
            "INSERT INTO impact (id, event_id, type_id, value, notes, created_at, signature, public_key)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            rusqlite::params![
                impact.id,
                impact.event_id,
                impact.type_id,
                if impact.value { 1 } else { 0 },
                impact.notes,
                impact.created_at,
                impact.signature,
                impact.public_key,
            ],
        )?;
    }

    for metric in data.metrics {
        tx.execute(
            "INSERT INTO progress_metrics (id, timestamp, total_events, total_events_group, total_positive_impact, total_positive_impact_group, total_negative_impact, total_negative_impact_group, trend, trend_group)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)",
            rusqlite::params![
                metric.id,
                metric.timestamp,
                metric.total_events,
                metric.total_events_group,
                metric.total_positive_impact,
                metric.total_positive_impact_group,
                metric.total_negative_impact,
                metric.total_negative_impact_group,
                metric.trend,
                metric.trend_group
            ],
        )?;
    }

    tx.commit()?;
    Ok(())
}

/// Добавить утверждение
pub fn add_statement(conn: &Connection, new_stmt: NewStatement) -> Result<i64, CoreError> {
    if new_stmt.text.trim().is_empty() {
        return Err(CoreError::InvalidArg("statement text is empty".into()));
    }

    let now = chrono::Utc::now().timestamp();
    conn.execute(
        r#"INSERT INTO statements (event_id, text, context, truth_score, created_at, updated_at)
          VALUES (?1, ?2, ?3, ?4, ?5, ?6)"#,
        params![
            new_stmt.event_id,
            new_stmt.text,
            new_stmt.context,
            new_stmt.truth_score,
            now,
            now,
        ],
    )?;

    Ok(conn.last_insert_rowid())
}

/// Получить утверждение по id
pub fn get_statement(conn: &Connection, id: i64) -> Result<Option<Statement>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT id, event_id, text, context, truth_score, created_at, updated_at, signature, public_key
           FROM statements WHERE id = ?1"#,
    )?;

    let row_opt = stmt
        .query_row(params![id], |row| {
            Ok(Statement {
                id: row.get(0)?,
                event_id: row.get(1)?,
                text: row.get(2)?,
                context: row.get(3)?,
                truth_score: row.get(4)?,
                created_at: row.get(5)?,
                updated_at: row.get(6)?,
                signature: row.get(7)?,
                public_key: row.get(8)?,
            })
        })
        .optional()?;

    Ok(row_opt)
}

/// Получить все утверждения для события
pub fn get_statements_for_event(conn: &Connection, event_id: i64) -> Result<Vec<Statement>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT id, event_id, text, context, truth_score, created_at, updated_at, signature, public_key
           FROM statements WHERE event_id = ?1 ORDER BY created_at DESC"#,
    )?;

    let rows = stmt.query_map(params![event_id], |row| {
        Ok(Statement {
            id: row.get(0)?,
            event_id: row.get(1)?,
            text: row.get(2)?,
            context: row.get(3)?,
            truth_score: row.get(4)?,
            created_at: row.get(5)?,
            updated_at: row.get(6)?,
            signature: row.get(7)?,
            public_key: row.get(8)?,
        })
    })?;

    let mut statements = Vec::new();
    for s in rows {
        statements.push(s?);
    }
    Ok(statements)
}

/// Получить все утверждения
pub fn load_statements(conn: &Connection) -> Result<Vec<Statement>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT id, event_id, text, context, truth_score, created_at, updated_at, signature, public_key FROM statements ORDER BY created_at DESC",
    )?;

    let rows = stmt.query_map([], |row| {
        Ok(Statement {
            id: row.get(0)?,
            event_id: row.get(1)?,
            text: row.get(2)?,
            context: row.get(3)?,
            truth_score: row.get(4)?,
            created_at: row.get(5)?,
            updated_at: row.get(6)?,
            signature: row.get(7)?,
            public_key: row.get(8)?,
        })
    })?;

    let mut statements = Vec::new();
    for s in rows {
        statements.push(s?);
    }
    Ok(statements)
}

/// Обновить оценку правдивости утверждения
pub fn update_statement_score(conn: &Connection, id: i64, truth_score: f32) -> Result<(), CoreError> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        r#"UPDATE statements SET truth_score = ?2, updated_at = ?3 WHERE id = ?1"#,
        params![id, truth_score, now],
    )?;
    Ok(())
}

/// Загружаем все события
pub fn load_truth_events(conn: &Connection) -> Result<Vec<TruthEvent>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code, signature, public_key FROM truth_events",
    )?;

    let rows = stmt.query_map([], |row| {
        Ok(TruthEvent {
            id: row.get(0)?,
            description: row.get(1)?,
            context_id: row.get(2)?,
            vector: row.get::<_, i64>(3)? != 0,
            detected: row.get::<_, Option<i64>>(4)?.map(|v| v != 0),
            corrected: row.get::<_, i64>(5)? != 0,
            timestamp_start: row.get(6)?,
            timestamp_end: row.get(7)?,
            code: row.get(8)?,
            signature: row.get(9)?,
            public_key: row.get(10)?,
        })
    })?;

    let mut events = Vec::new();
    for e in rows {
        events.push(e?);
    }
    Ok(events)
}

/// Загружаем все записи влияния
pub fn load_impacts(conn: &Connection) -> Result<Vec<Impact>, CoreError> {
    let mut stmt = conn.prepare("SELECT id, event_id, type_id, value, notes, created_at, signature, public_key FROM impact")?;

    let rows = stmt.query_map([], |row| {
        Ok(Impact {
            id: row.get(0)?,
            event_id: row.get(1)?,
            type_id: row.get(2)?,
            value: row.get::<_, i64>(3)? != 0,
            notes: row.get(4)?,
            created_at: row.get(5)?,
            signature: row.get(6)?,
            public_key: row.get(7)?,
        })
    })?;

    let mut impacts = Vec::new();
    for i in rows {
        impacts.push(i?);
    }
    Ok(impacts)
}

/// Загружаем все метрики прогресса
pub fn load_metrics(conn: &Connection) -> Result<Vec<ProgressMetrics>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT id, timestamp, total_events, total_events_group, total_positive_impact, total_positive_impact_group, total_negative_impact, total_negative_impact_group, trend, trend_group FROM progress_metrics",
    )?;

    let rows = stmt.query_map([], |row| {
        Ok(ProgressMetrics {
            id: row.get(0)?,
            timestamp: row.get(1)?,
            total_events: row.get(2)?,
            total_events_group: row.get(3)?,
            total_positive_impact: row.get(4)?,
            total_positive_impact_group: row.get(5)?,
            total_negative_impact: row.get(6)?,
            total_negative_impact_group: row.get(7)?,
            trend: row.get(8)?,
            trend_group: row.get(9)?,
        })
    })?;

    let mut metrics = Vec::new();
    for m in rows {
        metrics.push(m?);
    }
    Ok(metrics)
}

/// Добавить запись в журнал синхронизации
pub fn log_sync(
    conn: &Connection,
    op: &str,
    table_name: &str,
    record_id: &str,
    signature: Option<String>,
    public_key: Option<String>,
) -> Result<i64, CoreError> {
    let created_at = Utc::now().timestamp();
    conn.execute(
        r#"INSERT INTO sync_log (op, table_name, record_id, signature, public_key, created_at)
           VALUES (?1, ?2, ?3, ?4, ?5, ?6)"#,
        params![op, table_name, record_id, signature, public_key, created_at],
    )?;
    Ok(conn.last_insert_rowid())
}

/// Добавить запись высокого уровня о попытке синхронизации
pub fn log_sync_event(
    conn: &Connection,
    peer_url: &str,
    mode: &str,
    status: &str,
    details: &str,
) -> Result<i64, CoreError> {
    let ts = Utc::now().timestamp();
    conn.execute(
        r#"INSERT INTO sync_logs (timestamp, peer_url, mode, status, details)
           VALUES (?1, ?2, ?3, ?4, ?5)"#,
        params![ts, peer_url, mode, status, details],
    )?;
    Ok(conn.last_insert_rowid())
}

/// Получить последние N записей sync_logs (DESC по id)
pub fn get_recent_sync_logs(conn: &Connection, limit: usize) -> Result<Vec<SyncLog>, CoreError> {
    let mut stmt = conn.prepare(
        "SELECT id, timestamp, peer_url, mode, status, details FROM sync_logs ORDER BY id DESC LIMIT ?1",
    )?;
    let rows = stmt.query_map(params![limit as i64], |row| {
        Ok(SyncLog {
            id: row.get(0)?,
            timestamp: row.get(1)?,
            peer_url: row.get(2)?,
            mode: row.get(3)?,
            status: row.get(4)?,
            details: row.get(5)?,
        })
    })?;
    let mut out = Vec::new();
    for r in rows { out.push(r?); }
    Ok(out)
}

/// Очистить журнал sync_logs
pub fn clear_sync_logs(conn: &Connection) -> Result<(), CoreError> {
    conn.execute("DELETE FROM sync_logs", [])?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::trust_propagation::blend_trust;

    #[test]
    fn inserts_and_sync_log_work() {
        let mut conn = open_db(":memory:").expect("open db");
        // Ensure FK targets exist
        seed_knowledge_base(&mut conn, "en").expect("seed kb");

        // Вставка события
        let new_ev = NewTruthEvent {
            description: "Test event".to_string(),
            context_id: 1,
            vector: true,
            timestamp_start: 1_700_000_000,
            code: 1,
        };
        let ev_id = add_truth_event(&conn, new_ev).expect("insert event");
        let ev = get_truth_event(&conn, ev_id).expect("get event").expect("event exists");
        assert_eq!(ev.id, ev_id);
        assert_eq!(ev.description, "Test event");
        assert!(ev.signature.is_none());
        assert!(ev.public_key.is_none());

        // Запись в sync_log
        let log_id = log_sync(
            &conn,
            "insert",
            "truth_events",
            &ev_id.to_string(),
            None,
            None,
        )
        .expect("log sync");
        assert!(log_id > 0);

        // Проверка количества записей sync_log
        let count: i64 = conn
            .query_row("SELECT COUNT(*) FROM sync_log", [], |r| r.get(0))
            .expect("count sync_log");
        assert_eq!(count, 1);

        // Вставка impact и чтение
        let _impact_id = add_impact(&conn, ev_id, 1, true, Some("note".to_string()))
            .expect("add impact");
        let impacts = load_impacts(&conn).expect("load impacts");
        assert!(!impacts.is_empty());
        let imp = &impacts[0];
        assert!(imp.signature.is_none());
        assert!(imp.public_key.is_none());
    }

    #[test]
    fn recalc_ratings_basic() {
        let mut conn = open_db(":memory:").expect("open db");
        seed_knowledge_base(&mut conn, "en").expect("seed kb");

        // Author nodeA creates an event
        let new_ev = NewTruthEvent {
            description: "Rated event".to_string(),
            context_id: 1,
            vector: true,
            timestamp_start: 1_700_000_100,
            code: 1,
        };
        let ev_id = add_truth_event(&conn, new_ev).expect("insert event");
        conn.execute(
            "UPDATE truth_events SET public_key=?1 WHERE id=?2",
            params!["nodeA", ev_id],
        )
        .expect("set event author pk");

        // Statement with positive truth_score for this event
        let stmt_id = add_statement(&conn, NewStatement {
            event_id: ev_id,
            text: "Looks true".to_string(),
            context: None,
            truth_score: Some(0.9),
        }).expect("add statement");
        assert!(stmt_id > 0);

        // Validator nodeB adds positive impact for the event
        let impact_id = add_impact(&conn, ev_id, 1, true, Some("agree".to_string())).expect("add impact");
        conn.execute(
            "UPDATE impact SET public_key=?1 WHERE id=?2",
            params!["nodeB", impact_id],
        ).expect("set impact validator pk");

        // Recalc ratings
        recalc_ratings(&conn, 1_700_000_200).expect("recalc ratings");

        // Load and assert node ratings
        let ratings = load_node_ratings(&conn).expect("load node ratings");
        assert!(ratings.iter().any(|r| r.node_id == "nodeA"));
        assert!(ratings.iter().any(|r| r.node_id == "nodeB"));
        let ra = ratings.iter().find(|r| r.node_id == "nodeA").unwrap();
        assert_eq!(ra.events_true, 1);
        assert_eq!(ra.events_false, 0);
        assert!(ra.trust_score > 0.0);
        let rb = ratings.iter().find(|r| r.node_id == "nodeB").unwrap();
        assert!(rb.validations >= 1);

        // Group rating exists and includes members
        let groups = load_group_ratings(&conn).expect("load group ratings");
        assert!(!groups.is_empty());
        let g = &groups[0];
        assert!(g.members.contains(&"nodeA".to_string()));
        assert!(g.members.contains(&"nodeB".to_string()));

        // Graph data contains nodes and link between nodeB (validator) and nodeA (author)
        let graph = load_graph(&conn).expect("load graph");
        assert!(graph.nodes.len() >= 2);
        assert!(graph.links.iter().any(|l| l.source == "nodeB" && l.target == "nodeA"));
    }

    #[test]
    fn merge_ratings_conflict_resolution() {
        let conn = open_db(":memory:").expect("open db");
        // стартовые данные
        conn.execute(
            "INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, last_updated) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params!["nodeX", 1, 0, 0, 0, 0.5_f64, 100],
        ).unwrap();

        // входящие: ниже trust_score и более новый — не должен перезаписать, если trust ниже
        let incoming_nodes = vec![NodeRating {
            node_id: "nodeX".into(),
            events_true: 2,
            events_false: 0,
            validations: 0,
            reused_events: 0,
            trust_score: 0.4,
            last_updated: 200,
        }];
        let incoming_groups: Vec<GroupRating> = vec![];
        let _ = merge_ratings(&conn, &incoming_nodes, &incoming_groups).unwrap();
        let cur = load_node_ratings(&conn).unwrap();
        let nx = cur.iter().find(|r| r.node_id == "nodeX").unwrap();
        // Сначала применяется временной спад: 0.5 * 0.9 = 0.45
        // Затем смешивание: 0.45 * 0.8 + 0.4 * 0.2 = 0.36 + 0.08 = 0.44
        assert!((nx.trust_score - 0.44).abs() < 1e-6);
        // last_updated обновляется до now_ts при merge_ratings
        assert!(nx.last_updated >= 100);

        // входящие: выше trust_score — должен перезаписать
        let incoming_nodes = vec![NodeRating {
            node_id: "nodeX".into(),
            events_true: 3,
            events_false: 0,
            validations: 0,
            reused_events: 0,
            trust_score: 0.9,
            last_updated: 150,
        }];
        let _ = merge_ratings(&conn, &incoming_nodes, &incoming_groups).unwrap();
        let cur = load_node_ratings(&conn).unwrap();
        let nx = cur.iter().find(|r| r.node_id == "nodeX").unwrap();
        // Смешивание: 0.44 * 0.8 + 0.9 * 0.2 = 0.352 + 0.18 = 0.532
        assert!((nx.trust_score - 0.532).abs() < 1e-6);
        // last_updated обновляется до now_ts при merge_ratings
        assert!(nx.last_updated >= 150);

        // Groups: берем более новый по last_updated
        conn.execute(
            "INSERT INTO group_ratings (group_id, members, avg_score, coherence, last_updated) VALUES ('g', '[]', 0.1, 0.5, 100)",
            [],
        ).unwrap();
        let incoming_groups = vec![GroupRating {
            group_id: "g".into(),
            members: vec!["a".into()],
            avg_score: 0.2,
            coherence: 0.6,
            last_updated: 200,
        }];
        let _ = merge_ratings(&conn, &[], &incoming_groups).unwrap();
        let groups = load_group_ratings(&conn).unwrap();
        let g = groups.iter().find(|g| g.group_id == "g").unwrap();
        assert!((g.avg_score - 0.2).abs() < 1e-6);
        assert!((g.coherence - 0.6).abs() < 1e-6);
        assert_eq!(g.last_updated, 200);
    }

    #[test]
    fn trust_blend_and_decay_apply() {
        let conn = open_db(":memory:").expect("open db");
        // вставляем локальную запись
        conn.execute(
            "INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, last_updated) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params!["nodeY", 1, 0, 0, 0, 0.5_f64, 100],
        ).unwrap();

        // входящая с более низким скором, blended должен быть 0.5*0.8 + 0.1*0.2 = 0.42
        let incoming_nodes = vec![NodeRating {
            node_id: "nodeY".into(),
            events_true: 2,
            events_false: 0,
            validations: 0,
            reused_events: 0,
            trust_score: 0.1,
            last_updated: 200,
        }];
        let diffs = merge_ratings(&conn, &incoming_nodes, &[]).unwrap();
        let cur = load_node_ratings(&conn).unwrap();
        let ny = cur.iter().find(|r| r.node_id == "nodeY").unwrap();
        // Сначала применяется временной спад: 0.5 * 0.9 = 0.45
        // Затем смешивание: 0.45 * 0.8 + 0.1 * 0.2 = 0.36 + 0.02 = 0.38
        assert!((ny.trust_score - 0.38).abs() < 1e-6);
        assert!(diffs.iter().any(|(id, _)| id == "nodeY"));

        // Проверим clamp и саму функцию смешивания
        assert!((blend_trust(1.0, 1.0) - 1.0).abs() < 1e-6);
        assert!((blend_trust(-1.0, -1.0) - (-1.0)).abs() < 1e-6);
    }

    #[test]
    fn rbac_users_basic_crud_and_sync() {
        let conn = open_db(":memory:").expect("open db");
        // initially empty
        let u = get_user_by_pubkey(&conn, "nodeA").unwrap();
        assert!(u.is_none());

        // grant role
        update_user_role(&conn, "nodeA", "admin").unwrap();
        let u = get_user_by_pubkey(&conn, "nodeA").unwrap().unwrap();
        assert_eq!(u.role, "admin");
        assert!((u.trust_score - 0.0).abs() < 1e-6);

        // adjust trust
        let v = adjust_trust_score(&conn, "nodeA", 0.3).unwrap();
        assert!((v - 0.3).abs() < 1e-6);

        // mirror from node_ratings
        conn.execute(
            "INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, last_updated) VALUES ('nodeA',0,0,0,0,0.8,1)",
            [],
        ).unwrap();
        let n = sync_users_with_node_ratings(&conn).unwrap();
        assert!(n >= 1);
        let u = get_user_by_pubkey(&conn, "nodeA").unwrap().unwrap();
        assert!((u.trust_score - 0.8).abs() < 1e-6);
    }
}
