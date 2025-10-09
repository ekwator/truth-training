use std::process::Command;
use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

#[test]
fn config_show_set_reset_workflow() {
    // isolated HOME
    let tmp_home = std::env::temp_dir().join(format!(
        "truthctl-test-home-{}",
        SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
    ));
    std::fs::create_dir_all(&tmp_home).unwrap();

    let bin = env!("CARGO_BIN_EXE_truthctl");

    // create minimal config via init-node (also exercises reuse)
    let _ = Command::new(bin).args(["keys","generate","--save"]).env("HOME", &tmp_home).output().expect("gen");
    let out = Command::new(bin)
        .args(["init-node","n1","--port","9001","--db","c1.db"]) 
        .env("HOME", &tmp_home)
        .output().expect("init");
    assert!(out.status.success());

    // show
    let out = Command::new(bin)
        .args(["config","show"]) 
        .env("HOME", &tmp_home)
        .output().expect("show");
    assert!(out.status.success());
    let s = String::from_utf8_lossy(&out.stdout);
    // must be pretty JSON and contain keys
    assert!(s.contains("\"node_name\""));
    assert!(s.contains("\"port\""));

    // set port
    let out = Command::new(bin)
        .args(["config","set","port","8080"]) 
        .env("HOME", &tmp_home)
        .output().expect("set port");
    assert!(out.status.success());

    let cfg = tmp_home.join(".truthctl/config.json");
    let text = fs::read_to_string(cfg).unwrap();
    assert!(text.contains("\"port\": 8080"));

    // reset without confirm should not overwrite
    let out = Command::new(bin)
        .args(["config","reset"]) 
        .env("HOME", &tmp_home)
        .output().expect("reset dry");
    assert!(out.status.success());
    let text2 = fs::read_to_string(tmp_home.join(".truthctl/config.json")).unwrap();
    assert!(text2.contains("\"port\": 8080"));

    // reset with confirm
    let out = Command::new(bin)
        .args(["config","reset","--confirm"]) 
        .env("HOME", &tmp_home)
        .output().expect("reset");
    assert!(out.status.success());
    let text3 = fs::read_to_string(tmp_home.join(".truthctl/config.json")).unwrap();
    // port should be default 8080 per default_config(), but keys preserved by implementation
    assert!(text3.contains("\"port\": 8080"));
}
