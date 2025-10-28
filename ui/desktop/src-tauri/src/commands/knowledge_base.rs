use serde::Serialize;
use tauri::command;
use std::path::PathBuf;

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
    // try bundled json first in future; for now read markdown
    let mut path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    path.push("../../docs/Data_Schema.md");
    let content = std::fs::read_to_string(&path).map_err(|e| e.to_string())?;
    let items = parse_kb_from_markdown(&content);
    Ok(KBListResponse { items })
}


