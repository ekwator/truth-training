#[cfg(feature = "p2p-client-sync")]
#[test]
fn logs_show_and_clear_persist() {
    use std::process::Command;
    use std::time::{SystemTime, UNIX_EPOCH};

    // isolated HOME and working directory
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

    // ensure a key exists by importing a dummy one (32-byte hex)
    let dummy_priv = "11".repeat(32);
    let dummy_pub = "22".repeat(32);
    let out = Command::new(bin)
        .args(["keys", "import", &dummy_priv, &dummy_pub])
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("keys import");
    assert!(out.status.success());

    // add a peer with different public key so it's not skipped as self
    let other_pub = "33".repeat(32);
    let out = Command::new(bin)
        .args(["peers", "add", "http://127.0.0.1:18080", &other_pub])
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("peers add");
    assert!(out.status.success());

    // run sync-all dry-run to create a log entry
    let out = Command::new(bin)
        .args(["peers", "sync-all", "--mode", "full", "--dry-run"])
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("peers sync-all");
    assert!(out.status.success());

    // show logs (db at tmp_dir/truth.db by default)
    let out = Command::new(bin)
        .args(["logs", "show", "--limit", "10"]) // default db path truth.db
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("logs show");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("Sync logs:"), "stdout was: {}", stdout);

    // clear logs
    let out = Command::new(bin)
        .args(["logs", "clear"]) // default db path truth.db
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("logs clear");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("Logs cleared"), "stdout was: {}", stdout);

    // show again must be empty
    let out = Command::new(bin)
        .args(["logs", "show", "--limit", "10"]) 
        .env("HOME", &tmp_home)
        .current_dir(&tmp_dir)
        .output()
        .expect("logs show empty");
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    assert!(stdout.contains("No sync logs"), "stdout was: {}", stdout);
}
