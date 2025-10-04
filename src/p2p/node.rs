use std::sync::Arc;
use tokio::sync::Mutex;
use rusqlite::Connection;
use crate::p2p::sync::sync_with_peer;
use crate::p2p::encryption::CryptoIdentity;

pub struct Node {
    pub peers: Vec<String>,
    pub conn: Arc<Mutex<Connection>>,
    pub identity: CryptoIdentity,
}

impl Node {
    pub fn new(
        peers: Vec<String>,
        conn: Arc<Mutex<Connection>>,
        identity: CryptoIdentity,
    ) -> Self {
        Self { peers, conn, identity }
    }

    pub async fn start(&self) {
        println!(
            "🚀 Node started with {} peers, public key: {}",
            self.peers.len(),
            self.identity.public_key_hex()
        );

        {
            let db = self.conn.lock().await;
            println!("SQLite connected: autocommit={}", db.is_autocommit());
        }

        loop {
            for peer in &self.peers {
                println!("🔄 Syncing with peer: {}", peer);
                if let Err(err) = sync_with_peer(peer, self.conn.clone(), &self.identity).await {
                    eprintln!("⚠️ Sync with {} failed: {}", peer, err);
                }
            }

            tokio::time::sleep(std::time::Duration::from_secs(10)).await;
        }
    }
}
