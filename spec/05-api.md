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
- POST /incremental_sync (signed) → SyncResult

Notes
- Signed endpoints require Ed25519 signature of a message pattern documented in code.
- /get_data is unauthenticated (local/LAN debug). Avoid exposing publicly.

Future alignment
- Consider consolidating GET /events and GET /get_data, and adding pagination.
- Add OpenAPI in a follow-up.
