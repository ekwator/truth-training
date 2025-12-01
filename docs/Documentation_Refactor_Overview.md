# Documentation Refactor — Overview

Use this checklist to run the complete documentation refactor workflow for Truth Training v1.0.0.

## Pipeline Phases

1. **Inventory** — scan every `*.md` file and produce `reports/doc_refactor/inventory.json`.
2. **Link Discovery & Validation** — normalize inline `.md` references, build `link_report.json`, and flag missing or external warnings.
3. **Version Sync** — ensure every doc surfaces `_Version: v1.0.0_`.
4. **Restructuring** — compress `[README.md](README.md)`, migrate verbose sections to `/docs`, archive orphans, and refresh index gateways.
5. **Duplicate Detection** — record overlapping blocks between `/docs` and `/spec` in `dedupe.json`.
6. **Spec Optimization** — condense `/spec` files, enforce AI directives, and remove duplicated blocks flagged as `needs_review`.

## Commands

From the repo root:

- Run everything end-to-end:

```bash
make doc-refactor-run
```

- Execute tests for the Python helpers:

```bash
make doc-refactor-test
```

- Run a single phase (example: link discovery):

```bash
python scripts/doc_refactor/main.py run --phases link_discovery
```

## Reports & Artifacts

| File | Description |
|------|-------------|
| `reports/doc_refactor/inventory.json` | Documentation node manifest with audience, version tags, and orphan flags. |
| `reports/doc_refactor/link_report.json` | Reference graph with `ReferenceEdge` status (`ok`, `missing`, `external_warning`). |
| `reports/doc_refactor/validation.json` | Summary of missing links, external warnings, and remaining plain paths. |
| `reports/doc_refactor/dedupe.json` | Duplicate block entries classified as `harmless` or `needs_review`. |
| `reports/doc_refactor/spec_opt.json` | Spec compression profiles and duplicate removal summary. |
| `reports/doc_refactor/run_summary.json` | Roll-up of the latest pipeline execution. |

## Success Criteria

- `[README.md](README.md)` summary ≤400 words and links into `/docs` and `/spec`.
- `[/docs/README.md](/docs/README.md)` indexes every non-archived human-facing doc.
- `[/spec/README.md](/spec/README.md)` directs AI agents to rely on `/spec` first and acknowledges the doc refactor spec.
- `link_report.json` contains zero `status: "missing"` entries before merging changes.
- `dedupe.json` contains only `classification: "harmless"` after spec optimization (any `needs_review` entries must be resolved or documented).

Keep this document close when triaging documentation PRs or running the release prep automation. It serves as the canonical entry point for the refactor workflow referenced by `[README.md](README.md)`.
