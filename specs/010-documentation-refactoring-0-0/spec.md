# Feature Specification: Documentation Refactoring v1.0.0

**Feature Branch**: `010-documentation-refactoring-0-0`  
**Created**: 2025-11-27  
**Status**: Draft  
**Input**: User description: "— Documentation Refactoring v1.0.0"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Repository-wide audit & planning (Priority: P1)

As a release steward I need an automated Phase 1 that inventories every Markdown file (excluding `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md`) and confirms `/docs` vs `/spec` boundaries so the refactor begins with a complete, trustworthy map.

**Why this priority**: Without a full inventory, later phases cannot guarantee link integrity or hierarchy, jeopardizing SC-001..SC-008.

**Independent Test**: Run the audit CLI and verify the inventory lists 100% of `.md` files, depth, role, and exclusion status.

**Acceptance Scenarios**:

1. **Given** the repo root, **When** Phase 1 runs, **Then** it outputs an inventory covering every `.md` file with depth/role metadata plus flags for excluded docs.
2. **Given** content violating the `/docs` (human) or `/spec` (AI) boundaries, **When** audit completes, **Then** it reports violations for remediation.

---

### User Story 2 - Reference integrity sweep (Priority: P1)

As a documentation maintainer I need Phase 2 to start at `README.md`, traverse every Markdown link, convert plaintext `.md` paths into Markdown links, create missing targets with v1.0.0 content, and ensure no file remains orphaned.

**Why this priority**: Broken links or unreachable content fail SC-001/SC-002 and block the release.

**Independent Test**: Run the link validator post-phase; it must exit clean with zero warnings and regex scan finds no plaintext `.md` strings.

**Acceptance Scenarios**:

1. **Given** raw text like `spec/01-product-vision.md`, **When** Phase 2 runs, **Then** it becomes `[spec/01-product-vision.md](spec/01-product-vision.md)` with the destination verified or generated.
2. **Given** orphaned Markdown files, **When** traversal finishes, **Then** each file links from `README.md`, `/docs` indexes, `/spec` indexes, or moves to `docs/archive/` with the archive template.

---

### User Story 3 - Restructuring & /spec optimization (Priority: P2)

As a reader or AI agent I need high-level documentation—especially `README.md`—to remain 500–700 words of summaries + navigation, `/docs` to hold detailed human text, and `/spec` to provide condensed decision directives (with "Use /spec before /docs") so I can consume the right depth quickly.

**Why this priority**: Depth/detail mismatch and duplication slow onboarding and confuse automated tooling.

**Independent Test**: Measure `README.md` length, inspect reorganized `/docs` and `/spec`, and confirm spec README instructs AI agents to consult `/spec` first.

**Acceptance Scenarios**:

1. **Given** detailed sections (e.g., `## Release Information`), **When** restructuring completes, **Then** they relocate into `/docs/*` with concise summaries and links left in `README.md`.
2. **Given** narrative `/spec` files, **When** optimization completes, **Then** each spec compresses into goals/constraints/success criteria (<80 words per paragraph) referencing `/docs` and explicitly tagging `v1.0.0`.

---

### Edge Cases

- Duplicate numeric prefixes in `specs/*` (e.g., two `001-*`) must halt the workflow until resolved.
- Missing Markdown targets encountered during traversal must be created via templates with v1.0.0 scaffolding before proceeding.
- External URLs failing validation require replacement, authoritative citation, or `<!-- verified: reachable -->` annotations per policy.
- Historical/pre-v1.0.0 documents discovered mid-run must move to `docs/archive/` and exit active navigation.
- Excluded files should be logged but untouched even if linked.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Phase 1 MUST enumerate all `.md` files, capture depth/role metadata, and skip refactor actions on the four excluded docs.
- **FR-002**: Phase 1 MUST confirm `/docs` hosts human-readable depth and `/spec` hosts AI-optimized summaries, flagging deviations.
- **FR-003**: Phase 2 MUST traverse every Markdown link from `README.md`, convert plaintext `.md` strings into `[label](path.md)` links, and create missing targets with v1.0.0 content.
- **FR-004**: Phase 2 MUST maintain a processed-file ledger ensuring no Markdown file remains orphaned.
- **FR-005**: Phase 2 MUST fix or annotate external URLs according to policy (replacement/stub/verification).
- **FR-006**: Phase 3 MUST enforce depth=detail by moving detailed sections into `/docs`, archiving historical docs, and compressing `/spec` content with explicit AI guidance.
- **FR-007**: Phase 3 MUST deduplicate content so canonical versions live in `/docs` and `/spec` references remain concise.
- **FR-008**: All documentation MUST declare version `v1.0.0` consistently after restructuring.
- **FR-009**: Deliverables MUST include updated tree, diffs, change explanation, and a verification checklist covering link integrity, structure, readability, and `/docs` vs `/spec` separation.
- **FR-010**: Full refactor CLI execution has no strict runtime limit; successful completion takes precedence over duration targets.

### Key Entities *(include if feature involves data)*

- **DocumentationFile**: Metadata record for each `.md` file (path, depth, role, version tag, inbound/outbound links, archive status).
- **LinkGraph**: Directed graph used during validation to detect broken links and orphans.
- **SpecTemplate**: Structured format for AI-oriented specs enforcing concise decision-focused sections with cross-references to `/docs`.
- **ArchiveEntry**: Metadata descriptor for historical docs relocated under `docs/archive/` with provenance notes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Link validator finishes with zero broken internal/external links.
- **SC-002**: Regex `\b\S+\.md\b` returns zero plaintext `.md` references outside Markdown link syntax.
- **SC-003**: `README.md` word count is between 500 and 700 words and contains only summaries plus navigation.
- **SC-004**: Every non-archived Markdown file is reachable within three clicks from `README.md`/index chains; archived files reside solely in `docs/archive/`.
- **SC-005**: `/spec` files satisfy compression rules (paragraphs <80 words, declarative tone, "Use /spec before /docs" guidance) and reference `v1.0.0`.
- **SC-006**: Deduplication audit reports zero high-similarity pairs across active docs.
- **SC-007**: Version synchronization script confirms 100% of documentation declares `v1.0.0`.
- **SC-008**: Verification checklist (tree, diffs, validation report) is completed and attached to the PR.

## Clarifications

### Session 2025-11-27

- Q: Какое максимальное время выполнения полного запуска refactor CLI на стандартном ноутбуке разработчика допустимо? → A: without restrictions
