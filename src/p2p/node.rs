use crate::node::NodeConfig;
use crate::p2p::encryption::CryptoIdentity;
use crate::p2p::sync::{compute_ratings_hash, SyncData, SyncError};
use chrono::Utc;
use core_lib::{Node as DiscoveryNode, NodeFilter, NodePatch, NodeSource, NodeType};
use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use log::{error, info, warn};
use reqwest::Client;
use rusqlite::Connection;
use serde::Deserialize;
use socket2::{Domain, Protocol, Socket, Type};
use std::convert::TryInto;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::sync::Arc;
use std::time::Duration as StdDuration;
use tokio::net::UdpSocket;
use tokio::sync::Mutex;
use tokio::time::{self, Duration};

#[derive(Clone)]
pub struct Node {
    pub peers: Vec<String>,
    pub conn_data: Arc<Mutex<Connection>>, // shared DB connection
    pub crypto: Arc<CryptoIdentity>,
    pub config: NodeConfig,
}

impl Node {
    /// Теперь принимает готовую CryptoIdentity, пул БД и конфигурацию узла
    pub fn new(
        peers: Vec<String>,
        conn_data: Arc<Mutex<Connection>>,
        crypto: Arc<CryptoIdentity>,
        config: NodeConfig,
    ) -> Self {
        Self {
            peers,
            conn_data,
            crypto,
            config,
        }
    }

    /// Запуск узла — периодическая синхронизация с другими
    pub async fn start(&self) {
        // LAN discovery: advertiser + listener
        let advertiser_cfg = self.config.clone();
        let advertiser_id = self.crypto.clone();
        tokio::spawn(async move {
            if let Err(e) = run_lan_advertiser(advertiser_cfg, advertiser_id).await {
                warn!("LAN advertiser stopped with error: {e}");
            }
        });

        let listener_cfg = self.config.clone();
        let listener_conn = self.conn_data.clone();
        let listener_id = self.crypto.clone();
        tokio::spawn(async move {
            if let Err(e) = run_lan_listener(listener_cfg, listener_conn, listener_id).await {
                warn!("LAN listener stopped with error: {e}");
            }
        });

        let mut interval = time::interval(self.config.timing.lan_discovery_interval);

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
                        let node_metrics = core_lib::storage::load_all_node_metrics(&conn)
                            .map_err(|e| SyncError::Other(e.to_string()));
                        let (
                            events,
                            statements,
                            impacts,
                            metrics,
                            node_ratings,
                            group_ratings,
                            node_metrics,
                        ) = match (
                            events,
                            statements,
                            impacts,
                            metrics,
                            node_ratings,
                            group_ratings,
                            node_metrics,
                        ) {
                            (Ok(a), Ok(b), Ok(c), Ok(d), Ok(e1), Ok(f), Ok(g)) => {
                                (a, b, c, d, e1, f, g)
                            }
                            (Err(e), ..)
                            | (_, Err(e), ..)
                            | (_, _, Err(e), ..)
                            | (_, _, _, Err(e), ..)
                            | (_, _, _, _, Err(e), ..)
                            | (_, _, _, _, _, Err(e), _)
                            | (_, _, _, _, _, _, Err(e)) => {
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
                            node_metrics,
                            last_sync: Utc::now().timestamp(),
                        };
                        let ts = Utc::now().timestamp();
                        let rhash = match compute_ratings_hash(
                            &sync_data.node_ratings,
                            &sync_data.group_ratings,
                        ) {
                            Ok(h) => h,
                            Err(e) => {
                                error!("hash error: {e}");
                                return;
                            }
                        };
                        let msg = format!("sync_push:{}:{}", ts, rhash);
                        let sig = identity.sign(msg.as_bytes());
                        let sig_hex = hex::encode(sig.to_bytes());
                        let pub_hex = identity.public_key_hex();
                        (sync_data, sig_hex, pub_hex, ts, rhash)
                    };

                    // 2) Отправим на /sync
                    let client = match Client::builder()
                        .timeout(StdDuration::from_secs(30))
                        .build()
                    {
                        Ok(c) => c,
                        Err(e) => {
                            error!("client build: {e}");
                            return;
                        }
                    };
                    let url = format!("{}/sync", peer.trim_end_matches('/'));
                    match client
                        .post(url)
                        .header("X-Public-Key", pub_hex)
                        .header("X-Signature", sig_hex)
                        .header("X-Timestamp", ts.to_string())
                        .header("X-Ratings-Hash", rhash)
                        .json(&sync_data)
                        .send()
                        .await
                    {
                        Ok(resp) if resp.status().is_success() => {
                            info!("✅ Synced successfully with {peer}")
                        }
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
        let node_metrics = core_lib::storage::load_all_node_metrics(&conn)
            .map_err(|e| SyncError::Other(e.to_string()))?;
        drop(conn);
        let payload = SyncData {
            events: Vec::new(),
            statements: Vec::new(),
            impacts: Vec::new(),
            metrics: Vec::new(),
            node_ratings: node_ratings.clone(),
            group_ratings: group_ratings.clone(),
            node_metrics,
            last_sync: Utc::now().timestamp(),
        };

        let client = Client::builder()
            .timeout(StdDuration::from_secs(30))
            .build()?;
        let ts = Utc::now().timestamp();
        let rhash = compute_ratings_hash(&payload.node_ratings, &payload.group_ratings)
            .map_err(SyncError::from)?;
        let message = format!("incremental_sync:{}:{}", ts, rhash);
        let sig = self.crypto.sign(message.as_bytes());
        let signature_hex = hex::encode(sig.to_bytes());
        let public_key_hex = self.crypto.public_key_hex();

        // Отправка последовательно (избегаем зависимости от futures)
        // Приоритетная очередь: сортируем по propagation_priority (DESC); низким приоритетам даём задержку
        let mut peers_with_prio: Vec<(String, f32)> = Vec::new();
        for peer in &self.peers {
            let prio = {
                let conn = self.conn_data.lock().await;
                core_lib::storage::get_propagation_priority(&conn, &self.crypto.public_key_hex())
                    .unwrap_or(0.5)
            };
            peers_with_prio.push((peer.clone(), prio));
        }
        peers_with_prio.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        for (peer, prio) in peers_with_prio {
            let url = format!("{}/incremental_sync", peer.trim_end_matches('/'));
            if prio < 0.3 {
                tokio::time::sleep(StdDuration::from_millis(1200)).await;
            } else if prio < 0.6 {
                tokio::time::sleep(StdDuration::from_millis(600)).await;
            }
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
                return Err(SyncError::Other(format!(
                    "Peer {} responded {}",
                    peer,
                    resp.status()
                )));
            }
        }
        Ok(())
    }
}

/// Payload for LAN/Wi-Fi discovery announcements.
#[derive(Debug, serde::Serialize, serde::Deserialize)]
pub struct LanAnnouncement {
    node_id: String,
    address: String,
    node_type: NodeType,
    ttl: i64,
    timestamp: i64,
    signature: String,
}

#[derive(Debug, Deserialize)]
struct RegistryEnvelope {
    nodes: Vec<RegistryNode>,
}

#[derive(Debug, Deserialize)]
struct RegistryNode {
    address: String,
    #[serde(default)]
    node_type: Option<NodeType>,
    #[serde(default)]
    ttl: Option<i64>,
    #[serde(default)]
    node_id: Option<String>,
}

fn discovery_multicast_addr() -> SocketAddr {
    SocketAddr::new(IpAddr::V4(Ipv4Addr::new(239, 255, 0, 1)), 52_525)
}

fn announcement_payload(
    node_id: &str,
    address: &str,
    node_type: NodeType,
    ttl: i64,
    timestamp: i64,
) -> String {
    format!(
        "{node_id}|{address}|{}|{ttl}|{timestamp}",
        node_type.as_str()
    )
}

fn build_health_url(address: &str) -> String {
    let trimmed = address.trim_end_matches('/');
    if trimmed.ends_with("/api/v1") {
        format!("{trimmed}/nodes/health")
    } else if trimmed.ends_with("/api") {
        format!("{trimmed}/v1/nodes/health")
    } else {
        format!("{trimmed}/api/v1/nodes/health")
    }
}

fn verify_announcement_signature(ann: &LanAnnouncement) -> Result<(), SyncError> {
    let pk_bytes = hex::decode(&ann.node_id)
        .map_err(|e| SyncError::Other(format!("invalid node_id hex: {e}")))?;
    let pk_array: [u8; 32] = pk_bytes
        .as_slice()
        .try_into()
        .map_err(|_| SyncError::Other("invalid node_id length".into()))?;
    let verifying_key = VerifyingKey::from_bytes(&pk_array)
        .map_err(|e| SyncError::Other(format!("invalid node_id key: {e}")))?;

    let sig_bytes = hex::decode(&ann.signature)
        .map_err(|e| SyncError::Other(format!("invalid signature hex: {e}")))?;
    let sig_array: [u8; 64] = sig_bytes
        .as_slice()
        .try_into()
        .map_err(|_| SyncError::Other("invalid signature length".into()))?;
    let signature = Signature::from_bytes(&sig_array);

    let payload = announcement_payload(
        &ann.node_id,
        &ann.address,
        ann.node_type,
        ann.ttl,
        ann.timestamp,
    );
    verifying_key
        .verify(payload.as_bytes(), &signature)
        .map_err(|e| SyncError::Other(format!("announcement signature invalid: {e}")))
}

/// Periodic UDP multicast advertiser (every 10 seconds).
pub async fn run_lan_advertiser(
    config: NodeConfig,
    identity: Arc<CryptoIdentity>,
) -> Result<(), SyncError> {
    let addr = discovery_multicast_addr();
    let socket = Socket::new(Domain::IPV4, Type::DGRAM, Some(Protocol::UDP))
        .map_err(|e| SyncError::Other(e.to_string()))?;
    socket
        .set_reuse_address(true)
        .map_err(|e| SyncError::Other(e.to_string()))?;
    let bind_addr = SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 0);
    socket
        .bind(&bind_addr.into())
        .map_err(|e| SyncError::Other(e.to_string()))?;
    let std_sock: std::net::UdpSocket = socket.into();
    std_sock
        .set_nonblocking(true)
        .map_err(|e| SyncError::Other(e.to_string()))?;
    let sock = UdpSocket::from_std(std_sock).map_err(|e| SyncError::Other(e.to_string()))?;

    let mut interval = time::interval(config.timing.lan_discovery_interval);
    loop {
        interval.tick().await;
        let timestamp = Utc::now().timestamp();
        let node_type = NodeType::Lan;
        let ttl = node_type.min_ttl_secs();
        let address = config.canonical_address();
        let node_id = identity.public_key_hex();
        let signing_blob = announcement_payload(&node_id, &address, node_type, ttl, timestamp);
        let signature = identity.sign(signing_blob.as_bytes());
        let payload = LanAnnouncement {
            node_id,
            address,
            node_type,
            ttl,
            timestamp,
            signature: hex::encode(signature.to_bytes()),
        };
        match serde_json::to_vec(&payload) {
            Ok(bytes) => {
                if let Err(e) = sock.send_to(&bytes, &addr).await {
                    warn!("LAN advertiser send error: {e}");
                } else {
                    info!("📡 LAN announce: {} @ {}", payload.node_id, payload.address);
                }
            }
            Err(e) => {
                warn!("LAN advertiser serialize error: {e}");
            }
        }
    }
}

/// UDP multicast listener that upserts discovered nodes into SQLite.
pub async fn run_lan_listener(
    _config: NodeConfig,
    conn_data: Arc<Mutex<Connection>>,
    identity: Arc<CryptoIdentity>,
) -> Result<(), SyncError> {
    let group = Ipv4Addr::new(239, 255, 0, 1);
    let bind = SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 52_525);
    let socket = Socket::new(Domain::IPV4, Type::DGRAM, Some(Protocol::UDP))
        .map_err(|e| SyncError::Other(e.to_string()))?;
    socket
        .set_reuse_address(true)
        .map_err(|e| SyncError::Other(e.to_string()))?;
    socket
        .bind(&bind.into())
        .map_err(|e| SyncError::Other(e.to_string()))?;
    // Join group on all interfaces
    socket
        .join_multicast_v4(&group, &Ipv4Addr::UNSPECIFIED)
        .map_err(|e| SyncError::Other(e.to_string()))?;
    let std_sock: std::net::UdpSocket = socket.into();
    std_sock
        .set_nonblocking(true)
        .map_err(|e| SyncError::Other(e.to_string()))?;
    let sock = UdpSocket::from_std(std_sock).map_err(|e| SyncError::Other(e.to_string()))?;

    let self_node_id = identity.public_key_hex();
    let mut buf = vec![0u8; 2048];
    loop {
        let (len, _src) = match sock.recv_from(&mut buf).await {
            Ok(res) => res,
            Err(e) => {
                warn!("LAN listener recv error: {e}");
                continue;
            }
        };
        let slice = &buf[..len];
        let ann: LanAnnouncement = match serde_json::from_slice(slice) {
            Ok(a) => a,
            Err(e) => {
                warn!("LAN listener invalid JSON: {e}");
                continue;
            }
        };
        if ann.node_id == self_node_id {
            // Ignore self announcements
            continue;
        }
        if let Err(err) = verify_announcement_signature(&ann) {
            warn!(
                "LAN listener signature rejected from {}: {err}",
                ann.address
            );
            continue;
        }

        let now = Utc::now().timestamp();
        let ttl = ann.ttl.max(ann.node_type.min_ttl_secs());
        let conn = conn_data.lock().await;
        let node = DiscoveryNode {
            id: 0,
            address: ann.address.clone(),
            node_type: ann.node_type,
            reachable: true,
            last_seen: now,
            ttl,
            source: Some(NodeSource::LocalBroadcast),
            node_id: Some(ann.node_id.clone()),
            created_at: now,
            updated_at: now,
        };
        match core_lib::storage::upsert_node_by_address(&conn, &node) {
            Ok(_) => {
                info!("✅ Discovered LAN node {} at {}", ann.node_id, ann.address);
                info!(
                    "discovery.node.discovered source=local_broadcast node_id={} address={} node_type={}",
                    ann.node_id, ann.address, ann.node_type
                );
            }
            Err(e) => {
                warn!("Failed to upsert LAN node {}: {e}", ann.node_id);
                warn!(
                    "discovery.node.error node_id={} address={} error={}",
                    ann.node_id, ann.address, e
                );
            }
        }
    }
}

pub async fn poll_global_registries(
    config: &NodeConfig,
    conn_data: Arc<Mutex<Connection>>,
) -> Result<usize, SyncError> {
    if config.global_registry_urls.is_empty() {
        return Ok(0);
    }
    let client = Client::builder()
        .timeout(StdDuration::from_secs(5))
        .build()
        .map_err(|e| SyncError::Other(format!("http client build failed: {e}")))?;
    let mut total = 0usize;
    let start = std::time::Instant::now();
    for url in &config.global_registry_urls {
        let url_start = std::time::Instant::now();
        match fetch_registry_nodes(&client, url).await {
            Ok(nodes) => {
                let nodes_count = nodes.len();
                let mut added = 0usize;
                for record in nodes {
                    match registry_record_to_node(&record) {
                        Ok(node) => {
                            let conn = conn_data.lock().await;
                            match core_lib::storage::upsert_node_by_address(&conn, &node) {
                                Ok(_) => {
                                    total += 1;
                                    added += 1;
                                    info!(
                                        "discovery.node.discovered source=global_registry node_id={} address={} node_type={}",
                                        node.node_id.as_deref().unwrap_or("unknown"),
                                        node.address,
                                        node.node_type
                                    );
                                }
                                Err(e) => {
                                    warn!(
                                        "Failed to store registry node {} from {}: {e}",
                                        node.address, url
                                    );
                                    warn!(
                                        "discovery.node.error source=global_registry address={} error={}",
                                        node.address, e
                                    );
                                }
                            }
                        }
                        Err(e) => warn!("Invalid registry node from {url}: {e}"),
                    }
                }
                let duration_ms = url_start.elapsed().as_millis() as u64;
                info!(
                    "discovery.registry.poll url={} found={} added={} duration_ms={}",
                    url, nodes_count, added, duration_ms
                );
            }
            Err(e) => {
                warn!("Registry poll for {url} failed: {e}");
                warn!(
                    "discovery.registry.error url={} error={}",
                    url, e
                );
            }
        }
    }
    let duration_ms = start.elapsed().as_millis() as u64;
    info!(
        "discovery.registry.poll.completed total_added={} duration_ms={}",
        total, duration_ms
    );
    Ok(total)
}

pub async fn run_http_reachability_checks(
    conn_data: Arc<Mutex<Connection>>,
    timeout: StdDuration,
    retries: u8,
) -> Result<usize, SyncError> {
    let nodes = {
        let conn = conn_data.lock().await;
        core_lib::storage::list_nodes(&conn, &NodeFilter::default())
            .map_err(|e| SyncError::Other(e.to_string()))?
    };
    if nodes.is_empty() {
        return Ok(0);
    }
    let client = Client::builder()
        .timeout(timeout)
        .build()
        .map_err(|e| SyncError::Other(format!("http client build failed: {e}")))?;

    let start = std::time::Instant::now();
    let mut processed = 0usize;
    let mut successful = 0usize;
    let mut failed = 0usize;
    
    for node in nodes {
        let url = build_health_url(&node.address);
        let check_start = std::time::Instant::now();
        let reachable = http_ping_with_retries(&client, &url, retries).await;
        let duration_ms = check_start.elapsed().as_millis() as u64;
        
        if reachable {
            successful += 1;
        } else {
            failed += 1;
        }
        
        info!(
            "discovery.reachability.check address={} status={} duration_ms={}",
            node.address,
            if reachable { "success" } else { "failure" },
            duration_ms
        );
        
        let mut patch = NodePatch::default();
        patch.reachable = Some(reachable);
        if reachable {
            patch.last_seen = Some(Utc::now().timestamp());
        }
        let conn = conn_data.lock().await;
        if let Err(e) = core_lib::storage::update_node(&conn, node.id, &patch) {
            warn!(
                "Failed to update node {} reachability (reachable={}): {e}",
                node.address, reachable
            );
        }
        processed += 1;
    }
    
    let duration_ms = start.elapsed().as_millis() as u64;
    info!(
        "discovery.reachability.batch total={} successful={} failed={} duration_ms={}",
        processed, successful, failed, duration_ms
    );

    Ok(processed)
}

async fn fetch_registry_nodes(client: &Client, url: &str) -> Result<Vec<RegistryNode>, SyncError> {
    let mut attempt = 0u8;
    loop {
        attempt = attempt.saturating_add(1);
        match client.get(url).send().await {
            Ok(resp) if resp.status().is_success() => {
                let body = resp.text().await?;
                return parse_registry_payload(&body).map_err(|e| {
                    SyncError::Other(format!("registry payload parse error for {url}: {e}"))
                });
            }
            Ok(resp) => {
                warn!(
                    "Registry {url} responded with status {} (attempt {attempt})",
                    resp.status()
                );
            }
            Err(e) => warn!("Registry {url} request failed (attempt {attempt}): {e}"),
        }
        if attempt >= 3 {
            break;
        }
        time::sleep(Duration::from_millis(200 * attempt as u64)).await;
    }
    Err(SyncError::Other(format!(
        "registry {url} failed after {attempt} attempts"
    )))
}

fn parse_registry_payload(body: &str) -> Result<Vec<RegistryNode>, serde_json::Error> {
    if let Ok(envelope) = serde_json::from_str::<RegistryEnvelope>(body) {
        Ok(envelope.nodes)
    } else {
        serde_json::from_str::<Vec<RegistryNode>>(body)
    }
}

fn registry_record_to_node(record: &RegistryNode) -> Result<DiscoveryNode, SyncError> {
    if record.address.trim().is_empty() {
        return Err(SyncError::Other("registry node missing address".into()));
    }
    let node_type = record.node_type.unwrap_or(NodeType::Global);
    let now = Utc::now().timestamp();
    let ttl = record
        .ttl
        .map(|value| value.max(node_type.min_ttl_secs()))
        .unwrap_or_else(|| node_type.min_ttl_secs());
    Ok(DiscoveryNode {
        id: 0,
        address: record.address.trim().to_string(),
        node_type,
        reachable: false,
        last_seen: now,
        ttl,
        source: Some(NodeSource::GlobalRegistry),
        node_id: record.node_id.clone(),
        created_at: now,
        updated_at: now,
    })
}

async fn http_ping_with_retries(client: &Client, url: &str, retries: u8) -> bool {
    let mut attempt = 0u8;
    loop {
        match client.get(url).send().await {
            Ok(resp) if resp.status().is_success() => return true,
            Ok(resp) => warn!(
                "Health endpoint {} returned {} (attempt {})",
                url,
                resp.status(),
                attempt + 1
            ),
            Err(e) => warn!(
                "Health request {} failed (attempt {}): {e}",
                url,
                attempt + 1
            ),
        }
        attempt = attempt.saturating_add(1);
        if attempt > retries {
            break;
        }
        time::sleep(Duration::from_millis(250 * attempt as u64)).await;
    }
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    #[cfg(feature = "desktop")]
    fn lan_announcement_roundtrip() {
        let ann = LanAnnouncement {
            node_id: "abc".into(),
            address: "http://127.0.0.1:8080/api".into(),
            node_type: NodeType::Lan,
            ttl: NodeType::Lan.min_ttl_secs(),
            timestamp: 1_700_000_000,
            signature: "00".repeat(64),
        };
        let bytes = serde_json::to_vec(&ann).expect("serialize");
        let decoded: LanAnnouncement = serde_json::from_slice(&bytes).expect("deserialize");
        assert_eq!(decoded.node_id, ann.node_id);
        assert_eq!(decoded.address, ann.address);
    }

    #[test]
    fn canonical_address_uses_public_over_bind() {
        let cfg = NodeConfig {
            bind_host: "0.0.0.0".into(),
            bind_port: 8080,
            public_host: Some("example.com".into()),
            public_port: Some(80),
            timing: core_lib::DEFAULT_DISCOVERY_TIMING,
            global_registry_urls: vec![],
        };
        assert_eq!(
            cfg.canonical_address(),
            "http://example.com:80/api/v1".to_string()
        );
    }
}
