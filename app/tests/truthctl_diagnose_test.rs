use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};

#[test]
fn diagnose_prints_checks() {
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();
    let tmp_dir = std::env::temp_dir().join(format!(
        "truthctl-test-dir-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_dir).unwrap();

    let bin = env!("CARGO_BIN_EXE_truthctl");
    let out = Command::new(bin)
        .args(["diagnose"]) 
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output().expect("run diagnose");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    for label in ["Diagnostics", "Config", "Keys", "Peers", "Database", "P2P Feature"] {
        assert!(stdout.contains(label), "stdout missing {label}: {}", stdout);
    }
}

#[test]
fn diagnose_verbose_includes_json() {
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();
    let tmp_dir = std::env::temp_dir().join(format!(
        "truthctl-test-dir-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_dir).unwrap();

    let bin = env!("CARGO_BIN_EXE_truthctl");
    // Prepare minimal config via init-node
    let _ = Command::new(bin).args(["keys","generate","--save"]) 
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output().expect("keys generate");
    let _ = Command::new(bin)
        .args(["init-node","n1","--port","9001","--db","c1.db"]) 
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output().expect("init node");

    let out = Command::new(bin)
        .args(["diagnose","--verbose"]) 
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output().expect("run diagnose --verbose");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("Verbose JSON:"), "stdout was: {}", stdout);
    assert!(stdout.contains("\"config\""), "stdout was: {}", stdout);
}
