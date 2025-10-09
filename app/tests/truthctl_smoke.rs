#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_help_works() {
    use std::process::Command;
    
    let output = Command::new("cargo")
        .args(&["run", "--bin", "truthctl", "--", "--help"])
        .current_dir("..")
        .output()
        .expect("failed to run truthctl --help");
    
    assert!(output.status.success());
    let stdout = String::from_utf8_lossy(&output.stdout);
    assert!(stdout.contains("truthctl"));
    assert!(stdout.contains("CLI для P2P синхронизации"));
}

#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_status_works() {
    use std::process::Command;
    
    let output = Command::new("cargo")
        .args(&["run", "--bin", "truthctl", "--", "status", "--db", "test.db"])
        .current_dir("..")
        .output()
        .expect("failed to run truthctl status");
    
    // Should not crash even if DB doesn't exist
    assert!(output.status.success());
}

