# Implementation Plan: Documentation Refactoring v1.0.0

**Branch**: `010-documentation-refactoring-0-0` | **Date**: 2025-11-27 | **Spec**: `/specs/010-documentation-refactoring-0-0/spec.md`  
**Input**: Feature specification from `/specs/010-documentation-refactoring-0-0/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec (Input path)
   → ERROR if file missing
2. Populate Technical Context (clear NEEDS CLARIFICATION markers)
   → Detect project type (documentation/CLI tooling) from repo layout
   → Record Structure Decision referencing actual directories
3. Constitution check gate
   → Map Truth Training constitution principles to this effort
   → If violations appear, document under Complexity Tracking or halt
4. Phase 0 research → `/specs/010-documentation-refactoring-0-0/research.md`
   → Resolve unknowns about inventory, link graph, hierarchy rules
5. Phase 1 design outputs
   → `/specs/010-documentation-refactoring-0-0/data-model.md`
   → `/specs/010-documentation-refactoring-0-0/contracts/*.md`
   → `/specs/010-documentation-refactoring-0-0/quickstart.md`
6. Re-evaluate constitution check (post-design)
7. Outline Phase 2 task planning approach (do NOT create tasks.md)
8. Update Progress Tracking to reflect completed phases
9. STOP – Handoff to `/tasks`
```

## Summary
Establish a deterministic documentation refactoring program for Truth Training v1.0.0. The plan codifies how the Python-based `scripts/doc_refactor` CLI inventories >300 Markdown files, enforces /docs vs /spec boundaries, repairs every link/plain-path, compresses AI-facing specs, and archives historical content. The outcome is a fully automated workflow (inventory → reference integrity → restructuring) plus playbooks for running, validating, and iterating the refactor without regressions.

## Technical Context
**Language/Version**: Python 3.11 (doc_refactor CLI), Bash (Speckit scripts)  
**Primary Dependencies**: `mistune` (Markdown parsing), `scikit-learn`/`numpy` (duplicate detection TF-IDF), `jinja2` (templates), `rich` (CLI UX), `pathlib`/`networkx`-style internal graph utilities  
**Storage**: Local file system + JSON/CSV inventories written under `.artifacts/doc_refactor/` (per run)  
**Testing**: `pytest` suites in `scripts/doc_refactor/tests`, golden snapshot fixtures under `tests/doc_refactor/fixtures`, ad-hoc `scripts/doc_refactor/sample_repo` smoke harness  
**Target Platform**: Linux/macOS developer laptops + CI runners (Ubuntu 22.04)  
**Project Type**: Mono-repo documentation tooling (Python CLI + Markdown tree)  
**Performance Goals**: Link graph validation < 8 minutes on 8-core laptop; end-to-end refactor runtime unbounded per clarification but should stream progress and never exceed 1 GB RAM  
**Constraints**: Must keep repo-compatible relative paths, respect excluded files, use deterministic ordering for inventories, re-run safe on dirty working tree (no destructive ops), produce zero broken links.  
**Scale/Scope**: ~350 Markdown files, 96 orphans, 9 oversized specs, README cap 700 words, multi-phase CLI orchestrating 8 sub-steps (inventory, link collect, validation, creation, version sync, spec compression, dedupe, report)

## Constitution Check
*Gate satisfied before Phase 0; re-evaluated after Phase 1 – PASS.*

1. **Separation of Concerns by Crate/Module** – ✅ The refactor confines automation to `scripts/doc_refactor/` (core logic) and documentation directories (`docs/`, `spec/`). No shared business logic leaks into API crates.  
2. **API- & CLI-First Interfaces** – ✅ Plan formalizes CLI contract (`doc-refactor run …`) and ensures every automated edit is reproducible from the command line (and later CI).  
3. **Observability, Versioning & Simplicity** – ✅ Inventory, validation, and summary reports are persisted under `reports/doc_refactor/` with semantic version tagging `v1.0.0`. We intentionally avoid DB/microservice creep; filesystem + JSON suffices.  
4. **Integration Testing Across Layers** – ✅ Quickstart + research prescribe smoke and regression runs on representative docs prior to merging; `pytest` validators will cover inventory + graph logic.  
5. **Collective Intelligence Principles** – ✅ Separating `/docs` (human narratives) from `/spec` (AI directives) directly supports collective intelligence guidelines by ensuring agents consume canonical context first.  
6. **Governance & Traceability** – ✅ Generated artifacts (research, data-model, contracts, quickstart) trace requirements to implementation playbooks and ultimately to `/tasks`. Complexity tracking not required; no constitutional violations introduced.

**Initial/Post-Design Constitution Check**: ✅ PASS

## Project Structure

### Documentation (feature assets)
```text
/specs/010-documentation-refactoring-0-0/
├── plan.md              # This implementation plan
├── research.md          # Phase 0 findings
├── data-model.md        # Phase 1 data + process schema
├── quickstart.md        # Operator validation scenarios
├── contracts/
│   ├── README.md        # Contract index + responsibilities
│   └── doc_refactor_cli.md  # CLI contract & phases
└── tasks.md             # Generated later by /tasks
```

### Repository Surface Area
```text
scripts/
└── doc_refactor/
    ├── main.py                  # CLI entrypoint (phases orchestrator)
    ├── inventory.py             # Markdown scanner + depth classifier
    ├── link_graph.py            # (new) helper for adjacency graph export
    ├── file_creator.py          # Missing file templating
    ├── duplicate_detector.py    # TF-IDF + cosine similarity engine
    ├── spec_optimizer.py        # (new) spec compression helpers
    ├── templates/               # README/spec/archive templates
    └── tests/                   # pytest suites + fixtures

docs/                            # Human-facing docs (post-refactor target)
├── README.md / index files
├── archive/                     # Historical materials relocated here
└── **/*.md                      # Detailed content referenced from README

spec/                            # AI-oriented compressed specs
├── README.md                    # "Use /spec first" directive
└── **/*.md                      # Per-topic specs (<80-word paragraphs)

reports/
└── doc_refactor/                # Inventory, link graph, dedupe, validator outputs
```

**Structure Decision**: Treat `scripts/doc_refactor` as the automation crate and operate only on documentation trees plus generated reports. No application crates need modification, keeping constitution-compliant separation of concerns.

## Phase 0: Outline & Research
**Research Output**: `/specs/010-documentation-refactoring-0-0/research.md` (2025-11-27)

Key takeaways:
1. **Inventory Strategy** – Use `pathlib.Path.rglob('*.md')` with exclusion filters, compute depth from relative segments, and always coerce README depth=0 to satisfy validator expectations.  
2. **Link Graph Construction** – Parse Markdown with `mistune`, capture `[label](target)` edges, normalize anchors, and emit adjacency + orphan lists for later enforcement. Broken URLs get categorized for repair vs stub vs verification comment.  
3. **Hierarchy Enforcement Rules** – README summary limit 500–700 words, top-level headings only for navigation, deeper detail relocated under `/docs`. Depth correlates to path length (max 3).  
4. **Spec Compression Pattern** – `/spec` files limited to <80-word paragraphs, present Goals → Constraints → Success Criteria, add directive "Use /spec before /docs" plus cross-links back to `/docs`.  
5. **Version Synchronization & Runtime Policy** – All documents must explicitly reference `v1.0.0`; CLI runtime has no hard cap (per clarification) but progress logging + partial commits are mandatory for operator observability.

No open research questions remain.

## Phase 1: Design & Contracts
**Artifacts Generated**:  
- `/specs/010-documentation-refactoring-0-0/data-model.md` – Defines `DocumentationFile`, `LinkEdge`, `LinkGraphReport`, `SpecCompressionProfile`, and validator result payloads.  
- `/specs/010-documentation-refactoring-0-0/contracts/doc_refactor_cli.md` – CLI contract covering phases, required arguments, exit codes, and artifacts.  
- `/specs/010-documentation-refactoring-0-0/contracts/README.md` – Responsibilities, invocation examples, and handoff expectations.  
- `/specs/010-documentation-refactoring-0-0/quickstart.md` – Operator playbook with six validation scenarios (inventory, link fix, README compression, orphan handling, spec compression, final audit).

Design highlights:
1. **Data Model** – Every Markdown file captured as `DocumentationFile` with depth, role (`README`, `INDEX`, `DETAIL`, `SPEC`, `ARCHIVE`), version tags, inbound/outbound edges, and status flags (orphan, broken_links, needs_archive). Reports stored as JSON for incremental runs.  
2. **Processing Pipeline** – CLI phases executed sequentially with resumable checkpoints: `inventory → link_discovery → validation → missing_file_creation → version_sync → restructuring → dedupe → spec_opt`. Each phase outputs a structured artifact under `reports/doc_refactor/PHASE`.  
3. **Link Repair Rules** – Plaintext `.md` tokens replaced with `[path](path)` using repository-relative paths; external URLs validated via `requests.head`, and unreachable URLs converted to citation text unless explicitly verified.  
4. **README & Depth Enforcement** – Word-count gate enforces 500–700 words; table-of-contents regenerated from inventory depth metadata; detailed material automatically extracted into `/docs/...` with summary stubs inserted.  
5. **Spec Optimization** – `spec_optimizer.py` enforces <80-word paragraphs, injects "Use /spec before /docs" admonition, and ensures each spec cross-links back to supporting docs plus success criteria.  
6. **Runtime Policy** – Clarification FR-010 recorded: CLI has no strict runtime limit, but instrumentation logs elapsed times per phase to aid future tuning.

## Phase 2: Task Planning Approach
*Executed later by `/tasks`; documented here for visibility.*

**Task Sources**: spec, plan, research findings, data-model definitions, CLI contracts, quickstart scenarios.  
**Themes & Order**:
1. **Tooling Hardening** – Update `scripts/doc_refactor` modules per data-model (inventory depth fixes, link graph builder, spec optimizer).  
2. **Documentation Movers** – Generate /docs indexes, archive flows, README compression, spec rewrite tasks.  
3. **Validator + Reporting** – Integrate adjacency/orphan checks, HTML/plaintext link validator, version sync script.  
4. **Testing & Observability** – Add pytest coverage for new modules, snapshot outputs, CLI exit-code contract tests.  
5. **Operational Playbooks** – Document runbooks, include quickstart scenarios, update contributor docs.

`/tasks` will emit ~30 ordered tasks grouped by these themes with explicit dependencies (inventory before link repair, link repair before restructure, etc.). Parallelizable subtasks (e.g., spec compression vs README rewrite) will be tagged accordingly.

## Progress Tracking
| Phase | Status | Notes |
|-------|--------|-------|
| Constitution Gate | ✅ Complete | No violations detected; FR-010 captured | 
| Phase 0 – Research | ✅ Complete | research.md committed with five key decisions |
| Phase 1 – Design & Contracts | ✅ Complete | data-model, contracts, quickstart in place |
| Phase 2 – Tasks Prep | 🔜 Pending | Will be executed via `/tasks` |

No Complexity Tracking entries are required; the solution stays within constitutional bounds.

