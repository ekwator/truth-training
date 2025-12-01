<!-- Archived from [docs/prompt/ekwator/specify2/spec1.md](docs/prompt/ekwator/specify2/spec1.md) -->

/speckit.constitution 
Task: Verify and update the project constitution (constitution.md) for the Truth Training project.
Mode: overwrite/merge existing constitution as needed. Do not ask questions. Use repository files as source of truth.

Requirements:
1) Scope update — cross-platform:
   - Ensure constitution explicitly declares project cross-platform scope covering:
     a) CLI (app/src/bin/truthctl.rs) — control & diagnostics for core.
     b) Server — autonomous node behavior (FidoNet-like concept, but fully automated, no sysop).
     c) Desktop UI — Linux/Windows/macOS (Tauri + glib/GTK on Linux), capable of offline and networked operation.
     d) Mobile clients — Android (Kotlin) and iOS (Swift) with same functional model as Desktop UI.

2) Source documents as authority:
   - When updating, reference and respect contents of: [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [CHANGELOG.md](CHANGELOG.md).
   - Record any deviations and justify in constitution change log section.

3) Releases & installation:
   - Constitution must require that releases are installable with clear choices: CLI, Server, Desktop UI, Mobile.
   - All install instructions must be documented in [README.md](README.md).
   - Release process must be automatable by the Cursor AI agent following the scenario in [CONTRIBUTING.md](CONTRIBUTING.md) (section "## 2. Release Preparation Requirements"); constitution must mandate creation/maintenance of release automation scripts and their documentation.

4) Dependency & vulnerability policy:
   - Add rule: proactively update vulnerable modules when a safe non-breaking upgrade exists; for platform-specific vulnerabilities (e.g., glib < 0.20.0 affecting GTK3/Linux), require immediate remediation strategy and documented conditional builds.
   - Require that linux/GTK builds include a periodic check for glib version and that release automation validates updated dependency versions.

5) Database & schema principles:
   - Constitution must mandate that project data schemas follow relational normal forms and data hygiene:
     - Enumerate required standards: 1NF, 2NF, 3NF, BCNF, 4NF, 5NF and note optional 6NF/DKNF for temporal/domain-driven cases.
     - Require unique keys, no unused tables, and documented migration strategy.
     - Require DB schema review as part of release checklist.

6) CI / tooling / automation:
   - Constitution must require that spec-kit artifacts, docs, release scripts, and validation checks exist and are runnable by the Cursor agent.
   - Record the canonical locations for constitution and spec-kit state (`.[specify/memory/constitution.md](specify/memory/constitution.md)`) and require updates there.

7) Security & privacy:
   - Keep pointer to [SECURITY.md](SECURITY.md); require periodic dependency scans (Dependabot/CodeQL or equivalent) and assign remediation SLAs in constitution.

Deliverable:
- Update (or create) `.[specify/memory/constitution.md](specify/memory/constitution.md)` with the revised constitution, including:
  - Short summary (purpose & scope)
  - Formal rules (numbered) covering items 1–7
  - Change history section with date and author "Cursor AI"
  - Acceptance criteria: constitution saved to `.[specify/memory/constitution.md](specify/memory/constitution.md)` and referenced by spec-kit commands.

Constraints:
- Do not modify code files.
- Use existing repository files for factual data.
- Do not prompt for clarification; apply the merge/overwrite that best preserves existing rules while extending scope per above.

End.

_Version: v1.0.0_

