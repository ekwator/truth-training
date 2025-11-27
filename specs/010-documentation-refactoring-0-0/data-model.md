# Data Model & Processing Contracts

**Feature**: Documentation Refactoring v1.0.0  
**Date**: 2025-11-27  
**Status**: Draft (ready for /tasks)

## 1. Core Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| `DocumentationFile` | Canonical record for every Markdown file scanned during inventory. | `path` (Path), `slug`, `depth` (int), `role` (`README`, `INDEX`, `DETAIL`, `SPEC`, `ARCHIVE`), `version` (str), `word_count`, `inbound_links[]`, `outbound_links[]`, `flags` (`orphan`, `needs_archive`, `broken_links`) |
| `LinkEdge` | Directed edge representing `[label](target)` discovered in Markdown. | `source_path`, `target_path`, `label`, `is_external`, `status` (`ok`, `missing`, `needs_stub`, `verified`) |
| `LinkGraphReport` | Aggregated output for validators and dashboards. | `nodes` (list of `DocumentationFile` metadata), `edges` (list of `LinkEdge`), `orphans[]`, `broken_urls[]`, `plain_paths[]`, `stats` (counts per directory/role) |
| `SpecCompressionProfile` | Captures the compressed representation of each `/spec` file. | `path`, `section_order` (Goals/Constraints/Success/Links), `paragraph_lengths[]`, `ai_directive_present` (bool) |
| `RunArtifact` | Snapshot of a CLI phase execution. | `phase_name`, `started_at`, `finished_at`, `elapsed_sec`, `status`, `output_path` |

## 2. Allowed Roles & Depth Logic
- `README`: Depth forced to 0 regardless of physical location. Serves navigation.  
- `INDEX`: Depth 1–2 index files (e.g., `/docs/index.md`).  
- `DETAIL`: Depth 1–3 standard documentation pages.  
- `SPEC`: Files under `/spec`. Must remain compressed.  
- `ARCHIVE`: Anything under `docs/archive/` or explicitly marked historical. Not linked from navigation.

Depth calculation = `min(segments_under_root, 3)` to keep tree shallow. CLI will flag items exceeding depth 3 for manual review.

## 3. Phase Outputs

| Phase | Artifact(s) | Schema Highlights |
|-------|-------------|-------------------|
| `inventory` | `reports/doc_refactor/inventory.json` | Array of `DocumentationFile` objects sorted by path; includes exclusion reasons. |
| `link_discovery` | `reports/doc_refactor/link_graph.json` | `LinkGraphReport` capturing adjacency + unresolved references. |
| `validation` | `reports/doc_refactor/validation.json` | Broken link list, plain-path ledger, README word-count summary, depth violations. |
| `file_creation` | filesystem side-effects + `reports/doc_refactor/file_creation.json` | List of files created from templates with template source + relative path. |
| `version_sync` | `reports/doc_refactor/version_sync.json` | Files updated with `v1.0.0` markers. |
| `restructuring` | `reports/doc_refactor/restructure.json` | Mappings of moved sections (from README/spec → /docs). |
| `dedupe` | `reports/doc_refactor/dedupe.json` | Candidate duplicate clusters with similarity score, action taken. |
| `spec_opt` | `reports/doc_refactor/spec_opt.json` | `SpecCompressionProfile` array + directive enforcement status. |

## 4. CLI State Machine
```
INIT → INVENTORY → LINK_DISCOVERY → VALIDATION → FILE_CREATION → VERSION_SYNC → RESTRUCTURING → DEDUPE → SPEC_OPT → REPORT
```
- Each phase writes a `RunArtifact`.  
- CLI resumes from last successful phase if interrupted (reads latest `RunArtifact`).  
- Exit codes: `0` success, `1` recoverable validation failure (broken links remain), `>1` fatal.

## 5. Derived Metrics
- `coverage_ratio = (linked_files / total_files)` – must reach 100% for SC-004.  
- `spec_compression_score = mean(paragraph_lengths)` – target < 80 words.  
- `plain_path_count` – must be 0 for SC-002.  
- `readme_word_count` – must be between 500 and 700 for SC-003.  
- `orphan_count` – must be 0 post-archival (non-archived set).  
- `runtime_profile` – list of phase elapsed times (analytics only; no enforcement per FR-010).

## 6. Validation Rules
1. Any `DocumentationFile.version != "v1.0.0"` post-version-sync → error.  
2. Every `/spec` file must include "Use /spec before /docs" admonition + cross-link to support doc.  
3. README summary must expose `/docs` and `/spec` entry points and limit headings to navigation depth ≤2.  
4. External links flagged unreachable twice are rewritten as citations (no hyperlink).  
5. Duplicate clusters with similarity ≥0.92 require merge or archive before completion.

