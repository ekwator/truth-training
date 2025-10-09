#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_sync_runs() {
    // Минимальная проверка запуска бинарника с --help (чтобы не требовать реальный сервер)
    let bin_path = match std::env::var("CARGO_BIN_EXE_truthctl") {
        Ok(p) => p,
        Err(_) => {
            // Если переменная окружения недоступна для этого теста (другой пакет) — пропускаем
            eprintln!("skip: CARGO_BIN_EXE_truthctl not set");
            return;
        }
    };
    let status = std::process::Command::new(bin_path)
        .arg("--help")
        .status()
        .expect("failed to start truthctl");
    assert!(status.success());
}



