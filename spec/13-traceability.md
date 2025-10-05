## Traceability Matrix (snapshot)

- Requirements → Code
  - Knowledge Base → core-lib/src/storage.rs (schema, seed_knowledge_base)
  - Event Tracking → core-lib/src/storage.rs (add_truth_event, set_event_detected)
  - Statements → core-lib/src/storage.rs (add_statement, getters)
  - Impacts → core-lib/src/storage.rs (add_impact)
  - Expert Heuristics → core-lib/src/expert_simple.rs; app/src/main.rs (assess)
  - Progress Metrics → core-lib/src/storage.rs (recalc_progress_metrics)
  - HTTP API → src/api.rs
  - P2P Discovery → src/net.rs
  - Sync → src/p2p/*.rs

- Docs → Spec
  - docs/Technical_Specification.md → spec/02-requirements.md
  - docs/Data_Schema.md → spec/04-data-model.md
  - docs/event_rating_protocol.md → spec/07-event-rating-protocol.md
  - docs/p2p_release.md → spec/08-p2p-sync.md
  - docs/ui_guidelines.md → spec/09-ux-guidelines.md
