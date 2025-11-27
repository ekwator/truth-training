
# Data Model: Constitution Compliance Changes

## Entities

### TruthEvent
- id: UUID
- payload: JSON/Text (may include confession content)
- created_at: timestamp
- envelope_signature: bytes (from transport envelope)
- envelope_sender_node_id: string (public key or node id)
- status_weight: float (derived; accumulated confirmation weight)
- decay_score: float (derived; inconsistency decay)

Constraints:
- No author metadata stored in the event body.
- Validation uses envelope signature and node id; reject if anti-replay fails.

### Judgment
- id: UUID
- event_id: UUID (fk TruthEvent)
- signal: enum { confirm, reject, abstain }
- envelope_sender_node_id: string (unique per envelope)
- created_at: timestamp

Constraints:
- Independence = distinct envelope_sender_node_id within anti-replay window.

### NodeReputation
- node_id: string
- accuracy_score: float
- last_updated: timestamp

### SyncEnvelope
- envelope_id: string (unique)
- sender_node_id: string
- signature: bytes
- payload_hash: bytes
- received_at: timestamp

## Storage Policies
- Anonymous confessions: plaintext at rest; rely on TLS in transport.

## Derived Calculations
- status_weight = sum(weights(confirm)) - sum(weights(reject))
- decay_score increases with inconsistent signals over time.

_Version: v1.0.0_
