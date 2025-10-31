
#[allow(unused_imports)]
use std::net::{SocketAddr, UdpSocket};
#[allow(unused_imports)]
use std::time::{Duration, Instant};
#[allow(unused_imports)]
use std::str::FromStr;

#[derive(Debug, Clone)]
pub struct WifiDirectConfig {
    pub enabled: bool,
    pub http_port: u16,
    pub broadcast_interval_ms: u64,
}

#[derive(Debug, Clone)]
pub struct NearbyPeer {
    pub addr: String,
    pub port: u16,
    pub pubkey: String,
}

#[derive(Debug, Clone)]
pub struct WifiDirectSync {
    pub config: WifiDirectConfig,
}

impl WifiDirectSync {
    pub fn new(http_port: u16) -> Self {
        Self {
            config: WifiDirectConfig { enabled: true, http_port, broadcast_interval_ms: 3_000 },
        }
    }
}

/// Start a simple UDP broadcast-based neighbor discovery loop and attempt HTTP sync.
/// Note: This is a best-effort LAN discovery approximation to Wi‑Fi Direct, using standard sockets.
#[cfg(any(test, feature = "p2p-client-sync"))]
pub async fn start_nearby_sync(
    conn: std::sync::Arc<tokio::sync::Mutex<rusqlite::Connection>>,
    identity: std::sync::Arc<crate::p2p::encryption::CryptoIdentity>,
    http_port: u16,
    broadcast_interval_ms: u64,
) {
    let pubkey = identity.public_key_hex();
    // Bind listener socket
    let listen_addr = SocketAddr::from_str(&format!("0.0.0.0:{}", 35878)).unwrap();
    let sock = UdpSocket::bind(listen_addr).expect("bind udp");
    sock.set_nonblocking(true).ok();
    sock.set_broadcast(true).ok();
    let sock_recv = sock.try_clone().expect("clone udp");

    // Spawn receiver loop
    let conn_arc = conn.clone();
    let id_arc = identity.clone();
    let pubkey_recv = pubkey.clone();
    tokio::spawn(async move {
        let mut buf = [0u8; 512];
        loop {
            match sock_recv.recv_from(&mut buf) {
                Ok((len, src)) => {
                    if len == 0 { continue; }
                    let msg = String::from_utf8_lossy(&buf[..len]).to_string();
                    if let Some(peer) = parse_beacon(&msg) {
                        // Ignore self
                        if peer.pubkey == pubkey_recv { continue; }
                        // Attempt bidirectional sync over HTTP
                        let url = format!("http://{}:{}", src.ip(), peer.port);
                        let url_cloned = url.clone();
                        let conn_clone = conn_arc.clone();
                        let id_clone = id_arc.clone();
                        tokio::spawn(async move {
                            if let Err(e) = crate::p2p::sync::bidirectional_sync_with_peer(&url_cloned, &id_clone, conn_clone.clone()).await {
                                log::warn!("nearby sync with {} failed: {}", url_cloned, e);
                            } else {
                                log::info!("nearby sync with {} succeeded", url_cloned);
                            }
                        });
                    }
                }
                Err(_e) => {
                    // no data; sleep briefly
                }
            }
            tokio::time::sleep(Duration::from_millis(200)).await;
        }
    });

    // Broadcast loop
    loop {
        let beacon = format!("TT_BEACON v1 pubkey={} port={}", pubkey, http_port);
        let _ = sock.send_to(beacon.as_bytes(), SocketAddr::from_str("255.255.255.255:35878").unwrap());
        tokio::time::sleep(Duration::from_millis(broadcast_interval_ms)).await;
    }
}

fn parse_beacon(s: &str) -> Option<NearbyPeer> {
    // Very simple format: TT_BEACON v1 pubkey=<hex> port=<u16>
    if !s.starts_with("TT_BEACON v1 ") { return None; }
    let mut pubkey = String::new();
    let mut port: u16 = 0;
    for part in s[13..].split_whitespace() { // skip prefix
        if let Some(rest) = part.strip_prefix("pubkey=") { pubkey = rest.to_string(); }
        if let Some(rest) = part.strip_prefix("port=") { port = rest.parse().unwrap_or(0); }
    }
    if pubkey.is_empty() || port == 0 { return None; }
    Some(NearbyPeer { addr: String::new(), port, pubkey })
}

// no helper functions


