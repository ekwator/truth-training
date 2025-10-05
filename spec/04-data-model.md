## Data Model

Authoritative source: `docs/Data_Schema.md`.

Implemented tables
- knowledge_base: category, cause, develop, effect, forma, context, impact_type.
- base: truth_events (with code u8), impact, progress_metrics, statements.

Notes
- impact.id is UUID TEXT; created_at unix seconds.
- truth_events.detected is tri-state (NULL/0/1), corrected boolean, vector boolean.
- progress_metrics stores aggregate trend; MVP uses simple counts.

Gaps
- impact.user_id (validator) missing; planned per Event Rating Protocol.
- Optional event_score persistence not implemented.
