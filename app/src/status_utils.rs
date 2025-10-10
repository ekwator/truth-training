use anyhow::Result;
use colored::*;
use rusqlite::Connection;

// Переиспользуем структуру журнала из core-lib
pub type SyncLogEntry = core_lib::models::SyncLog;

#[derive(Debug, Clone, serde::Serialize, serde::Deserialize, Default)]
pub struct Peers {
    pub peers: Vec<PeerItem>,
}

#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct PeerItem {
    pub url: String,
    pub public_key: String,
}

/// Получить последние N записей из sync_logs
pub fn get_recent_sync_events(conn: &Connection, limit: usize) -> Result<Vec<SyncLogEntry>> {
    let logs = core_lib::storage::get_recent_sync_logs(conn, limit)
        .map_err(|e| anyhow::anyhow!(e))?;
    Ok(logs)
}

/// Печатает краткую сводку статуса узла
pub fn print_status_summary(
    config: &crate::config_utils::Config,
    peers: &Peers,
    logs: &[SyncLogEntry],
) {
    // Заголовок узла
    println!("{} {} (port {})", "Node:".blue(), config.node_name, config.port);
    println!("{} {}", "Database:".blue(), config.db_path);

    // Пиры
    let peer_count = peers.peers.len();
    if peer_count == 0 {
        println!("{} {}", "Peers:".blue(), "0".yellow());
    } else {
        let first_three: Vec<String> = peers
            .peers
            .iter()
            .take(3)
            .map(|p| p.url.clone())
            .collect();
        let suffix = if peer_count > 3 {
            format!(" (+{} more)", peer_count - 3)
        } else {
            String::new()
        };
        println!(
            "{} {}{}",
            "Peers:".blue(),
            first_three.join(", "),
            suffix
        );
    }

    // Логи синхронизации
    if logs.is_empty() {
        println!("{} {}", "Sync:".blue(), "No sync history yet.".yellow());
        return;
    }

    println!("{}", "Last sync events:".blue());
    for l in logs.iter() {
        let ts = chrono::DateTime::<chrono::Utc>::from(std::time::UNIX_EPOCH
            + std::time::Duration::from_secs(l.timestamp as u64))
            .to_rfc3339();
        let (icon, _colored_status) = match l.status.as_str() {
            "success" => ("✅".green(), l.status.green()),
            "error" => ("❌".red(), l.status.red()),
            _ => ("⚠️".yellow(), l.status.yellow()),
        };
        // Строка события
        println!(
            "#{} {} {} {} {}",
            l.id,
            ts,
            l.peer_url,
            l.mode,
            icon
        );
        if !l.details.is_empty() {
            println!("   {} {}", "details:".dimmed(), l.details);
        }
    }
}
