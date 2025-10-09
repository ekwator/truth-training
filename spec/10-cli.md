# CLI Specification (truthctl)

## Overview
Administrative CLI over truth-core for synchronization, verification, and ratings.

## Commands
- sync — bidirectional/incremental/push/pull P2P sync
- verify — verify local data integrity and signatures
- ratings — show or recalc node/group ratings
- status — DB status, identity and basic stats
- keys — key management
- init-node — initialize node config and optional auto-peer registration
- peers — list/add peers; sync-all with known peers

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

## P2P Sync Integration
The CLI invokes `truth_core::p2p::sync` functions:
- `bidirectional_sync_with_peer`
- `incremental_sync_with_peer`
- `resolve_event_conflicts` (available for conflict inspection tooling)

Feature-gated with `p2p-client-sync`.


