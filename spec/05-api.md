# HTTP API (current implementation)
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 05

## HTTP API (current implementation)
Base URL: http://<host>:<port>/

- GET /health → 200 OK "OK"
- POST /init → initialize DB
- POST /seed { locale?: "ru"|"en" }
- GET /events (signed) → list of events; headers: X-Public-Key, X-Signature
- POST /events { description, context_id, vector }
- POST /impacts Impact
  - Impact: { id: string, event_id: string, type_id: number, value: boolean, notes?: string, created_at: number }
- GET /statements → list
- POST /statements { event_id, text, context?, truth_score? }
- POST /detect { event_id, detected, corrected? }
- POST /recalc → { status, metric_id }
- POST /api/v1/recalc_collective → { status: "ok" }
- GET /progress → list of progress_metrics rows
- GET /get_data → { events, impacts, metrics }
- POST /sync (signed) → SyncResult
  - Headers: X-Public-Key, X-Signature, X-Timestamp
  - Message signed: `sync_push:{ts}`
  - Body: SyncData { events, statements, impacts, metrics, node_ratings, group_ratings, node_metrics, last_sync }
- POST /incremental_sync (signed) → SyncResult
  - Headers: X-Public-Key, X-Signature, X-Timestamp
  - Message signed: `incremental_sync:{ts}`
  - Body: SyncData with recent changes only

Notes
- Signed endpoints require Ed25519 signature of the message pattern above.
- /get_data is unauthenticated (local/LAN debug). Avoid exposing publicly.

### Authentication & Tokens

- `POST /api/v1/auth`
  - Headers: `X-Public-Key`, `X-Signature`, `X-Timestamp`
  - Message to sign: `auth:{ts}`
  - Response 200:
    ```json
  { "access_token": "<jwt>", "refresh_token": "<refresh>", "token_type": "Bearer", "expires_in": 3600 }
    ```
  - 401: `{ "error": "unauthorized", "code": 401 }`

- `POST /api/v1/refresh`
  - Body: `{ "refresh_token": "<refresh>" }`
  - Response 200: same as auth (rotated refresh)
  - 401 on invalid/expired refresh

Protected endpoints (require header `Authorization: Bearer <jwt>`):
- `POST /api/v1/recalc`
- `POST /api/v1/ratings/sync`
- `POST /api/v1/reset`
- `POST /api/v1/reinit`

### RBAC and Trust Delegation

- `GET /api/v1/users` (admin only)
  - Returns: array of `RbacUser` with `pubkey`, `role` (`admin|node|observer`), `trust_score`

- `POST /api/v1/users/role` (admin only)
  - Body: `{ "pubkey": "<hex>", "role": "admin|node|observer" }`
  - Sets role or creates user if absent.

- `POST /api/v1/trust/delegate` (role >= node)
  - Body: `{ "target_pubkey": "<hex>", "delta": 0.1 }` (|delta| ≤ 0.2, not self)
  - Adjusts target trust score locally; propagated via ratings sync.

Role hierarchy (implied permissions): `admin → node → observer`.

Mermaid:

```mermaid
graph TD
    A["Admin"] --> B["Node"]
    B --> C["Observer"]
    B --> D["Peer Node"]
    A -->|"delegates trust"| D
```

JWT Claims include role and trust_score:
```json
{
  "sub": "<pubkey>",
  "exp": 1710003600,
  "iat": 1710000000,
  "role": "node",
  "trust_score": 0.42
}
```

Future alignment
- Consider consolidating GET /events and GET /get_data, and adding pagination.
- Add OpenAPI in a follow-up.

### JSON Schemas (informal)

TruthEvent
```json
{
  "id": 1,
  "description": "string",
  "context_id": 1,
  "vector": true,
  "detected": null,
  "corrected": false,
  "timestamp_start": 1710000000,
  "timestamp_end": null,
  "code": 1,
  "signature": "hex|null",
  "public_key": "hex|null",
  "collective_score": 0.75
}
```

Statement
```json
{
  "id": 1,
  "event_id": 1,
  "text": "string",
  "context": "string|null",
  "truth_score": 0.5,
  "created_at": 1710000000,
  "updated_at": 1710000000,
  "signature": "hex|null",
  "public_key": "hex|null"
}
```

Impact
```json
{
  "id": "uuid",
  "event_id": "1",
  "type_id": 1,
  "value": true,
  "notes": "string|null",
  "created_at": 1710000000,
  "signature": "hex|null",
  "public_key": "hex|null"
}
```

ProgressMetrics
```json
{
  "id": 1,
  "timestamp": 1710000000,
  "total_events": 10,
  "total_events_group": 10,
  "total_positive_impact": 1.0,
  "total_positive_impact_group": 1.0,
  "total_negative_impact": 0.0,
  "total_negative_impact_group": 0.0,
  "trend": 1.0,
  "trend_group": 1.0
}
```

SyncData
```json
{
  "events": [/* TruthEvent[] */],
  "statements": [/* Statement[] */],
  "impacts": [/* Impact[] */],
  "metrics": [/* ProgressMetrics[] */],
  "node_ratings": [/* NodeRating[] */],
  "group_ratings": [/* GroupRating[] */],
  "node_metrics": [/* NodeMetrics[]; includes relay_success_rate, quality_index, propagation_priority */],
  "last_sync": 1710000000
}
```

SyncResult
```json
{
  "conflicts_resolved": 0,
  "events_added": 0,
  "statements_added": 0,
  "impacts_added": 0,
  "errors": ["string"],
  "nodes_trust_changed": 0,
  "trust_diff": [{"node_id":"hex","delta":0.1}],
  "avg_quality_index": 0.82
}
```
