# CLI Specification (truthctl)

## Overview
Administrative CLI over truth-core for synchronization, verification, and ratings.

## Commands
- sync — bidirectional/incremental/push/pull P2P sync
- verify — verify local data integrity and signatures
- ratings — show or recalc node/group ratings
- status — DB status, identity and basic stats
- keys — key management

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


