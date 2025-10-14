use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use serde_json::{Value, json};

/// Минимальная версия для Android без проблемных зависимостей
#[no_mangle]
pub extern "system" fn Java_com_truth_training_client_TruthCore_processJsonRequest(
    mut env: JNIEnv,
    _class: JClass,
    request: JString,
) -> jstring {
    let input: String = match env.get_string(&request) {
        Ok(jstr) => jstr.into(),
        Err(_) => return env.new_string(r#"{"error":"invalid_input"}"#).unwrap().into_raw(),
    };

    // Простая обработка JSON без верификации подписи
    let response = match serde_json::from_str::<Value>(&input) {
        Ok(parsed) => {
            match parsed["action"].as_str() {
                Some("ping") => json!({
                    "status": "ok",
                    "message": "pong",
                    "timestamp": chrono::Utc::now().timestamp()
                }),
                Some("info") => json!({
                    "status": "ok",
                    "version": "0.3.0",
                    "platform": "android"
                }),
                _ => json!({
                    "status": "error",
                    "reason": "unknown_action"
                }),
            }
        }
        Err(_) => json!({
            "status": "error",
            "reason": "invalid_json"
        }),
    };

    match env.new_string(response.to_string()) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => env.new_string(r#"{"error":"response_creation_failed"}"#).unwrap().into_raw(),
    }
}
