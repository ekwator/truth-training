use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::time::{self, Duration};
use crate::p2p::encryption::CryptoIdentity;
use crate::p2p::sync::sync_with_peer;
use rusqlite::Connection;
use log::{info, error};

pub struct Node {
    pub peers: Vec<String>,
    pub db: Arc<Mutex<Connection>>,
    pub identity: Arc<CryptoIdentity>,
}

impl Node {
    /// Теперь принимает готовую CryptoIdentity
    pub fn new(peers: Vec<String>, db: Arc<Mutex<Connection>>, identity: Arc<CryptoIdentity>) -> Self {
        Self { peers, db, identity }
    }

    /// Запуск узла — периодическая синхронизация с другими
    pub async fn start(&self) {
        let mut interval = time::interval(Duration::from_secs(30)); // каждые 30 секунд

        loop {
            interval.tick().await;

            for peer in &self.peers {
                let peer = peer.clone();
                let identity = self.identity.clone();

                tokio::spawn(async move {
                    match sync_with_peer(&peer, &identity).await {
                        Ok(_) => info!("✅ Synced successfully with {peer}"),
                        Err(e) => error!("❌ Sync with {peer} failed: {e}"),
                    }
                });
            }
        }
    }
}