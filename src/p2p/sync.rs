use std::sync::Arc;
use tokio::sync::Mutex;
use rusqlite::Connection;
use reqwest::Client;
use crate::p2p::encryption::CryptoIdentity;
use ed25519_dalek::Signature;

pub async fn sync_with_peer(
    peer_url: &str,
    conn: Arc<Mutex<Connection>>,
    identity: &CryptoIdentity,
) -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::new();

    // Формируем сообщение для подписи
    let message = format!("sync_request:{}", chrono::Utc::now().timestamp());
    let signature: Signature = identity.sign(message.as_bytes());

    // Отправляем запрос с подписью
    let resp = client
        .get(format!("{}/events", peer_url))
        .header("X-Public-Key", identity.public_key_hex())
        .header("X-Signature", hex::encode(signature.to_bytes()))
        .send()
        .await?;

    if resp.status().is_success() {
        let remote_events: serde_json::Value = resp.json().await?;
        println!("📡 Got {} events from {}", remote_events, peer_url);

        // TODO: сверить с локальной БД
    } else {
        println!("❌ Peer {} returned {}", peer_url, resp.status());
    }

    Ok(())
}
