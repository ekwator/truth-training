use serde_json::Value;
use std::process::Command;
use tempfile::TempDir;

fn temp_home() -> TempDir {
    tempfile::tempdir().expect("temp dir")
}

#[test]
fn nodes_add_list_remove_flow() {
    let tmp = temp_home();
    let db_path = tmp.path().join("nodes.db");
    let db_str = db_path.to_string_lossy().to_string();
    let bin = env!("CARGO_BIN_EXE_truthctl");

    // add node
    let status = Command::new(bin)
        .env("HOME", tmp.path())
        .args([
            "nodes",
            "add",
            "--address",
            "http://127.0.0.1:8080",
            "--type",
            "lan",
            "--ttl",
            "120",
            "--db",
            &db_str,
        ])
        .status()
        .expect("run nodes add");
    assert!(status.success());

    // list as JSON
    let output = Command::new(bin)
        .env("HOME", tmp.path())
        .args(["nodes", "list", "--db", &db_str, "--format", "json"])
        .output()
        .expect("nodes list");
    assert!(output.status.success());
    let json: Value = serde_json::from_slice(&output.stdout).expect("json");
    assert!(
        json.as_array().map(|arr| arr.len()).unwrap_or_default() == 1,
        "expected one node in list"
    );

    // remove by address
    let status = Command::new(bin)
        .env("HOME", tmp.path())
        .args([
            "nodes",
            "remove",
            "--address",
            "http://127.0.0.1:8080",
            "--db",
            &db_str,
        ])
        .status()
        .expect("nodes remove");
    assert!(status.success());

    let output = Command::new(bin)
        .env("HOME", tmp.path())
        .args(["nodes", "list", "--db", &db_str, "--format", "json"])
        .output()
        .expect("nodes list empty");
    assert!(output.status.success());
    let json: Value = serde_json::from_slice(&output.stdout).expect("json");
    assert!(
        json.as_array().map(|arr| arr.is_empty()).unwrap_or(true),
        "all nodes should be removed"
    );
}
