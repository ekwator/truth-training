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
- GET /progress → list of progress_metrics rows
- GET /get_data → { events, impacts, metrics }
- POST /sync (signed) → SyncResult
  - Headers: X-Public-Key, X-Signature, X-Timestamp
  - Message signed: `sync_push:{ts}`
  - Body: SyncData { events, statements, impacts, metrics, last_sync }
- POST /incremental_sync (signed) → SyncResult
  - Headers: X-Public-Key, X-Signature, X-Timestamp
  - Message signed: `incremental_sync:{ts}`
  - Body: SyncData with recent changes only

Notes
- Signed endpoints require Ed25519 signature of the message pattern above.
- /get_data is unauthenticated (local/LAN debug). Avoid exposing publicly.

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
  "public_key": "hex|null"
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
  "errors": ["string"]
}
```
