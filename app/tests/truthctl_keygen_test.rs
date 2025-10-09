use std::process::Command;
use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

#[test]
fn keygen_generates_and_saves() {
    // isolated HOME
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();

    // run generate
    let bin = env!("CARGO_BIN_EXE_truthctl");
    let out = Command::new(bin)
        .args(["keys", "generate", "--save"]) 
        .env("HOME", &tmp_home)
        .output().expect("run");
    assert!(out.status.success());

    // check keys.json exists
    let path = tmp_home.join(".truthctl/keys.json");
    assert!(path.exists());

    // naive check: contains hex strings length 64
    let data = fs::read_to_string(path).unwrap();
    assert!(data.contains("\"private_key_hex\":"));
}

