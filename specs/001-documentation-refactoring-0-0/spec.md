# Feature Specification: Documentation Refactoring v1.0.0

**Feature Branch**: `001-documentation-refactoring-0-0`  
**Created**: 2025-11-26  
**Status**: Draft  
**Input**: User description: "Documentation Refactoring v1.0.0 — restore complete link integrity, enforce structural hierarchy, and separate human-readable docs from AI-oriented specs."

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

### User Story 1 - Human reader can navigate v1.0.0 docs without dead ends (Priority: P1)

As a human user (developer, integrator, or operator), I want all documentation links in the v1.0.0 tree to be valid and lead to the correct, current content so that I can follow any path starting from `README.md` without encountering missing pages, stale filenames, or duplicated/conflicting information.

**Why this priority**: Broken or stale links are the fastest way to erode trust in documentation; fixing link integrity is a prerequisite for any deeper restructuring and directly impacts onboarding and support costs.

**Independent Test**: Starting from `README.md`, crawl all Markdown links and verify that every target exists, renders, and reflects v1.0.0; the test passes if there are zero 404s, zero references to removed/renamed files, and no references to pre-v1.0.0 behavior.

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - AI agents can rely on a compact /spec surface (Priority: P1)

As an AI assistant or automation agent, I want a compressed, decision-ready `/spec` tree that encodes the current architecture, API contracts, and feature states without narrative noise so that I can answer questions and make changes using a stable, authoritative context instead of scraping long-form human docs.

**Why this priority**: Many automated flows (spec-kit, codegen, refactors) depend on concise, machine-optimized specs; separating `/spec` from `/docs` reduces hallucinations, duplicated logic, and drift between natural language docs and actual system behavior.

**Independent Test**: When an AI agent is pointed only at `/spec`, it can correctly answer questions about supported features, APIs, and versions for v1.0.0 and produce plans that agree with `/docs` and code; manual reviewers confirm no critical information is only present in `/docs`.

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - Contributors can quickly find and extend the right doc (Priority: P2)

As a contributor making a change (feature, bugfix, or release), I want a clear documentation hierarchy where high-level summaries live in `README.md` and indices, and detailed flows live in `/docs`, so that I can easily locate where to update text without duplicating content or guessing which file is authoritative.

**Why this priority**: Clear structure reduces friction for future edits and makes the refactor durable instead of a one-off cleanup; it also supports release processes that need predictable locations for migration guides, test reports, and design decisions.

**Independent Test**: Given a set of change scenarios (new API, new platform feature, test findings), reviewers independently choose the same target document(s) to edit based on guidance in `README.md`, `docs/` indices, and `spec/README.md`.

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- What happens when a Markdown file is not reachable from `README.md` but is still required for compliance (e.g., security, legal, or archived research)?  
- How do we handle references to files that are intentionally removed or deprecated (e.g., v0.3.0-only docs) while keeping historical integrity?  
- What if two documents must describe overlapping concepts (e.g., high-level design vs. migration details) — how do we prevent duplication and specify the “source of truth”?  
- How are future versions (v1.1.0+) expected to extend this structure without breaking v1.0.0 links or overloading top-level files?

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

#### Inventory & Audit (Phase 1)

- **FR-001**: The system MUST generate a complete inventory of all `.md` files in the repository, including subdirectories, excluding `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, and `CHANGELOG.md`.  
- **FR-002**: The system MUST record, for each `.md` file, whether it is reachable from `README.md` via a chain of Markdown links.  
- **FR-003**: The system MUST identify “orphaned” design-oriented docs (docs/specs not reachable from any index) and propose a parent index (e.g., `README.md`, `docs/DESIGN_INDEX.md`, or `spec/README.md`) for each.  
- **FR-004**: The system MUST not rename or move excluded files (`CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md`) but MAY link to them.

#### Link Integrity Enforcement (Phase 2)

- **FR-005**: Starting from `README.md`, the system MUST traverse all Markdown links (relative and absolute paths) and build a list of all reachable `.md` files.  
- **FR-006**: For every plain text path that matches an existing `.md` file (e.g., `spec/01-product-vision.md`), the system MUST convert it into a Markdown hyperlink `[...]()` using a relative path that remains valid from the current document.  
- **FR-007**: For every outbound link to a `.md` file, the system MUST verify that the target exists; if it does not, the refactor MUST either (a) create the file with v1.0.0-accurate stub content or (b) update the link to a valid replacement doc.  
- **FR-008**: The system MUST maintain a central “processed files” list covering all `.md` files that were visited or edited during traversal.  
- **FR-009**: After traversal, any `.md` files not in the processed list MUST be added to an index document appropriate to their role (e.g., `docs/DESIGN_INDEX.md` for design docs, `spec/README.md` for AI specs, `README.md` for top-level guides).

#### Structural Hierarchy & Content Restructuring (Phase 3)

- **FR-010**: `README.md` MUST be refactored to contain only concise, high-level sections: project overview, current versions, platform matrix, and links into `/docs` and `/spec`, with no long-form release notes, migration guides, or API details.  
- **FR-011**: Detailed content currently in `README.md` (e.g., “Release Information”, full HTTP API lists, deep CLI or architecture sections) MUST be moved into dedicated documents under `/docs` and replaced with short teasers + links.  
- **FR-012**: The `/docs` tree MUST be the primary home for human-readable, narrative documentation: guides, migration docs, test reports, architecture write-ups, release notes, troubleshooting, etc.  
- **FR-013**: The `/spec` tree MUST contain compressed, structured specifications optimized for AI agents (plans, data models, contracts, checklists) without story-like prose, and spec documents MUST reference `/docs` where additional narrative context exists.  
- **FR-014**: `spec/README.md` MUST include explicit guidance instructing AI systems to consult `/spec` first for decision-making and to treat `/docs` as supporting context.  
- **FR-015**: For any concept described in both `/docs` and `/spec`, there MUST be a single canonical “source of truth” (declared either in `/spec` or `/docs`) and cross-references from the secondary location, to avoid duplicated logic.

#### Versioning & Consistency

- **FR-016**: All updated docs MUST reflect v1.0.0 as the current baseline; any references to older version behavior MUST be clearly marked as historical or migration-only.  
- **FR-017**: The system MUST ensure that `/docs/VERSION_REGISTRY.md` and `spec/README.md` agree on active versions for all major components (core, desktop, Android).  
- **FR-018**: Any doc created or modified by this refactor MUST explicitly note that it is current as of v1.0.0.

#### Non-Goals / Clarifications

- **FR-019**: The refactor MUST NOT change runtime code behavior; it is purely about documentation structure and content.  
- **FR-020**: The refactor MUST preserve Git history where possible (edits-in-place preferred over deleting/re-adding files).

### Key Entities *(include if feature involves data)*

- **Documentation File**: Any `.md` file in the repository, with attributes such as `path`, `type` (readme, index, guide, spec, test-report), `version_scope` (global, v1.0.0, historical), and `reachability` (reachable from README, reachable from index, orphan).  
- **Link Graph**: Directed graph where nodes are documentation files and edges represent Markdown links; used to detect orphans and verify traversal coverage.  
- **Index Document**: A file whose primary purpose is to list and categorize other docs (e.g., `README.md`, `docs/DESIGN_INDEX.md`, `spec/README.md`), with attributes `scope` and `children`.  
- **Spec Document**: Compressed, AI-oriented markdown under `/spec` that encodes requirements, entities, and success criteria for a feature, with explicit cross-links to `/docs`.  
- **Human-Readable Guide**: Narrative documentation under `/docs` (e.g., migration guides, release notes) that elaborates on behavior, rationale, and operational detail.

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001 (Link Integrity)**: Automated link validation across all Markdown files (including those under `/docs` and `/spec`) reports zero broken links and zero references to non-existent `.md` files.  
- **SC-002 (Reachability)**: 100% of design-focused `.md` files are reachable from at least one index document (`README.md`, `docs/DESIGN_INDEX.md`, or `spec/README.md`) within two clicks.  
- **SC-003 (Structural Clarity)**: Independent reviewers classify at least 90% of sampled docs correctly as “human-readable guide” vs. “AI-oriented spec” using only location and the guidance in `spec/README.md`.  
- **SC-004 (README Conciseness)**: `README.md` word count is reduced by at least 30% compared to pre-refactor while retaining all high-level information through links.  
- **SC-005 (Version Consistency)**: Spot checks of key docs (`README.md`, `docs/VERSION_REGISTRY.md`, `docs/Truth-training/Truth-training.md`, selected `/spec` files) show no contradictory statements about the current version baseline (v1.0.0).

## Clarifications

### Session 2025-11-26

- Q: How should we handle historical (pre‑v1.0.0) documentation that still exists in the repo as of v1.0.0? → A: Update in-place to v1.0.0

**Historical docs handling**

- Historical (pre‑v1.0.0) documents SHOULD be updated in-place to reflect v1.0.0 behavior and terminology, even if they originally described older versions; explicit history SHOULD be captured in dedicated migration or release notes (e.g., under `/docs`) rather than by keeping separate archived copies of old docs.
