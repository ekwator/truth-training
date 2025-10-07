use clap::{Parser, Subcommand};
use serde::{Deserialize, Serialize};
use std::fs;
use truth_core::p2p::encryption::CryptoIdentity;
use truth_core::p2p::sync::{push_local_data, pull_remote_data, reconcile};
use core_lib::storage::{open_db, init_db};

#[derive(Debug, Clone, Serialize, Deserialize)]
struct PeerEntry {
    addr: String,
    public_key_hex: String,
}

#[derive(Parser)]
#[command(name = "truthctl")]
#[command(about = "Peer management and sync CLI", long_about = None)]
struct Cli {
    /// Path to sqlite database file
    #[arg(long, default_value = "truth_db.sqlite")]
    db: String,

    /// Path to peers store file (JSON)
    #[arg(long, default_value = "peers.json")]
    peers: String,

    /// Verbose output
    #[arg(long, default_value_t = false)]
    verbose: bool,

    #[command(subcommand)]
    cmd: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Manage peers
    Peers { #[command(subcommand)] action: PeerCmd },
    /// Trigger sync with all peers
    Sync { #[arg(long, default_value_t = false)] pull_only: bool },
}

#[derive(Subcommand)]
enum PeerCmd {
    /// Add a peer
    Add { addr: String, pubkey: String },
    /// List peers
    List,
}

fn load_peers(path: &str) -> Vec<PeerEntry> {
    if !std::path::Path::new(path).exists() {
        return Vec::new();
    }
    let s = fs::read_to_string(path).unwrap_or_else(|_| "[]".to_string());
    serde_json::from_str(&s).unwrap_or_default()
}

fn save_peers(path: &str, peers: &[PeerEntry]) -> std::io::Result<()> {
    let json = serde_json::to_string_pretty(peers).unwrap_or_else(|_| "[]".to_string());
    fs::write(path, json)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    let conn = open_db(&cli.db)?;
    init_db(&conn)?;

    match cli.cmd {
        Commands::Peers { action } => match action {
            PeerCmd::Add { addr, pubkey } => {
                let mut peers = load_peers(&cli.peers);
                peers.push(PeerEntry { addr, public_key_hex: pubkey });
                save_peers(&cli.peers, &peers)?;
                println!("Peer added. Total peers: {}", peers.len());
            }
            PeerCmd::List => {
                let peers = load_peers(&cli.peers);
                if peers.is_empty() { println!("No peers configured"); }
                for p in peers {
                    println!("{} {}", p.addr, p.public_key_hex);
                }
            }
        },
        Commands::Sync { pull_only } => {
            let peers = load_peers(&cli.peers);
            if peers.is_empty() {
                println!("No peers configured");
                return Ok(());
            }
            let identity = CryptoIdentity::new();
            if cli.verbose { println!("Public key: {}", identity.public_key_hex()); }

            let mut total = 0u32;
            for p in peers {
                if cli.verbose { println!("Syncing with {}", p.addr); }
                if pull_only {
                    let remote = pull_remote_data(&p.addr, &identity).await?;
                    let res = reconcile(&conn, &remote)?;
                    total += res.events_added + res.statements_added + res.impacts_added;
                } else {
                    let res = push_local_data(&p.addr, &identity, &conn).await?;
                    total += res.events_added + res.statements_added + res.impacts_added;
                }
            }
            println!("Synced items: {}", total);
        }
    }

    Ok(())
}
