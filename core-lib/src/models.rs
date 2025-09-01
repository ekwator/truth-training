use serde::{Deserialize, Serialize};

/// Ошибки уровня ядра
#[derive(thiserror::Error, Debug)]
pub enum CoreError {
    #[error("Database error: {0}")]
    Db(#[from] rusqlite::Error),
    #[error("Invalid argument: {0}")]
    InvalidArg(String),
    #[error("Not found: {0}")]
    NotFound(String),
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    #[error("Serde JSON error: {0}")]
    Serde(#[from] serde_json::Error),
}

/// Категория (таблица: category)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Category {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
}

/// Причина (таблица: cause)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Cause {
    pub id: i64,
    pub name: String,
    pub quality: bool, // true = положительная, false = отрицательная
    pub description: Option<String>,
}

/// Проявление (таблица: develop)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Develop {
    pub id: i64,
    pub name: String,
    pub quality: bool, // true = положительная, false = отрицательная
    pub description: Option<String>,
}

/// Следствие (таблица: effect)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Effect {
    pub id: i64,
    pub name: String,
    pub quality: bool, // true = положительная, false = отрицательная
    pub description: Option<String>,
}

/// Форма логики (таблица: forma)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Forma {
    pub id: i64,
    pub name: String,
    pub quality: bool, // true = положительная, false = отрицательная
    pub description: Option<String>,
}

/// Контекст (таблица: context)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Context {
    pub id: i64,
    pub name: String,
    pub category_id: Option<i64>,
    pub forma_id: Option<i64>,
    pub cause_id: Option<i64>,
    pub develop_id: Option<i64>,
    pub effect_id: Option<i64>,
    pub description: Option<String>,
}

/// Тип воздействия (таблица: impact_type)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImpactType {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
}

/// Событие правды/лжи (таблица: truth_events)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TruthEvent {
    pub id: i64,                    // INTEGER, PK
    pub description: String,        // TEXT
    pub context_id: i64,            // INTEGER (FK → context.id)
    pub vector: bool,               // BOOLEAN (true = исходящее, false = входящее)
    pub detected: Option<bool>,     // BOOLEAN NULLABLE (распознано ли как ложь/правда)
    pub corrected: bool,            // BOOLEAN
    pub timestamp_start: i64,       // INTEGER (UNIX secs)
    pub timestamp_end: Option<i64>, // INTEGER NULLABLE (UNIX secs)
}

/// Вспомогательная структура для вставки события
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NewTruthEvent {
    pub description: String,
    pub context_id: i64,
    pub vector: bool,
    pub timestamp_start: i64,
}

/// Воздействие (таблица: impact)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Impact {
    pub id: i64,
    pub event_id: i64, // FK → truth_events.id
    pub type_id: i64,  // FK → impact_type.id
    pub value: bool,   // true = позитивное, false = негативное
    pub notes: Option<String>,
}

/// Метрики прогресса (таблица: progress_metrics)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProgressMetrics {
    pub id: i64,
    pub timestamp: i64,
    pub total_events: i64,
    pub total_events_group: i64,
    pub total_positive_impact: f64,
    pub total_positive_impact_group: f64,
    pub total_negative_impact: f64,
    pub total_negative_impact_group: f64,
    pub trend: f64,
    pub trend_group: f64,
}

/// Пользователь (для будущего расширения)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: String, // Уникальный идентификатор пользователя
    pub created_at: i64,
    pub last_sync: Option<i64>,
}
