## Event Rating Protocol
Source: docs/event_rating_protocol.md

Status: Partially implemented.
- Implemented: truth_events.code (u8) field; impacts storage; basic recalc metrics.
- Implemented: optional per-record signatures (truth_events, statements, impact) with public_key for P2P verification.
- Missing (Next):
  - Weighted scoring S_e from validator votes (require impact.user_id and weights).
  - Reputation model (R_u, W_v) and threshold-based code transitions (T_up, T_down, T_confirm).
  - Propagation counter semantics for 8-bit code; reset 01→00 signed by author.
  - Persisted event_score and periodic recalc integration.

Action items
- Extend schema: impact.user_id TEXT, optional truth_events.event_score REAL.
- Implement recalc pipeline computing S_e and updating code + reputations.
- Sync: include signatures and aggregate hints, resolve conflicts; ensure verification on ingress.
