use reqwest::Client;
use std::time::Duration;
use crate::p2p::encryption::CryptoIdentity;

/// Асинхронная синхронизация с peer’ом
pub async fn sync_with_peer(peer_url: &str, identity: &CryptoIdentity) -> anyhow::Result<()> {
    // Создаём асинхронный HTTP клиент
    let client = Client::builder()
        .timeout(Duration::from_secs(10))
        .build()?;

    // Формируем сообщение для подписи
    let message = format!("sync_request:{}", chrono::Utc::now().timestamp());

    // Подписываем сообщение приватным ключом
    let signature = identity.sign(message.as_bytes());
    let public_key_hex = identity.public_key_hex();
    let signature_hex = hex::encode(signature.to_bytes());

    // Выполняем асинхронный GET-запрос
    let response = client
        .get(format!("{peer_url}/events"))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .send()
        .await?;

    // Проверяем HTTP-код ответа
    if !response.status().is_success() {
        anyhow::bail!("Peer returned non-success status: {}", response.status());
    }

    // Асинхронно читаем тело ответа
    let body = response.text().await?;

    // Здесь можно будет обработать полученные данные
    log::info!("Received from {peer_url}: {body}");

    Ok(())
}
