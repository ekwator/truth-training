// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;
mod storage;

pub const LOGS_PAGE_SIZE: usize = 35;

use commands::config::{get_app_config, save_app_config, core_status, test_http_connection, init_app};
use commands::events::{create_event_fast, get_event_fast, list_events_fast, health_check_core};
use commands::impacts::add_impact;
use commands::judgments::{submit_judgment_fast, judgments_list_fast, get_judgment_stats};
use commands::knowledge_base::knowledge_base_list;
use commands::logs::{list_logs, clear_logs};
use commands::summary::{get_overall_metrics, list_event_rows, export_overall_summary_txt};

fn main() {
    let db = storage::Db::initialize().expect("failed to init db");

    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            get_app_config,
            save_app_config,
            core_status,
            test_http_connection,
            init_app,
            create_event_fast,
            get_event_fast,
            health_check_core,
            list_events_fast,
            add_impact,
            submit_judgment_fast,
            judgments_list_fast,
            get_judgment_stats,
            knowledge_base_list,
            list_logs,
            clear_logs,
            get_overall_metrics,
            list_event_rows,
            export_overall_summary_txt
        ])
        .manage(db)
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
