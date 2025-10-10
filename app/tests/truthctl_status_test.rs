use std::process::Command;
use tempfile::tempdir;

#[test]
fn status_on_fresh_node_shows_no_history() {
    let tmp_home = tempdir().unwrap();
    let tmp_dir = tempdir().unwrap();
    let bin = env!("CARGO_BIN_EXE_truthctl");

    // init node: requires a key
    let _ = Command::new(bin).args(["keys","generate","--save"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("keys generate");
    let _ = Command::new(bin)
        .args(["init-node","testnode-status","--port","9092","--db","status.db","--auto-peer"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("init node");

    // run status (fresh DB, no syncs yet)
    let out = Command::new(bin)
        .args(["status","--db","status.db"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("status");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("testnode-status"), "stdout was: {}", stdout);
    assert!(stdout.contains("port 9092"), "stdout was: {}", stdout);
    assert!(stdout.contains("No sync history yet."), "stdout was: {}", stdout);
}

#[test]
fn status_after_logs_shows_events() {
    let tmp_home = tempdir().unwrap();
    let tmp_dir = tempdir().unwrap();
    let bin = env!("CARGO_BIN_EXE_truthctl");

    // init node
    let _ = Command::new(bin).args(["keys","generate","--save"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("keys generate");
    let _ = Command::new(bin)
        .args(["init-node","testnode2","--port","9093","--db","status2.db"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("init node");

    // add a peer and run dry-run sync-all to create log entries
    let _ = Command::new(bin)
        .args(["peers","add","http://127.0.0.1:9093","deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("peers add");
    let _ = Command::new(bin)
        .args(["peers","sync-all","--mode","full","--dry-run"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("sync-all dry-run");

    // run status
    let out = Command::new(bin)
        .args(["status","--db","status2.db"]) 
        .env("HOME", tmp_home.path())
        .current_dir(tmp_dir.path())
        .output().expect("status");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("testnode2"), "stdout was: {}", stdout);
    assert!(stdout.contains("port 9093"), "stdout was: {}", stdout);
    assert!(stdout.contains("Last sync events:") || stdout.contains("details:"), "stdout was: {}", stdout);
}
