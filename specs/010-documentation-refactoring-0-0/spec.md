# Feature Specification: Documentation Refactoring v1.0.0

**Feature Branch**: `010-documentation-refactoring-0-0`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: Recreate spec for the documentation refactor to restore /clarify and /plan workflows.

## Overview

Truth Training’s documentation drifted across dozens of Markdown files with inconsistent structure, duplicate narratives, and broken links. Version v1.0.0 requires a deterministic refactor that rebuilds the information hierarchy, separates human-oriented docs (`/docs`) from AI-oriented specs (`/spec`), and enforces link integrity so automated validators and human readers share the same source of truth.

## Goals

- Establish a complete, validated inventory of every Markdown file in the repository (excluding `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md`).
- Normalize link references so no plaintext file paths remain and every link target exists (create the file when missing).
- Enforce “depth = detail” so top-level files remain high-level summaries while deeper paths hold the full context.
- Rewrite `README.md` to a concise (500–700 words) navigation hub that points to `/docs` and `/spec`.
- Compress `/spec` content into declarative, decision-oriented references optimized for AI agents, including guidance directing agents to consult `/spec` before `/docs`.
- Remove duplicate narratives by consolidating to canonical documents or archiving superseded copies under `docs/archive/`.
- Guarantee that the documentation graph has no orphaned Markdown files.

## Scope

### In Scope

- All tracked Markdown files within the repository root (except excluded list).
- Automatic document generation templates under `scripts/doc_refactor/templates/`.
- Link graph creation, validation, and repair.
- `/docs`, `/spec`, release notes, and README hierarchy updates.

### Out of Scope

- Non-Markdown assets (images, code snippets) beyond ensuring referenced paths remain valid.
- Product feature changes outside of documentation wording.
- Localized translations (English only for v1.0.0).

## User Scenarios & Testing

### User Story 1 – Documentation maintainer builds audited, linked tree (Priority P1)

- Maintainer inventories every `.md` file, classifies audience/role, converts plaintext paths, and generates missing targets.
- **Independent Test**: Run the CLI inventory + link pass; expect zero missing files and a processed-file list covering every Markdown path except exclusions.
- **Acceptance**:
  1. Given repo HEAD, when the audit runs, then the generated inventory enumerates 100% of Markdown files with role/depth metadata.
  2. Given any plaintext path detected, when the normalization pass runs, then the path becomes a Markdown link and the target file exists (created if needed).

### User Story 2 – Technical writer enforces hierarchy & README brevity (Priority P2)

- Writer moves verbose sections into `/docs`, keeps README succinct with navigation, and ensures `/docs` indices provide deep links.
- **Independent Test**: Count README words (500–700) and verify each detailed topic is linked to `/docs/*`.
- **Acceptance**:
  1. Given the prior README, when restructuring completes, then all sections from “## Release Information” downward move to `/docs` equivalents.
  2. Given any `/docs` subdirectory, when index files are opened, then they contain forward links to relevant deep documentation.

### User Story 3 – AI agent relies on optimized `/spec` (Priority P3)

- AI workflows read `/spec` first for decisions, ensuring content is compressed, declarative, and non-narrative.
- **Independent Test**: Scan `/spec` files to confirm ≤400 words each, bullet/outline format, and explicit instructions telling agents to prioritize `/spec`.
- **Acceptance**:
  1. Given any spec file, when validated, then it contains bullet decisions without narrative paragraphs.
  2. Given `spec/README.md`, when opened, then it instructs AI automations to read `/spec` before `/docs`.

## Edge Cases

- Linked files outside `/docs` or `/spec` (e.g., platform-specific directories) must either gain index coverage or be explicitly excluded with justification.
- External URLs that are unreachable must become text stubs noting the source and citing why direct linking failed, keeping validators satisfied.
- Historical duplicate docs must move to `docs/archive/` when not relevant to v1.0.0, preserving provenance but removing them from the main graph.

## Requirements

### Functional Requirements

- **FR-001**: Inventory generator MUST list every Markdown file with attributes (path, role, depth, audience, reachability) while skipping the excluded filenames.
- **FR-002**: Link traversal MUST start at `README.md`, recursively visit every Markdown link, and log processed files.
- **FR-003**: Reference normalizer MUST convert plaintext paths to Markdown links and instantiate missing targets using the appropriate template.
- **FR-004**: Restructuring workflow MUST move detailed content out of `README.md`, enforce 500–700 word limit, and add navigation sections linking into `/docs` and `/spec`.
- **FR-005**: Spec optimizer MUST compress `/spec` content to declarative bullet points (<400 words per file) and add instructions for AI agents in `spec/README.md`.
- **FR-006**: Duplicate detector MUST merge or archive redundant Markdown files, ensuring a single canonical source per topic.
- **FR-007**: Validation MUST confirm zero plain paths, zero broken links, explicit v1.0.0 identifiers, and that every Markdown file is reachable from `README.md`, `/docs`, or `/spec` indices.

### Non-Functional Requirements

- **NFR-001**: Refactor tooling must complete a full repository pass in under 15 minutes on a standard developer laptop (16 GB RAM).
- **NFR-002**: Generated files must remain ASCII unless existing content requires Unicode.
- **NFR-003**: Scripts must be idempotent so re-running them does not produce duplicate entries or oscillating diffs.

### Key Entities

- **DocumentationFile**: `path`, `role (README|Docs|Spec|Archive)`, `depth`, `audience (Human|AI)`, `links[]`, `version_status (Current|Historical)`.
- **LinkGraph**: Nodes reference `DocumentationFile`; edges represent resolved Markdown links. Supports DFS traversal and orphan detection.
- **ValidationReport**: Aggregates inventory stats, broken links, orphaned files, word counts, and spec compression compliance.

## Phases & Deliverables

1. **Phase 1 – Audit & Planning**  
   - Generate inventory of `.md` files and confirm `/docs` vs `/spec` split.  
   - Deliverables: `inventory.json`, initial link graph, exclusion confirmation.

2. **Phase 2 – Reference Integrity Pass**  
   - Convert plaintext paths, repair/buffer external links, create missing files using templates.  
   - Deliverables: updated Markdown files, list of created files, validated link graph.

3. **Phase 3 – Restructuring & Content Reorganization**  
   - Enforce hierarchy, rewrite README, move detailed content to `/docs`, deduplicate content.  
   - Deliverables: rewritten `README.md`, reorganized `/docs` tree, archive moves, dedup report.

4. **Phase 4 – Spec Optimization**  
   - Compress `/spec`, add AI guidance, ensure ≤9 optimized files with ≤400 words each.  
   - Deliverables: updated `/spec`, AI guidance snippet, validator proof.

5. **Phase 5 – Validation & Reporting**  
   - Run automated checks (SC-001…SC-004), regenerate link graph, confirm no orphaned files, produce final summary.  
   - Deliverables: validation logs, final report, instructions for maintainers.

## File Structure (Expected)

```
README.md
docs/
  README.md
  indices/...
  archive/...
spec/
  README.md
  01-product-vision.md
  02-requirements.md
scripts/doc_refactor/
  main.py
  utils/
  templates/
```

## Success Criteria

- **SC-001**: 100% of Markdown files reachable from `README.md`, `/docs` indices, or `/spec` indices in the link graph.
- **SC-002**: README word count 500–700, containing summary navigation only.
- **SC-003**: `/spec` contains ≤9 files, each ≤400 words, with explicit AI-agent guidance.
- **SC-004**: Validator reports zero plain-text paths, zero broken links, and consistent v1.0.0 identifiers.
- **SC-005**: Duplicate detector reports no unresolved duplicate groups (all merged or archived).

## Risks & Mitigations

- **Risk**: Aggressive deduplication could delete historical context → Mitigation: move legacy docs to `docs/archive/` with index links.  
- **Risk**: External URLs remain unreliable → Mitigation: convert to stub text citing the source with updated context.  
- **Risk**: README rewrite breaks onboarding expectations → Mitigation: provide a “Quick Start” card linking to the new `/docs` pages.

## Tooling & Automation

- Python CLI in `scripts/doc_refactor/main.py` orchestrating phases (`inventory`, `links`, `validate`, `create`, `update`, `organize`, `deduplicate`, `optimize`).
- `mistune` for Markdown parsing, TF-IDF for duplicate detection, `pathlib` for normalization.
- YAML-based templates for generating missing files.

## Clarifications

### Session 2025-11-27

- *(No clarifications captured yet — to be populated via `/clarify`.)*

---

This specification re-establishes the required structure so `/clarify`, `/plan`, and downstream workflows can proceed.
# Feature Specification: Documentation Refactoring v1.0.0

**Feature Branch**: `010-documentation-refactoring-0-0`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: User description: "Perform a full documentation refactoring for version v1.0.0, ensuring structural clarity, reference integrity, and separation between human-readable docs and AI-optimized specs."

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Documentation Maintainer builds audited, linked tree (Priority: P1)

A documentation maintainer needs to inventory every Markdown file, traverse links from `README.md`, convert plaintext paths to Markdown links, and ensure missing targets are created so the entire repo becomes link-complete for v1.0.0.

**Why this priority**: Link integrity is prerequisite for any further restructuring and prevents regressions in downstream consumers.

**Independent Test**: Run the refactoring CLI against the repo; it should output a processed-file list where every Markdown file is reachable from an index and no missing-link errors remain.

**Acceptance Scenarios**:

1. **Given** the repo at HEAD, **When** the maintainer runs the audit phase, **Then** the inventory reports every `.md` file except the excluded list.
2. **Given** a plaintext path in any processed file, **When** the maintainer executes the reference pass, **Then** that path becomes a clickable link and the target file exists or is generated.

---

### User Story 2 - Technical Writer enforces hierarchy & README brevity (Priority: P2)

A technical writer must reorganize content so top-level docs provide summaries with navigation while deep `/docs` files hold full explanations, and `README.md` becomes a concise entry point that links into `/docs` and `/spec`.

**Why this priority**: Users land on `README.md` first; without strict hierarchy the documentation remains overwhelming and inconsistent with v1.0.0.

**Independent Test**: Inspect `README.md` to confirm ≤700 words, only summary sections, and presence of links to reorganized `/docs` indices containing the moved detailed content.

**Acceptance Scenarios**:

1. **Given** the prior verbose README, **When** the reorganization completes, **Then** detailed sections live in `/docs` and README only contains summaries plus navigation.

---

### User Story 3 - AI Agent consumer relies on optimized /spec (Priority: P3)

An AI assistant must be able to read `/spec` files to obtain concise, decision-oriented context before referencing `/docs`, meaning `/spec` content is compressed, declarative, and free from narrative duplication.

**Why this priority**: Ensures AI-driven automations act deterministically and do not parse verbose human guidance first.

**Independent Test**: Verify `/spec/README.md` instructs agents to use `/spec` first, and spot-check spec files to confirm each section uses structured bullets and versioned decisions only.

**Acceptance Scenarios**:

1. **Given** a spec file containing narrative paragraphs, **When** the optimization pass runs, **Then** the file reduces to structured decision bullets under 400 words without story-like prose.

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- What happens when a linked file outside `/docs` or `/spec` (e.g., under `/android/`) is discovered? → Ensure the tool either excludes it or adds a bridging index entry without breaking platform-specific structure.
- How does system handle links to deprecated external URLs? → Replace with local stubs or add in-text notes while keeping the link policy consistent so validations pass.

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: Inventory generator MUST list every `.md` file with role, depth, and reachability while excluding `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, and `CHANGELOG.md`.
- **FR-002**: Link traversal MUST start from `README.md`, follow every Markdown link recursively, and record processed files.
- **FR-003**: Reference normalizer MUST convert all plaintext file paths to Markdown links and create placeholder files when targets do not exist.
- **FR-004**: Restructuring workflow MUST relocate detailed content into `/docs` and ensure `README.md` stays within 500–700 words.
- **FR-005**: Spec optimizer MUST compress `/spec` files into declarative bullet structures, deduplicate overlapping paragraphs, and add guidance directing AI agents to prefer `/spec` before `/docs`.
- **FR-006**: Validation MUST confirm zero broken links, no orphaned Markdown files, and consistent v1.0.0 identifiers in every processed document.

### Key Entities *(include if feature involves data)*

- **DocumentationFile**: Represents a Markdown file with attributes `path`, `role (README|Docs|Spec|Archive)`, `depth`, `audience`, `links`, and `version_status`.
- **LinkGraph**: Directed graph connecting documentation nodes; used for reachability, orphan detection, and DFS traversal order.

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: 100% of Markdown files reachable from `README.md`, `/docs` indices, or `/spec` indices in the generated link graph.
- **SC-002**: README word count between 500 and 700 and contains only summaries plus navigation links.
- **SC-003**: `/spec` directory contains ≤9 optimized files, each under 400 words, with explicit instruction for AI agents in `spec/README.md`.
- **SC-004**: Automated validator reports zero plaintext paths and zero broken links after the refactoring run.
