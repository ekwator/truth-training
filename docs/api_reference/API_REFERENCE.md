## Truth Training API Reference (v1.0.0)

**Audience:** Android, Desktop, and automation clients targeting Truth Core/Server v1.0.0.  
**Canonical source:** [specs/007-title-align-truth/contracts/openapi.yaml](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/contracts/openapi.yaml). Use that OpenAPI file for machine-consumable schemas; this document provides a human-readable overview and updated examples.

---

### 🔐 Authentication

| Endpoint | Purpose | Notes |
|----------|---------|-------|
| `POST /api/v1/auth` | Exchange credentials or signed challenge for a JWT. | Returns `access_token`, `refresh_token`, expiry metadata. |
| `POST /api/v1/refresh` | Refresh an expiring JWT. | Requires valid refresh token. |

All protected endpoints require `Authorization: Bearer <token>`. Tokens are scoped to a tenant; attach `X-Client-Platform: android|desktop` when debugging cross-platform sync.

Example request:
```json
POST /api/v1/auth
{
  "username": "demo",
  "password": "demo-password"
}
```

Example response:
```json
{
  "access_token": "<jwt>",
  "refresh_token": "<refresh>",
  "expires_in": 3600
}
```

---

### 🧱 Core system endpoints

- `GET /api/v1/info` — build metadata, node id, enabled features. Version string MUST be `1.0.0` for Android/Desktop parity.  
- `GET /api/v1/stats` — aggregate counts (events, impacts, judgments, peers).  
- `GET /graph/json` — visualization data (nodes + links) with filter parameters.

---

### 📋 Events API (embedded context fields)

All event payloads use embedded context identifiers (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`). Legacy `context_id` is no longer accepted.

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/events` | Paginated list, filters by `status`, `limit`, `offset`. |
| `POST /api/v1/events` | Create event. Validates FK references, date ranges, and template linkage. |
| `GET /api/v1/events/{id}` | Fetch detailed view including impacts, judgments, consensus. |
| `PUT /api/v1/events/{id}` | Update event fields; rejects stale versions via `updated_at` concurrency token. |
| `DELETE /api/v1/events/{id}` | Soft-delete; returns `204`. |

Create example:
```json
POST /api/v1/events
{
  "title": "Satellite failure briefing",
  "description": "Telemetry gap observed...",
  "category_id": "cat.communication",
  "forma_id": "forma.orbit",
  "cause_id": "cause.power",
  "develop_id": "dev.mitigation",
  "effect_id": "eff.training",
  "starts_at": "2025-11-04T09:00:00Z",
  "ends_at": "2025-11-04T12:00:00Z",
  "priority": "high",
  "status": "active"
}
```

---

### 🧩 Context Templates API

Provides CRUD plus intelligent matching for templates that prefill event fields.

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/contexts` | List templates. |
| `POST /api/v1/contexts` | Create template; enforces duplicate detection (`409`). |
| `GET /api/v1/contexts/by-name/{name}` | Lookup by unique name. |
| `POST /api/v1/contexts/match` | Suggest template for partially filled event; returns similarity scores. |
| `POST /api/v1/contexts/from-event` | Generate a template from an existing event payload. |

Matching example:
```json
POST /api/v1/contexts/match
{
  "category_id": "cat.communication",
  "forma_id": "forma.orbit",
  "cause_id": "cause.power"
}
```
Response:
```json
{
  "matches": [
    {
      "template_id": "tmpl.orbit.power",
      "score": 0.93,
      "fields": { "...": "..." }
    }
  ]
}
```

---

### ⚖️ Judgments & Consensus

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/judgments?event_id=...` | Paginated list of judgments for event. |
| `POST /api/v1/judgments` | Submit assessment (`true|false|uncertain`) with confidence + reasoning. |
| `GET /api/v1/judgments/stats/{event_id}` | Aggregated stats (counts, confidence averages). |
| `POST /api/v1/consensus/{event_id}/calculate` | Forces consensus recalculation (admin). |

Judgment payload includes Ed25519 signature of canonical JSON message; identical to Desktop workflow. Refer to **P2P Envelope** section below for signature formatting.

---

### 📊 Impacts & Knowledge

- `POST /api/v1/impacts` — Add impact assessment (level 1-5, justification).  
- `GET /api/v1/impacts?event_id=...` — Retrieve impacts.  
- `POST /api/v1/knowledge-base/import` — Bulk import curated context catalogues (used by Desktop and Android offline seeding).

---

### 🔄 Offline & Sync Endpoints

Android and Desktop operate against a local SQLite cache and rely on sync endpoints for reconciliation:

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/sync` | Push queued operations (events/contexts/judgments/impacts). |
| `POST /api/v1/sync/incremental` | Push only changes since `last_sync`. |
| `GET /api/v1/sync/pull` | Fetch remote changes; supports `since`, `limit`, and entity filters. |

Sync payloads contain an array of typed operations:
```json
{
  "last_sync": "2025-11-04T08:30:00Z",
  "operations": [
    {
      "entity": "event",
      "operation": "update",
      "payload": { ... }
    }
  ]
}
```

Conflict strategy is **local-wins**: the client queues local mutations, the server merges, and conflicting server data is re-fetched on the next pull.

---

### 📡 P2P Envelope (LAN sync & secure messaging)

Android retains Ed25519-signed envelopes for LAN P2P handoff, now aligned with Desktop:

```json
{
  "node_id": "device-1",
  "payload": {
    "action": "sync_ping",
    "nonce": 42,
    "events": []
  },
  "public_key": "<base64 32-byte ed25519 public key>",
  "signature": "<base64 64-byte signature over canonical payload>"
}
```

Verification rules:
- Serialize `payload` with canonical JSON (Rust: `serde_json::to_vec`).  
- Validate signature against `public_key`.  
- Reject mismatched key order / numeric format to prevent replay attacks.

Server responses:
```json
{ "status": "ok", "verified": true }
```
or
```json
{ "status": "error", "reason": "invalid_signature" }
```

---

### 🧭 Versioning & Compatibility Notes

- API version: `1.0.0`. Android v1.0.0 and Desktop v1.0.0 are the baseline clients.  
- Legacy v0.3.0 endpoints (`context_id`, missing templates/judgments) are **deprecated** and removed from this reference.  
- For schema details, refer to:
  - [specs/007-title-align-truth/contracts/openapi.yaml](https://github.com/ekwator/truth-training/blob/main/specs/007-title-align-truth/contracts/openapi.yaml)
  - [truth-android-client/README.md](../../truth-android-client/README.md) (networking section)
  - [docs/Truth-training/Truth-training.md](../Truth-training/Truth-training.md) (platform comparison)

---

### ✅ Testing & Tooling

- Contract tests validate every endpoint using the OpenAPI spec.  
- Postman collection: `integration/tests/postman/TruthTraining_v1_0_0.json` (auto-generated from OpenAPI).  
- CI ensures any change to `openapi.yaml` triggers schema validation and regenerates client bindings.

---

Need more detail? Sync the latest OpenAPI contract and regenerate client code:
```bash
cd specs/007-title-align-truth/contracts
openapi-generator generate -i openapi.yaml -g typescript-fetch -o ../../clients/ts
```

This keeps documentation, clients, and tests aligned with the v1.0.0 APIs.
