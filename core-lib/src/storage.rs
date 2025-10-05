use crate::{CoreError, Impact, NewTruthEvent, ProgressMetrics, TruthEvent, Statement, NewStatement};
use rusqlite::{Connection, OptionalExtension, params};
use serde::{Deserialize, Serialize};
// serde_json используется через полные пути
use uuid::Uuid;
use chrono::Utc;
use std::fs;

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
    Ok(())
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
        r#"SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code
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
            "INSERT INTO truth_events (id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",
            rusqlite::params![
                event.id,
                event.description,
                event.context_id,
                event.vector,
                event.detected,
                event.corrected,
                event.timestamp_start,
                event.timestamp_end,
                event.code
            ],
        )?;
    }

    for impact in data.impacts {
        tx.execute(
            "INSERT INTO impact (id, event_id, type_id, value, notes, created_at)
 VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            rusqlite::params![
                impact.id,
                impact.event_id,
                impact.type_id,
                if impact.value { 1 } else { 0 },
                impact.notes,
                impact.created_at,
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
        r#"SELECT id, event_id, text, context, truth_score, created_at, updated_at
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
            })
        })
        .optional()?;

    Ok(row_opt)
}

/// Получить все утверждения для события
pub fn get_statements_for_event(conn: &Connection, event_id: i64) -> Result<Vec<Statement>, CoreError> {
    let mut stmt = conn.prepare(
        r#"SELECT id, event_id, text, context, truth_score, created_at, updated_at
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
        "SELECT id, event_id, text, context, truth_score, created_at, updated_at FROM statements ORDER BY created_at DESC",
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
        "SELECT id, description, context_id, vector, detected, corrected, timestamp_start, timestamp_end, code FROM truth_events",
    )?;

    let rows = stmt.query_map([], |row| {
        Ok(TruthEvent {
            id: row.get(0)?,
            description: row.get(1)?,
            context_id: row.get(2)?,
            vector: row.get(3)?,
            detected: row.get(4)?,
            corrected: row.get(5)?,
            timestamp_start: row.get(6)?,
            timestamp_end: row.get(7)?,
            code: row.get(8)?,
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
    let mut stmt = conn.prepare("SELECT id, event_id, type_id, value, notes, created_at FROM impact")?;

    let rows = stmt.query_map([], |row| {
        Ok(Impact {
            id: row.get(0)?,
            event_id: row.get(1)?,
            type_id: row.get(2)?,
            value: row.get::<_, i64>(3)? != 0,
            notes: row.get(4)?,
            created_at: row.get(5)?,
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
