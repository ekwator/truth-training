## Event Rating Protocol
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

### Trust propagation

- Узловое доверие `trust_score` смешивается при синхронизации с удалёнными рейтингами:

  \[ new\_trust = local\_trust \cdot 0.8 + remote\_trust \cdot 0.2 \]

- Корректировка по времени: если `last_updated` старше 7 суток на момент применения, к значению применяется спад `×0.9`. Значение затем ограничивается в диапазоне [-1, 1].
- Распространение и спад выполняются прозрачно во время `/sync` и `/incremental_sync`.

Action items
- Extend schema: impact.user_id TEXT, optional truth_events.event_score REAL.
- Implement recalc pipeline computing S_e and updating code + reputations.
- Sync: include signatures and aggregate hints, resolve conflicts; ensure verification on ingress.
 - Expose read-only score/progress via API; align UI.
