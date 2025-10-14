use base64::{engine::general_purpose, Engine as _};
use ed25519_dalek::{Signer, SigningKey};
use serde_json::json;

use truth_core::android::verify_json::verify_json_message;

#[test]
fn verify_android_envelope_ok_and_tamper() {
    // Generate ed25519 keypair
    let mut rng = rand::rngs::OsRng;
    let sk = SigningKey::generate(&mut rng);
    let vk = sk.verifying_key();
    let pub_b64 = general_purpose::STANDARD.encode(vk.to_bytes());

    // Payload to sign
    let payload = json!({"action":"ping","n":1});
    let data = serde_json::to_vec(&payload).unwrap();
    let sig = sk.sign(&data);
    let sig_b64 = general_purpose::STANDARD.encode(sig.to_bytes());

    let envelope = json!({
        "node_id": "device-1",
        "payload": payload,
        "signature": sig_b64,
        "public_key": pub_b64
    });
    let env_str = serde_json::to_string(&envelope).unwrap();
    let res = verify_json_message(&env_str).unwrap();
    assert!(res.verified);
    assert_eq!(res.payload["n"], 1);

    // Tamper the payload value but keep same signature -> must fail
    let tampered_env = json!({
        "node_id": "device-1",
        "payload": {"action":"ping","n":2},
        "signature": general_purpose::STANDARD.encode(sig.to_bytes()),
        "public_key": general_purpose::STANDARD.encode(vk.to_bytes()),
    });
    let tampered_str = serde_json::to_string(&tampered_env).unwrap();
    let err = verify_json_message(&tampered_str).err();
    assert!(err.is_some());
}


