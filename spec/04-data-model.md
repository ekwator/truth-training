# Data Model

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 04

Authoritative source: [docs/Data_Schema.md](../docs/Data_Schema.md).

Implemented tables
- knowledge_base: category, cause, develop, effect, forma, context, impact_type.
- base: truth_events (with embedded context fields: category_id, forma_id, cause_id, develop_id, effect_id; code u8, collective_score REAL NULL), impact, progress_metrics, statements.

Notes
- impact.id is INTEGER (PK, AUTOINCREMENT); created_at unix seconds.
- truth_events.detected is tri-state (NULL/0/1), corrected boolean, vector boolean.
- truth_events: context fields embedded directly (category_id, forma_id, cause_id, develop_id, effect_id) - context_id removed in v1.0.0.
- progress_metrics stores aggregate trend; MVP uses simple counts.

Gaps
- impact.user_id (validator) missing; planned per Event Rating Protocol.
- Optional event_score persistence not implemented.

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.
