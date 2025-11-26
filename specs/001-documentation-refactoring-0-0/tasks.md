# Tasks: Documentation Refactoring v1.0.0

**Input**: Design documents from `/specs/001-documentation-refactoring-0-0/`  
**Prerequisites**: `[plan.md](specs/001-documentation-refactoring-0-0/plan.md)`, `[research.md](specs/001-documentation-refactoring-0-0/research.md)`, `[data-model.md](specs/001-documentation-refactoring-0-0/data-model.md)`, `[contracts/STRUCTURE.md](specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md)`, `[quickstart.md](specs/001-documentation-refactoring-0-0/quickstart.md)`

## Phase 3.1: Setup & Inventory

- [x] **T001** Generate complete Markdown inventory (excluding system files)  
  - Use a script (e.g., `find . -name "*.md"`) from repo root to list all `.md` files.  
  - Save the inventory to `specs/001-documentation-refactoring-0-0/tmp/all_markdown_files.txt`.  
  - Explicitly exclude `CONTRIBUTING.md`, `LICENSE.txt`, `SECURITY.md`, `CHANGELOG.md` from later refactor steps (but keep them in the list for link checking).

- [x] **T002 [P]** Classify documentation files into roles (`DocumentationFile.role`)  
  - For each path in `all_markdown_files.txt`, assign a `role` and `audience` per `[data-model.md](specs/001-documentation-refactoring-0-0/data-model.md)` (README, INDEX, GUIDE, SPEC, REPORT, OTHER; HUMAN/AI/BOTH).  
  - Store the classification in `specs/001-documentation-refactoring-0-0/tmp/doc_roles.csv`.  

- [x] **T003 [P]** Identify index documents and seeds for traversal  
  - Mark `README.md`, `docs/DESIGN_INDEX.md`, and `spec/README.md` as `IndexDocument` seeds.  
  - Ensure all three exist; if missing, create stubs that comply with `[contracts/STRUCTURE.md](specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md)`.

## Phase 3.2: Link Graph & Integrity (TDD Style)

> These tasks define and validate link integrity before restructuring content.

- [x] **T004** Build link graph extractor for Markdown docs  
  - Implement or script a tool (`scripts/doc_link_graph.sh` or similar) that:  
    - Parses all `.md` files for Markdown links and plain-path candidates.  
    - Normalizes targets to repository-relative `.md` paths where applicable.  
    - Outputs a graph representation (e.g., JSON) to `specs/001-documentation-refactoring-0-0/tmp/link_graph.json`.  

- [x] **T005 [P]** Detect broken `.md` links  
  - Using `link_graph.json` and the inventory from T001, list all edges whose target `.md` does not exist.  
  - Save report to `specs/001-documentation-refactoring-0-0/tmp/broken_links.md`.  
  - This report will drive concrete fixes in later tasks.

- [x] **T006 [P]** Detect plain-path references needing Markdown links  
  - From the same graph extractor, identify any `Link` with `is_plain_path_candidate == true` that resolves to an existing `.md`.  
  - Save a list of `(source_path, raw_text, normalized_target)` triplets to `tmp/plain_paths_to_fix.md`.

## Phase 3.3: Index & Reachability Enforcement

- [x] **T007** Ensure `README.md` matches structural contract  
  - Edit `README.md` so that:  
    - It contains only high-level overview, version badges, and platform matrix.  
    - All detailed sections (release notes, deep API lists, long test sections) are replaced by short summaries plus links into `/docs`.  
    - It links explicitly to main `/docs` entrypoints and to `spec/README.md`.  
  - Follow rules in `[contracts/STRUCTURE.md](specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md)` §2.1.

- [x] **T008** Ensure `docs/DESIGN_INDEX.md` covers all design docs  
  - Update `docs/DESIGN_INDEX.md` to list all design-related docs under `docs/` and `specs/*`, using data from T002.  
  - Confirm each design doc has at least one path from `README.md` or `docs/DESIGN_INDEX.md` within two clicks (per `LinkGraph` rules).

- [x] **T009** Ensure `spec/README.md` guides AI agents  
  - Update `spec/README.md` so that it:  
    - States explicitly that `/spec` is the primary decision source for AI agents.  
    - Instructs agents to consult `/spec` first, then `/docs` for narrative detail.  
    - Links to key specs: product vision, requirements, architecture, API, test plan, traceability, quality gates.  

## Phase 3.4: Link Normalization & Broken Link Fixes

- [x] **T010** Normalize all plain-path references to Markdown links  
  - For each entry in `plain_paths_to_fix.md`, update the source file so that the plain path becomes a proper Markdown link `[text](target_path)`.  
  - Respect relative vs. absolute path conventions consistent with existing docs.  

- [x] **T011** Fix or create missing `.md` targets  
  - For each entry in `broken_links.md`:  
    - If the referenced document is conceptually needed (based on context and spec), create a new `.md` file with v1.0.0-accurate stub content under the appropriate directory (`/docs` or `/spec`).  
    - If the reference is obsolete, update the link to point to the correct existing v1.0.0 doc or remove it if no longer needed.  
  - After edits, re-run the link-checker (from T004/T005) to confirm zero broken `.md` links remain.

## Phase 3.5: Depth = Detail Restructuring

- [x] **T012** Move detailed release information out of `README.md` into `/docs`  
  - Identify all sections in `README.md` starting with release histories, long “What’s New” lists, or platform-specific changelogs.  
  - Create or update dedicated docs under `/docs` (e.g., `docs/RELEASE_v1.0.0_DRAFT.md`, other release files) to hold full details.  
  - Replace the removed sections in `README.md` with short summaries and links to these docs.

- [x] **T013** Enforce depth rule for architecture and API docs  
  - Ensure detailed architecture descriptions live in `/docs/architecture.md`, `docs/Technical_Specification.md`, or equivalent, with specs in `/spec` holding the compressed structural version.  
  - Ensure full API details live in `docs/api_reference/API_REFERENCE.md` and related docs; `/spec/05-api.md` should remain concise and canonical.  
  - Remove duplicated paragraphs where both `/docs` and `/spec` restate the same behavior; leave a concise pointer instead.

- [x] **T014** Align Android/desktop comparison and migration docs  
  - Verify `docs/Truth-training/Truth-training.md`, `docs/ANDROID_MIGRATION.md`, and `docs/TEST_REPORT_ANDROID_v1.0.0.md` reflect v1.0.0 and do not contradict `/spec`.  
  - Ensure these docs point back to the relevant specs (e.g., API and data model) rather than redefining contracts.

## Phase 3.6: /spec Optimization for AI

- [x] **T015** Tighten `/spec` documents to be non-narrative  
  - Review key specs (`spec/01-product-vision.md`, `spec/02-requirements.md`, `spec/03-architecture.md`, `spec/05-api.md`, `spec/16-test-plan.md`, etc.).  
  - Remove long narrative sections; keep requirements, models, contracts, and success criteria.  
  - Where necessary, move explanatory narratives into `/docs` and add back-links.

- [x] **T016 [P]** Update `spec/README.md` per STRUCTURE contract  
  - Implement the AI guidance text and links as defined in `[contracts/STRUCTURE.md](specs/001-documentation-refactoring-0-0/contracts/STRUCTURE.md)` §2.3 and §5.  

## Phase 3.7: Validation & Quickstart Execution

- [x] **T017** Run quickstart validation scenarios  
  - Follow `specs/001-documentation-refactoring-0-0/quickstart.md` end-to-end:  
    - Link integrity walk from `README.md`.  
    - `/docs` vs `/spec` separation checks.  
    - Reachability and version consistency checks.  
  - Record any deviations in a short report `specs/001-documentation-refactoring-0-0/validation-notes.md` and fix remaining issues.

## Dependencies

- **T001** precedes all other tasks (inventory required).  
- **T002–T003** depend on T001.  
- **T004–T006** depend on T001 and T002.  
- **T007–T009** depend on T002–T003 (index docs must exist).  
- **T010–T011** depend on T004–T006 (link issues identified first).  
- **T012–T014** depend on T007–T011 (indices and links stabilized).  
- **T015–T016** depend on T012–T014 (docs cleaned up and non-duplicative).  
- **T017** depends on all previous tasks.

## Parallel Execution Example

```bash
# After T001–T003 complete, run in parallel:
Task: "T004 Build link graph extractor for Markdown docs"
Task: "T005 Detect broken .md links"
Task: "T006 Detect plain-path references needing Markdown links"

# After T012 completes, tasks T013 and T014 can be partially parallelized
# as they touch mostly disjoint doc subsets (architecture/API vs Android docs),
# but avoid editing the same file in two tasks at once.
```

## Validation Checklist

- [ ] All Markdown links to `.md` files resolve successfully.  
- [ ] All design and spec docs are reachable from at least one index within two clicks.  
- [ ] `README.md` is concise and delegates detail to `/docs`.  
- [ ] `/spec` is compact, structural, and clearly marked as AI entrypoint.  
- [ ] No significant duplicated content remains between `/docs` and `/spec`.  
- [ ] Version references for v1.0.0 are consistent across `README.md`, `/docs`, and `/spec`.  

