use std::process::Command;
use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

#[test]
fn init_node_writes_config_and_peers() {
    // isolated HOME
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();

    // ensure a key exists
    let _ = Command::new("cargo").args(["run","--bin","truthctl","--","keys","generate","--save"]) 
        .current_dir("..")
        .env("HOME", &tmp_home)
        .output().expect("run");

    let out = Command::new("cargo")
        .args(["run","--bin","truthctl","--","init-node","testnode","--port","9091","--db","mytest.db","--auto-peer"]) 
        .current_dir("..")
        .env("HOME", &tmp_home)
        .output().expect("run");
    assert!(out.status.success());

    let cfg = tmp_home.join(".truthctl/config.json");
    let peers = tmp_home.join(".truthctl/peers.json");
    assert!(cfg.exists());
    assert!(peers.exists());

    let cfg_text = fs::read_to_string(cfg).unwrap();
    assert!(cfg_text.contains("\"node_name\": \"testnode\""));
}

