## Test Plan

- Storage
  - init_db creates all tables idempotently
  - seed_knowledge_base inserts RU/EN references
- API
  - /init, /seed return 200
  - /events POST creates; GET requires signature (401 on invalid)
  - /impacts POST inserts row
  - /recalc inserts metrics row
  - /get_data returns arrays
  - /sync with valid headers returns SyncResult; missing/invalid headers → 400/401
  - /incremental_sync with valid headers returns SyncResult
- Expert
  - evaluate_answers extremes produce +/-1 with confidence ~1
  - unknown answers reduce confidence
- P2P
  - beacon sender/listener discover peers on localhost network
  - sync_with_peer performs GET /get_data (signed client-side), parses SyncData
  - push_local_data posts signed /sync; reconcile merges data by timestamps
  - incremental_sync_with_peer posts signed /incremental_sync with deltas

Security
- verify_signature success on correct message; failure on tampered message
- consistent message construction for `sync_request`, `sync_push`, `incremental_sync`
