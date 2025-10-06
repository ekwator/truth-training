## P2P & Sync

Components
- UDP Beacons: discovery on port 37020. [src/net.rs]
- CryptoIdentity: Ed25519 keypair; sign/verify; hex helpers. [src/p2p/encryption.rs]
- Node: periodic sync loop. [src/p2p/node.rs]
- Sync flows: /get_data (pull), /sync (push), /incremental_sync (delta). [src/p2p/sync.rs, src/api.rs]

Security
- Signed requests with `X-Public-Key`, `X-Signature`, `X-Timestamp`.
- Message patterns (string to sign):
  - `sync_request:{ts}` for GET pull flows
  - `sync_push:{ts}` for POST /sync
  - `incremental_sync:{ts}` for POST /incremental_sync
- Future: per-item signatures and validator identity on impacts.

Roadmap
- Integrate conflict resolution into API/service layer.
- Apply incoming payloads to DB; idempotency and upserts.
- Add validator user_id to impacts and enforce sign/verify.

Request/Response formats

POST /sync
- Headers: X-Public-Key, X-Signature, X-Timestamp
- Body (JSON):
```json
{
  "events": [TruthEvent...],
  "statements": [Statement...],
  "impacts": [Impact...],
  "metrics": [ProgressMetrics...],
  "last_sync": 1710000000
}
```
- Response: SyncResult { conflicts_resolved, events_added, statements_added, impacts_added, errors }

POST /incremental_sync — same headers; body contains only recent changes.
