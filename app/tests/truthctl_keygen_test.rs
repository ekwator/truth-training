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
    let out = Command::new("cargo")
        .args(["run", "--bin", "truthctl", "--", "keys", "generate", "--save"]) 
        .current_dir("..")
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

