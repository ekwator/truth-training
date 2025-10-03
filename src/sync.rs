use core_lib::models::{TruthEvent};
use reqwest::Client;
use std::time::Duration;
use anyhow::Result;

pub async fn push_to_peer(peer: &str, events: &[TruthEvent]) -> Result<()> {
    let client = Client::new();
    let url = format!("{}/sync", peer.trim_end_matches('/'));
    let payload = serde_json::json!({ "events": events });
    client.post(&url).json(&payload).timeout(Duration::from_secs(10)).send().await?.error_for_status()?;
    Ok(())
}

pub async fn pull_from_peer(peer: &str) -> Result<Vec<TruthEvent>> {
    let client = Client::new();
    let url = format!("{}/events", peer.trim_end_matches('/'));
    let res = client.get(&url).timeout(Duration::from_secs(10)).send().await?.error_for_status()?;
    let events = res.json::<Vec<TruthEvent>>().await?;
    Ok(events)
}

// TODO: periodic sync loop, conflict resolution, signatures
