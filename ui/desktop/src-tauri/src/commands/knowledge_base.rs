use dirs;
use log::{info, error};
use rusqlite::{params, Transaction};
use serde::{Serialize, Deserialize};
use std::fs;
use std::path::PathBuf;
use tauri::{command, State};
use crate::storage::Db;
use core_lib::storage as truth_storage;
use core_lib::models::Context;

#[derive(Debug, Serialize)]
pub struct KBItem {
    pub id: String,
    pub label: String,
}

#[derive(Debug, Serialize)]
pub struct ReseedResult {
    pub success: bool,
    pub message: String,
    pub tables_updated: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct KBListResponse {
    pub items: Vec<KBItem>,
}

fn parse_kb_from_markdown(md: &str) -> Vec<KBItem> {
    // naive parse: extract lines under heading starting with "## 1. " until next heading starting with "## "
    let mut items = Vec::new();
    let mut in_section = false;
    for line in md.lines() {
        if line.trim_start().starts_with("## ") {
            if in_section {
                break;
            }
            if line.contains("knowledge_base") {
                in_section = true;
            }
            continue;
        }
        if in_section {
            let t = line.trim();
            if t.starts_with("-") || t.starts_with("*") {
                let label = t.trim_start_matches(&['-', '*', ' '][..]).to_string();
                if !label.is_empty() {
                    let id = format!("kb:{}", label.to_lowercase().replace(' ', "_"));
                    items.push(KBItem { id, label });
                }
            }
        }
    }
    items
}

/// Entity name structure for frontend
#[derive(Debug, Serialize)]
pub struct EntityName {
    pub id: i64,
    pub name: String,
}

/// Get entity names by type (category, forma, cause, develop, effect)
/// Used by frontend for entity name resolution
#[command]
pub async fn get_entity_names(
    entity_type: String,
    db: State<'_, Db>,
) -> Result<Vec<EntityName>, String> {
    let conn = db.0.lock();
    
    let query = match entity_type.as_str() {
        "category" => "SELECT id, name FROM category ORDER BY id",
        "forma" => "SELECT id, name FROM forma ORDER BY id",
        "cause" => "SELECT id, name FROM cause ORDER BY id",
        "develop" => "SELECT id, name FROM develop ORDER BY id",
        "effect" => "SELECT id, name FROM effect ORDER BY id",
        _ => return Err(format!("Unknown entity type: {}", entity_type)),
    };
    
    let mut stmt = conn
        .prepare(query)
        .map_err(|e| format!("Failed to prepare query: {}", e))?;
    
    let rows = stmt
        .query_map([], |row| {
            Ok(EntityName {
                id: row.get(0)?,
                name: row.get(1)?,
            })
        })
        .map_err(|e| format!("Failed to query entities: {}", e))?;
    
    let mut entities = Vec::new();
    for row in rows {
        entities.push(row.map_err(|e| format!("Failed to read row: {}", e))?);
    }
    
    Ok(entities)
}

#[command]
pub async fn knowledge_base_list() -> Result<KBListResponse, String> {
    // Resolution order:
    // 1) User-provided markdown at ~/.truth-training/Data_Schema.md
    // 2) Development markdown near repo (best-effort)
    // 3) Built-in minimal defaults

    // 1) Check user config dir
    if let Some(home) = dirs::home_dir() {
        let user_md = home.join(".truth-training").join("Data_Schema.md");
        if user_md.exists() {
            if let Ok(content) = fs::read_to_string(&user_md) {
                let items = parse_kb_from_markdown(&content);
                if !items.is_empty() {
                    return Ok(KBListResponse { items });
                }
            }
        }
    }

    // 2) Try dev-relative file (non-fatal on failure)
    let mut items = Vec::new();
    let mut dev_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    dev_path.push("../../docs/Data_Schema.md");
    if let Ok(content) = fs::read_to_string(&dev_path) {
        items = parse_kb_from_markdown(&content);
    }
    if items.is_empty() {
        // 3) Fallback minimal defaults
        items = vec![
            KBItem {
                id: "kb:general".to_string(),
                label: "General".to_string(),
            },
            KBItem {
                id: "kb:technology".to_string(),
                label: "Technology".to_string(),
            },
            KBItem {
                id: "kb:science".to_string(),
                label: "Science".to_string(),
            },
        ];
    }
    Ok(KBListResponse { items })
}

// ============================================================================
// Safe Database Reseeding with Temporary Tables
// ============================================================================
// These functions implement safe database reseeding using temporary tables
// to maintain FK → PK integrity during knowledge base updates.
// Full implementation will be completed in Phase 5 (User Story 3).

/// Create temporary tables for knowledge base reseeding.
/// Creates temp_category, temp_forma, temp_cause, temp_develop, temp_effect, temp_context.
fn create_temp_tables(tx: &Transaction) -> Result<(), String> {
    info!("Creating temporary tables for reseeding...");
    
    // Create temp_category
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_category (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_category: {}", e))?;

    // Create temp_forma
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_forma (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            quality INTEGER NOT NULL,
            description TEXT
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_forma: {}", e))?;

    // Create temp_cause
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_cause (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            quality INTEGER NOT NULL,
            description TEXT
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_cause: {}", e))?;

    // Create temp_develop
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_develop (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            quality INTEGER NOT NULL,
            description TEXT
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_develop: {}", e))?;

    // Create temp_effect
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_effect (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            quality INTEGER NOT NULL,
            description TEXT
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_effect: {}", e))?;

    // Create temp_context (with FK references to temp_* tables)
    tx.execute(
        "CREATE TABLE IF NOT EXISTS temp_context (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            category_id INTEGER,
            forma_id INTEGER,
            cause_id INTEGER,
            develop_id INTEGER,
            effect_id INTEGER,
            description TEXT,
            FOREIGN KEY(category_id) REFERENCES temp_category(id),
            FOREIGN KEY(forma_id) REFERENCES temp_forma(id),
            FOREIGN KEY(cause_id) REFERENCES temp_cause(id),
            FOREIGN KEY(develop_id) REFERENCES temp_develop(id),
            FOREIGN KEY(effect_id) REFERENCES temp_effect(id)
        )",
        [],
    ).map_err(|e| format!("Failed to create temp_context: {}", e))?;

    info!("Temporary tables created successfully");
    Ok(())
}

/// Fill temporary tables with English-only data.
/// Inserts data into all temporary tables.
fn fill_temp_tables(tx: &Transaction) -> Result<(), String> {
    info!("Filling temporary tables with English-only data...");
    
    // Insert categories (English)
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
            "INSERT INTO temp_category (id, name, description) VALUES (?1, ?2, ?3)",
            params![id, name, desc],
        ).map_err(|e| format!("Failed to insert into temp_category: {}", e))?;
    }

    // Insert causes (English)
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
        tx.execute(
            "INSERT INTO temp_cause (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        ).map_err(|e| format!("Failed to insert into temp_cause: {}", e))?;
    }

    // Insert develops (English)
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
            "INSERT INTO temp_develop (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        ).map_err(|e| format!("Failed to insert into temp_develop: {}", e))?;
    }

    // Insert effects (English)
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
            "INSERT INTO temp_effect (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        ).map_err(|e| format!("Failed to insert into temp_effect: {}", e))?;
    }

    // Insert formas (English)
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
            "INSERT INTO temp_forma (id, name, quality, description) VALUES (?1, ?2, ?3, ?4)",
            params![id, name, q, desc],
        ).map_err(|e| format!("Failed to insert into temp_forma: {}", e))?;
    }

    // Insert contexts (English) - with FK references to temp_* tables
    let contexts: &[(i64, &str, i64, i64, i64, i64, i64, &str)] = &[
        (1, "Interpersonal: openness", 1, 2, 5, 3, 2, "Honest dialogue, strengthening trust"),
        (2, "Interpersonal: concealment", 1, 1, 1, 1, 1, "Withholding a significant fact, trust erosion"),
        (3, "Finance: fraud", 2, 1, 2, 2, 5, "Deception for profit, legal consequences"),
        (4, "Finance: transparent reporting", 2, 2, 5, 4, 8, "Verifiable facts, reputation growth"),
        (5, "Politics: treaty breach", 3, 1, 2, 1, 1, "Hidden violations, loss of trust"),
        (6, "Politics: treaty compliance", 3, 2, 5, 4, 2, "Confirmed execution of obligations"),
        (7, "Organization: admitting a mistake", 6, 2, 5, 6, 6, "Admission and correction improve learning"),
        (8, "Media: disinformation", 7, 1, 7, 2, 3, "Manipulations leading to conflict"),
    ];
    for (id, name, cat, forma, cause, develop, effect, desc) in contexts {
        tx.execute(
            "INSERT INTO temp_context (id, name, category_id, forma_id, cause_id, develop_id, effect_id, description) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![id, name, cat, forma, cause, develop, effect, desc],
        ).map_err(|e| format!("Failed to insert into temp_context: {}", e))?;
    }

    info!("Temporary tables filled successfully");
    Ok(())
}

/// Validate FK integrity of temporary tables before swap.
/// Ensures all foreign keys in temp tables reference valid primary keys.
fn validate_temp_table_fks(tx: &Transaction) -> Result<(), String> {
    info!("Validating FK integrity of temporary tables...");
    
    // Check that all FK references in temp_context are valid
    let invalid_fks: i64 = tx.query_row(
        "SELECT COUNT(*) FROM temp_context tc
         WHERE (tc.category_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM temp_category WHERE id = tc.category_id))
            OR (tc.forma_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM temp_forma WHERE id = tc.forma_id))
            OR (tc.cause_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM temp_cause WHERE id = tc.cause_id))
            OR (tc.develop_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM temp_develop WHERE id = tc.develop_id))
            OR (tc.effect_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM temp_effect WHERE id = tc.effect_id))",
        [],
        |row| row.get(0),
    ).map_err(|e| format!("Failed to validate FK integrity: {}", e))?;

    if invalid_fks > 0 {
        return Err(format!("FK integrity violation: {} invalid foreign key references found in temp_context", invalid_fks));
    }

    info!("FK integrity validation passed");
    Ok(())
}

/// Atomic swap of temporary tables to main tables.
/// Uses transaction to ensure atomicity.
fn atomic_swap(tx: &Transaction) -> Result<(), String> {
    info!("Performing atomic swap of temporary tables...");
    
    // Swap category
    tx.execute("ALTER TABLE category RENAME TO old_category", [])
        .map_err(|e| format!("Failed to rename category: {}", e))?;
    tx.execute("ALTER TABLE temp_category RENAME TO category", [])
        .map_err(|e| format!("Failed to rename temp_category to category: {}", e))?;
    tx.execute("DROP TABLE old_category", [])
        .map_err(|e| format!("Failed to drop old_category: {}", e))?;

    // Swap forma
    tx.execute("ALTER TABLE forma RENAME TO old_forma", [])
        .map_err(|e| format!("Failed to rename forma: {}", e))?;
    tx.execute("ALTER TABLE temp_forma RENAME TO forma", [])
        .map_err(|e| format!("Failed to rename temp_forma to forma: {}", e))?;
    tx.execute("DROP TABLE old_forma", [])
        .map_err(|e| format!("Failed to drop old_forma: {}", e))?;

    // Swap cause
    tx.execute("ALTER TABLE cause RENAME TO old_cause", [])
        .map_err(|e| format!("Failed to rename cause: {}", e))?;
    tx.execute("ALTER TABLE temp_cause RENAME TO cause", [])
        .map_err(|e| format!("Failed to rename temp_cause to cause: {}", e))?;
    tx.execute("DROP TABLE old_cause", [])
        .map_err(|e| format!("Failed to drop old_cause: {}", e))?;

    // Swap develop
    tx.execute("ALTER TABLE develop RENAME TO old_develop", [])
        .map_err(|e| format!("Failed to rename develop: {}", e))?;
    tx.execute("ALTER TABLE temp_develop RENAME TO develop", [])
        .map_err(|e| format!("Failed to rename temp_develop to develop: {}", e))?;
    tx.execute("DROP TABLE old_develop", [])
        .map_err(|e| format!("Failed to drop old_develop: {}", e))?;

    // Swap effect
    tx.execute("ALTER TABLE effect RENAME TO old_effect", [])
        .map_err(|e| format!("Failed to rename effect: {}", e))?;
    tx.execute("ALTER TABLE temp_effect RENAME TO effect", [])
        .map_err(|e| format!("Failed to rename temp_effect to effect: {}", e))?;
    tx.execute("DROP TABLE old_effect", [])
        .map_err(|e| format!("Failed to drop old_effect: {}", e))?;

    // Swap context
    tx.execute("ALTER TABLE context RENAME TO old_context", [])
        .map_err(|e| format!("Failed to rename context: {}", e))?;
    tx.execute("ALTER TABLE temp_context RENAME TO context", [])
        .map_err(|e| format!("Failed to rename temp_context to context: {}", e))?;
    tx.execute("DROP TABLE old_context", [])
        .map_err(|e| format!("Failed to drop old_context: {}", e))?;

    info!("Atomic swap completed successfully");
    Ok(())
}

/// Safe database reseeding command.
/// Reseeds knowledge base using temporary tables for safe updates.
/// Note: Frontend should refresh knowledge base data after successful reseeding.
#[command]
pub async fn reseed_knowledge_base(
    db: State<'_, Db>,
) -> Result<ReseedResult, String> {
    info!("Starting knowledge base reseeding process...");
    
    let mut conn = db.0.lock();
    
    // Use core function to reseed knowledge base with English locale
    // The core function handles transaction safety internally
    truth_storage::seed_knowledge_base(&mut conn, "en")
        .map_err(|e| {
            error!("Reseeding failed: {}", e);
            format!("Reseeding failed: {}", e)
        })?;

    // Note: Frontend should refresh knowledge base data after successful reseeding
    // Event emission can be handled via separate mechanism if needed
    info!("Knowledge base reseeded successfully");
    
    Ok(ReseedResult {
        success: true,
        message: "Knowledge base reseeded successfully".to_string(),
        tables_updated: vec![
            "category".to_string(),
            "forma".to_string(),
            "cause".to_string(),
            "develop".to_string(),
            "effect".to_string(),
            "context".to_string(),
        ],
    })
}
