use serde::Serialize;
use reqwest::Client;
use crate::db::{get_all, upsert_statement};
use crate::models::Statement;
use crate::net::PeerSet;
use rusqlite::Connection;
use std::sync::Arc;
use tokio::{sync::Mutex, time::{interval, Duration}};

#[derive(Serialize)]
struct SyncRequest {
    node_id: String,
    data: Vec<String>,
}

#[derive(Clone)]
pub struct SyncCtx {
    pub http_addr: String,
    pub peers: PeerSet,
    pub client: Client,
    pub conn: Arc<Mutex<Connection>>,
}

pub async fn send_sync(target: &str, node_id: &str, data: Vec<String>) -> anyhow::Result<()> {
    let client = Client::new();
    let url = format!("{}/sync", target);

    let payload = SyncRequest {
        node_id: node_id.to_string(),
        data,
    };

    let res = client.post(&url)
        .json(&payload)
        .send()
        .await?;

    println!("Sync response: {:?}", res.text().await?);
    Ok(())
}

pub async fn run_periodic_sync(ctx: SyncCtx) -> anyhow::Result<()> {
    let mut tick = interval(Duration::from_secs(9));
    loop {
        tick.tick().await;
        let peers = ctx.peers.0.read().await.clone();

        // соберём наши данные один раз на цикл
        let ours = {
            let c = ctx.conn.lock().await;
            get_all(&c).unwrap_or_default()
        };

        for peer in peers {
            // не синкаться сам с собой
            if peer == ctx.http_addr {
                continue;
            }

            // PULL: забрать данные соседа
            if let Ok(list) = pull_from_peer(&ctx.client, &peer).await {
                let c = ctx.conn.lock().await;
                for s in list {
                    let _ = upsert_statement(&c, &s);
                }
            }

            // PUSH: отправить соседу наши данные
            let _ = push_to_peer(&ctx.client, &peer, &ours).await;
        }
    }
}

async fn pull_from_peer(client: &Client, peer: &str) -> anyhow::Result<Vec<Statement>> {
    let url = format!("{peer}/data");
    let r = client.get(url).send().await?.error_for_status()?;
    let items = r.json::<Vec<Statement>>().await?;
    Ok(items)
}

async fn push_to_peer(client: &Client, peer: &str, ours: &[Statement]) -> anyhow::Result<()> {
    let url = format!("{peer}/sync");
    let payload = serde_json::json!({ "statements": ours });
    let _ = client.post(url).json(&payload).send().await?.error_for_status()?;
    Ok(())
}