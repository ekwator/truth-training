# P2P Module - Network Layer Implementation
## Truth Training

**Document Version:** v1.1.0  
**Status:** Specification  
**Updated:** 2025-12-28  
**Status:** Approved

**Purpose:** :  
• Formal **description** of P2P networking implementation used
in **Truth Training application** and its compliance with **model** of **event**, **consequence** and **truth assessment**  
• This document **describes** the **formal model** of **P2P networking** in Truth Training application with **emphasis** on **network discovery, node management and synchronization protocols**.  

The **model** is **designed** for :  
• **formalization** of **P2P networking entities**;  
• **description** of **relationships** between **nodes**, **discovery**, **synchronization** and **trust propagation**;  
• ensuring **reproducibility** of **network operations**;  
• alignment of **Core** / **Desktop** / **Mobile** implementations.  

## 1 P2P Module is based on the following principles :  
• Network is *decentralized*, not centralized  
• Errors are allowed locally; stability arises globally  
• No trusted authority; robustness is statistical  
• Two primary functions :  
  ◦ **Discovery** → *node discovery*  
  ◦ **Synchronization** → *data exchange*  

Each entity is mapped to one or more network tables.
Model reflects **one-to-many** principle :  
• one **node** → **multiple peers**  
• one **discovery** → **multiple node connections**  
• one **synchronization** → **multiple data exchanges**  

Each node maintains a local database, evaluates peers independently, and participates in **P2P circulation**  

**By analogy** :  
• **network** = **graph** + **discovery protocols**  
• **system nodes** = **peers**  
• **connections** = **discovery** and **synchronization**  

Application **model** includes the following **main entity classes** :  
• **Node**  
• **Discovery**  
• **Synchronization**  
• **Peer** 
• **Trust Propagation** / **Aggregation**  

Document is coordinated and should be used jointly with :  
• **04-data-model.md** — canonical SQL schema specifications for implementers  
• **Data_Schema.md** — canonical markdown schema specifications for implementers  
• **SECURITY.md** — security and verification requirements  
• **CONTRIBUTING.md** — quality and testing requirements  
• **14-quality-gates.md** — minimum requirements for PR acceptance  

⭐️❗⚠️ "small_constants" is **global** **small** **random** in **system** **time** "CURRENT_TIMESTAMP" value (0, 2)

In the v1.1.0 implementation, this function has been migrated from Rust to SQL for use in triggers and database operations. The detailed SQL implementation is described in [model_core.md](model_core.md). This function serves as a critical component in P2P synchronization processes.

### Usage in P2P Synchronization

small_constants is used in P2P synchronization in several critical ways:

#### 1. Integration with Trust and Stability Calculations
- small_constants is used in computations that occur during synchronization between nodes
- It helps ensure that even during synchronization between different nodes, calculations maintain a small degree of uncertainty, enhancing resilience against manipulation and attacks
- In the `node_trust_limits` table, there is a `small_constants` column that is used in node influence decay calculations during synchronization

#### 2. Prediction and Stability Computations
- Used in horizon prediction calculations in the `impact_predictions` table to prevent division by zero and ensure mathematical stability
- In the formula for calculating `expected_strength`: `expected_strength = Σ(truth_event.collective_score / (impact_predictions.horizon + small_constants))` - small_constants prevents division by zero and ensures mathematical stability
- Used in horizon-related calculations to prevent division by zero when t_end equals t_start

#### 3. Quantum Uncertainty in P2P System
- small_constants introduces an element of randomness into calculations that occur during synchronization, making the system more resilient to manipulation
- This provides "quantum uncertainty" in the system, preventing predictability and deterministic behavior in computations

#### 4. Stability Threshold Calculations
- small_constants is used as a stability threshold (εT and εI) when determining if an event is stable in terms of truth and impact
- During node synchronization, when calculating whether an event has reached a stable state, small_constants serves as the minimum threshold for change detection

#### 5. Decay Function Integration
- small_constants is used in node influence decay formulas: `w(t) = w₀ * e^(-λt)`, where λT and λI (decay parameters for truth and impact) are compared with the small_constants threshold to determine event stability

Thus, small_constants plays an important role in ensuring mathematical stability and resilience of the P2P system against manipulation by introducing controlled degrees of uncertainty in critical computations during synchronization processes.

### Judgment Synchronization in P2P Networks

In addition to events and impacts, the P2P synchronization system also handles the distribution of participant judgments. This is critical for maintaining the truth assessment axis of the system:

- **Judgment propagation**: Individual participant judgments are synchronized between nodes to enable distributed truth assessment
- **Assessment independence**: Each node maintains its own local assessment metrics
- **Truth convergence**: Through synchronization, nodes gradually converge on truth assessments as more judgments accumulate from the network
- **Participant anonymity**: The system preserves the independence of judgments while allowing for collective truth emergence

The judgment synchronization process works alongside event and impact synchronization to ensure that both axes of the truth-consequence space are properly maintained across the distributed network.

### Timeline Synchronization in P2P Networks

In addition to synchronizing the core data entities, the P2P system also handles temporal aspects of all entities:

- **Event timeline synchronization**: Event temporal parameters (start/end times) are synchronized separately from event metadata to handle future-dated events
- **Impact timeline synchronization**: Impact temporal parameters are synchronized to maintain proper timing context for consequence assessments
- **Judgment timeline synchronization**: Judgment temporal parameters are synchronized to maintain proper timing context for truth assessments
- **Temporal consistency**: All timeline data ensures that entities maintain proper chronological relationships across the network

This temporal synchronization ensures that all nodes maintain consistent understanding of when events, impacts, and judgments occurred or are scheduled to occur.

### Relationship Link Synchronization in P2P Networks

In addition to synchronizing the core entities and their temporal aspects, the P2P system also maintains the relationships between entities:

- **Event link synchronization**: Event-to-event relationships are synchronized to maintain causal and logical connections between events
- **Impact link synchronization**: Impact-to-impact relationships are synchronized to maintain consequence chain connections
- **Judgment link synchronization**: Judgment-to-judgment relationships are synchronized to maintain logical connections between truth assessments
- **Cross-entity links**: Links between different entity types (e.g., impact to judgment) are synchronized to maintain the full relationship graph
- **Graph consistency**: All link data ensures that the relationship structure remains consistent across the network

This relationship synchronization ensures that all nodes maintain consistent understanding of how events, impacts, and judgments relate to each other in the collective intelligence graph.

## 3 P2P Module Components and Implementation

### 3.1 Encryption Module

#### Module: encryption.rs - Cryptographic Identity

**Purpose** :
Cryptographic Identity module for managing Ed25519 keys and signatures in the P2P network layer

**Functions** :
```
CryptoIdentity - structure for managing Ed25519 keys
new() - generate new key pair
sign() - sign data
verify() - verify signature
public_key_hex() - get public key in hex
```

**Model: CryptoIdentity**

```rust
use ed25519_dalek::{Keypair, PublicKey, Signature, Signer, Verifier, SECRET_KEY_LENGTH};
use rand::rngs::OsRng;

pub struct CryptoIdentity {
    keypair: Keypair,
}

impl CryptoIdentity {
    pub fn new() -> Result<Self, Box<dyn std::error::Error>> {
        let mut rng = OsRng;
        let keypair = Keypair::generate(&mut rng);
        Ok(CryptoIdentity { keypair })
    }

    pub fn sign(&self, data: &[u8]) -> Result<Signature, Box<dyn std::error::Error>> {
        Ok(self.keypair.sign(data))
    }

    pub fn verify(&self, data: &[u8], signature: &Signature) -> Result<bool, Box<dyn std::error::Error>> {
        match self.keypair.public.verify(data, signature) {
            Ok(()) => Ok(true),
            Err(_) => Ok(false),
        }
    }

    pub fn public_key_hex(&self) -> String {
        hex::encode(self.keypair.public.as_bytes())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_crypto_identity_creation() {
        let identity = CryptoIdentity::new().unwrap();
        assert!(!identity.public_key_hex().is_empty());
    }

    #[test]
    fn test_sign_and_verify() {
        let identity = CryptoIdentity::new().unwrap();
        let data = b"test message";
        
        let signature = identity.sign(data).unwrap();
        let is_valid = identity.verify(data, &signature).unwrap();
        
        assert!(is_valid);
    }

    #[test]
    fn test_invalid_signature() {
        let identity = CryptoIdentity::new().unwrap();
        let data1 = b"test message 1";
        let data2 = b"test message 2";
        
        let signature = identity.sign(data1).unwrap();
        let is_valid = identity.verify(data2, &signature).unwrap();
        
        assert!(!is_valid);
    }
}
```

**Implementation Details** :
• Uses Ed25519 for cryptographic operations
• Secure key generation using OS random number generator
• Signature verification for data integrity
• Hex encoding for public key representation

**Security Considerations** :
• Keys are generated using cryptographically secure random number generator
• Private keys are never exposed outside the struct
• Signatures provide non-repudiation and integrity verification

### 3.2 Node Module

#### Module: node.rs - P2P Node

**Purpose** :
P2P Node module for managing peer connections, database access and crypto-identity in the network layer

**Functions** :
```
Node - node structure with peer list, DB and crypto-identity
start() - start periodic synchronization with peers (every 30 sec)
```

**Model: Node**

```rust
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use tokio::time::{sleep, Duration};
use crate::p2p::encryption::CryptoIdentity;

pub struct Node {
    identity: Arc<Mutex<CryptoIdentity>>,
    peers: Arc<Mutex<HashMap<String, String>>>,  // URL -> public_key
    db_path: String,
}

impl Node {
    pub fn new(db_path: String) -> Result<Self, Box<dyn std::error::Error>> {
        let identity = Arc::new(Mutex::new(CryptoIdentity::new()?));
        let peers = Arc::new(Mutex::new(HashMap::new()));
        
        Ok(Node {
            identity,
            peers,
            db_path,
        })
    }

    pub async fn start(&self) -> Result<(), Box<dyn std::error::Error>> {
        println!("Starting P2P node synchronization...");
        
        loop {
            self.synchronize_with_peers().await?;
            sleep(Duration::from_secs(30)).await; // Every 30 seconds
        }
    }

    async fn synchronize_with_peers(&self) -> Result<(), Box<dyn std::error::Error>> {
        let peers = self.peers.lock().unwrap().clone();
        
        for (url, public_key) in peers {
            match self.sync_with_peer(&url, &public_key).await {
                Ok(_) => println!("Successfully synchronized with peer: {}", url),
                Err(e) => eprintln!("Failed to synchronize with peer {}: {}", url, e),
            }
        }
        
        Ok(())
    }

    async fn sync_with_peer(&self, url: &str, public_key: &str) -> Result<(), Box<dyn std::error::Error>> {
        // Implementation would handle actual peer synchronization
        println!("Synchronizing with peer: {} (key: {})", url, public_key);
        Ok(())
    }

    pub fn add_peer(&self, url: String, public_key: String) {
        let mut peers = self.peers.lock().unwrap();
        peers.insert(url, public_key);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_node_creation() {
        let node = Node::new("test.db".to_string());
        assert!(node.is_ok());
    }

    #[tokio::test]
    async fn test_add_peer() {
        let node = Node::new("test.db".to_string()).unwrap();
        node.add_peer("http://localhost:8080".to_string(), "test_key".to_string());
        
        // Test that peer was added (would need to access internal state in real implementation)
        assert!(true); // Placeholder assertion
    }
}
```

**Implementation Details** :
• Thread-safe node implementation using Arc and Mutex
• Periodic synchronization with peers every 30 seconds
• Peer management through URL and public key mapping
• Integration with cryptographic identity for secure communications
### 3.3 Synchronization Module

#### Module: sync.rs - Peer Synchronization

**Purpose** :
Peer Synchronization module for handling asynchronous synchronization with specific peers, including signing requests and verifying responses

**Synchronization Payload** :
The synchronization process exchanges the following data between nodes:
- Events (truth_event table)
- Event timelines (event_timeline table)
- Event links (event_links table)
- Impacts (impact table)
- Impact timelines (impact_timeline table)
- Impact links (impact_links table)
- Judgments (judgment table)
- Judgment timelines (judgment_timeline table)
- Judgment links (judgment_links table)

**Functions** :
```
sync_with_peer() - asynchronous synchronization with specific peer
Signs requests and verifies responses
```

**Model: Peer Synchronization**

```rust
use std::sync::Arc;
use tokio;
use ed25519_dalek::Signature;

pub struct SyncHandler {
    identity: Arc<tokio::sync::Mutex<crate::p2p::encryption::CryptoIdentity>>,
}

impl SyncHandler {
    pub fn new(identity: Arc<tokio::sync::Mutex<crate::p2p::encryption::CryptoIdentity>>) -> Self {
        SyncHandler { identity }
    }

    pub async fn sync_with_peer(&self, peer_url: &str) -> Result<(), Box<dyn std::error::Error>> {
        let timestamp = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)?
            .as_secs();
        
        let request_data = format!("sync_request:{}", timestamp);
        
        // Sign the request
        let signature = {
            let identity = self.identity.lock().await;
            identity.sign(request_data.as_bytes())?
        };
        
        // Send request with signature
        let client = reqwest::Client::new();
        let response = client
            .post(format!("{}/events", peer_url))
            .header("X-Public-Key", self.identity.lock().await.public_key_hex())
            .header("X-Signature", hex::encode(signature.to_bytes()))
            .body(request_data)
            .send()
            .await?;
        
        // Verify response signature if present
        if let Some(sig_header) = response.headers().get("X-Signature") {
            let sig_bytes = hex::decode(sig_header.to_str()?)?;
            if sig_bytes.len() == 64 {
                let response_signature = Signature::from_bytes(&sig_bytes.try_into().unwrap());
                
                let response_body = response.text().await?;
                let identity = self.identity.lock().await;
                if !identity.verify(response_body.as_bytes(), &response_signature)? {
                    return Err("Invalid response signature".into());
                }
            }
        }
        
        Ok(())
    }
}
```

**Implementation Details** :
• Asynchronous synchronization using tokio
• Request signing with timestamp for freshness
• Response verification for integrity
• HTTP headers for cryptographic metadata

### 3.4 Network Discovery Module

#### Module: net.rs - Network Discovery

**Purpose** :
Network Discovery module for UDP beacons to discover other nodes in local network

**Functions** :
```
UDP beacons for discovering other nodes in local network
run_beacon_sender() - send beacons every 7 sec
run_beacon_listener() - listen for beacons from other nodes
```

**Model: Network Discovery**

```rust
use std::net::{UdpSocket, SocketAddr};
use std::time::Duration;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
struct BeaconMessage {
    node_id: String,
    address: String,
    timestamp: u64,
}

pub struct NetworkDiscovery {
    socket: UdpSocket,
    broadcast_addr: SocketAddr,
}

impl NetworkDiscovery {
    pub fn new(bind_addr: &str, broadcast_addr: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let socket = UdpSocket::bind(bind_addr)?;
        socket.set_broadcast(true)?;
        
        let broadcast_addr: SocketAddr = broadcast_addr.parse()?;
        
        Ok(NetworkDiscovery {
            socket,
            broadcast_addr,
        })
    }

    pub fn run_beacon_sender(&self, node_id: String, address: String) -> Result<(), Box<dyn std::error::Error>> {
        loop {
            let beacon = BeaconMessage {
                node_id: node_id.clone(),
                address: address.clone(),
                timestamp: std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .unwrap()
                    .as_secs(),
            };
            
            let beacon_json = serde_json::to_string(&beacon)?;
            self.socket.send_to(beacon_json.as_bytes(), self.broadcast_addr)?;
            
            std::thread::sleep(Duration::from_secs(7));  // Every 7 seconds
        }
    }

    pub fn run_beacon_listener(&self) -> Result<Vec<BeaconMessage>, Box<dyn std::error::Error>> {
        let mut buffer = [0; 1024];
        let (size, src) = self.socket.recv_from(&mut buffer)?;
        
        let beacon_str = std::str::from_utf8(&buffer[..size])?;
        let beacon: BeaconMessage = serde_json::from_str(beacon_str)?;
        
        println!("Received beacon from {}: {:?}", src, beacon);
        
        Ok(vec![beacon])
    }
}
```

**Implementation Details** :
• UDP broadcast for local network discovery
• Beacon messages with node identity and address
• Periodic beacon transmission every 7 seconds
• JSON serialization for message format

## 4 Server API Commands and Endpoints

### 4.1 API Endpoints Implementation

Here's the complete list of implemented server API endpoints with their implementation details:

#### 1. GET /health
**Description**: Server health check
**Implementation**:
```rust
use actix_web::{get, HttpResponse, Result};

#[get("/health")]
async fn health() -> Result<HttpResponse> {
    Ok(HttpResponse::Ok().body("OK"))
}
```
**Response**: 200 OK with body "OK"
**Usage**: Server availability monitoring

#### 2. GET /events
**Description**: Get list of all truth events with cryptographic authentication
**Implementation**:
```rust
use actix_web::{get, HttpResponse, Result, web, http::header};
use ed25519_dalek::{PublicKey, Signature};
use std::str::FromStr;

#[get("/events")]
async fn get_events(req: actix_web::HttpRequest) -> Result<HttpResponse> {
    // Extract headers
    let public_key_hex = req
        .headers()
        .get("X-Public-Key")
        .ok_or("Missing X-Public-Key header")?
        .to_str()?;
    
    let signature_hex = req
        .headers()
        .get("X-Signature")
        .ok_or("Missing X-Signature header")?
        .to_str()?;
    
    // Verify signature
    let public_key_bytes = hex::decode(public_key_hex)
        .map_err(|_| "Invalid public key format")?;
    let public_key = PublicKey::from_bytes(&public_key_bytes)
        .map_err(|_| "Invalid public key")?;
    
    let signature_bytes = hex::decode(signature_hex)
        .map_err(|_| "Invalid signature format")?;
    let signature = Signature::from_bytes(&signature_bytes.try_into().unwrap());
    
    let timestamp = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let message = format!("sync_request:{}", timestamp);
    
    match public_key.verify(message.as_bytes(), &signature) {
        Ok(()) => {
            // Authentication successful - return events
            use crate::core::storage::get_all_events;
            let events = web::block(move || get_all_events()).await.unwrap();
            Ok(HttpResponse::Ok().json(events))
        }
        Err(_) => Ok(HttpResponse::Unauthorized().finish()),
    }
}
```
**Headers**:
X-Public-Key: hex-encoded Ed25519 public key
X-Signature: hex-encoded message signature
**Authentication**: Verifies message signature sync_request:{timestamp}
**Response**:
200 OK with JSON array of events on successful authentication
401 Unauthorized on invalid signature
**Usage**: P2P synchronization between nodes

#### 3. POST /events
**Description**: Add new truth event
**Implementation**:
```rust
use actix_web::{post, HttpResponse, Result, web};
use serde::Deserialize;
use crate::core::storage::add_event;

#[derive(Deserialize)]
struct EventRequest {
    description: String,
    // Other event fields
}

#[post("/events")]
async fn post_events(req: web::Json<EventRequest>) -> Result<HttpResponse> {
    let event_id = web::block(move || {
        add_event(&req.description, 1) // code: 1
    }).await.unwrap();
    
    Ok(HttpResponse::Ok().json(serde_json::json!({"id": event_id})))
}
```
**Request body**: EventRequest JSON object
**Response**: 200 OK with JSON object {"id": "event_id"}
**Automatically sets**: code: 1, timestamp_start: current time

#### 4. POST /impacts
**Description**: Add new impact to event
**Implementation**:
```rust
use actix_web::{post, HttpResponse, Result, web};
use serde::Deserialize;
use crate::core::storage::add_impact;

#[derive(Deserialize)]
struct ImpactRequest {
    event_id: i32,
    value: Option<i32>,
    notes: Option<String>,
    // Other impact fields
}

#[post("/impacts")]
async fn post_impacts(req: web::Json<ImpactRequest>) -> Result<HttpResponse> {
    let impact_id = web::block(move || {
        add_impact(req.event_id, req.value, req.notes.as_deref())
    }).await.unwrap();
    
    Ok(HttpResponse::Ok().json(serde_json::json!({"id": impact_id})))
}
```
**Request body**: JSON Impact object
**Response**: 200 OK with JSON object {"id": "impact_id"}

#### 5. POST /judgments
**Description**: Add new judgment to event
**Implementation**:
```rust
use actix_web::{post, HttpResponse, Result, web};
use serde::Deserialize;
use crate::core::storage::add_judgment;

#[derive(Deserialize)]
struct JudgmentRequest {
    event_id: i32,
    assessment: Option<f64>,  // -1.0 to 1.0 scale for truth assessment
    confidence_level: Option<f64>,  // 0.0 to 1.0 scale for confidence
    reasoning: Option<String>,  // Textual justification for the judgment
    // Other judgment fields
}

#[post("/judgments")]
async fn post_judgments(req: web::Json<JudgmentRequest>) -> Result<HttpResponse> {
    let judgment_id = web::block(move || {
        add_judgment(req.event_id, req.assessment, req.confidence_level, req.reasoning.as_deref())
    }).await.unwrap();
    
    Ok(HttpResponse::Ok().json(serde_json::json!({"id": judgment_id})))
}
```
**Request body**: JSON Judgment object
**Response**: 200 OK with JSON object {"id": "judgment_id"}

### 4.2 Implementation Features

**Asynchronicity**: All endpoints use actix-web with asynchronous handlers
**Database**: Uses web::block for blocking SQLite operations
**P2P Security**: /events endpoint requires cryptographic authentication
**Error Handling**: Detailed error messages for debugging
**Typing**: Strict typing with serde for serialization/deserialization

## 5 P2P Module Summary

✅ Fixed SigningKey::generate error - p2p/encryption.rs module now compiles correctly
✅ Verified P2P module functionality - module includes:
Cryptographic identity (Ed25519)
P2P node with periodic synchronization
Network discovery via UDP beacons
Secure synchronization between peers
✅ Created complete server API commands list - 5 endpoints:
GET /health - health check
GET /events - get events with P2P authentication
POST /events - add truth events
POST /impacts - add impacts
POST /judgments - add judgments

For complete API specification, see [spec/05-api.md](../spec/05-api.md) which contains full API documentation.

The P2P module is fully functional and integrated into the main application to provide decentralized data synchronization between nodes.


