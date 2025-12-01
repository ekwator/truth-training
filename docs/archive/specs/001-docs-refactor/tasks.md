<!-- Archived from [specs/001-docs-refactor/tasks.md](specs/001-docs-refactor/tasks.md) -->

# Tasks: Documentation Refactor v1.0.0

**Input**: Design documents from `specs/001-docs-refactor/`  
**Prerequisites**: [plan.md](plan.md) (required), [spec.md](spec.md), [data-model.md](data-model.md), [research.md](research.md), [quickstart.md](quickstart.md), contracts/

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ensure doc-refactor tooling and environment are ready to run.

- [ ] T001 Create or update Python virtual environment in project root (`python3 -m venv .venv` and `source .venv/bin/activate`)
- [ ] T002 Install documentation tooling dependencies with `pip install -r requirements.txt` in the project root
- [ ] T003 [P] Verify `scripts/doc_refactor/main.py` is executable by running `python scripts/doc_refactor/main.py --help`
- [ ] T004 [P] Confirm `make doc-refactor-run` and `make doc-refactor-test` targets exist in `Makefile`

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before any user-story-specific refactor.

- [ ] T005 Define or update `reports/doc_refactor/` directory structure and ensure it is git-ignored or included as per existing project policy
- [ ] T006 Implement or confirm implementation of a Markdown inventory writer in `scripts/doc_refactor/main.py` that produces `reports/doc_refactor/inventory.json`
- [ ] T007 Implement or confirm implementation of link discovery and validation logic in `scripts/doc_refactor/main.py` that records `ReferenceEdge` status for all `.md` links
- [ ] T008 Implement or confirm implementation of restructuring and spec optimization phases (`restructuring`, `spec_opt`) in `scripts/doc_refactor/main.py`
- [ ] T009 Ensure CI configuration includes a job or step to run `make doc-refactor-run` and `make doc-refactor-test` by updating `.github/workflows/*.yml` if necessary

## Phase 3: User Story 1 — Repository Inventory & Coverage (Priority: P1) 🎯 MVP

**Goal**: Every Markdown file is cataloged, version-tagged for v1.0.0, and associated with at least one index.

**Independent Test**: Run the inventory phase and verify the manifest lists 100% of `.md` files (excluding explicitly excluded ones) with audience classification and v1.0.0 tagging.

### Implementation for User Story 1

- [ ] T010 [P] [US1] Implement `DocumentationNode` structure and serialization in `scripts/doc_refactor/inventory.py` (or equivalent module)
- [ ] T011 [P] [US1] Implement repository walk that discovers all `.md` files and classifies them as `root`, `docs`, `spec`, or `other` in `scripts/doc_refactor/inventory.py`
- [ ] T012 [US1] Integrate inventory generation into `scripts/doc_refactor/main.py run --phases inventory` to write `reports/doc_refactor/inventory.json`
- [ ] T013 [P] [US1] Add logic to mark excluded files (`[CONTRIBUTING.md](CONTRIBUTING.md)`, `LICENSE.txt`, `[SECURITY.md](SECURITY.md)`, `[CHANGELOG.md](CHANGELOG.md)`) via `is_excluded` in `scripts/doc_refactor/inventory.py`
- [ ] T014 [P] [US1] Implement v1.0.0 `version_tag` inference for each node (or mark as legacy) in `scripts/doc_refactor/inventory.py`
- [ ] T015 [US1] Add summary statistics (counts by audience, orphan candidates) to `InventoryReport` in `scripts/doc_refactor/inventory.py`
- [ ] T016 [P] [US1] Create or update unit tests for inventory logic in `scripts/doc_refactor/tests/test_inventory.py`
- [ ] T017 [US1] Document how to run the inventory phase and interpret the report in `[docs/Documentation_Refactor_Inventory.md](docs/Documentation_Refactor_Inventory.md)`

## Phase 4: User Story 2 — Link Integrity & Reference Graph (Priority: P2)

**Goal**: Traversal from `[README.md](README.md)` through all Markdown links succeeds with no broken internal links, and all inline `.md` references are normalized to clickable links.

**Independent Test**: Execute `python scripts/doc_refactor/main.py run --phases link_discovery,validation` and confirm zero broken internal links and a complete `ReferenceEdge` set for all `.md` files.

### Implementation for User Story 2

- [ ] T018 [P] [US2] Implement `ReferenceEdge` model and serialization in `scripts/doc_refactor/links.py`
- [ ] T019 [P] [US2] Implement link discovery that parses Markdown links and bare `.md` references in `scripts/doc_refactor/links.py`
- [ ] T020 [US2] Implement normalization step that converts internal `.md` path mentions to `[path](path)` form in `scripts/doc_refactor/links.py`
- [ ] T021 [US2] Integrate link discovery and normalization into `scripts/doc_refactor/main.py run --phases link_discovery,validation`
- [ ] T022 [P] [US2] Ensure every internal `.md` link resolves to a known `DocumentationNode` from the inventory, marking status as `ok` or `missing`
- [ ] T023 [P] [US2] Implement external URL validation with timeouts and record `external_ok` or `external_warning` in `ReferenceEdge` within `scripts/doc_refactor/links.py`
- [ ] T024 [P] [US2] Create or update tests for link discovery and validation in `scripts/doc_refactor/tests/test_links.py`
- [ ] T025 [US2] Generate a concise broken-link report in `reports/doc_refactor/link_report.json` or `.md` from `scripts/doc_refactor/main.py`
- [ ] T026 [US2] Update `[docs/Documentation_Refactor_Links.md](docs/Documentation_Refactor_Links.md)` with instructions and interpretation examples for link validation reports

## Phase 5: User Story 3 — Hierarchical Restructure & Content Separation (Priority: P3)

**Goal**: `[README.md](README.md)` is concise and links into `/docs` and `/spec`; `/docs` contains detailed human-friendly docs; `/spec` contains compressed AI-facing specifications with minimal duplication.

**Independent Test**: Run restructuring and spec optimization phases, then manually verify hierarchy and automatically confirm no duplicated paragraphs between `/docs` and `/spec` beyond acceptable minimal overlap.

### Implementation for User Story 3

- [ ] T027 [P] [US3] Implement restructuring rules that move verbose sections (e.g., release details) from `[README.md](README.md)` into `docs/` in `scripts/doc_refactor/restructuring.py`
- [ ] T028 [US3] Implement logic to enforce a ≤400-word summary in `[README.md](README.md)` with links to `[docs/README.md](docs/README.md)` and `[spec/README.md](spec/README.md)` in `scripts/doc_refactor/restructuring.py`
- [ ] T029 [P] [US3] Implement reorganization of `/docs` so that release notes, tutorials, and troubleshooting live in topic-based files referenced from `[docs/README.md](docs/README.md)` in `scripts/doc_refactor/restructuring.py`
- [ ] T030 [P] [US3] Implement `/spec` cleanup rules to keep only compressed, directive-style content and add references back to `/docs` where narrative detail is needed in `scripts/doc_refactor/spec_opt.py`
- [ ] T031 [US3] Implement `DuplicateBlock` detection between `/docs` and `/spec` and classification (`harmless`, `needs_review`) in `scripts/doc_refactor/spec_opt.py`
- [ ] T032 [P] [US3] Create or update tests for restructuring and spec optimization in `scripts/doc_refactor/tests/test_restructuring.py`
- [ ] T033 [US3] Ensure `[spec/README.md](spec/README.md)` explicitly instructs AI agents to prioritize `/spec` while referencing `/docs` for narrative depth
- [ ] T034 [US3] Ensure `[docs/README.md](docs/README.md)` provides index coverage for all non-archived human-facing documentation nodes

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final consistency, reporting, and CI integration polish.

- [ ] T035 [P] Add or update high-level documentation page `[docs/Documentation_Refactor_Overview.md](docs/Documentation_Refactor_Overview.md)` summarizing phases and reports
- [ ] T036 [P] Ensure `[README.md](README.md)` mentions the doc-refactor workflow and links to `[docs/Documentation_Refactor_Overview.md](docs/Documentation_Refactor_Overview.md)`, `[docs/Documentation_Refactor_Inventory.md](docs/Documentation_Refactor_Inventory.md)`, and `[docs/Documentation_Refactor_Links.md](docs/Documentation_Refactor_Links.md)`
- [ ] T037 [P] Integrate doc-refactor inventory and link checks into existing quality gates (e.g., referenced from `[docs/Constitution-Compliance.md](docs/Constitution-Compliance.md)` or `[spec/14-quality-gates.md](spec/14-quality-gates.md)`)
- [ ] T038 [P] Add cross-reference from `[spec/README.md](spec/README.md)` to the documentation refactor spec `[specs/001-docs-refactor/spec.md](specs/001-docs-refactor/spec.md)`
- [ ] T039 Run full pipeline (`make doc-refactor-run` and `make doc-refactor-test`) and attach generated reports under `reports/doc_refactor/` to the release or CI artifacts
- [ ] T040 Review and remove any remaining duplicated paragraphs between `/docs` and `/spec` that are classified as `needs_review` in the duplication report

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — must run before other phases.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Stories (Phases 3–5)**: All depend on Foundational completion; can proceed in priority order (US1 → US2 → US3).
- **Polish (Phase 6)**: Depends on all user stories being functionally complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other user stories.
- **User Story 2 (P2)**: Depends on User Story 1 inventory data to validate links against known `DocumentationNode` entries.
- **User Story 3 (P3)**: Depends on User Story 1 and 2 to know which docs exist, how they are linked, and where restructuring is safe.

### Parallel Opportunities

- All tasks marked `[P]` can run in parallel if they touch different files or are logically independent.
- Within a phase, non-[P] tasks should be completed in ID order to respect dependencies.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1 (inventory).
4. **Stop and validate**: Confirm inventory and coverage before proceeding to link integrity and restructuring.

### Incremental Delivery

1. Foundation ready → deliver inventory (US1).
2. Add link integrity and reference graph (US2).
3. Add hierarchical restructuring and duplication detection (US3).
4. Apply Polish & cross-cutting tasks (Phase 6).



