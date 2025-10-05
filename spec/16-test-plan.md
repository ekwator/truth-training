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
- Expert
  - evaluate_answers extremes produce +/-1 with confidence ~1
  - unknown answers reduce confidence
- P2P
  - beacon sender/listener discover peers on localhost network
  - sync_with_peer performs signed GET /get_data
