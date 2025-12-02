# Data Model: Truth Training Desktop UI — Text-Only Interface

## Entities

### Event
- id (UUID)
- name (string, required)
- description (string)
- context_id (string, required; from KB)
- start_date (date)
- end_date (date)
- created_at (datetime)
- updated_at (datetime)

Validation:
- name not empty
- context_id must reference KB item
- start_date <= end_date (if both present)

### Impact
- id (UUID)
- event_id (UUID, required)
- impact_level (integer 1..5)
- notes (string)
- created_at (datetime)

Validation:
- event_id references Event
- impact_level in [1..5]

### Summary
- id (UUID)
- event_id (UUID, unique)
- summary_text (string)
- recommendations (string)
- updated_at (datetime)

Validation:
- event_id references Event
- one Summary per Event

### Context (from Knowledge Base)
- id (string)
- label (string)
- path (string)
- description (string)

Note:
- Read-only, parsed from `[docs/Data_Schema.md](../../docs/Data_Schema.md)(d[ocs/Data_Schema.md](ocs/Data_Schema.md))`.

### LogEntry
- id (UUID)
- timestamp (datetime)
- source (enum: UI|CORE|CLIENT)
- level (enum: INFO|WARN|ERROR)
- message (string)

---

## Relationships
- Event 1—N Impact
- Event 1—1 Summary
- Event — Context (by context_id)

## State Transitions
- Event: Draft → Saved → Updated (timestamps track changes)
- Summary: Absent → Created → Updated

## Constraints
- No event save if KB empty or missing (Context required)
- Offline-first; conflicts: Local-wins
- Logs: pagination 35 lines/page

_Version: v1.0.0_
