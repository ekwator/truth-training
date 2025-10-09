#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_help_works() {
    use std::process::Command;
    let bin = env!("CARGO_BIN_EXE_truthctl");
    let output = Command::new(bin)
        .arg("--help")
        .output()
        .expect("failed to run truthctl --help");
    assert!(output.status.success());
}

#[cfg(feature = "p2p-client-sync")]
#[tokio::test]
async fn truthctl_status_works() {
    use std::process::Command;
    let bin = env!("CARGO_BIN_EXE_truthctl");
    let output = Command::new(bin)
        .args(["status","--db","test.db"]) 
        .output()
        .expect("failed to run truthctl status");
    assert!(output.status.success());
}

