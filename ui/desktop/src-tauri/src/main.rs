// Prevents additional console window on Windows in release, DO NOT REMOVE!!
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;
mod discovery;
mod logging;
mod settings;
mod storage;

pub const LOGS_PAGE_SIZE: usize = 35;

use commands::config::{
    core_status, get_app_config, init_app, save_app_config, test_http_connection,
};
use commands::events::{create_event_fast, get_event_fast, health_check_core, list_events_fast};
use commands::impacts::add_impact;
use commands::judgments::{get_judgment_stats, judgments_list_fast, submit_judgment_fast};
use commands::knowledge_base::knowledge_base_list;
use commands::logs::{clear_logs, list_logs};
use commands::summary::{export_overall_summary_txt, get_overall_metrics, list_event_rows};
use discovery::{
    cleanup_nodes, get_discovery_settings, list_nodes, manual_discover, run_nodes_health_check,
    save_discovery_settings_cmd, DiscoveryManager,
};

fn main() {
    if std::env::args().any(|a| a == "--version") {
        println!("Truth UI Desktop v{}", env!("CARGO_PKG_VERSION"));
        return;
    }

    let db = storage::Db::initialize().expect("failed to init db");
    let discovery_manager =
        DiscoveryManager::init_from_disk().expect("failed to initialize discovery manager");

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
            export_overall_summary_txt,
            list_nodes,
            manual_discover,
            cleanup_nodes,
            run_nodes_health_check,
            get_discovery_settings,
            save_discovery_settings_cmd
        ])
        .manage(db)
        .manage(discovery_manager)
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
