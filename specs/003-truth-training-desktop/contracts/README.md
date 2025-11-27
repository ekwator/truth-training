# Contracts: Truth Training Desktop UI — Text-Only Interface

Contract surfaces focus on Tauri command boundaries (desktop UI ↔ backend) and file export.

## Tauri Commands (conceptual)
- get_versions() -> { core: string, ui: string }
- get_sync_status() -> { status: string, last_sync: string }
- list_clients() -> Client[]
- list_kb_contexts() -> Context[]
- create_event(input) -> Event
- list_events(filter?) -> Event[]
- get_event(id) -> Event
- add_impact(event_id, payload) -> Impact
- get_summary(event_id) -> Summary
- save_summary(event_id, payload) -> Summary
- list_logs(page, page_size=35) -> { items: LogEntry[], page, total }
- clear_logs() -> void
- export_overall_summary_txt(filter?) -> { path: string }

Note: Exact signatures to be refined to match existing codebase patterns. Tests will assert schemas.

## File Export
- Overall Summary exported to `.txt` file using fixed template.

_Version: v1.0.0_
