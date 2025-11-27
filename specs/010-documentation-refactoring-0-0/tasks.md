# Tasks: Documentation Refactoring v1.0.0

**Input**: `/specs/010-documentation-refactoring-0-0/` (plan, research, data-model, contracts, quickstart)

**Prerequisites**: Python 3.11 toolchain, `pip install -r requirements.txt`, clean working tree snapshot

## Phase 1 – Setup (Shared Infrastructure)

- [ ] T001 Update `requirements.txt` with pinned versions for `mistune`, `scikit-learn`, `numpy`, `jinja2`, `rich`, and add a `doc_refactor` extra section in `README.md` describing `python3 -m venv .venv && source .venv/bin/activate`.
- [ ] T002 [P] Add `make doc-refactor-run` and `make doc-refactor-test` targets to repository `Makefile` that wrap `python scripts/doc_refactor/main.py run --phases all` and `pytest scripts/doc_refactor/tests` respectively.

## Phase 2 – Foundational (Blocking Prerequisites)

- [ ] T003 Create package scaffold `scripts/doc_refactor/models/__init__.py` exporting shared dataclasses plus `scripts/doc_refactor/models/base.py` with common `to_dict()` helper.
- [ ] T004 [P] Implement `DocumentationFile` dataclass in `scripts/doc_refactor/models/documentation_file.py` (fields per data-model) and wire it into `inventory.py`.
- [ ] T005 [P] Implement `LinkEdge` dataclass in `scripts/doc_refactor/models/link_edge.py` and integrate with upcoming link graph builder.
- [ ] T006 [P] Implement `LinkGraphReport` dataclass in `scripts/doc_refactor/models/link_graph_report.py` and register serialization helpers.
- [ ] T007 [P] Implement `SpecCompressionProfile` dataclass in `scripts/doc_refactor/models/spec_compression_profile.py` with paragraph metrics.
- [ ] T008 [P] Implement `RunArtifact` dataclass in `scripts/doc_refactor/models/run_artifact.py` and update `main.py` resume logic to persist/load it.

**Checkpoint**: Data layer ready; proceed to user stories.

---

## Phase 3 – User Story 1: Repo-wide integrity sweep (Priority: P1)
**Goal**: CLI inventories every Markdown file, builds link graph, fixes plaintext paths, and leaves zero orphan/broken references.
**Independent Test**: Run quickstart Scenarios 1 & 2 to validate inventory and link validation outputs.

### Tests (write first)
- [ ] T009 [P] [US1] Add contract test `scripts/doc_refactor/tests/test_cli_contract.py` that shells `python scripts/doc_refactor/main.py run --phases inventory,link_discovery,validation --dry-run` and asserts exit codes per `contracts/doc_refactor_cli.md`.
- [ ] T010 [P] [US1] Add integration test `scripts/doc_refactor/tests/test_inventory_depth.py` covering README depth coercion, exclusion list, and `reports/doc_refactor/inventory.json` contents (quickstart Scenario 1).
- [ ] T011 [P] [US1] Add integration test `scripts/doc_refactor/tests/test_plain_paths.py` that seeds plaintext `.md` snippets and asserts validation replaces them plus broken URL handling (quickstart Scenario 2).

### Implementation
- [ ] T012 [US1] Enhance `scripts/doc_refactor/inventory.py` to use new `DocumentationFile` model, enforce depth cap=3, and tag roles (`README`,`INDEX`,`DETAIL`,`SPEC`,`ARCHIVE`).
- [ ] T013 [US1] Create new module `scripts/doc_refactor/link_graph.py` that walks Markdown AST via `mistune` and emits `LinkEdge` + `LinkGraphReport` persisted to `reports/doc_refactor/link_graph.json`.
- [ ] T014 [US1] Update `scripts/doc_refactor/validation.py` to convert plaintext `.md` paths into `[path](path)` links, categorize external URLs (`ok`, `needs_stub`, `verified`), and output `validation.json`.
- [ ] T015 [US1] Extend `scripts/doc_refactor/main.py` phase orchestration to include resumable checkpoints, printing `RunArtifact` per phase, and fail-fast toggle (`--fail-fast`).
- [ ] T016 [US1] Implement ledger logging in `scripts/doc_refactor/file_creator.py` for created `/docs` or `/spec` placeholders with relative path + template name.

**Checkpoint**: `inventory → link_discovery → validation → file_creation` phases complete and validated by Scenarios 1–2.

---

## Phase 4 – User Story 2: Hierarchical restructuring (Priority: P2)
**Goal**: README shrinks to 500–700 words, `/docs` hosts detailed content, orphans moved to `docs/archive/`, /docs indexes regenerated.
**Independent Test**: Quickstart Scenarios 3 & 4.

### Tests
- [ ] T017 [P] [US2] Add integration test `scripts/doc_refactor/tests/test_readme_compression.py` that feeds oversized README and asserts restructuring enforces word count + summary-only sections (Scenario 3).
- [ ] T018 [P] [US2] Add integration test `scripts/doc_refactor/tests/test_orphan_archival.py` verifying orphan detection, archive relocation, and link graph coverage (Scenario 4).

### Implementation
- [ ] T019 [US2] Implement `scripts/doc_refactor/restructuring.py` to split README detail sections into `/docs/**/*.md`, regenerate navigation headings (depth ≤2), and ensure summary stubs remain.
- [ ] T020 [US2] Enhance `scripts/doc_refactor/file_creator.py` to move pre-v1.0.0 docs under `docs/archive/` with archive template + provenance note.
- [ ] T021 [US2] Add `/docs/archive/README.md` index plus update `/docs/README.md` to link every archive subtree produced by CLI.
- [ ] T022 [US2] Update `reports/doc_refactor/restructure.json` writer to capture before/after paths for moved sections and orphans resolved.

**Checkpoint**: README and documentation hierarchy comply with SC-003/SC-004; archive tree populated.

---

## Phase 5 – User Story 3: Spec optimization for AI agents (Priority: P3)
**Goal**: `/spec` files stay compressed (<80-word paragraphs), include "Use /spec before /docs" directive, dedupe near-identical content, sync versions.
**Independent Test**: Quickstart Scenarios 5 & 6.

### Tests
- [ ] T023 [P] [US3] Add integration test `scripts/doc_refactor/tests/test_spec_compression.py` ensuring spec optimizer enforces paragraph length, directive, and `/docs` cross-links (Scenario 5).
- [ ] T024 [P] [US3] Add end-to-end test `scripts/doc_refactor/tests/test_full_run.py` that executes `--phases all` on sample repo and asserts run_summary conditions: zero broken links, 100% coverage, dedupe actions recorded (Scenario 6).

### Implementation
- [ ] T025 [US3] Create `scripts/doc_refactor/spec_optimizer.py` to restructure spec files per `SpecCompressionProfile`, inject AI guidance, and log output to `spec_opt.json`.
- [ ] T026 [US3] Implement version synchronization phase in `scripts/doc_refactor/version_sync.py` to ensure every active Markdown file mentions `v1.0.0` (using `DocumentationFile` list) and update contract FR-010 reference.
- [ ] T027 [US3] Extend `scripts/doc_refactor/duplicate_detector.py` to categorize clusters (`probable_duplicate`, `needs_merge`, `safe`) and emit dedupe actions in `reports/doc_refactor/dedupe.json`.
- [ ] T028 [US3] Update `/spec/README.md` and affected spec files with "Use /spec before /docs" admonition plus cross-links generated by CLI (one-time script + template update under `scripts/doc_refactor/templates/spec_compressed.md.j2`).

**Checkpoint**: `/spec` directory optimized, dedupe + version sync phases clean.

---

## Phase 6 – Polish & Cross-Cutting

- [ ] T029 [P] Update `docs/CONTRIBUTING.md` with a new section "Running doc_refactor" referencing quickstart scenarios.
- [ ] T030 [P] Add logging/telemetry hooks (elapsed seconds, memory snapshot) per phase by instrumenting `scripts/doc_refactor/main.py` and surface summary in `run_summary.json`.
- [ ] T031 Run full quickstart Scenario 6 manually, attach `reports/doc_refactor/run_summary.json` to PR, and capture screenshots/diffs for reviewers.

---

## Dependencies & Parallel Execution

### Phase Dependencies
- Setup (T001–T002) → Foundational (T003–T008) → US1 (T009–T016) → US2 (T017–T022) → US3 (T023–T028) → Polish (T029–T031).

### Parallel Opportunities
- `[P]` tasks operate on distinct files/modules:
  - Foundational entity implementations (T004–T008) can run concurrently once T003 completes.
  - US1/US2/US3 tests (T009–T024) can execute in parallel while implementation is underway (TDD).
  - Polish doc/log tasks (T029–T030) can run simultaneously after core stories finish.

### Task Agent Launch Examples
```bash
# Parallelize dataclass work (after T003):
[cursor-task "T004" -- python -m task_runner scripts/doc_refactor/models/documentation_file.py]
[cursor-task "T005" -- python -m task_runner scripts/doc_refactor/models/link_edge.py]

# Parallel test authoring for User Story 1:
[cursor-task "T009" -- pytest scripts/doc_refactor/tests/test_cli_contract.py -k dry_run]
[cursor-task "T010" -- pytest scripts/doc_refactor/tests/test_inventory_depth.py -k readme]
```

Deliver each task sequentially unless explicitly marked `[P]`. Ensure tests are created before their corresponding implementation steps to preserve TDD flow.
