# CLI Specification (truthctl)
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 10

## Overview
Administrative CLI over truth-core for synchronization, verification, and ratings.

## Commands
- sync — bidirectional/incremental/push/pull P2P sync
- verify — verify local data integrity and signatures
- ratings — show or recalc node/group ratings
  - ratings trust [--verbose] — show local/network trust levels and deltas
- status — summarizes node state (config, peers, recent sync logs)
- diagnose — health checks and environment diagnostics
- reset-data — clear local node data; optional reinit
## Status & Monitoring

Command:
```bash
truthctl status [--db PATH] [--identity FILE]
```

Behavior:
- Reads `~/.truthctl/config.json` (node_name, port, db_path)
- Reads `~/.truthctl/peers.json` (known peers)
- Opens SQLite DB and fetches last 5 rows from `sync_logs` (if present)

Output example:
```
Node: mynode (port 8080)
Database: truth.db
Peers: http://127.0.0.1:8080, http://10.0.0.2:8081 (+5 more)
Last sync events:
#42 2025-10-10T10:00:00Z http://127.0.0.1:8080 full ✅
   details: E10 S7 I3 C0
#41 2025-10-10T09:55:00Z http://10.0.0.2:8081 incremental ❌
   details: timeout
```

Notes:
- If DB or `sync_logs` is missing, print: `Sync: No sync history yet.` (yellow)
- Use colors: green for success, red for errors, yellow for warnings.
- keys — key management
- init-node — initialize node config and optional auto-peer registration
- peers — list/add peers; sync-all with known peers
- logs — show/clear persistent synchronization logs
 - config — manage `~/.truthctl/config.json`

## Diagnose

Command:
```bash
truthctl diagnose [--verbose]
```

Checks:
- Config — `~/.truthctl/config.json` exists and fields valid (`node_name`, `port`, `db_path`)
- Keys — `~/.truthctl/keys.json` exists with at least one valid 64-hex pair
- Peers — `~/.truthctl/peers.json` present and entries parsable
- Database — `db_path` exists
- P2P Feature — build flag `p2p-client-sync`

`--verbose` prints JSON with `config`, `peers`, `keys`.

## Reset Data and Reinit

Command:
```bash
truthctl reset-data [--confirm] [--reinit]
```

Behavior:
1. Deletes SQLite DB at `config.db_path` (if exists) and clears sync logs.
2. Removes `~/.truthctl/peers.json` if `--confirm` or user confirms.
3. Prints `🧹 Node data cleared successfully.`

`--reinit` flow:
1. If no keypair — generates new Ed25519, saves to `~/.truthctl/keys.json` (`🔑 New keypair generated.`).
2. If exists — prompts to keep or replace; `[2]` overwrites (`🔁 Keypair replaced.`).
3. Runs `truthctl init-node <node_name> --port <port> --db <db_path> --auto-peer` based on config; prints `🚀 Node reinitialized successfully.`

## Key Management

Store keys at `~/.truthctl/keys.json`:
```json
{
  "keys": [
    { "id": 1, "private_key_hex": "...", "public_key_hex": "...", "created_at": "2025-10-06T09:00:00Z" }
  ]
}
```

Commands:
```bash
a) Import keypair
truthctl keys import <priv_hex> <pub_hex>

b) List keys
truthctl keys list
```

Notes:
- `sync` and `verify` use the first available key by default if `--identity` is not provided.
- Keys are validated with `CryptoIdentity::from_keypair_hex` (Ed25519 hex).

## Node Initialization & Peers
`truthctl init-node <node_name> [--port <u16>] [--db <PATH>] [--auto-peer]`

Files:
- `~/.truthctl/config.json` — node_name, port, db_path, public_key, private_key
- `~/.truthctl/peers.json` — `{ "peers": [{ "url": "http://127.0.0.1:<port>", "public_key": "<hex>" }] }`

`--auto-peer` appends the node to peers.json if not present.

See also: `docs/CLI_Usage.md` for examples.

## Configuration Management
Command group to manage node configuration at `~/.truthctl/config.json`.

Commands:
```bash
truthctl config show
truthctl config set <key> <value>
truthctl config reset [--confirm]
```

Supported keys and semantics:
- `node_name` — string
- `port` — u16
- `database` — sets `db_path`
- `auto_peer` — boolean
- `p2p_enabled` — boolean

Behavior:
- `show`: prints pretty JSON
- `set`: validates key and writes file, creating `~/.truthctl/` as needed
- `reset`: writes defaults; preserves existing keys (public/private) if present; requires `--confirm`

## P2P Sync Integration
The CLI invokes `truth_core::p2p::sync` functions:
- `bidirectional_sync_with_peer`
- `incremental_sync_with_peer`
- `resolve_event_conflicts` (available for conflict inspection tooling)

Feature-gated with `p2p-client-sync`.

## Ratings Trust

Command:
```bash
truthctl ratings trust [--verbose]
```

Behavior:
- Prints local node trust (AVG of `node_ratings.trust_score`) and average network trust (`group_ratings.global.avg_score`).
- In verbose mode, shows samples with symbols: 🟢 + (increase), 🔴 – (decrease), ⚪ = (no change).
- Trust propagation is applied transparently during `/sync` and `/incremental_sync`.

## Sync Logs
Schema (SQLite table `sync_logs`):
```
id INTEGER PRIMARY KEY AUTOINCREMENT,
timestamp INTEGER,
peer_url TEXT,
mode TEXT,
status TEXT,
details TEXT
```

Commands:
```bash
truthctl logs show --limit 100 --db truth.db
truthctl logs clear --db truth.db
```
Entries are appended from `peers sync-all` after each peer attempt (success or failure).


