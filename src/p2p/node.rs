use std::sync::Arc;
use tokio::time::{self, Duration};
use tokio::sync::Mutex;
use crate::p2p::encryption::CryptoIdentity;
use crate::p2p::sync::{SyncError, SyncData, compute_ratings_hash};
// already imported above
use log::{info, error};
use rusqlite::Connection;
use reqwest::Client;
use std::time::Duration as StdDuration;
use chrono::Utc;

#[derive(Clone)]
pub struct Node {
    pub peers: Vec<String>,
    pub conn_data: Arc<Mutex<Connection>>, // shared DB connection
    pub crypto: Arc<CryptoIdentity>,
}

impl Node {
    /// Теперь принимает готовую CryptoIdentity и пул БД
    pub fn new(peers: Vec<String>, conn_data: Arc<Mutex<Connection>>, crypto: Arc<CryptoIdentity>) -> Self {
        Self { peers, conn_data, crypto }
    }

    /// Запуск узла — периодическая синхронизация с другими
    pub async fn start(&self) {
        let mut interval = time::interval(Duration::from_secs(30)); // каждые 30 секунд

        loop {
            interval.tick().await;

            for peer in &self.peers {
                let peer = peer.clone();
                let identity = self.crypto.clone();

                let conn_data = self.conn_data.clone();
                tokio::spawn(async move {
                    // 1) Считаем данные из БД синхронно и освободим соединение до await
                    let (sync_data, sig_hex, pub_hex, ts, rhash) = {
                        let conn = conn_data.lock().await;
                        let events = core_lib::storage::load_truth_events(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let statements = core_lib::storage::load_statements(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let impacts = core_lib::storage::load_impacts(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let metrics = core_lib::storage::load_metrics(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let node_ratings = core_lib::storage::load_node_ratings(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let group_ratings = core_lib::storage::load_group_ratings(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let (events, statements, impacts, metrics, node_ratings, group_ratings) = match (events, statements, impacts, metrics, node_ratings, group_ratings) {
                            (Ok(a), Ok(b), Ok(c), Ok(d), Ok(e1), Ok(f)) => (a,b,c,d,e1,f),
                            (Err(e), ..) | (_, Err(e), ..) | (_, _, Err(e), ..) | (_, _, _, Err(e), ..) | (_, _, _, _, Err(e), ..) | (_, _, _, _, _, Err(e)) => {
                                error!("❌ DB read failed: {e}");
                                return;
                            }
                        };
                        let sync_data = SyncData {
                            events,
                            statements,
                            impacts,
                            metrics,
                            node_ratings: node_ratings.clone(),
                            group_ratings: group_ratings.clone(),
                            last_sync: Utc::now().timestamp(),
                        };
                        let ts = Utc::now().timestamp();
                        let rhash = match compute_ratings_hash(&sync_data.node_ratings, &sync_data.group_ratings) { Ok(h) => h, Err(e) => { error!("hash error: {e}"); return; } };
                        let msg = format!("sync_push:{}:{}", ts, rhash);
                        let sig = identity.sign(msg.as_bytes());
                        let sig_hex = hex::encode(sig.to_bytes());
                        let pub_hex = identity.public_key_hex();
                        (sync_data, sig_hex, pub_hex, ts, rhash)
                    };

                    // 2) Отправим на /sync
                    let client = match Client::builder().timeout(StdDuration::from_secs(30)).build() { Ok(c) => c, Err(e) => { error!("client build: {e}"); return; } };
                    let url = format!("{}/sync", peer.trim_end_matches('/'));
                    match client
                        .post(url)
                        .header("X-Public-Key", pub_hex)
                        .header("X-Signature", sig_hex)
                        .header("X-Timestamp", ts.to_string())
                        .header("X-Ratings-Hash", rhash)
                        .json(&sync_data)
                        .send()
                        .await {
                        Ok(resp) if resp.status().is_success() => info!("✅ Synced successfully with {peer}"),
                        Ok(resp) => error!("❌ Sync with {peer} failed: {}", resp.status()),
                        Err(e) => error!("❌ Sync with {peer} failed: {e}"),
                    }
                });
            }
        }
    }

    /// Широковещательная отправка локальных рейтингов всем известным пирам
    pub async fn broadcast_ratings(&self) -> Result<(), SyncError> {
        let conn = self.conn_data.lock().await;
        let node_ratings = core_lib::storage::load_node_ratings(&conn)
            .map_err(|e| SyncError::Other(e.to_string()))?;
        let group_ratings = core_lib::storage::load_group_ratings(&conn)
            .map_err(|e| SyncError::Other(e.to_string()))?;
        drop(conn);

        let payload = SyncData {
            events: Vec::new(),
            statements: Vec::new(),
            impacts: Vec::new(),
            metrics: Vec::new(),
            node_ratings: node_ratings.clone(),
            group_ratings: group_ratings.clone(),
            last_sync: Utc::now().timestamp(),
        };

        let client = Client::builder().timeout(StdDuration::from_secs(30)).build()?;
        let ts = Utc::now().timestamp();
        let rhash = compute_ratings_hash(&payload.node_ratings, &payload.group_ratings)
            .map_err(SyncError::from)?;
        let message = format!("incremental_sync:{}:{}", ts, rhash);
        let sig = self.crypto.sign(message.as_bytes());
        let signature_hex = hex::encode(sig.to_bytes());
        let public_key_hex = self.crypto.public_key_hex();

        // Отправка последовательно (избегаем зависимости от futures)
        for peer in &self.peers {
            let url = format!("{}/incremental_sync", peer.trim_end_matches('/'));
            let resp = client
                .post(url)
                .header("X-Public-Key", public_key_hex.clone())
                .header("X-Signature", signature_hex.clone())
                .header("X-Timestamp", ts.to_string())
                .header("X-Ratings-Hash", rhash.clone())
                .json(&payload)
                .send()
                .await?;
            if !resp.status().is_success() {
                return Err(SyncError::Other(format!("Peer {} responded {}", peer, resp.status())));
            }
        }
        Ok(())
    }
}