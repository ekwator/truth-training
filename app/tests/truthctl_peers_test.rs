use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};
use std::fs;

#[test]
fn peers_add_and_list() {
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();
    let bin = env!("CARGO_BIN_EXE_truthctl");

    // add
    let out = Command::new(bin)
        .args(["peers","add","http://127.0.0.1:8082","deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"]) 
        .env("HOME", &tmp_home)
        .output().expect("run add");
    assert!(out.status.success());

    // list
    let out = Command::new(bin)
        .args(["peers","list"]) 
        .env("HOME", &tmp_home)
        .output().expect("run list");
    assert!(out.status.success());

    // peers.json exists
    let peers = tmp_home.join(".truthctl/peers.json");
    assert!(peers.exists());
    let text = fs::read_to_string(peers).unwrap();
    assert!(text.contains("127.0.0.1:8082"));
}

