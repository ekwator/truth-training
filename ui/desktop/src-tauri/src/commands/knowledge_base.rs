use serde::Serialize;
use tauri::command;
use std::path::PathBuf;
use std::fs;
use dirs;

#[derive(Debug, Serialize)]
pub struct KBItem {
    pub id: String,
    pub label: String,
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
            if in_section { break; }
            if line.contains("knowledge_base") { in_section = true; }
            continue;
        }
        if in_section {
            let t = line.trim();
            if t.starts_with("-") || t.starts_with("*") {
                let label = t.trim_start_matches(&['-','*',' '][..]).to_string();
                if !label.is_empty() {
                    let id = format!("kb:{}", label.to_lowercase().replace(' ', "_"));
                    items.push(KBItem{ id, label });
                }
            }
        }
    }
    items
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
                let mut items = parse_kb_from_markdown(&content);
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
            KBItem { id: "kb:general".to_string(), label: "General".to_string() },
            KBItem { id: "kb:technology".to_string(), label: "Technology".to_string() },
            KBItem { id: "kb:science".to_string(), label: "Science".to_string() },
        ];
    }
    Ok(KBListResponse { items })
}


