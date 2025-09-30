use serde::{Deserialize, Serialize};
use std::{collections::HashSet, net::SocketAddr, sync::Arc, time::Duration};
use tokio::{
    net::UdpSocket,
    sync::RwLock,
    time::interval,
};

#[derive(Clone, Debug)]
pub struct PeerSet(pub Arc<RwLock<HashSet<String>>>);

#[derive(Serialize, Deserialize, Debug)]
struct Beacon {
    app: String,
    ver: String,
    http: String,
}

const BEACON_PORT: u16 = 37020;
const APP_TAG: &str = "truth_training";
const APP_VER: &str = "0.1";

pub async fn run_beacon_sender(http_addr: String) -> anyhow::Result<()> {
    let sock = UdpSocket::bind(("0.0.0.0", 0)).await?;
    sock.set_broadcast(true)?;
    let mut tick = interval(Duration::from_secs(7));
    loop {
        tick.tick().await;
        let b = Beacon {
            app: APP_TAG.to_string(),
            ver: APP_VER.to_string(),
            http: http_addr.clone(),
        };
        let buf = serde_json::to_vec(&b)?;
        let _ = sock.send_to(&buf, SocketAddr::from(([255,255,255,255], BEACON_PORT))).await;
    }
}

pub async fn run_beacon_listener(peers: PeerSet) -> anyhow::Result<()> {
    let sock = UdpSocket::bind(("0.0.0.0", BEACON_PORT)).await?;
    let mut buf = vec![0u8; 2048];
    loop {
        let (n, _addr) = sock.recv_from(&mut buf).await?;
        if let Ok(data) = String::from_utf8(buf[..n].to_vec()) {
            if let Ok(b) = serde_json::from_str::<Beacon>(&data) {
                if b.app == APP_TAG {
                    let mut g = peers.0.write().await;
                    g.insert(b.http);
                }
            }
        }
    }
}

pub async fn run_peer_logger(peers: PeerSet) -> anyhow::Result<()> {
    let mut tick = interval(Duration::from_secs(30));
    loop {
        tick.tick().await;
        let set = peers.0.read().await.clone();
        println!("[peers] {} known: {:?}", set.len(), set);
    }
}
