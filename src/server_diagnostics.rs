use serde::{Deserialize, Serialize};
use std::time::Duration as StdDuration;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerDiagnosticResult {
    pub check: String,
    pub status: String,
    pub message: String,
}

/// Простейшие проверки состояния запущенного узла
/// - Проверка HTTP API (`/health`)
/// - Проверка доступности БД (открытие и простое чтение)
/// - Проверка P2P слушателя (занят ли UDP порт 37020)
pub async fn run_diagnostics(base_url: &str, db_path: &str, p2p_enabled: bool) -> Vec<ServerDiagnosticResult> {
    let mut results: Vec<ServerDiagnosticResult> = Vec::new();

    // 1) API /health
    let client = match reqwest::Client::builder().timeout(StdDuration::from_secs(5)).build() {
        Ok(c) => c,
        Err(e) => {
            results.push(ServerDiagnosticResult { check: "API".into(), status: "❌ Error".into(), message: format!("client build: {}", e) });
            return results; // без HTTP клиента остальное можно продолжать, но вернёмся позже
        }
    };
    let url = format!("{}/health", base_url.trim_end_matches('/'));
    match client.get(&url).send().await {
        Ok(resp) if resp.status().is_success() => {
            results.push(ServerDiagnosticResult { check: "API".into(), status: "✅ OK".into(), message: url });
        }
        Ok(resp) => {
            results.push(ServerDiagnosticResult { check: "API".into(), status: "❌ Error".into(), message: format!("{} => {}", url, resp.status()) });
        }
        Err(e) => {
            results.push(ServerDiagnosticResult { check: "API".into(), status: "❌ Error".into(), message: format!("{} => {}", url, e) });
        }
    }

    // 2) База данных (открытие и простое чтение)
    match core_lib::storage::open_db(db_path) {
        Ok(conn) => {
            // Простая проверка чтения (метрики прогресса)
            match core_lib::storage::load_metrics(&conn) {
                Ok(_) => results.push(ServerDiagnosticResult { check: "Database".into(), status: "✅ OK".into(), message: db_path.to_string() }),
                Err(e) => results.push(ServerDiagnosticResult { check: "Database".into(), status: "❌ Error".into(), message: e.to_string() }),
            }
        }
        Err(e) => results.push(ServerDiagnosticResult { check: "Database".into(), status: "❌ Error".into(), message: e.to_string() }),
    }

    // 3) P2P listener (UDP 37020 из модуля net)
    if !p2p_enabled {
        results.push(ServerDiagnosticResult { check: "P2P".into(), status: "⚠️ Disabled".into(), message: String::new() });
    } else {
        // Если порт уже занят другим процессом (ожидаемо сервером) — считаем, что listener активен
        // Иначе — порт свободен и listener, вероятно, не запущен
        let bind_addr = ("0.0.0.0", 37020);
        match std::net::UdpSocket::bind(bind_addr) {
            Ok(sock) => {
                // порт свободен — слушателя нет; отпускаем сокет сразу
                drop(sock);
                results.push(ServerDiagnosticResult { check: "P2P".into(), status: "⚠️ Inactive".into(), message: "UDP 37020 is free".into() });
            }
            Err(e) => {
                if e.kind() == std::io::ErrorKind::AddrInUse {
                    results.push(ServerDiagnosticResult { check: "P2P".into(), status: "✅ Listening".into(), message: "UDP 37020 bound".into() });
                } else {
                    results.push(ServerDiagnosticResult { check: "P2P".into(), status: "❌ Error".into(), message: e.to_string() });
                }
            }
        }
    }

    results
}
