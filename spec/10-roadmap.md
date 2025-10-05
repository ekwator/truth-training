## Roadmap (High-level)

Milestone M1 (MVP harden)
- Align docs and API names (done).
- Add OpenAPI and examples.
- Apply incoming sync to DB; idempotent upserts.

Milestone M2 (Event Rating v1)
- Add impact.user_id; compute S_e; update code transitions.
- Persist event_score; scheduled recalc.
- Basic validator reputation updates.

Milestone M3 (Security & Sync)
- Per-item signatures; conflict resolution in service.
- Reset flow 01→00 (author-signed).

Milestone M4 (UX/DevEx)
- CI gates; reproducible builds; tests.
- UI prototypes (web/tui) against API.
