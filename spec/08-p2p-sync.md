## P2P & Sync

Components
- UDP Beacons: discovery on port 37020. [src/net.rs]
- CryptoIdentity: Ed25519 keypair; sign/verify; hex helpers. [src/p2p/encryption.rs]
- Node: periodic sync loop. [src/p2p/node.rs]
- Sync flows: /get_data (pull), /sync (push), /incremental_sync (delta). [src/p2p/sync.rs, src/api.rs]

Security
- Signed requests with X-Public-Key and X-Signature; message patterns: sync_request, sync_push, incremental_sync with timestamp.
- Future: per-item signatures and validator identity on impacts.

Roadmap
- Integrate conflict resolution into API/service layer.
- Apply incoming payloads to DB; idempotency and upserts.
- Add validator user_id to impacts and enforce sign/verify.
