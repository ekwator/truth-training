# Quickstart: CLI Installation and Usage

This document provides step-by-step instructions for installing, using, and uninstalling the Truth Training CLI (`truthctl`) tool.

## Prerequisites

- Rust toolchain (≥ 1.75)
- Cargo package manager
- Network access for P2P synchronization

## Installation

### From Source

#### Step 1: Clone Repository
```bash
git clone https://github.com/ekwator/truth-training.git
cd truth-training
```

#### Step 2: Build CLI
```bash
cargo build --release --bin truthctl --features p2p-client-sync
```

#### Step 3: Install Binary (Optional)
```bash
# Install to system path
sudo cp target/release/truthctl /usr/local/bin/

# Or add to local bin
cp target/release/truthctl ~/.local/bin/
export PATH="$HOME/.local/bin:$PATH"
```

#### Step 4: Verify Installation
```bash
truthctl --version
# Expected: truthctl v1.0.0

truthctl --help
# Expected: Command help output
```

### From Pre-built Binary

#### Linux/macOS
```bash
# Download binary from releases
wget https://github.com/ekwator/truth-training/releases/download/v1.0.0/truthctl-linux-x86_64
chmod +x truthctl-linux-x86_64
sudo mv truthctl-linux-x86_64 /usr/local/bin/truthctl
```

#### Windows
```powershell
# Download binary from releases
Invoke-WebRequest -Uri "https://github.com/ekwator/truth-training/releases/download/v1.0.0/truthctl-windows-x86_64.exe" -OutFile "truthctl.exe"
# Move to PATH or use from current directory
```

## Initial Setup

### Step 1: Initialize Database
```bash
truthctl --db truth.db init
# Expected: Database schema created
```

### Step 2: Generate Identity
```bash
truthctl --db truth.db --identity keys/node1.json init-node
# Expected: Identity file created with Ed25519 key pair
```

### Step 3: Configure Node
```bash
truthctl config set --node-name "my-node" --port 8080
# Expected: Configuration saved to ~/.truthctl/config.json
```

### Step 4: Seed Knowledge Base (Optional)
```bash
truthctl --db truth.db seed --locale ru
# Or
truthctl --db truth.db seed --locale en
# Expected: Knowledge base seeded with reference data
```

## Basic Usage

### Node Status
```bash
truthctl --db truth.db --identity keys/node1.json status
```

**Output includes:**
- Node name and port (from `~/.truthctl/config.json`)
- Database path
- Number of peers (from `~/.truthctl/peers.json`)
- Last 5 sync records from `sync_logs` table
- Network metrics: average `propagation_priority`, average `relay_success_rate`, average `quality_index`

### Add Peer
```bash
truthctl peers add --address "http://192.168.1.100:8080" --name "peer-node"
# Expected: Peer added to ~/.truthctl/peers.json
```

### List Peers
```bash
truthctl peers list
# Expected: List of configured peers
```

### Synchronize with Peer
```bash
# Full sync
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode full

# Incremental sync
truthctl sync --peer http://127.0.0.1:8080 --identity keys/node1.json --mode incremental
```

### Node Discovery
```bash
# List discovered nodes
truthctl nodes list --db truth.db

# Discover nodes
truthctl nodes discover --db truth.db

# Add node manually
truthctl nodes add --db truth.db \
  --address "http://192.168.1.100:8080/api/v1" \
  --type lan \
  --ttl 120
```

### View Sync Logs
```bash
truthctl logs show --db truth.db
# Expected: Recent sync operation logs

truthctl logs clear --db truth.db
# Expected: All sync logs cleared
```

### Network Graph
```bash
# ASCII graph
truthctl graph show --db truth.db --format ascii

# JSON graph
truthctl graph show --db truth.db --format json
```

### Diagnostics
```bash
# Run diagnostics
truthctl diagnose --db truth.db --server

# Expected: Health checks for API, database, and P2P
```

## Advanced Usage

### Configuration Management
```bash
# Show current configuration
truthctl config show

# Set configuration values
truthctl config set --node-name "new-name" --port 9090

# Reset to defaults
truthctl config reset
```

### Peer Statistics
```bash
# Show peer statistics
truthctl peers stats --db truth.db

# Show peer history
truthctl peers history --db truth.db --peer "http://192.168.1.100:8080"
```

### Data Management
```bash
# Reset local data
truthctl reset-data --db truth.db

# Reset and reinitialize
truthctl reset-data --db truth.db --reinit
```

## Uninstallation

### Step 1: Remove Binary

#### Linux/macOS
```bash
# If installed to system path
sudo rm /usr/local/bin/truthctl

# If installed to local bin
rm ~/.local/bin/truthctl
```

#### Windows
```powershell
# Remove from PATH or delete executable
Remove-Item truthctl.exe
```

### Step 2: Remove Configuration (Optional)

#### Linux/macOS
```bash
# Remove CLI configuration
rm -rf ~/.truthctl/
```

#### Windows
```powershell
# Remove CLI configuration
Remove-Item -Recurse -Force "$env:USERPROFILE\.truthctl"
```

### Step 3: Remove Database Files (Optional)

**Note:** Database files are typically in the current directory or specified via `--db` flag. Remove them manually if needed:

```bash
# Remove database file
rm truth.db
# Or
rm ~/truth_training.db
```

### Step 4: Verify Removal
```bash
truthctl --version
# Expected: Command not found

ls ~/.truthctl/
# Expected: Directory does not exist (if removed)
```

## Data Backup

Before uninstalling, backup your data:

```bash
# Backup configuration
cp -r ~/.truthctl ~/.truthctl.backup

# Backup database
cp truth.db truth.db.backup

# Backup identity keys
cp -r keys keys.backup
```

## Configuration Locations

| Platform | Config Location | Peers File |
|----------|----------------|------------|
| Linux | `~/.truthctl/config.json` | `~/.truthctl/peers.json` |
| macOS | `~/.truthctl/config.json` | `~/.truthctl/peers.json` |
| Windows | `%USERPROFILE%\.truthctl\config.json` | `%USERPROFILE%\.truthctl\peers.json` |

## Troubleshooting

### Database Locked
```bash
# Check if another process is using the database
lsof truth.db

# Or on Windows
# Use Process Explorer to find processes using truth.db
```

### Identity File Not Found
```bash
# Generate new identity
truthctl --db truth.db --identity keys/node1.json init-node
```

### Peer Connection Failed
```bash
# Check peer reachability
curl http://192.168.1.100:8080/api/v1/info

# Verify firewall settings
# Check network connectivity
```

### Sync Errors
```bash
# Check sync logs
truthctl logs show --db truth.db

# Run diagnostics
truthctl diagnose --db truth.db
```

## Related Documentation

- [CLI Usage](CLI_Usage.md) - Complete CLI reference
- [P2P & Sync](../spec/08-p2p-sync.md) - Synchronization protocol
- [Node Discovery](../spec/18-cross-platform-architecture.md) - Discovery mechanisms

_Version: v1.0.0_

