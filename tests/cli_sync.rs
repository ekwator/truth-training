#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_sync_runs() {
    // Минимальная проверка запуска бинарника с --help (чтобы не требовать реальный сервер)
    let status = std::process::Command::new(env!("CARGO_BIN_EXE_truthctl"))
        .arg("--help")
        .status()
        .expect("failed to start truthctl");
    assert!(status.success());
}



