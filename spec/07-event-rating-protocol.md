# Event Rating Protocol
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 07
Source: docs/event_rating_protocol.md

Status: MVP logic in place; full protocol pending.
- Implemented (MVP):
  - `truth_events.code` (u8) control/counter field.
  - Optional per-record signatures (`truth_events`, `statements`, `impact`) for P2P verification.
  - `statements.truth_score` optional per-statement scalar; `progress_metrics` aggregates via `/recalc`.
- Missing (Next):
  - Weighted event score S_e from validator votes (requires `impact.user_id`, weights).
  - Reputation model (R_u, W_v) and threshold-based code transitions (T_up, T_down, T_confirm).
  - Propagation counter semantics for 8-bit code; author-signed reset 01→00.
  - Persisted `event_score` and periodic recalc integration.

Action items
- Extend schema: impact.user_id TEXT, optional truth_events.event_score REAL.
- Implement recalc pipeline computing S_e and updating code + reputations.
- Sync: include signatures and aggregate hints, resolve conflicts; ensure verification on ingress.
 - Expose read-only score/progress via API; align UI.
