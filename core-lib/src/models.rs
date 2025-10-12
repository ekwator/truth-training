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
    pub code: u8,                   // 8-bit event code (2 control bits + 6 counter bits)
    pub signature: Option<String>,  // Подпись события
    pub public_key: Option<String>, // Публичный ключ автора
}

/// Вспомогательная структура для вставки события
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NewTruthEvent {
pub description: String,
pub context_id: i64,
pub vector: bool,
pub timestamp_start: i64,
    pub code: u8,
}

/// Воздействие (таблица: impact)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Impact {
    pub id: String,
    pub event_id: String, // FK → truth_events.id
    pub type_id: i64,  // FK → impact_type.id
    pub value: bool,   // true = позитивное, false = негативное
pub notes: Option<String>,
    pub created_at: i64,
    pub signature: Option<String>,  // Подпись записи влияния
    pub public_key: Option<String>, // Публичный ключ автора
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

/// Утверждение (таблица: statements)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Statement {
    pub id: i64,                    // INTEGER, PK
    pub event_id: i64,              // INTEGER (FK → truth_events.id)
    pub text: String,               // TEXT - текст утверждения
    pub context: Option<String>,    // TEXT NULLABLE - дополнительный контекст
    pub truth_score: Option<f32>,   // REAL NULLABLE - оценка правдивости (-1..+1)
    pub created_at: i64,            // INTEGER (UNIX secs)
    pub updated_at: i64,            // INTEGER (UNIX secs)
    pub signature: Option<String>,  // Подпись утверждения
    pub public_key: Option<String>, // Публичный ключ автора
}

/// Вспомогательная структура для вставки утверждения
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NewStatement {
    pub event_id: i64,
    pub text: String,
    pub context: Option<String>,
    pub truth_score: Option<f32>,
}

/// Пользователь (для будущего расширения)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: String, // Уникальный идентификатор пользователя
    pub created_at: i64,
    pub last_sync: Option<i64>,
}

/// Запись пользователя в БД (таблица: users)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserRecord {
    pub public_key: String,
    pub role: String, // "admin" | "node" | "observer"
    pub created_at: i64,
    pub updated_at: i64,
}

/// Представление пользователя с доверием (users × node_ratings)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserView {
    pub public_key: String,
    pub role: String,
    pub trust_score: f32, // -1..1 (из node_ratings)
}

/// Рейтинг узла/ноды (node_id = публичный ключ в hex)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NodeRating {
    pub node_id: String,
    pub events_true: u32,
    pub events_false: u32,
    pub validations: u32,
    pub reused_events: u32,
    pub trust_score: f32, // -1.0 .. 1.0
    pub propagation_priority: f32, // 0.0 .. 1.0 — скорость ретрансляции
    pub last_updated: i64,
}

/// Рейтинг группы узлов
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GroupRating {
    pub group_id: String,
    pub members: Vec<String>,
    pub avg_score: f32,
    pub coherence: f32, // 0..1
    pub last_updated: i64,
}

/// Метрики узла для мониторинга сети
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NodeMetrics {
    pub pubkey: String,
    pub last_seen: i64,
    pub relay_success_rate: f32,
    pub quality_index: f32, // 0.0..1.0 — индикатор непрерывности доверия
    pub propagation_priority: f32, // 0.0..1.0 — адаптивный приоритет распространения
}

/// Узел графа для визуализации
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GraphNode {
    pub id: String,
    pub score: f32,
    pub propagation_priority: f32,
    pub last_seen: Option<i64>,
    pub relay_success_rate: Option<f32>,
    pub quality_index: f32,
}

/// Ребро графа между валидатором (source) и автором события (target)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GraphLink {
    pub source: String,
    pub target: String,
    pub weight: f32, // 0..1
    pub latency_ms: Option<u32>,
}

/// Данные графа доверия
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GraphData {
    pub nodes: Vec<GraphNode>,
    pub links: Vec<GraphLink>,
}

/// Сводные метрики графа для /graph/summary
#[derive(Serialize, Deserialize)]
pub struct GraphSummary {
    pub total_nodes: usize,
    pub total_links: usize,
    pub avg_trust: f64,
    // Среднее качество (0..1) — для обзора сети
    // Не входит в расчёт avg_trust; это независимый индикатор континуальности
    // Значение агрегируется на уровне API, но поле предусмотрено для потенциального расширения
    pub top_nodes: Vec<(String, f64)>,
}

/// Подсчитать агрегаты и топ-узлы по trust_score
pub fn summarize_graph(graph: &GraphData) -> GraphSummary {
    let total_nodes = graph.nodes.len();
    let total_links = graph.links.len();
    let avg_trust: f64 = if total_nodes == 0 {
        0.0
    } else {
        let sum: f64 = graph.nodes.iter().map(|n| n.score as f64).sum();
        sum / (total_nodes as f64)
    };
    let mut top: Vec<(String, f64)> = graph
        .nodes
        .iter()
        .map(|n| (n.id.clone(), n.score as f64))
        .collect();
    top.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));
    // Ограничим топ до 10 элементов для компактности
    if top.len() > 10 { top.truncate(10); }

    GraphSummary {
        total_nodes,
        total_links,
        avg_trust,
        top_nodes: top,
    }
}

/// История по пиру (агрегированная запись)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PeerHistoryEntry {
    pub id: i64,
    pub peer_url: String,
    pub last_sync: Option<i64>,
    pub success_count: i64,
    pub fail_count: i64,
    pub last_quality_index: f32,
    pub last_trust_score: f32,
}

/// Сводка по локальной сети/пирам
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PeerSummary {
    pub total_peers: usize,
    pub avg_success_rate: f32,
    pub avg_quality_index: f32,
}

/// Запись журнала синхронизации высокого уровня
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncLog {
    pub id: i64,
    pub timestamp: i64,
    pub peer_url: String,
    pub mode: String,
    pub status: String,
    pub details: String,
}

/// Учетная запись пользователя с ролью и доверием (RBAC)
#[derive(Debug, Clone, Serialize, Deserialize, utoipa::ToSchema)]
pub struct RbacUser {
    pub pubkey: String,
    pub role: String,        // observer | node | admin
    pub trust_score: f32,    // -1.0 .. 1.0 (зеркалит node_ratings)
    pub last_updated: i64,   // unix seconds
    pub display_name: Option<String>,
}
