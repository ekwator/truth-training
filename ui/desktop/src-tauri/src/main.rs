// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;
mod storage;

use commands::events::{create_event_fast, get_event_fast, list_events_fast, health_check_core};
use commands::judgments::{submit_judgment_fast, judgments_list_fast, get_judgment_stats};
use commands::knowledge_base::knowledge_base_list;
use commands::logs::{list_logs, clear_logs};
use commands::summary::{get_overall_metrics, list_event_rows, export_overall_summary_txt};

fn main() {
    let db = storage::Db::initialize().expect("failed to init db");

    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            create_event_fast,
            get_event_fast,
            health_check_core,
            list_events_fast,
            submit_judgment_fast,
            judgments_list_fast,
            get_judgment_stats,
            knowledge_base_list
            , list_logs
            , clear_logs
            , get_overall_metrics
            , list_event_rows
            , export_overall_summary_txt
        ])
        .manage(db)
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
