# Quickstart: Core Library Integration

This document provides step-by-step instructions for integrating and using the Truth Training Core library in your applications.

## Prerequisites

- Rust toolchain (≥ 1.75)
- Cargo package manager
- SQLite support (bundled with rusqlite)

## Installation

### As a Dependency

#### Step 1: Add to Cargo.toml
```toml
[dependencies]
truth_core = { path = "../truth-training" }
# Or from git:
# truth_core = { git = "https://github.com/ekwator/truth-training.git", tag = "v1.0.0" }
```

#### Step 2: Enable Features
```toml
[dependencies.truth_core]
features = ["desktop"]  # For desktop applications
# Or
features = ["mobile"]   # For mobile applications
```

### Build from Source

#### Step 1: Clone Repository
```bash
git clone https://github.com/ekwator/truth-training.git
cd truth-training
```

#### Step 2: Build Library
```bash
# Desktop features
cargo build --release --features desktop

# Mobile features
cargo build --release --features mobile
```

## Basic Usage

### Initialize Database

```rust
use core_lib::{init_db, open_db};

// Open or create database
let conn = open_db("truth_training.db")?;

// Initialize schema
init_db(&conn)?;
```

### Create Event

```rust
use core_lib::{add_truth_event, NewTruthEvent};
use chrono::Utc;

let event = NewTruthEvent {
    description: "Example event".to_string(),
    category_id: Some(1),
    forma_id: None,
    cause_id: None,
    develop_id: None,
    effect_id: None,
    vector: true,
    timestamp_start: Utc::now().timestamp(),
    code: 1,
};

let event_id = add_truth_event(&conn, event)?;
```

### Retrieve Event

```rust
use core_lib::get_truth_event;

let event = get_truth_event(&conn, event_id)?;
match event {
    Some(e) => println!("Event: {:?}", e),
    None => println!("Event not found"),
}
```

### Add Statement

```rust
use core_lib::{add_statement, NewStatement};

let statement = NewStatement {
    event_id: 1,
    text: "This is a statement".to_string(),
    context: Some("Additional context".to_string()),
    truth_score: Some(0.8),
};

let statement_id = add_statement(&conn, statement)?;
```

### Use Expert System

```rust
use core_lib::expert_simple::{questions_for_context, evaluate_answers};
use std::collections::HashMap;

// Get questions for context
let questions = questions_for_context("example_context");

// Evaluate answers
let mut answers = HashMap::new();
answers.insert("src_independent".to_string(), "yes".to_string());
answers.insert("alt_hypothesis".to_string(), "no".to_string());

let suggestion = evaluate_answers(&questions, &answers);
println!("Score: {}, Confidence: {}", suggestion.score, suggestion.confidence);
```

### Manage Context Templates

```rust
use core_lib::{add_context, get_context_by_name, NewContext};

// Create context template
let context = NewContext {
    name: "Example Context".to_string(),
    description: Some("Context description".to_string()),
    category_id: Some(1),
    forma_id: None,
    cause_id: None,
    develop_id: None,
    effect_id: None,
};

let context_id = add_context(&conn, context)?;

// Retrieve by name
let context = get_context_by_name(&conn, "Example Context")?;
```

### Node Management

```rust
use core_lib::{add_node, list_nodes, NewNode, NodeSource, NodeType};

// Add peer node
let node = NewNode {
    address: "http://192.168.1.100:8080".to_string(),
    node_type: NodeType::Leaf,
    source: NodeSource::Manual,
    node_id: Some("node-123".to_string()),
    ttl: 3600,
};

let node_id = add_node(&conn, node)?;

// List nodes
let nodes = list_nodes(&conn, None)?;
for node in nodes {
    println!("Node: {} at {}", node.id, node.address);
}
```

## Advanced Usage

### Trust Propagation

```rust
use core_lib::trust_propagation::{propagate_from_remote, compute_quality_index};

// Blend trust scores
let local_trust = 0.8;
let remote_trust = 0.6;
let blended = propagate_from_remote(local_trust, remote_trust);

// Compute quality index
let quality = compute_quality_index(&conn, node_id)?;
```

### Synchronization

```rust
use core_lib::sync::merge_node_lists;

// Merge local and remote node lists
let local_nodes = list_nodes(&conn, None)?;
let remote_nodes = vec![]; // From sync
let merged = merge_node_lists(local_nodes, remote_nodes);
```

### Collective Intelligence

```rust
use core_lib::recalc_collective_truth;

// Recalculate collective truth scores
recalc_collective_truth(&conn)?;
```

## Platform-Specific Features

### Desktop Features

```rust
#[cfg(feature = "desktop")]
use truth_core::api;

// Start HTTP server
#[cfg(feature = "desktop")]
async fn start_server() -> Result<(), Box<dyn std::error::Error>> {
    // Server implementation
    Ok(())
}
```

### Mobile Features

```rust
#[cfg(feature = "mobile")]
use truth_core::android;

// Use Android-specific functions
#[cfg(feature = "mobile")]
fn verify_json_signature(json: &str) -> bool {
    // Verification implementation
    true
}
```

## Error Handling

```rust
use core_lib::CoreError;

match add_truth_event(&conn, event) {
    Ok(id) => println!("Event created: {}", id),
    Err(CoreError::InvalidArg(msg)) => eprintln!("Invalid argument: {}", msg),
    Err(CoreError::DatabaseError(e)) => eprintln!("Database error: {}", e),
    Err(e) => eprintln!("Error: {}", e),
}
```

## Uninstallation

### Remove from Project

#### Step 1: Remove Dependency
```toml
# Remove from Cargo.toml
# [dependencies]
# truth_core = { ... }
```

#### Step 2: Clean Build
```bash
cargo clean
```

### Remove Database Files

```bash
# Remove database file
rm truth_training.db

# Or from code
std::fs::remove_file("truth_training.db")?;
```

## Data Backup

Before removing the library, backup your data:

```rust
use std::fs;

// Backup database
fs::copy("truth_training.db", "truth_training.db.backup")?;
```

## Configuration

### Database Path

```rust
// Default locations
// Linux: ~/.local/share/TruthTraining/truth_training.sqlite
// macOS: ~/Library/Application Support/TruthTraining/truth_training.sqlite
// Windows: %APPDATA%\TruthTraining\truth_training.sqlite

// Or specify custom path
let db_path = "custom/path/truth_training.db";
let conn = open_db(db_path)?;
```

## Troubleshooting

### Database Locked
```rust
// Check if connection is properly closed
// Use connection pooling for concurrent access
```

### Migration Issues
```rust
// v1.0.0 includes schema changes
// See docs/Deployment.md for migration instructions
```

### Feature Conflicts
```rust
// Ensure only one feature set is enabled
// Don't enable both "desktop" and "mobile" simultaneously
```

## Related Documentation

- [Core Functional Specification](../spec/22-function_core.md) - Complete API reference
- [Data Schema](Data_Schema.md) - Database schema documentation
- [Architecture Overview](../spec/03-architecture.md) - Architecture details
- [Cross-Platform Architecture](../spec/18-cross-platform-architecture.md) - Platform-specific features
- [Logging](logging.md) - Log file locations, reading, and clearing logs

_Version: v1.0.0_

