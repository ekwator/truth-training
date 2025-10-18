# Desktop Integration Guide (Truth Core v0.4.0)

## Overview

This guide provides comprehensive instructions for integrating Truth Core with desktop applications (Linux, Windows, macOS) using the full feature set including HTTP server, CLI tools, and complete P2P networking.

## Prerequisites

- Rust toolchain (≥ 1.75)
- Platform-specific build tools:
  - **Linux**: `build-essential`, `libsqlite3-dev`
  - **Windows**: Visual Studio Build Tools
  - **macOS**: Xcode Command Line Tools

## Build Process

### 1. Build Desktop Application
```bash
# Build with full desktop features
cargo build --release --features desktop

# Output: target/release/truth_core (executable)
#         target/release/libtruth_core.so (shared library)
```

### 2. Build Platform-Specific
```bash
# Linux
cargo build --release --target x86_64-unknown-linux-gnu --features desktop

# Windows
cargo build --release --target x86_64-pc-windows-gnu --features desktop

# macOS
cargo build --release --target x86_64-apple-darwin --features desktop
```

## HTTP API Integration

### 1. Start HTTP Server
```rust
use truth_core::desktop::server;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Start HTTP server on port 8080
    server::start_server(8080).await?;
    Ok(())
}
```

### 2. API Endpoints

**Node Information:**
```bash
curl http://localhost:8080/api/v1/info
```

Response:
```json
{
    "name": "truth-core",
    "version": "0.4.0",
    "uptime_sec": 12345,
    "started_at": 1640995200,
    "features": ["p2p-client-sync", "jwt"],
    "peer_count": 3
}
```

**Database Statistics:**
```bash
curl http://localhost:8080/api/v1/stats
```

Response:
```json
{
    "events": 120,
    "statements": 340,
    "impacts": 21,
    "node_ratings": 8,
    "group_ratings": 2,
    "avg_trust_score": 0.62,
    "avg_propagation_priority": 0.71,
    "avg_relay_success_rate": 0.84,
    "active_nodes": 7
}
```

**Network Graph:**
```bash
curl http://localhost:8080/graph/json?min_priority=0.5&limit=10
```

Response:
```json
{
    "nodes": [
        {
            "id": "nodeA",
            "score": 0.78,
            "propagation_priority": 0.82,
            "last_seen": 1640995500,
            "relay_success_rate": 0.93
        }
    ],
    "links": [
        {
            "source": "nodeA",
            "target": "nodeB",
            "weight": 0.7,
            "latency_ms": 42
        }
    ]
}
```

## CLI Integration

### 1. truthctl Commands

**Node Status:**
```bash
truthctl status --db truth.db
```

Output:
```
Node: mynode (port 8080)
Database: truth.db
Peers: http://127.0.0.1:8080, http://10.0.0.2:8081 (+5 more)
Last sync events:
#42 2025-01-18T10:00:00Z http://127.0.0.1:8080 full ✅
   details: E10 S7 I3 C0
#41 2025-01-18T09:55:00Z http://10.0.0.2:8081 incremental ❌
   details: timeout
```

**Peer Management:**
```bash
# List peers
truthctl peers list

# Add peer
truthctl peers add http://192.168.1.100:8080 <public_key_hex>

# Sync with all peers
truthctl peers sync-all --mode full
```

**Key Management:**
```bash
# Generate new keypair
truthctl keys generate --save

# List keys
truthctl keys list

# Import keypair
truthctl keys import <private_hex> <public_hex>
```

### 2. Programmatic CLI Usage
```rust
use truth_core::desktop::cli;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize CLI
    let cli = cli::TruthCli::new()?;
    
    // Run status command
    cli.run_status()?;
    
    // Sync with specific peer
    cli.run_sync("http://192.168.1.100:8080")?;
    
    Ok(())
}
```

## P2P Network Integration

### 1. Start P2P Node
```rust
use truth_core::p2p::Node;
use truth_core::p2p::encryption::CryptoIdentity;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create crypto identity
    let identity = CryptoIdentity::new()?;
    
    // Create P2P node
    let mut node = Node::new(
        "mynode".to_string(),
        "truth.db".to_string(),
        identity,
    )?;
    
    // Start P2P operations
    node.start().await?;
    
    Ok(())
}
```

### 2. Peer Discovery
```rust
use truth_core::net;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Start UDP beacon sender
    let beacon_sender = net::run_beacon_sender("mynode".to_string()).await?;
    
    // Start UDP beacon listener
    let beacon_listener = net::run_beacon_listener().await?;
    
    // Wait for discovery
    tokio::select! {
        _ = beacon_sender => {},
        _ = beacon_listener => {},
    }
    
    Ok(())
}
```

### 3. Synchronization
```rust
use truth_core::p2p::sync;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let peer_url = "http://192.168.1.100:8080";
    
    // Full synchronization
    let result = sync::sync_with_peer(peer_url).await?;
    println!("Sync result: {:?}", result);
    
    // Incremental synchronization
    let result = sync::incremental_sync_with_peer(peer_url).await?;
    println!("Incremental sync result: {:?}", result);
    
    Ok(())
}
```

## Database Integration

### 1. Direct Database Access
```rust
use truth_core::core::storage;
use truth_core::core::models::TruthEvent;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Open database
    let db = storage::open_database("truth.db")?;
    
    // Add event
    let event = TruthEvent {
        id: None,
        description: "Test event".to_string(),
        context_id: 1,
        vector: true,
        timestamp_start: chrono::Utc::now(),
        timestamp_end: None,
        code: 0,
        detected: false,
        corrected: false,
        collective_score: None,
    };
    
    storage::add_truth_event(&db, &event)?;
    
    // Get events
    let events = storage::get_truth_events(&db)?;
    println!("Events: {:?}", events);
    
    Ok(())
}
```

### 2. Expert System Integration
```rust
use truth_core::core::expert;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Assess event
    let event_id = 1;
    let answers = serde_json::json!({
        "src_independent": "yes",
        "alt_hypothesis": "yes",
        "incentives": "no",
        "reproducible": "yes",
        "logs_evidence": "yes",
        "belief_pressure": "no",
        "time_distance": "yes"
    });
    
    let assessment = expert::assess_event(event_id, &answers)?;
    println!("Assessment: {:?}", assessment);
    
    Ok(())
}
```

## Web UI Integration

### 1. Swagger UI
Access the interactive API documentation at:
```
http://localhost:8080/api/docs
```

### 2. Custom Web Interface
```html
<!DOCTYPE html>
<html>
<head>
    <title>Truth Core Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <h1>Truth Core Dashboard</h1>
    
    <div id="stats">
        <h2>Statistics</h2>
        <div id="stats-content"></div>
    </div>
    
    <div id="graph">
        <h2>Network Graph</h2>
        <canvas id="graph-canvas"></canvas>
    </div>
    
    <script>
        // Fetch statistics
        fetch('/api/v1/stats')
            .then(response => response.json())
            .then(data => {
                document.getElementById('stats-content').innerHTML = `
                    <p>Events: ${data.events}</p>
                    <p>Statements: ${data.statements}</p>
                    <p>Impacts: ${data.impacts}</p>
                    <p>Average Trust Score: ${data.avg_trust_score}</p>
                `;
            });
        
        // Fetch network graph
        fetch('/graph/json')
            .then(response => response.json())
            .then(data => {
                // Render network graph using Chart.js
                const ctx = document.getElementById('graph-canvas').getContext('2d');
                new Chart(ctx, {
                    type: 'scatter',
                    data: {
                        datasets: [{
                            label: 'Network Nodes',
                            data: data.nodes.map(node => ({
                                x: node.score,
                                y: node.propagation_priority
                            }))
                        }]
                    },
                    options: {
                        scales: {
                            x: { title: { display: true, text: 'Trust Score' } },
                            y: { title: { display: true, text: 'Propagation Priority' } }
                        }
                    }
                });
            });
    </script>
</body>
</html>
```

## Authentication Integration

### 1. JWT Authentication
```rust
use truth_core::desktop::auth;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Authenticate user
    let token = auth::authenticate("username", "password").await?;
    println!("JWT Token: {}", token);
    
    // Verify token
    let claims = auth::verify_token(&token)?;
    println!("User: {}", claims.sub);
    
    Ok(())
}
```

### 2. Role-Based Access Control
```rust
use truth_core::desktop::rbac;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Grant role to user
    rbac::grant_role("user_public_key", "node")?;
    
    // Check permissions
    if rbac::has_permission("user_public_key", "sync")? {
        println!("User can sync");
    }
    
    Ok(())
}
```

## Performance Optimization

### 1. Async Runtime Configuration
```rust
use tokio::runtime::Runtime;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create optimized runtime
    let rt = Runtime::new()?;
    
    rt.block_on(async {
        // Run Truth Core operations
        truth_core::desktop::server::start_server(8080).await?;
        Ok::<(), Box<dyn std::error::Error>>(())
    })?;
    
    Ok(())
}
```

### 2. Database Optimization
```rust
use truth_core::core::storage;

fn optimize_database() -> Result<(), Box<dyn std::error::Error>> {
    let db = storage::open_database("truth.db")?;
    
    // Enable WAL mode
    db.execute("PRAGMA journal_mode=WAL", [])?;
    
    // Set cache size
    db.execute("PRAGMA cache_size=10000", [])?;
    
    // Enable foreign keys
    db.execute("PRAGMA foreign_keys=ON", [])?;
    
    Ok(())
}
```

## Testing

### 1. Unit Tests
```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[tokio::test]
    async fn test_http_server() {
        let server = server::start_server(0).await;
        assert!(server.is_ok());
    }
    
    #[test]
    fn test_cli_commands() {
        let cli = cli::TruthCli::new().unwrap();
        assert!(cli.run_status().is_ok());
    }
}
```

### 2. Integration Tests
```rust
#[tokio::test]
async fn test_p2p_sync() {
    let peer_url = "http://127.0.0.1:8080";
    let result = sync::sync_with_peer(peer_url).await;
    assert!(result.is_ok());
}
```

## Troubleshooting

### Common Issues

**Port Already in Use:**
```bash
# Check what's using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

**Database Locked:**
```bash
# Check for lock files
ls -la truth.db*

# Remove lock files if safe
rm truth.db-wal truth.db-shm
```

**P2P Discovery Issues:**
```bash
# Check UDP port 37020
netstat -an | grep 37020

# Test UDP connectivity
nc -u 192.168.1.100 37020
```

This integration guide provides comprehensive desktop development with Truth Core's full feature set while maintaining optimal performance and reliability.
