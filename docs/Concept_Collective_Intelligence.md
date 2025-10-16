## Collective Intelligence Layer (Wisdom of the Crowd)

The Collective Intelligence Layer aggregates multiple independent judgments into a unified collective truth score for each event: `collective_score ∈ [0,1]`.

- Principle: Independent evaluators submit votes as `impact` entries (`value` ∈ {0,1}). Outliers cancel each other; consensus emerges as more evaluations arrive.
- Aggregation: For MVP, `collective_score = AVG(impact.value)` per `event_id`. This aligns with the Event Rating Protocol’s `S_e` after remapping from `[-1,1]` to `[0,1]` via `S_e' = (S_e + 1)/2`.
- Relationship to existing metrics:
  - `trust_score` (per node) reflects validator reliability; future versions may weight votes by validator trust.
  - `quality_index` captures continuity/health of propagation and network consistency; it influences how quickly consensus spreads.
- Distribution through P2P: `collective_score` is stored in `truth_events.collective_score` and shared via sync payloads as part of the `TruthEvent` JSON. Nodes recompute locally and exchange values; convergence is iterative.

Example
- Users evaluate an event by posting `impact` entries. If votes split evenly, `collective_score ≈ 0.5`. With a strong majority of positive confirmations, `collective_score → 1.0`; with a strong majority of negative evaluations, `collective_score → 0.0`.

Roadmap
- Weight votes by validator trust (`W_v`) consistent with `spec/07-event-rating-protocol.md` (`S_e`).
- Integrate thresholds to influence `truth_events.code` transitions.
- Propagate and reconcile weighted consensus in the P2P layer.
