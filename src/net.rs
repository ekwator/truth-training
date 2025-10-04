use serde::{Deserialize, Serialize};
use std::{collections::HashSet, net::SocketAddr, sync::Arc, time::Duration};
use tokio::{
    net::UdpSocket,
    sync::RwLock,
    time::interval,
};

/// Множество пиров (HTTP-адресов) с потокобезопасным доступом
#[derive(Clone, Debug)]
pub struct PeerSet(pub Arc<RwLock<HashSet<String>>>);

/// Структура маяка для обмена информацией о присутствии
#[derive(Serialize, Deserialize, Debug)]
struct Beacon {
    app: String,
    ver: String,
    http: String, // пример: http://192.168.1.10:8080
}

const BEACON_PORT: u16 = 37020;
const APP_TAG: &str = "truth_training";
const APP_VER: &str = "0.3";

/// Фоновая задача: отправка маяков всем в локальной сети
pub async fn run_beacon_sender(http_addr: String) -> anyhow::Result<()> {
    // UDP-сокет для широковещательной отправки
    let sock = UdpSocket::bind(("0.0.0.0", 0)).await?;
    sock.set_broadcast(true)?;

    let mut tick = interval(Duration::from_secs(7));

    loop {
        tick.tick().await;

        let beacon = Beacon {
            app: APP_TAG.to_string(), // ✅ конвертация &str → String
            ver: APP_VER.to_string(), // ✅ конвертация &str → String
            http: http_addr.clone(),
        };

        // сериализация в JSON
        let buf = serde_json::to_vec(&beacon)?;

        // отправка на broadcast-адрес
        if let Err(e) = sock
            .send_to(&buf, SocketAddr::from(([255, 255, 255, 255], BEACON_PORT)))
            .await
        {
            eprintln!("[beacon sender] send error: {:?}", e);
        }
    }
}

/// Фоновая задача: прослушивание маяков от других узлов
pub async fn run_beacon_listener(peers: PeerSet) -> anyhow::Result<()> {
    let sock = UdpSocket::bind(("0.0.0.0", BEACON_PORT)).await?;
    let mut buf = vec![0u8; 2048];

    loop {
        let (n, _addr) = sock.recv_from(&mut buf).await?;

        // создаем строку с копированием, чтобы избежать borrow проблем
        if let Ok(data) = String::from_utf8(buf[..n].to_vec()) {
            if let Ok(beacon) = serde_json::from_str::<Beacon>(&data) {
                if beacon.app == APP_TAG {
                    let mut g = peers.0.write().await;
                    g.insert(beacon.http);
                }
            }
        }
    }
}

/// Логирование известных пиров каждые 30 секунд
pub async fn run_peer_logger(peers: PeerSet) -> anyhow::Result<()> {
    let mut tick = interval(Duration::from_secs(30));
    loop {
        tick.tick().await;
        let set = peers.0.read().await.clone();
        println!("[peers] {} known: {:?}", set.len(), set);
    }
}
