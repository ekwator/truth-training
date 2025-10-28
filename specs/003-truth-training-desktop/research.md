# Research: Truth Training Desktop UI — Text-Only Interface (Phase 0)

## Decisions
- Offline-first with Local-wins.
- Logs pagination: 35 lines/page.
- Export format: Plain text (.txt) fixed template.
- Context required: forbid saving without KB Context.

## Rationale
- Offline-first ensures usability without network; Local-wins preserves user work.
- 35 lines/page keeps performance predictable in text views.
- Plain text export is universally readable and scriptable.
- Requiring Context enforces data quality and consistent summaries.

## Alternatives Considered
- Last-writer-wins → risks data loss; rejected.
- Markdown export → richer but exceeds strict text-only requirement.
- Infinite scroll logs → memory/control concerns; pagination preferred.

## Open Items
- None
