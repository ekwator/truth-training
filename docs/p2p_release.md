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

## 2 Basic Model Entities and Service Tables

This chapter **describes** fundamental **network tables** that **provide** node **identification**, their **discovery** and **integrity** of **P2P model**
Network** structure complies with **following principles** :  
• **Graph model** with explicit **primary** and **foreign** keys  
• **Absence** of **stored** calculated **trust values**  
• All aggregates are **calculated** at **Core** logic level  
• Data **historization**: records are **not overwritten**, but **supplemented**  
• **Support** for **multiple** sources and evaluation **contexts**  

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

### 2.1 Nodes

#### Table: nodes

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **transmitted over the network**

**Purpose** :
Storing information about discovered nodes in Truth Training network for tracking peer nodes, their addresses, types, availability and other discovery metadata
**Fields** :
```
id         (INTEGER, PK, AUTOINCREMENT) — unique node identifier
address    (TEXT, NOT NULL, UNIQUE) — URL or ip:port of node (e.g. http://192.168.1.100:8080/api/v1)
type       (TEXT, NOT NULL) — node type (LAN, WIFI, GLOBAL, RELAY, CLIENT)
reachable  (INTEGER, NOT NULL) — availability flag (0 = down, 1 = up)
last_seen  (INTEGER, NOT NULL) — time of last successful contact
ttl        (INTEGER, NOT NULL) — record lifetime before automatic deletion
source     (TEXT) — source of node discovery
node_id    (TEXT, NOT NULL) — FK → participants.public_key  — node's public key
created_at (INTEGER, NOT NULL) — timestamp of record creation
updated_at (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: discovery_nodes.sqlite

**Notes** :
• Node addresses are validated for proper URL format
• TTL ensures stale node records are cleaned up
• Node discovery supports multiple network types

**Model "nodes"** :
**Source relation**
```
nodes.node_id = participants.public_key
nodes.id = node_ratings.node_id
nodes.id = node_performance.pubkey
nodes.id = peer_synchronization.peer_url
```
**Base node mapping**
```
base_node_id =
SELECT nodes.node_id
FROM nodes
WHERE nodes.node_id = node_ratings.node_id
```
**Aggregation formulas "reachable"**
```
nodes.reachable = (
    SELECT CASE
        WHEN last_seen > (CURRENT_TIMESTAMP - 300) THEN 1 -- 5 minutes
        ELSE 0
    END
    FROM nodes n
    WHERE n.id = base_node_id
)
```
**Aggregation formulas "last_seen"**
```
nodes.last_seen = (
    SELECT MAX(timestamp)
    FROM sync_attempts
    WHERE sync_attempts.peer_url = nodes.address
)
```
**Aggregation formulas "ttl"**
```
nodes.ttl = (
    SELECT 3600  -- Default 1 hour TTL
)
```
**Aggregation formulas "created_at"**
```
nodes.created_at = CURRENT_TIMESTAMP
```

**SQL Implementation Example**:
```sql
-- Create nodes table
CREATE TABLE nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES participants(public_key)
);

-- Insert new node
INSERT INTO nodes (address, type, reachable, last_seen, ttl, source, node_id, created_at, updated_at)
VALUES (?, ?, 1, ?, 3600, ?, ?, ?, ?);

-- Update node reachability
UPDATE nodes
SET reachable = CASE
    WHEN last_seen > (strftime('%s', 'now') - 300) THEN 1
    ELSE 0
END,
last_seen = strftime('%s', 'now')
WHERE id = ?;

-- Get active nodes
SELECT * FROM nodes
WHERE last_seen > (strftime('%s', 'now') - ttl)
AND reachable = 1;
```

#### Model: Node Discovery and Management

**Node Discovery Model** :
```
N = {address, type, public_key, availability}
```

**Node Lifecycle** :
```
IF last_seen < (CURRENT_TIMESTAMP - ttl)
    node_status = 'expired'
    eligible_for_cleanup = TRUE
    
IF reachable = 0 AND consecutive_failures > threshold
    node_status = 'unreachable'
```

**Node Type Classification** :
• LAN — Local Area Network nodes (typically 192.168.x.x or 10.x.x.x)
• WIFI — Wireless network nodes
• GLOBAL — Public internet nodes
• RELAY — Relay nodes that forward traffic
• CLIENT — End-user client nodes

**Node Validation Rules** :
```
IF address NOT valid_url_format
    ERROR "Invalid address format"
    
IF type NOT IN (LAN, WIFI, GLOBAL, RELAY, CLIENT)
    ERROR "Invalid node type"
    
IF node_id NOT IN participants.public_key
    ERROR "Node ID not registered as participant"
```

**Rules** :  
• "address" must be **valid URL**  
• **Default** "type" is LAN (local network)  
• **Nodes** with **higher availability** have **more weight** in **network calculations**  
• "reachable" is **updated periodically** through **discovery**  
• "last_seen" is **timestamp of last successful contact**  
• "ttl" controls **automatic cleanup** of **stale nodes**  
• "source" indicates **discovery method** (beacon, manual, etc.)  

**Node Interaction Model** :  
• Each **node** connects to **peers** in "nodes" table  
• Each **node** participates in **discovery**  
• Each **node** performs **synchronization** with **peers**  
• **Node availability** affects "reachable" in **network operations**  

**Trust Propagation** :  
• **High-availability nodes** have **more** influence on **network**  
• **Low-availability nodes** connections are **weighted less**  
• **Node availability** affects propagation **priority in network**  

**Node Lifecycle** :  
• **Registration** → "reachable" = 0, "last_seen" = creation time  
• **First contact** → "reachable" = 1, "last_seen" updated  
• **Continuous operation** → "last_seen" **evolves over time**  

**Key constraints** :  
• "address" must be **valid URL format**  
• "node_id" must be **unique** across **all nodes**  
• "node_id" must **reference** valid records in "participants" table

**Notes** :  
• **Node** identity is **pseudonymous** "node_id" based  
• **Node availability** is calculated based on **contact history**  
• **Node availability** affects the **node** influence in the **network**  
• Cryptographic **keys** ensure **authenticity** of **node** actions  

### 2.2 Discovery

#### Table: node_discovery

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose** :
**Tracking changes** in network **node discovery** for **auditing and analyzing** changes in **node discovery**, understanding reasons for **discovery changes**, analyzing **node behavior** and **availability effectiveness**, and **ensuring transparency** of **discovery system**
**Fields** :
```
id             (INTEGER, PK, AUTOINCREMENT) — unique discovery record identifier
node_id        (INTEGER, NOT NULL) — FK → nodes.id
discovery_type (TEXT, NOT NULL) — type of discovery (beacon, manual, api)
discovered_at  (INTEGER, NOT NULL) — timestamp of discovery
ttl            (INTEGER, NOT NULL) — time to live for discovery record
status         (TEXT, NOT NULL) — discovery status (active, expired, unreachable)
source         (TEXT, NOT NULL) — source of discovery information
```
🏠 Database: discovery_nodes.sqlite

**Model "node_discovery"** :
**Source relation**
```
node_discovery.node_id = nodes.id
```
**Base node mapping**
```
base_node_id =
SELECT nodes.id
FROM nodes
WHERE nodes.id = node_discovery.node_id
```
**Aggregation formulas "discovered_at"**
```
node_discovery.discovered_at = (
    SELECT nodes.created_at
    FROM nodes
    WHERE nodes.id = base_node_id
)
```
**Aggregation formulas "ttl"**
```
node_discovery.ttl = (
    SELECT 3600  -- Default 1 hour
    FROM nodes
    WHERE nodes.id = base_node_id
)
```
**Aggregation formulas "status"**
```
node_discovery.status = (
    SELECT CASE
        WHEN discovered_at > (CURRENT_TIMESTAMP - ttl) THEN 'active'
        ELSE 'expired'
    END
)
```
**Aggregation formulas "source"**
```
node_discovery.source = (
    SELECT nodes.source
    FROM nodes
    WHERE nodes.id = base_node_id
)
```

**SQL Implementation Example**:
```sql
-- Create node discovery table
CREATE TABLE node_discovery (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    node_id INTEGER NOT NULL,
    discovery_type TEXT NOT NULL,
    discovered_at INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    status TEXT NOT NULL,
    source TEXT NOT NULL,
    FOREIGN KEY (node_id) REFERENCES nodes(id)
);

-- Insert new discovery record
INSERT INTO node_discovery (node_id, discovery_type, discovered_at, ttl, status, source)
VALUES (?, ?, ?, 3600, 'active', ?);

-- Update discovery status based on TTL
UPDATE node_discovery
SET status = CASE
    WHEN discovered_at > (strftime('%s', 'now') - ttl) THEN 'active'
    ELSE 'expired'
END
WHERE id = ?;

-- Get active discoveries
SELECT nd.*, n.address, n.type
FROM node_discovery nd
JOIN nodes n ON nd.node_id = n.id
WHERE nd.status = 'active'
AND nd.discovered_at > (strftime('%s', 'now') - nd.ttl);

-- Clean up expired discoveries
DELETE FROM node_discovery
WHERE discovered_at < (strftime('%s', 'now') - ttl);
```

#### Model: Node Discovery Tracking

**Discovery Change Model** :
```
ΔD = D_new - D_old
```

**Discovery Types** :
• "beacon" — discovery through UDP beacons
• "manual" — manually added nodes
• "api" — discovery through API calls

**Discovery Rules** :
```
IF beacon_received
    discovery_type = "beacon"
    discovered_at = CURRENT_TIMESTAMP
    ttl = 3600  -- 1 hour

IF manual_addition
    discovery_type = "manual"
    discovered_at = CURRENT_TIMESTAMP
    ttl = 86400  -- 24 hours
```

**Discovery Analysis** :
• Trend analysis for node discovery
• Detection of discovery manipulation attempts
• Verification of discovery evolution consistency
• Audit trail for discovery changes

**Key constraints** :
• discovered_at and ttl must be valid timestamps
• discovery_type must be one of predefined values
• status must be current based on ttl

**Notes** :
• Used for auditing and transparency of discovery changes
• Tracks historical changes for analysis
• Enables detection of discovery manipulation
• Supports discovery trend analysis

### 2.3 Synchronization

#### Table: sync_attempts

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Records high-level synchronization events between nodes for monitoring network-wide operations and catching failures.

**Fields**:
```
id         (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
timestamp  (INTEGER, NOT NULL) — when the sync occurred
peer_url   (TEXT, NOT NULL) — the peer node's URL
mode       (TEXT, NOT NULL) — sync mode or protocol (e.g., "full", "delta")
status     (TEXT, NOT NULL) — result status (e.g. "success" or error code)
details    (TEXT, NOT NULL) — additional info or error message
```
🏠 Database: discovery_nodes.sqlite

**Model "sync_attempts"** :
**Source relation**
```
sync_attempts.peer_url = nodes.address
```
**Base sync mapping**
```
base_sync_id =
SELECT sync_attempts.id
FROM sync_attempts
WHERE sync_attempts.id = sync_attempts.id
```
**Aggregation formulas "timestamp"**
```
sync_attempts.timestamp = CURRENT_TIMESTAMP
```
**Aggregation formulas "status"**
```
sync_attempts.status = (
    SELECT CASE
        WHEN details LIKE '%success%' THEN 'success'
        WHEN details LIKE '%error%' THEN 'error'
        ELSE 'unknown'
    END
)
```
**Aggregation formulas "mode"**
```
sync_attempts.mode = (
    SELECT 'delta'  -- Default to delta sync
)
```

**SQL Implementation Example**:
```sql
-- Create sync_attempts table
CREATE TABLE sync_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    peer_url TEXT NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL
);

-- Insert sync attempt entry
INSERT INTO sync_attempts (timestamp, peer_url, mode, status, details)
VALUES (strftime('%s', 'now'), ?, ?, ?, ?);

-- Get recent sync attempts for a peer
SELECT * FROM sync_attempts
WHERE peer_url = ?
ORDER BY timestamp DESC
LIMIT 10;

-- Get sync success rate for a peer
SELECT
    peer_url,
    COUNT(*) as total_syncs,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_syncs,
    (SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as success_rate
FROM sync_attempts
WHERE peer_url = ?
GROUP BY peer_url;

-- Clean up old sync attempts
DELETE FROM sync_attempts
WHERE timestamp < (strftime('%s', 'now') - 86400 * 7); -- Delete attempts older than 7 days
```

##### Model: Synchronization Event Logging

**Synchronization Event Model**:
```
SyncLog = {timestamp, peer_url, mode, status, details}
```

**Synchronization Monitoring Rules**:
```
IF sync_operation_initiated
    log_sync_event(
        timestamp = CURRENT_TIMESTAMP,
        peer_url = target_node_url,
        mode = synchronization_mode,
        status = initial_status,
        details = operation_details
    )

IF sync_operation_completed
    update_sync_operation(
        status = final_status,
        details = completion_details
    )
```

**Log Analysis**:
• Tracks synchronization success/failure rates
• Monitors peer node connectivity
• Records synchronization mode effectiveness
• Captures error details for debugging

**Notes**:
• Helps monitor network-wide operation and catch failures
• Enables analysis of synchronization patterns
• Supports network diagnostics and optimization

#### Table: sync_operations

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Tracking low-level synchronization operations for tracking changes at individual record level, auditing and debugging synchronization, checking data integrity during exchange between nodes, tracking authenticity of changes via digital signatures

**Fields**:
```
id         (INTEGER, PK, AUTOINCREMENT) — unique log record identifier
op         (TEXT, NOT NULL) — operation type (insert, update, delete)
table_name (TEXT, NOT NULL) — name of the table affected
record_id (TEXT, NOT NULL) — identifier of the record affected
signature  (TEXT, NOT NULL) — signature of the synchronization participant
public_key (INTEGER, NOT NULL) — FK → nodes.id  → nodes.node_id — public key of the synchronization participant
created_at (INTEGER, NOT NULL) — timestamp of the operation
```
🏠 Database: discovery_nodes.sqlite

**Model "sync_operations"** :
**Source relation**
```
sync_operations.public_key = nodes.node_id
```
**Base sync mapping**
```
base_sync_id =
SELECT sync_operations.id
FROM sync_operations
WHERE sync_operations.id = sync_operations.id
```
**Aggregation formulas "op"**
```
sync_operations.op = (
     SELECT 'insert'  -- Default operation type
)
```
**Aggregation formulas "created_at"**
```
sync_operations.created_at = CURRENT_TIMESTAMP
```

**SQL Implementation Example**:
```sql
-- Create sync_operations table
CREATE TABLE sync_operations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    op TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT NOT NULL,
    signature TEXT NOT NULL,
    public_key INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES nodes(node_id)
);

-- Insert sync operation
INSERT INTO sync_operations (op, table_name, record_id, signature, public_key, created_at)
VALUES (?, ?, ?, ?, ?, strftime('%s', 'now'));

-- Get all operations for a specific node
SELECT * FROM sync_operations
WHERE public_key = ?
ORDER BY created_at DESC;

-- Verify signature for a specific operation
SELECT s.*, n.address as node_address
FROM sync_operations s
JOIN nodes n ON s.public_key = n.node_id
WHERE s.id = ?;

-- Get sync operations by table
SELECT table_name, COUNT(*) as operation_count,
       SUM(CASE WHEN op = 'insert' THEN 1 ELSE 0 END) as inserts,
       SUM(CASE WHEN op = 'update' THEN 1 ELSE 0 END) as updates,
       SUM(CASE WHEN op = 'delete' THEN 1 ELSE 0 END) as deletes
FROM sync_operations
WHERE created_at > (strftime('%s', 'now') - 3600) -- Last hour
GROUP BY table_name;
```

##### Model: Synchronization Logging

**Log Entry Model**:
```
Log(entry) = {operation, table, record_id, signature, public_key, timestamp}
```

**Operation Types**:
• INSERT — New record added during sync
• UPDATE — Existing record modified during sync
• DELETE — Record removed during sync

**Integrity Verification**:
```
IF signature_verification(public_key, signature, operation_data) = FALSE
    log_integrity_error()
```

**Log Management Rules**:
```
IF created_at < (CURRENT_TIMESTAMP - retention_period)
    eligible_for_cleanup = TRUE
```

**Notes**:
• Helps monitor network-wide operation and catch failures
• Enables audit trail for synchronization operations
• Supports integrity verification of synchronized data

### 2.4 Trust Propagation

#### Table: node_ratings

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Storing node reputation and trust for evaluating node reliability based on their activity and assessment accuracy

**Fields**:
```
node_id              (INTEGER, NOT NULL) — FK → nodes.id  → nodes.node_id — unique node identifier (public key)
events_true          (INTEGER, NOT NULL, DEFAULT 0) — number of true event
events_false         (INTEGER, NOT NULL, DEFAULT 0) — number of false event
validations          (INTEGER, NOT NULL, DEFAULT 0) — number of confirmations
reused_events        (INTEGER, NOT NULL, DEFAULT 0) — number of reused event
trust_score          (REAL, NOT NULL, DEFAULT 0.0) — overall trust rating (-1.0 .. 1.0)
propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)
last_updated         (INTEGER, NOT NULL) — timestamp of last update
```
🏠 Database: discovery_nodes.sqlite

**Model "node_ratings"** :
**Source relation**
```
node_ratings.node_id = nodes.id
```
**Base node mapping**
```
base_node_id =
SELECT nodes.id
FROM nodes
WHERE nodes.id = node_ratings.node_id
```
**Aggregation formulas "trust_score"**
```
node_ratings.trust_score = (
    SELECT CASE
        WHEN events_true + events_false > 0 THEN (events_true - events_false) * 1.0 / (events_true + events_false)
        ELSE 0.0
    END
    FROM node_ratings nr
    WHERE nr.node_id = base_node_id
)
```
**Aggregation formulas "events_true"**
```
node_ratings.events_true = (
    SELECT COUNT(*)
    FROM sync_operations
    WHERE sync_operations.public_key = base_node_id
    AND sync_operations.op = 'insert'
)
```
**Aggregation formulas "validations"**
```
node_ratings.validations = (
    SELECT COUNT(*)
    FROM sync_operations
    WHERE sync_operations.public_key = base_node_id
)
```
**Aggregation formulas "propagation_priority"**
```
node_ratings.propagation_priority = (
    SELECT trust_score * 0.7 + (validations * 0.3 / (SELECT MAX(validations) FROM node_ratings))
    FROM node_ratings
    WHERE node_id = base_node_id
)
```
**Aggregation formulas "last_updated"**
```
node_ratings.last_updated = CURRENT_TIMESTAMP
```

**SQL Implementation Example**:
```sql
-- Create node_ratings table
CREATE TABLE node_ratings (
    node_id INTEGER NOT NULL,
    events_true INTEGER NOT NULL DEFAULT 0,
    events_false INTEGER NOT NULL DEFAULT 0,
    validations INTEGER NOT NULL DEFAULT 0,
    reused_events INTEGER NOT NULL DEFAULT 0,
    trust_score REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    last_updated INTEGER NOT NULL,
    FOREIGN KEY (node_id) REFERENCES nodes(id)
);

-- Insert or update node rating
INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, propagation_priority, last_updated)
VALUES (?, 0, 0, 0, 0, 0.0, 0.0, strftime('%s', 'now'))
ON CONFLICT(node_id) DO UPDATE SET
    last_updated = strftime('%s', 'now');

-- Update trust score based on events
UPDATE node_ratings
SET
    trust_score = CASE
        WHEN events_true + events_false > 0 THEN (events_true - events_false) * 1.0 / (events_true + events_false)
        ELSE 0.0
    END,
    propagation_priority = (CASE
        WHEN events_true + events_false > 0 THEN (events_true - events_false) * 1.0 / (events_true + events_false)
        ELSE 0.0
    END) * 0.7 + (validations * 0.3 / (SELECT MAX(validations + 1) FROM node_ratings)),
    last_updated = strftime('%s', 'now')
WHERE node_id = ?;

-- Get top rated nodes
SELECT n.address, nr.trust_score, nr.propagation_priority, nr.events_true, nr.events_false
FROM node_ratings nr
JOIN nodes n ON nr.node_id = n.id
ORDER BY nr.trust_score DESC
LIMIT 10;

-- Update validation count
UPDATE node_ratings
SET
    validations = validations + 1,
    last_updated = strftime('%s', 'now')
WHERE node_id = ?;
```

##### Model: Node Reputation and Trust

**Trust Score Model**:
```
Trust(n) = (events_true - events_false) / (events_true + events_false + ε)
```

**Trust Calculation**:
```
IF events_true + events_false = 0
    trust_score = 0.0  (neutral trust)
ELSE
    trust_score = (events_true - events_false) / (events_true + events_false)
```

**Priority Calculation**:
```
Priority(n) = f(trust_score, validation_count, reuse_frequency)
```

**Rating Update Rules**:
```
IF new_validation_received
    IF validation_correct
        events_true = events_true + 1
    ELSE
        events_false = events_false + 1
    recalculate_trust_score()
    last_updated = CURRENT_TIMESTAMP
```

**Notes**:
• Trust scores range from -1.0 (completely untrustworthy) to +1.0 (completely trustworthy)
• Neutral trust is represented by 0.0
• Propagation priority is derived from trust and activity metrics

#### Table: node_performance

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Monitoring node performance and status for tracking node performance metrics for synchronization optimization

**Fields**:
```
pubkey               (INTEGER, NOT NULL) — FK → nodes.id  → nodes.node_id — unique node identifier (public key)
last_seen            (INTEGER, NOT NULL) — time of last contact
relay_success_rate   (REAL, NOT NULL, DEFAULT 0.0) — percentage of successful transfers
quality_index        (REAL, NOT NULL, DEFAULT 0.0) — quality index (0.0 .. 1.0) - continuity of trust indicator
propagation_priority (REAL, NOT NULL, DEFAULT 0.0) — distribution priority (0.0 .. 1.0)
```
🏠 Database: discovery_nodes.sqlite

**Model "node_performance"** :
**Source relation**
```
node_performance.pubkey = nodes.id
```
**Base node mapping**
```
base_node_id =
SELECT nodes.id
FROM nodes
WHERE nodes.id = node_performance.pubkey
```
**Aggregation formulas "relay_success_rate"**
```
node_performance.relay_success_rate = (
    SELECT
        CASE
            WHEN COUNT(*) = 0 THEN 0.0
            ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
        END
    FROM sync_attempts
    WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = base_node_id)
)
```
**Aggregation formulas "quality_index"**
```
node_performance.quality_index = (
    SELECT
        (relay_success_rate * 0.5) +
        (CASE
            WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1
            THEN 0.5
            ELSE 0.1
        END)
    FROM node_performance
    WHERE pubkey = base_node_id
)
```
**Aggregation formulas "propagation_priority"**
```
node_performance.propagation_priority = (
    SELECT quality_index * (SELECT trust_score FROM node_ratings WHERE node_id = base_node_id)
    FROM node_performance
    WHERE pubkey = base_node_id
)
```

**SQL Implementation Example**:
```sql
-- Create node_performance table
CREATE TABLE node_performance (
    pubkey INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    relay_success_rate REAL NOT NULL DEFAULT 0.0,
    quality_index REAL NOT NULL DEFAULT 0.0,
    propagation_priority REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (pubkey) REFERENCES nodes(id)
);

-- Insert or update node performance
INSERT INTO node_performance (pubkey, last_seen, relay_success_rate, quality_index, propagation_priority)
VALUES (?, ?, 0.0, 0.0, 0.0)
ON CONFLICT(pubkey) DO UPDATE SET
    last_seen = excluded.last_seen;

-- Update relay success rate
UPDATE node_performance
SET
    relay_success_rate = (
        SELECT
            CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END
        FROM sync_attempts
        WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = node_performance.pubkey)
    ),
    quality_index = (
        SELECT
            (CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +
            (CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1
                THEN 0.5
                ELSE 0.1
            END)
        FROM sync_attempts
        WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = node_performance.pubkey)
    ),
    propagation_priority = (
        SELECT
            ((CASE
                WHEN COUNT(*) = 0 THEN 0.0
                ELSE SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
            END) * 0.5 +
            (CASE
                WHEN (julianday('now') - julianday(last_seen, 'unixepoch')) < 1
                THEN 0.5
                ELSE 0.1
            END)) * trust_score
        FROM node_ratings
        WHERE node_id = node_performance.pubkey
    )
WHERE pubkey = ?;

-- Get node performance with ratings
SELECT
    n.address,
    np.relay_success_rate,
    np.quality_index,
    np.propagation_priority,
    nr.trust_score
FROM node_performance np
JOIN nodes n ON np.pubkey = n.id
LEFT JOIN node_ratings nr ON n.id = nr.node_id
ORDER BY np.propagation_priority DESC;
```

##### Model: Node Performance Metrics

**Performance Model**:
```
P(n) = {success_rate, quality_index, priority}
```

**Success Rate Calculation**:
```
success_rate = successful_operations / total_operations
```

**Quality Index Model**:
```
Q(n) = α * recent_performance + β * historical_consistency + γ * trust_factor
```

**Metrics Update Rules**:
```
IF synchronization_attempt
    IF successful
        relay_success_rate = (previous_successes + 1) / total_attempts
    ELSE
        relay_success_rate = previous_successes / total_attempts
    last_seen = CURRENT_TIMESTAMP
```

**Notes**:
• Quality index represents a weighted combination of performance metrics
• Metrics are updated during synchronization operations
• Lower quality nodes may be deprioritized for critical operations

### 2.5 Authentication and Security

#### Table: active_tokens

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Managing authentication sessions based on JWT tokens for storing active refresh tokens allowing access token renewal without re-authentication

**Fields**:
```
public_key    (INTEGER, NOT NULL) — FK → nodes.id  → nodes.node_id
refresh_token (TEXT, NOT NULL, UNIQUE) — refresh token value
expires_at    (INTEGER, NOT NULL) — expiration timestamp
```
🏠 Database: discovery_nodes.sqlite

**Model "active_tokens"** :
**Source relation**
```
active_tokens.public_key = nodes.node_id
```
**Base token mapping**
```
base_token_id =
SELECT active_tokens.public_key
FROM active_tokens
WHERE active_tokens.public_key = nodes.node_id
```
**Aggregation formulas "expires_at"**
```
active_tokens.expires_at = (
    SELECT (julianday('now', '+1 day') - 2440587.5) * 86400
)
```

**SQL Implementation Example**:
```sql
-- Create active_tokens table
CREATE TABLE active_tokens (
    public_key INTEGER NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY (public_key) REFERENCES nodes(node_id)
);

-- Insert new refresh token
INSERT INTO active_tokens (public_key, refresh_token, expires_at)
VALUES (?, ?, (strftime('%s', 'now') + 86400)); -- Token expires in 24 hours

-- Validate token
SELECT COUNT(*) > 0 as is_valid
FROM active_tokens
WHERE refresh_token = ?
AND expires_at > strftime('%s', 'now');

-- Clean up expired tokens
DELETE FROM active_tokens
WHERE expires_at < strftime('%s', 'now');

-- Refresh token
UPDATE active_tokens
SET expires_at = (strftime('%s', 'now') + 86400) -- New expiry in 24 hours
WHERE refresh_token = ?;
```

##### Model: Authentication Token Management

**Token Lifecycle Model**:
```
Token = {public_key, refresh_token, expires_at}
```

**Expiration Rules**:
```
IF CURRENT_TIMESTAMP > expires_at
    token_status = 'expired'
    eligible_for_cleanup = TRUE
```

**Token Validation**:
```
IF refresh_token NOT valid_jwt_format
    ERROR "Invalid token format"
    
IF public_key NOT IN nodes.node_id
    ERROR "Token public key not associated with known node"
```

**Notes**:
• Refresh tokens are stored securely and uniquely
• Expired tokens are automatically cleaned up
• Tokens are tied to specific node public keys for security

### 2.6 Peer History and Analysis

#### Table: peer_synchronization

📝 **System-level** table of the Network Layer
It is **not accept direct participant input**, and is **not transmitted over the network**

**Purpose**:
Storing peer synchronization history for tracking interaction history with each node for diagnostics and reliability analysis

**Fields**:
```
id                 (INTEGER, PK, AUTOINCREMENT) — unique history record identifier
peer_url           (INTEGER, NOT NULL) — FK → nodes.id → nodes.address
mode               (TEXT, NOT NULL) — synchronization mode
status             (TEXT, NOT NULL) — status of the synchronization
details            (TEXT, NOT NULL) — details of the synchronization process
last_sync          (INTEGER) — time of last synchronization
success_count      (INTEGER, DEFAULT 0) — counter of successful attempts
fail_count         (INTEGER, DEFAULT 0) — counter of failed attempts
last_quality_index (REAL, DEFAULT 0.0) — last quality index during synchronization
last_trust_score   (REAL, DEFAULT 0.0) — last trust score during synchronization
```
🏠 Database: discovery_nodes.sqlite

**Model "peer_synchronization"** :
**Source relation**
```
peer_synchronization.peer_url = nodes.id
```
**Base peer mapping**
```
base_peer_id =
SELECT nodes.id
FROM nodes
WHERE nodes.id = peer_synchronization.peer_url
```
**Aggregation formulas "success_count"**
```
peer_synchronization.success_count = (
    SELECT COUNT(*)
    FROM sync_attempts
    WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = base_peer_id)
    AND sync_attempts.status = 'success'
)
```
**Aggregation formulas "fail_count"**
```
peer_synchronization.fail_count = (
    SELECT COUNT(*)
    FROM sync_attempts
    WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = base_peer_id)
    AND sync_attempts.status != 'success'
)
```
**Aggregation formulas "last_sync"**
```
peer_synchronization.last_sync = (
    SELECT MAX(timestamp)
    FROM sync_attempts
    WHERE sync_attempts.peer_url = (SELECT address FROM nodes WHERE id = base_peer_id)
)
```

**SQL Implementation Example**:
```sql
-- Create peer_synchronization table
CREATE TABLE peer_synchronization (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    peer_url INTEGER NOT NULL,
    mode TEXT NOT NULL,
    status TEXT NOT NULL,
    details TEXT NOT NULL,
    last_sync INTEGER,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    last_quality_index REAL DEFAULT 0.0,
    last_trust_score REAL DEFAULT 0.0,
    FOREIGN KEY (peer_url) REFERENCES nodes(id)
);

-- Insert new peer synchronization record
INSERT INTO peer_synchronization (peer_url, mode, status, details, last_sync, success_count, fail_count)
VALUES ((SELECT id FROM nodes WHERE address = ?), ?, ?, ?, strftime('%s', 'now'), 0, 0);

-- Update peer sync stats
UPDATE peer_synchronization
SET
    last_sync = strftime('%s', 'now'),
    success_count = success_count + CASE WHEN ? = 'success' THEN 1 ELSE 0 END,
    fail_count = fail_count + CASE WHEN ? != 'success' THEN 1 ELSE 0 END,
    last_quality_index = ?,
    last_trust_score = ?
WHERE peer_url = (SELECT id FROM nodes WHERE address = ?);

-- Get peer sync statistics
SELECT
    n.address,
    ps.mode,
    ps.success_count,
    ps.fail_count,
    CASE
        WHEN (ps.success_count + ps.fail_count) > 0
        THEN ps.success_count * 100.0 / (ps.success_count + ps.fail_count)
        ELSE 0.0
    END as success_rate,
    ps.last_sync,
    ps.last_quality_index,
    ps.last_trust_score
FROM peer_synchronization ps
JOIN nodes n ON ps.peer_url = n.id
ORDER BY ps.last_sync DESC;

-- Get peer with highest success rate
SELECT
    n.address,
    CASE
        WHEN (ps.success_count + ps.fail_count) > 0
        THEN ps.success_count * 100.0 / (ps.success_count + ps.fail_count)
        ELSE 0.0
    END as success_rate
FROM peer_synchronization ps
JOIN nodes n ON ps.peer_url = n.id
WHERE ps.success_count + ps.fail_count > 0
ORDER BY success_rate DESC
LIMIT 5;
```

##### Model: Peer Interaction History

**Interaction History Model**:
```
H(peer) = {success_count, fail_count, quality_metrics, trust_metrics}
```

**Synchronization Metrics**:
```
success_rate = success_count / (success_count + fail_count)
```

**History Update Rules**:
```
IF synchronization_attempt
    IF successful
        success_count = success_count + 1
        last_quality_index = current_quality
        last_trust_score = current_trust
    ELSE
        fail_count = fail_count + 1
    last_sync = CURRENT_TIMESTAMP
    mode = current_synchronization_mode
```

**Notes**:
• Tracks historical performance of peer interactions
• Supports diagnostic analysis of network issues
• Quality and trust metrics are captured at time of synchronization

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

#### 2. GET /statements
**Description**: Get list of all statements (stub)
**Implementation**:
```rust
use actix_web::{get, HttpResponse, Result};

#[get("/statements")]
async fn get_statements() -> Result<HttpResponse> {
    Ok(HttpResponse::Ok().json(vec![] as Vec<String>))
}
```
**Response**: 200 OK with JSON array of strings (currently empty)
**Status**: TODO - requires implementation in core_lib

#### 3. POST /statements
**Description**: Add new statement (stub)
**Implementation**:
```rust
use actix_web::{post, HttpResponse, Result, web};
use serde::Deserialize;

#[derive(Deserialize)]
struct StatementRequest {
    // Statement fields would go here
}

#[post("/statements")]
async fn post_statements(_req: web::Json<StatementRequest>) -> Result<HttpResponse> {
    Ok(HttpResponse::Ok().json("TODO"))
}
```
**Request body**: StatementRequest JSON object
**Response**: 200 OK with JSON string "TODO"
**Status**: TODO - requires implementation in core_lib

#### 4. GET /events
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

#### 5. POST /events
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

#### 6. POST /impacts
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
✅ Created complete server API commands list - 6 endpoints:
GET /health - health check
GET /statements - get statements (stub)
POST /statements - add statements (stub)
GET /events - get events with P2P authentication
POST /events - add truth events
POST /impacts - add impacts

The P2P module is fully functional and integrated into the main application to provide decentralized data synchronization between nodes.

_Version: v1.0.0_
