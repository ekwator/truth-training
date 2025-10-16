## API Reference — Android JSON Signature Verification (Ed25519)

### Envelope format

```json
{
  "node_id": "device-1",
  "payload": { "action": "ping", "n": 1 },
  "signature": "<base64 Ed25519 signature>",
  "public_key": "<base64 Ed25519 public key>"
}
```

### Verification rules
- Подпись вычисляется строго над канонической сериализацией `payload` (`serde_json::to_vec` на стороне Rust).
- `public_key` — base64 сырых 32 байт Ed25519.
- `signature` — base64 сырых 64 байт Ed25519.

### Server responses
- Success (verified):
```json
{ "status": "ok", "verified": true }
```
- Invalid signature:
```json
{ "status": "error", "reason": "invalid_signature" }
```

### Notes
- Для корректной верификации важно, чтобы порядок ключей и формат чисел в `payload` совпадали с тем, что подписал Android.
## Truth Core API Reference (v0.3.0)

Audience: Android, Web, and CLI clients. Responses are JSON. Unless noted, Content-Type: `application/json; charset=utf-8`.

- Authentication uses JWT. Include header: `Authorization: Bearer <jwt>` where required.
- Time values are Unix timestamps in seconds unless otherwise stated.

### Auth

#### POST /api/v1/auth
- **Description**: Exchange a signed challenge or credentials for a short-lived JWT.
- **Headers**: `Content-Type: application/json`
- **Request**:
```json
{
  "username": "demo",
  "password": "demo-password"
}
```
- **Response** 200:
```json
{
  "access_token": "<jwt>",
  "expires_in": 3600,
  "refresh_token": "<refresh>"
}
```

#### POST /api/v1/refresh
- **Description**: Refresh an access token using a refresh token.
- **Headers**: `Content-Type: application/json`
- **Request**:
```json
{ "refresh_token": "<refresh>" }
```
- **Response** 200:
```json
{
  "access_token": "<jwt>",
  "expires_in": 3600
}
```

### Info

#### GET /api/v1/info
- **Description**: Basic node info and build metadata.
- **Headers**: none
- **Response** 200:
```json
{
  "name": "truth-core",
  "version": "0.3.0",
  "uptime_sec": 12345,
  "started_at": 1710000000,
  "features": ["p2p-client-sync", "jwt"],
  "peer_count": 3
}
```

### Stats

#### GET /api/v1/stats
- **Description**: Aggregated DB and network health metrics.
- **Headers**: optional `Authorization: Bearer <jwt>` if configured.
- **Response** 200:
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

### Graph

#### GET /graph/json
- **Description**: Network graph with nodes and links for visualization.
- **Headers**: optional `Authorization: Bearer <jwt>`
- **Query params**: `min_score`, `max_links`, `depth`, `min_priority`, `limit`
- **Response** 200:
```json
{
  "nodes": [
    {
      "id": "nodeA",
      "score": 0.78,
      "propagation_priority": 0.82,
      "last_seen": 1710000500,
      "relay_success_rate": 0.93
    }
  ],
  "links": [
    { "source": "nodeA", "target": "nodeB", "weight": 0.7, "latency_ms": 42 }
  ]
}
```

### Ratings

#### GET /ratings/nodes
- **Description**: List node ratings and trust scores.
- **Headers**: optional `Authorization: Bearer <jwt>`
- **Response** 200:
```json
[
  {
    "node_id": "nodeA",
    "trust_score": 0.65,
    "propagation_priority": 0.72,
    "last_updated": 1710000400
  }
]
```

#### GET /ratings/groups
- **Description**: List group ratings.
- **Headers**: optional `Authorization: Bearer <jwt>`
- **Response** 200:
```json
[
  {
    "group_id": "group-1",
    "avg_score": 0.58,
    "members": 5,
    "last_updated": 1710000400
  }
]
```

### Recalc

#### POST /recalc
- **Description**: Trigger recalculation of aggregates and propagation metrics.
- **Headers**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`
- **Request**:
```json
{ "reason": "manual" }
```
- **Response** 200:
```json
{ "status": "ok", "recalculated_at": 1710000600 }
```

#### POST /api/v1/recalc_collective
- **Description**: Recalculate `collective_score` for events from current `impact` votes.
- **Headers**: none
- **Request**: empty
- **Response** 200:
```json
{ "status": "ok" }
```

### P2P Sync (brief)

- `POST /sync`: push local data; signed headers `X-Public-Key`, `X-Signature`, `X-Timestamp`.
- `POST /incremental_sync`: push only recent changes since `last_sync`.
- `GET /get_data`, `GET /statements`: pull subsets for reconciliation.

Security notes:
- Use HTTPS in production.
- JWT must be included in `Authorization` header for protected endpoints.


