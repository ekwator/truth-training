// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;

use commands::events::{create_event_fast, get_event_fast, health_check_core};
use commands::judgments::{submit_judgment_fast, calculate_consensus_fast, get_judgment_stats};

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            create_event_fast,
            get_event_fast,
            health_check_core,
            submit_judgment_fast,
            calculate_consensus_fast,
            get_judgment_stats
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
